package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.PacientePermissoes;
import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.PacienteRetratoDto;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.ResponsavelLegalDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.PacienteVersao;
import br.com.clinicaviene.entity.ResponsavelLegal;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.exceptions.PossivelDuplicidadeException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.repository.PacienteVersaoRepository;
import br.com.clinicaviene.repository.ResponsavelLegalRepository;
import br.com.clinicaviene.util.CpfHasher;
import br.com.clinicaviene.util.CpfUtils;
import br.com.clinicaviene.validation.UfValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cadastro de pacientes (CONTEXTO §4, D-053, D-091, D-092). Acesso: recepção (ADMIN) e MASTER. Toda leitura
 * e alteração vai para o {@code audit_log}; cada alteração gera uma versão cifrada do cadastro.
 */
@Service
@RequiredArgsConstructor
public class PacienteService {

    private static final String ENTIDADE = "Paciente";
    private static final int LIMITE_BUSCA = 50;
    private static final int IDADE_MAXIMA = 130;
    private static final ZoneId FUSO_CLINICA = ZoneId.of("America/Bahia");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PacienteRepository pacienteRepository;
    private final ResponsavelLegalRepository responsavelLegalRepository;
    private final PacienteVersaoRepository pacienteVersaoRepository;
    private final CpfHasher cpfHasher;
    private final AuditoriaService auditoriaService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /** Pré-cadastro pela recepção (D-091). */
    @Transactional
    public Paciente preCadastrar(NovoPacienteRequest dados, UsuarioAutenticado ator) {
        exigirAcessoAoCadastro(ator);
        Instant agora = agora();
        LocalDate hoje = hoje();

        String nome = semEspacosExtras(dados.nomeCompleto());
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Informe o nome completo.");
        }
        validarNascimento(dados.dataNascimento(), hoje);
        String email = normalizarEmail(dados.email());
        String telefone = vazioComoNulo(dados.telefone());
        if (email == null && telefone == null) {
            throw new RegraNegocioException("Informe ao menos um contato: e-mail ou telefone.");
        }
        EnderecoDto endereco = normalizarEndereco(dados.endereco());
        boolean menor = dados.dataNascimento().plusYears(Paciente.MAIORIDADE).isAfter(hoje);

        String cpf = vazioComoNulo(CpfUtils.normalizar(dados.cpf()));
        String cpfHash = null;
        if (cpf != null) {
            if (!CpfUtils.valido(cpf)) {
                throw new RegraNegocioException("CPF inválido.");
            }
            cpfHash = cpfHasher.calcular(cpf);
            if (pacienteRepository.existsByCpfHash(cpfHash)) {
                throw new RegraNegocioException("Já existe um paciente com este CPF.");
            }
        }

        ResponsavelLegalDto responsavel = dados.responsavel();
        String responsavelCpf = null;
        if (menor && responsavel == null) {
            throw new RegraNegocioException("Paciente menor de idade precisa de responsável legal (LGPD art. 14).");
        }
        if (responsavel != null) {
            responsavelCpf = CpfUtils.normalizar(responsavel.cpf());
            if (!CpfUtils.valido(responsavelCpf)) {
                throw new RegraNegocioException("CPF do responsável inválido.");
            }
            if (responsavelCpf.equals(cpf)) {
                throw new RegraNegocioException("O CPF do responsável não pode ser o mesmo do paciente.");
            }
        }

        boolean adultoSemCpf = cpf == null && !menor;
        if (adultoSemCpf) {
            if (ator.perfil() != Perfil.MASTER) {
                throw new AccessDeniedException("Paciente adulto sem CPF só com aprovação do MASTER");
            }
            if (dados.semCpfJustificativa() == null || dados.semCpfJustificativa().isBlank()) {
                throw new RegraNegocioException("Informe a justificativa para cadastrar adulto sem CPF.");
            }
        }

        String responsavelCpfHash = responsavelCpf == null ? null : cpfHasher.calcular(responsavelCpf);
        if (cpf == null && !dados.confirmarDuplicidade()) {
            List<UUID> semelhantes = semelhantes(nome, dados.dataNascimento(), responsavelCpfHash);
            if (!semelhantes.isEmpty()) {
                throw new PossivelDuplicidadeException(semelhantes);
            }
        }

        Paciente paciente = new Paciente(
                nome, cpf, cpfHash, dados.dataNascimento(), email, telefone, endereco, ator.id(), agora);
        if (adultoSemCpf) {
            paciente.registrarSemCpf(dados.semCpfJustificativa().trim(), ator.id(), agora);
        }
        pacienteRepository.save(paciente);

        ResponsavelLegal responsavelLegal = null;
        if (responsavel != null) {
            responsavelLegal = responsavelLegalRepository.save(new ResponsavelLegal(
                    paciente.getId(),
                    semEspacosExtras(responsavel.nomeCompleto()),
                    responsavelCpf,
                    responsavelCpfHash,
                    responsavel.parentesco(),
                    vazioComoNulo(responsavel.telefone()),
                    normalizarEmail(responsavel.email()),
                    agora));
        }

        registrarVersao(paciente, responsavelLegal, "pré-cadastro", OrigemAlteracao.RECEPCAO, ator.id(), agora);
        auditoriaService.registrar(
                RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.PACIENTE_CRIADO, ENTIDADE, paciente.getId()));
        if (adultoSemCpf) {
            auditoriaService.registrar(new RegistroAuditoria(
                    ator.id(),
                    AcaoAuditoria.PACIENTE_SEM_CPF_APROVADO,
                    ENTIDADE,
                    paciente.getId().toString(),
                    dados.semCpfJustificativa().trim()));
        }
        return paciente;
    }

    /** Ficha cadastral (sem dado clínico). A leitura é auditada. */
    @Transactional
    public Paciente buscar(UUID id, UsuarioAutenticado ator) {
        exigirAcessoAoCadastro(ator);
        Paciente paciente = pacienteRepository.findById(id).orElseThrow(PacienteNaoEncontradoException::new);
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.PACIENTE_LIDO, ENTIDADE, id));
        return paciente;
    }

    @Transactional(readOnly = true)
    public Optional<ResponsavelLegal> responsavelDe(UUID pacienteId) {
        return responsavelLegalRepository.findByPacienteId(pacienteId);
    }

    /**
     * Busca por CPF (pelo hash — com ou sem pontuação), por data de nascimento ({@code dd/MM/aaaa} ou
     * {@code aaaa-MM-dd}) ou por parte do nome. O termo buscado não vai para o log (pode ser um CPF).
     */
    @Transactional
    public List<Paciente> pesquisar(String termo, UsuarioAutenticado ator) {
        exigirAcessoAoCadastro(ator);
        String texto = termo == null ? "" : termo.trim();
        List<Paciente> resultado;
        String digitos = CpfUtils.normalizar(texto);
        Optional<LocalDate> data = comoData(texto);
        if (digitos.length() == 11 && texto.matches("[\\d.\\-\\s]+") && CpfUtils.valido(digitos)) {
            resultado = pacienteRepository.findByCpfHash(cpfHasher.calcular(digitos)).stream().toList();
        } else if (data.isPresent()) {
            resultado = pacienteRepository.findByDataNascimentoOrderByNomeCompleto(data.get(), PageRequest.of(0, LIMITE_BUSCA));
        } else if (texto.length() >= 2) {
            resultado = pacienteRepository.buscarPorNome(texto, PageRequest.of(0, LIMITE_BUSCA));
        } else {
            resultado = List.of();
        }
        auditoriaService.registrar(RegistroAuditoria.de(ator.id(), AcaoAuditoria.PACIENTE_PESQUISADO));
        return resultado;
    }

    /** Contato e endereço (D-092): sem nova assinatura, com versão auditada. Nome/CPF/nascimento não mudam aqui. */
    @Transactional
    public Paciente atualizarContato(
            UUID id, String email, String telefone, EnderecoDto endereco, UsuarioAutenticado ator) {
        exigirAcessoAoCadastro(ator);
        Paciente paciente = pacienteRepository.findById(id).orElseThrow(PacienteNaoEncontradoException::new);
        String novoEmail = normalizarEmail(email);
        String novoTelefone = vazioComoNulo(telefone);
        EnderecoDto novoEndereco = normalizarEndereco(endereco);
        if (novoEmail == null && novoTelefone == null) {
            throw new RegraNegocioException("Informe ao menos um contato: e-mail ou telefone.");
        }
        List<String> alterados = new ArrayList<>();
        if (!Objects.equals(paciente.getEmail(), novoEmail)) {
            alterados.add("e-mail");
        }
        if (!Objects.equals(paciente.getTelefone(), novoTelefone)) {
            alterados.add("telefone");
        }
        if (!paciente.endereco().equals(novoEndereco)) {
            alterados.add("endereço");
        }
        if (alterados.isEmpty()) {
            return paciente;
        }
        Instant agora = agora();
        paciente.atualizarContato(novoEmail, novoTelefone, novoEndereco, agora);
        registrarVersao(
                paciente,
                responsavelLegalRepository.findByPacienteId(id).orElse(null),
                String.join(", ", alterados),
                OrigemAlteracao.RECEPCAO,
                ator.id(),
                agora);
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.PACIENTE_ALTERADO, ENTIDADE, id));
        return paciente;
    }

    /** Histórico de alterações (o que mudou, por onde, quem, quando — sem os valores). */
    @Transactional
    public List<PacienteVersao> historico(UUID id, UsuarioAutenticado ator) {
        exigirAcessoAoCadastro(ator);
        if (!pacienteRepository.existsById(id)) {
            throw new PacienteNaoEncontradoException();
        }
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.PACIENTE_LIDO, ENTIDADE, id));
        return pacienteVersaoRepository.findByPacienteIdOrderByCriadoEmDesc(id);
    }

    public LocalDate hoje() {
        return LocalDate.now(clock.withZone(FUSO_CLINICA));
    }

    private void registrarVersao(
            Paciente p, ResponsavelLegal r, String campos, OrigemAlteracao origem, UUID ator, Instant agora) {
        PacienteRetratoDto retrato = new PacienteRetratoDto(
                p.getNomeCompleto(),
                p.getCpf(),
                p.getDataNascimento(),
                p.getEmail(),
                p.getTelefone(),
                p.endereco(),
                r == null
                        ? null
                        : new PacienteRetratoDto.Responsavel(
                                r.getNomeCompleto(), r.getCpf(), r.getParentesco(), r.getTelefone(), r.getEmail()));
        pacienteVersaoRepository.save(
                new PacienteVersao(p.getId(), jsonMapper.writeValueAsString(retrato), campos, origem, ator, agora));
    }

    /** Mesmo nome e nascimento, e mesmo responsável quando houver (D-053). */
    private List<UUID> semelhantes(String nome, LocalDate nascimento, String responsavelCpfHash) {
        return pacienteRepository.mesmoNomeENascimento(nome, nascimento).stream()
                .filter(p -> responsavelCpfHash == null
                        || responsavelLegalRepository
                                .findByPacienteId(p.getId())
                                .map(r -> r.getCpfHash().equals(responsavelCpfHash))
                                .orElse(false))
                .map(Paciente::getId)
                .toList();
    }

    private static void exigirAcessoAoCadastro(UsuarioAutenticado ator) {
        if (!PacientePermissoes.gerenciaCadastro(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para acessar o cadastro de pacientes");
        }
    }

    private static void validarNascimento(LocalDate nascimento, LocalDate hoje) {
        if (nascimento == null) {
            throw new RegraNegocioException("Informe a data de nascimento.");
        }
        if (nascimento.isAfter(hoje)) {
            throw new RegraNegocioException("A data de nascimento não pode ser no futuro.");
        }
        if (nascimento.isBefore(hoje.minusYears(IDADE_MAXIMA))) {
            throw new RegraNegocioException("Data de nascimento inválida.");
        }
    }

    private static EnderecoDto normalizarEndereco(EnderecoDto e) {
        if (e == null) {
            return EnderecoDto.vazio();
        }
        String uf = e.uf() == null ? null : vazioComoNulo(e.uf().toUpperCase(Locale.ROOT));
        if (uf != null && !UfValidator.valida(uf)) {
            throw new RegraNegocioException("UF do endereço inválida.");
        }
        String cep = vazioComoNulo(e.cep() == null ? null : e.cep().replaceAll("\\D", ""));
        if (cep != null && cep.length() != 8) {
            throw new RegraNegocioException("CEP inválido.");
        }
        return new EnderecoDto(
                cep,
                semEspacosExtras(vazioComoNulo(e.logradouro())),
                vazioComoNulo(e.numero()),
                vazioComoNulo(e.complemento()),
                semEspacosExtras(vazioComoNulo(e.bairro())),
                semEspacosExtras(vazioComoNulo(e.cidade())),
                uf);
    }

    private static Optional<LocalDate> comoData(String texto) {
        for (DateTimeFormatter formato : List.of(DATA_BR, DateTimeFormatter.ISO_LOCAL_DATE)) {
            try {
                return Optional.of(LocalDate.parse(texto, formato));
            } catch (DateTimeParseException e) {
                // tenta o próximo formato
            }
        }
        return Optional.empty();
    }

    private static String normalizarEmail(String email) {
        String e = vazioComoNulo(email);
        return e == null ? null : e.toLowerCase(Locale.ROOT);
    }

    private static String semEspacosExtras(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
