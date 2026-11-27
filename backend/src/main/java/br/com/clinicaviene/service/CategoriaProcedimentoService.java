package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AgendaPermissoes;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.CategoriaProcedimento;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.CategoriaProcedimentoRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Categorias de procedimento por área (D-106): o MASTER configura; agendamentos (e depois prontuário e financeiro)
 * usam. Exemplo de odontologia carregado por ação do MASTER, sem migration de INSERT (D-047).
 */
@Service
@RequiredArgsConstructor
public class CategoriaProcedimentoService {

    private static final String ENTIDADE = "CategoriaProcedimento";
    private static final String PADRAO_CODIGO = "[A-Z][A-Z0-9_]{1,39}";

    private final CategoriaProcedimentoRepository categoriaRepository;
    private final AuditoriaService auditoriaService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CategoriaProcedimento> listar(AreaAtuacao area, UsuarioAutenticado ator) {
        if (!AgendaPermissoes.usaAgenda(ator.perfil())) {
            throw new AccessDeniedException("Sem acesso à agenda");
        }
        return area == null
                ? categoriaRepository.findAllByOrderByAreaAtuacaoAscNomeAsc()
                : categoriaRepository.findAllByAreaAtuacaoOrderByNomeAsc(area);
    }

    @Transactional
    public CategoriaProcedimento criar(AreaAtuacao area, String codigo, String nome, UsuarioAutenticado ator) {
        exigirConfiguracao(ator);
        String nomeLimpo = nome.trim();
        String codigoFinal = codigo == null || codigo.isBlank() ? codigoDoNome(nomeLimpo) : codigo.trim().toUpperCase(Locale.ROOT);
        if (!codigoFinal.matches(PADRAO_CODIGO)) {
            throw new RegraNegocioException("Código inválido: use letras maiúsculas, números e _ (ex.: ORTODONTIA).");
        }
        if (categoriaRepository.existsByAreaAtuacaoAndCodigo(area, codigoFinal)) {
            throw new RegraNegocioException("Já existe uma categoria com este código nesta área.");
        }
        if (categoriaRepository.existsByAreaAtuacaoAndNomeIgnoreCase(area, nomeLimpo)) {
            throw new RegraNegocioException("Já existe uma categoria com este nome nesta área.");
        }
        CategoriaProcedimento c = categoriaRepository.save(new CategoriaProcedimento(area, codigoFinal, nomeLimpo, agora()));
        auditoriaService.registrar(
                RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.CATEGORIA_PROCEDIMENTO_CRIADA, ENTIDADE, c.getId()));
        return c;
    }

    @Transactional
    public CategoriaProcedimento atualizar(UUID id, String nome, boolean ativa, UsuarioAutenticado ator) {
        exigirConfiguracao(ator);
        CategoriaProcedimento c = buscar(id);
        String nomeLimpo = nome.trim();
        if (categoriaRepository.existsByAreaAtuacaoAndNomeIgnoreCaseAndIdNot(c.getAreaAtuacao(), nomeLimpo, id)) {
            throw new RegraNegocioException("Já existe uma categoria com este nome nesta área.");
        }
        c.atualizar(nomeLimpo, ativa, agora());
        auditoriaService.registrar(
                RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.CATEGORIA_PROCEDIMENTO_ALTERADA, ENTIDADE, id));
        return c;
    }

    /** Cria as categorias de exemplo da odontologia que ainda não existem (pelo código). */
    @Transactional
    public List<CategoriaProcedimento> carregarExemploOdontologia(UsuarioAutenticado ator) {
        exigirConfiguracao(ator);
        JsonNode exemplo = jsonMapper.readTree(TermoService.lerExemplo("exemplos/categorias/odontologia.json"));
        List<CategoriaProcedimento> criadas = new ArrayList<>();
        for (JsonNode item : exemplo) {
            String codigo = item.get("codigo").asString();
            if (!categoriaRepository.existsByAreaAtuacaoAndCodigo(AreaAtuacao.ODONTOLOGIA, codigo)) {
                criadas.add(criar(AreaAtuacao.ODONTOLOGIA, codigo, item.get("nome").asString(), ator));
            }
        }
        return criadas;
    }

    /** Categoria ativa (para um novo agendamento). */
    @Transactional(readOnly = true)
    public CategoriaProcedimento ativa(UUID id) {
        return categoriaRepository.findById(id)
                .filter(CategoriaProcedimento::isAtiva)
                .orElseThrow(() -> new RegraNegocioException("Categoria de procedimento não encontrada ou desativada."));
    }

    /** "Clínica geral" → CLINICA_GERAL. */
    static String codigoDoNome(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String codigo = semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (codigo.length() > 40) {
            codigo = codigo.substring(0, 40).replaceAll("_+$", "");
        }
        return codigo.isEmpty() || !Character.isLetter(codigo.charAt(0)) ? "C_" + codigo : codigo;
    }

    private CategoriaProcedimento buscar(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    private static void exigirConfiguracao(UsuarioAutenticado ator) {
        if (!AgendaPermissoes.configuraAgenda(ator.perfil())) {
            throw new AccessDeniedException("Só o MASTER configura categorias de procedimento");
        }
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
