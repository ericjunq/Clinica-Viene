package br.com.clinicaviene.service;

import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.TermoVersaoRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Termos LGPD versionados (D-089, D-093). O MASTER cria rascunhos, edita e publica; versão publicada é
 * imutável. A vigente de cada tipo é a publicada mais recente.
 */
@Service
@RequiredArgsConstructor
public class TermoService {

    private static final String ENTIDADE = "TermoVersao";

    private final TermoVersaoRepository termoVersaoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TermoVersao> listar(UsuarioAutenticado ator) {
        exigirMaster(ator);
        return termoVersaoRepository.findAllByOrderByTipoAscVersaoDesc();
    }

    @Transactional
    public TermoVersao criarRascunho(TipoTermo tipo, String titulo, String texto, UsuarioAutenticado ator) {
        exigirMaster(ator);
        TermoVersao termo = termoVersaoRepository.save(new TermoVersao(
                tipo, termoVersaoRepository.ultimaVersao(tipo) + 1, titulo.trim(), texto.trim(), ator.id(), agora()));
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.TERMO_CRIADO, ENTIDADE, termo.getId()));
        return termo;
    }

    @Transactional
    public TermoVersao editar(UUID id, String titulo, String texto, UsuarioAutenticado ator) {
        exigirMaster(ator);
        TermoVersao termo = buscar(id);
        termo.editar(titulo.trim(), texto.trim());
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.TERMO_ALTERADO, ENTIDADE, id));
        return termo;
    }

    @Transactional
    public TermoVersao publicar(UUID id, UsuarioAutenticado ator) {
        exigirMaster(ator);
        TermoVersao termo = buscar(id);
        termo.publicar(ator.id(), agora());
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.TERMO_PUBLICADO, ENTIDADE, id));
        return termo;
    }

    /** Versões vigentes (publicadas mais recentes) de cada tipo — o que o paciente lê no cadastro. */
    @Transactional(readOnly = true)
    public List<TermoVersao> vigentes() {
        List<TermoVersao> vigentes = new ArrayList<>();
        for (TipoTermo tipo : TipoTermo.values()) {
            vigente(tipo).ifPresent(vigentes::add);
        }
        return vigentes;
    }

    @Transactional(readOnly = true)
    public Optional<TermoVersao> vigente(TipoTermo tipo) {
        return termoVersaoRepository.findFirstByTipoAndStatusOrderByVersaoDesc(tipo, StatusVersao.PUBLICADO);
    }

    /**
     * Cria rascunhos com os textos de exemplo (D-089) para os tipos que ainda não têm nenhuma versão. Os
     * exemplos precisam de revisão do advogado antes de publicar.
     */
    @Transactional
    public List<TermoVersao> carregarExemplos(UsuarioAutenticado ator) {
        exigirMaster(ator);
        List<TermoVersao> criados = new ArrayList<>();
        for (TipoTermo tipo : TipoTermo.values()) {
            if (termoVersaoRepository.ultimaVersao(tipo) > 0) {
                continue;
            }
            String conteudo = lerExemplo("exemplos/termos/" + tipo.name().toLowerCase(Locale.ROOT) + ".md");
            int quebra = conteudo.indexOf('\n');
            String titulo = conteudo.substring(0, quebra).replaceFirst("^#\\s*", "").trim();
            criados.add(criarRascunho(tipo, titulo, conteudo.substring(quebra + 1).trim(), ator));
        }
        return criados;
    }

    private TermoVersao buscar(UUID id) {
        return termoVersaoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Termo não encontrado."));
    }

    static String lerExemplo(String caminho) {
        try {
            return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Exemplo não encontrado: " + caminho, e);
        }
    }

    static void exigirMaster(UsuarioAutenticado ator) {
        if (ator.perfil() != Perfil.MASTER) {
            throw new AccessDeniedException("Só o MASTER configura termos e modelos");
        }
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    static RegraNegocioException semVigente(TipoTermo tipo) {
        return new RegraNegocioException("Não há versão publicada do termo " + tipo + ".");
    }
}
