package br.com.clinicaviene.service;

import br.com.clinicaviene.dto.ModeloAnamneseResponse;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.ModeloAnamneseRepository;
import br.com.clinicaviene.validation.ModeloAnamneseValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Modelos de anamnese por área de atuação, versionados (D-089). Só o MASTER configura. O MASTER cuida das
 * perguntas, nunca vê respostas de pacientes (D-088).
 */
@Service
@RequiredArgsConstructor
public class ModeloAnamneseService {

    private static final String ENTIDADE = "ModeloAnamnese";
    private static final TypeReference<List<PerguntaAnamneseDto>> LISTA_PERGUNTAS = new TypeReference<>() {};

    private final ModeloAnamneseRepository modeloAnamneseRepository;
    private final AuditoriaService auditoriaService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ModeloAnamnese> listar(UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        return modeloAnamneseRepository.findAllByOrderByAreaAtuacaoAscVersaoDesc();
    }

    @Transactional(readOnly = true)
    public ModeloAnamnese buscar(UUID id, UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        return buscar(id);
    }

    @Transactional
    public ModeloAnamnese criarRascunho(
            AreaAtuacao area, String titulo, List<PerguntaAnamneseDto> perguntas, UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        ModeloAnamneseValidator.validar(perguntas);
        ModeloAnamnese modelo = modeloAnamneseRepository.save(new ModeloAnamnese(
                area, modeloAnamneseRepository.ultimaVersao(area) + 1, titulo.trim(), json(perguntas), ator.id(), agora()));
        auditoriaService.registrar(
                RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.MODELO_ANAMNESE_CRIADO, ENTIDADE, modelo.getId()));
        return modelo;
    }

    @Transactional
    public ModeloAnamnese editar(UUID id, String titulo, List<PerguntaAnamneseDto> perguntas, UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        ModeloAnamneseValidator.validar(perguntas);
        ModeloAnamnese modelo = buscar(id);
        modelo.editar(titulo.trim(), json(perguntas));
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.MODELO_ANAMNESE_ALTERADO, ENTIDADE, id));
        return modelo;
    }

    @Transactional
    public ModeloAnamnese publicar(UUID id, UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        ModeloAnamnese modelo = buscar(id);
        ModeloAnamneseValidator.validar(perguntas(modelo));
        modelo.publicar(ator.id(), agora());
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.MODELO_ANAMNESE_PUBLICADO, ENTIDADE, id));
        return modelo;
    }

    /** Nova versão em rascunho a partir de uma existente (o caminho para "alterar" um modelo publicado). */
    @Transactional
    public ModeloAnamnese duplicar(UUID id, UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        ModeloAnamnese origem = buscar(id);
        return criarRascunho(origem.getAreaAtuacao(), origem.getTitulo(), perguntas(origem), ator);
    }

    /** Versão vigente (publicada mais recente) da área — a que o paciente responde. */
    @Transactional(readOnly = true)
    public Optional<ModeloAnamnese> vigente(AreaAtuacao area) {
        return modeloAnamneseRepository.findFirstByAreaAtuacaoAndStatusOrderByVersaoDesc(area, StatusVersao.PUBLICADO);
    }

    /** Cria um rascunho com o exemplo completo de ficha odontológica (D-089), se a área ainda não tiver modelo. */
    @Transactional
    public ModeloAnamnese carregarExemploOdontologia(UsuarioAutenticado ator) {
        TermoService.exigirMaster(ator);
        if (modeloAnamneseRepository.ultimaVersao(AreaAtuacao.ODONTOLOGIA) > 0) {
            throw new RegraNegocioException("A odontologia já tem modelo. Use \"duplicar\" para criar uma nova versão.");
        }
        JsonNode exemplo = jsonMapper.readTree(TermoService.lerExemplo("exemplos/anamnese/odontologia.json"));
        List<PerguntaAnamneseDto> perguntas = jsonMapper.convertValue(exemplo.get("perguntas"), LISTA_PERGUNTAS);
        return criarRascunho(AreaAtuacao.ODONTOLOGIA, exemplo.get("titulo").asString(), perguntas, ator);
    }

    public List<PerguntaAnamneseDto> perguntas(ModeloAnamnese modelo) {
        return jsonMapper.readValue(modelo.perguntasJson(), LISTA_PERGUNTAS);
    }

    public ModeloAnamneseResponse resposta(ModeloAnamnese m) {
        return new ModeloAnamneseResponse(
                m.getId(), m.getAreaAtuacao(), m.getVersao(), m.getTitulo(), m.getStatus(), perguntas(m), m.getCriadoEm(),
                m.getPublicadoEm());
    }

    private ModeloAnamnese buscar(UUID id) {
        return modeloAnamneseRepository
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo de anamnese não encontrado."));
    }

    private String json(List<PerguntaAnamneseDto> perguntas) {
        return jsonMapper.writeValueAsString(perguntas);
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
