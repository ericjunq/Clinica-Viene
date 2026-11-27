package br.com.clinicaviene.service;

import br.com.clinicaviene.dto.AssinaturaColetada;
import br.com.clinicaviene.dto.DadosCadastroDto;
import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.dto.FichaAssinadaResponse;
import br.com.clinicaviene.dto.NovaFichaDto;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.RespostaAnamneseDto;
import br.com.clinicaviene.entity.Assinatura;
import br.com.clinicaviene.entity.Documento;
import br.com.clinicaviene.entity.FichaAnamnese;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.Parentesco;
import br.com.clinicaviene.enums.TipoDocumento;
import br.com.clinicaviene.enums.TipoSignatario;
import br.com.clinicaviene.repository.AssinaturaRepository;
import br.com.clinicaviene.repository.DocumentoRepository;
import br.com.clinicaviene.repository.FichaAnamneseRepository;
import br.com.clinicaviene.validation.RespostasAnamneseValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ficha de cadastro e saúde assinada (CONTEXTO §5, §9; D-088, D-090, D-102). Único ponto que grava fichas: gera o
 * PDF com a assinatura e a trilha de evidências, guarda-o cifrado no storage, registra o hash SHA-256 e grava a
 * ficha imutável com as respostas cifradas.
 */
@Service
@RequiredArgsConstructor
public class FichaAnamneseService {

    static final String TEMPLATE = "documentos/ficha-cadastro";
    private static final ZoneId FUSO_CLINICA = ZoneId.of("America/Bahia");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss");
    private static final String DECLARACAO =
            "Declaro que as informações desta ficha são verdadeiras, que li a política de privacidade e que as "
                    + "autorizações acima refletem a minha escolha.";

    private final FichaAnamneseRepository fichaAnamneseRepository;
    private final DocumentoRepository documentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final DocumentoPdfService documentoPdfService;
    private final ArmazenamentoService armazenamentoService;
    private final AuditoriaService auditoriaService;
    private final JsonMapper jsonMapper;

    @Transactional
    public FichaAssinadaResponse registrar(NovaFichaDto ficha) {
        String hashConteudo = hashConteudo(ficha);
        UUID documentoId = Documento.novoId();
        byte[] pdf = documentoPdfService.gerar(TEMPLATE, modeloPdf(ficha, documentoId, hashConteudo));
        String sha256 = sha256(pdf);
        String chave = "pacientes/" + ficha.pacienteId() + "/fichas/" + documentoId + ".pdf";
        armazenamentoService.guardar(chave, pdf);

        documentoRepository.save(new Documento(
                documentoId, TipoDocumento.FICHA_CADASTRO, ficha.pacienteId(), chave, sha256, pdf.length,
                ficha.assinadoEm()));
        Map<String, Object> evidencias = new LinkedHashMap<>(ficha.evidencias());
        evidencias.put("identidade", ficha.identidade());
        evidencias.put("hashConteudo", hashConteudo);
        evidencias.put("sha256Pdf", sha256);
        assinaturaRepository.save(new Assinatura(
                documentoId,
                ficha.provedor(),
                ficha.assinatura().identificadorProvedor(),
                ficha.peloResponsavel() ? TipoSignatario.RESPONSAVEL_LEGAL : TipoSignatario.PACIENTE,
                nomeSignatario(ficha),
                ficha.assinatura().metodo(),
                jsonMapper.writeValueAsString(evidencias),
                ficha.assinadoEm()));
        FichaAnamnese registrada = fichaAnamneseRepository.save(new FichaAnamnese(
                ficha.pacienteId(),
                ficha.modelo().getId(),
                ficha.conviteId(),
                documentoId,
                ficha.origem(),
                ficha.peloResponsavel(),
                jsonMapper.writeValueAsString(new TreeMap<>(ficha.respostas())),
                jsonMapper.writeValueAsString(ficha.alertas()),
                ficha.acompanhante() == null ? null : jsonMapper.writeValueAsString(ficha.acompanhante()),
                ficha.assinadoEm()));
        auditoriaService.registrar(new RegistroAuditoria(
                null, AcaoAuditoria.FICHA_ASSINADA, "Paciente", ficha.pacienteId().toString(),
                "ficha " + registrada.getId() + " assinada pelo " + (ficha.peloResponsavel() ? "responsável" : "paciente")));
        return new FichaAssinadaResponse(registrada.getId(), documentoId, sha256, ficha.assinadoEm(), null);
    }

    static String nomeSignatario(NovaFichaDto ficha) {
        return ficha.peloResponsavel() ? ficha.dados().responsavel().nomeCompleto() : ficha.dados().nomeCompleto();
    }

    /** Hash do conteúdo assinado (dados, respostas, decisões e assinatura), impresso na trilha de evidências. */
    private String hashConteudo(NovaFichaDto f) {
        Map<String, Object> conteudo = new LinkedHashMap<>();
        conteudo.put("paciente", f.pacienteId());
        conteudo.put("dados", f.dados());
        conteudo.put("modelo", f.modelo().getId());
        conteudo.put("respostas", new TreeMap<>(f.respostas()));
        conteudo.put("termos", f.termos().stream()
                .map(t -> t.getId() + "=" + f.aceitos().contains(t.getId()))
                .sorted()
                .toList());
        conteudo.put("acompanhante", f.acompanhante());
        conteudo.put("metodo", f.assinatura().metodo());
        conteudo.put("assinatura", f.assinatura().metodo() == MetodoAssinatura.DESENHO
                ? sha256(f.assinatura().imagemDataUri().getBytes(StandardCharsets.UTF_8))
                : f.assinatura().nomeDigitado());
        conteudo.put("assinadoEm", f.assinadoEm().toString());
        return sha256(jsonMapper.writeValueAsString(conteudo).getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> modeloPdf(NovaFichaDto f, UUID documentoId, String hashConteudo) {
        DadosCadastroDto d = f.dados();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assinadoEm", DATA_HORA.format(f.assinadoEm().atZone(FUSO_CLINICA)) + " (UTC-3)");
        m.put("origem", f.origem() == OrigemAlteracao.PACIENTE_QUIOSQUE
                ? "em aparelho da clínica (quiosque)"
                : "pelo link enviado ao paciente");
        m.put("paciente", Map.of(
                "nome", d.nomeCompleto(),
                "cpf", d.cpf() == null ? "não informado" : cpfFormatado(d.cpf()),
                "nascimento", DATA.format(d.dataNascimento()),
                "email", texto(d.email()),
                "telefone", texto(d.telefone()),
                "endereco", endereco(d.endereco())));
        m.put("responsavel", d.responsavel() == null ? null : Map.of(
                "nome", d.responsavel().nomeCompleto(),
                "cpf", cpfFormatado(d.responsavel().cpf()),
                "parentesco", parentesco(d.responsavel().parentesco()),
                "telefone", texto(d.responsavel().telefone()),
                "email", texto(d.responsavel().email())));
        m.put("modelo", f.modelo().getTitulo() + " (versão " + f.modelo().getVersao() + ")");
        m.put("secoes", secoes(f.perguntas(), f.respostas()));
        m.put("acompanhante", f.acompanhante() == null ? null
                : "Preenchida com a ajuda de " + f.acompanhante().nome()
                        + (f.acompanhante().motivo() == null ? "." : " (" + f.acompanhante().motivo() + ")."));
        m.put("termos", f.termos().stream()
                .map(t -> Map.of(
                        "titulo", t.getTitulo(),
                        "versao", t.getVersao(),
                        "decisao", decisao(t, f.aceitos().contains(t.getId())),
                        "texto", t.getTexto()))
                .toList());
        m.put("declaracao", DECLARACAO);
        AssinaturaColetada a = f.assinatura();
        Map<String, Object> assinatura = new LinkedHashMap<>();
        assinatura.put("imagem", a.imagemDataUri());
        assinatura.put("nomeDigitado", a.nomeDigitado());
        assinatura.put("signatario", nomeSignatario(f));
        assinatura.put("papel", f.peloResponsavel()
                ? "Responsável legal (" + parentesco(d.responsavel().parentesco()) + ")"
                : "Paciente");
        assinatura.put("metodo", a.metodo() == MetodoAssinatura.DESENHO ? "assinatura desenhada na tela" : "nome digitado");
        m.put("assinatura", assinatura);
        List<Map<String, Object>> evidencias = new ArrayList<>();
        evidencias.add(evidencia("Confirmação de identidade", f.identidade(), false));
        evidencias.add(evidencia("Data e hora", DATA_HORA.format(f.assinadoEm().atZone(FUSO_CLINICA)) + " (UTC-3) · "
                + f.assinadoEm(), false));
        evidencias.add(evidencia("Endereço IP", texto((String) f.evidencias().get("ip")), false));
        evidencias.add(evidencia("Aparelho / navegador", texto((String) f.evidencias().get("dispositivo")), false));
        evidencias.add(evidencia("Plataforma de assinatura", "Sistema ClinicaViene (provedor " + f.provedor() + ")", false));
        evidencias.add(evidencia("Documento", documentoId.toString(), true));
        evidencias.add(evidencia("Hash SHA-256 do conteúdo", hashConteudo, true));
        m.put("evidencias", evidencias);
        return m;
    }

    private static List<Map<String, Object>> secoes(
            List<PerguntaAnamneseDto> perguntas, Map<String, RespostaAnamneseDto> respostas) {
        Map<String, List<Map<String, Object>>> porSecao = new LinkedHashMap<>();
        for (PerguntaAnamneseDto p : perguntas) {
            porSecao.computeIfAbsent(Objects.requireNonNullElse(p.secao(), ""), s -> new ArrayList<>())
                    .add(Map.of("pergunta", p.texto(), "resposta", resposta(p, respostas.get(p.codigo()))));
        }
        return porSecao.entrySet().stream()
                .map(e -> Map.<String, Object>of("nome", e.getKey(), "itens", e.getValue()))
                .toList();
    }

    static String resposta(PerguntaAnamneseDto p, RespostaAnamneseDto r) {
        if (r == null || ((r.valor() == null || r.valor().isBlank()) && (r.valores() == null || r.valores().isEmpty()))) {
            return "não respondida";
        }
        String texto = switch (p.tipo()) {
            case SIM_NAO, SIM_NAO_DETALHE -> RespostasAnamneseValidator.SIM.equals(r.valor().trim()) ? "Sim" : "Não";
            case MULTIPLA_ESCOLHA -> String.join(", ", r.valores());
            case DATA -> data(r.valor().trim());
            default -> r.valor().trim();
        };
        if (r.detalhe() != null && !r.detalhe().isBlank()) {
            texto += " — " + r.detalhe().trim();
        }
        return texto;
    }

    private static String data(String iso) {
        try {
            return DATA.format(LocalDate.parse(iso));
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    private static String decisao(TermoVersao termo, boolean aceito) {
        if (termo.getTipo().obrigatorio()) {
            return "ciente e de acordo";
        }
        return aceito ? "AUTORIZO" : "NÃO autorizo";
    }

    private static Map<String, Object> evidencia(String rotulo, String valor, boolean codigo) {
        return Map.of("rotulo", rotulo, "valor", valor, "codigo", codigo);
    }

    private static String endereco(EnderecoDto e) {
        if (e == null) {
            return "não informado";
        }
        String linha = Stream.of(
                        e.logradouro(),
                        e.numero() == null ? null : "nº " + e.numero(),
                        e.complemento(),
                        e.bairro(),
                        e.cidade() == null ? null : e.cidade() + (e.uf() == null ? "" : "/" + e.uf()),
                        e.cep() == null ? null : "CEP " + e.cep().replaceFirst("(\\d{5})(\\d{3})", "$1-$2"))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return linha.isEmpty() ? "não informado" : linha;
    }

    private static String parentesco(Parentesco p) {
        return switch (p) {
            case MAE -> "mãe";
            case PAI -> "pai";
            case AVO -> "avô/avó";
            case TIO -> "tio(a)";
            case IRMAO -> "irmão(ã)";
            case TUTOR -> "tutor(a)";
            case OUTRO -> "outro";
        };
    }

    static String cpfFormatado(String cpf) {
        return cpf.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "não informado" : valor;
    }

    static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
