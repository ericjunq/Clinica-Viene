package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.PacientePermissoes;
import br.com.clinicaviene.dto.DocumentoResponse;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Assinatura;
import br.com.clinicaviene.entity.Documento;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.repository.AssinaturaRepository;
import br.com.clinicaviene.repository.DocumentoRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.enums.TipoDocumento;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Documentos assinados do paciente, para a tela interna: só metadados, nunca o conteúdo (D-088). */
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar(UUID pacienteId, UsuarioAutenticado ator) {
        if (!PacientePermissoes.gerenciaCadastro(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para ver os documentos do paciente");
        }
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new PacienteNaoEncontradoException();
        }
        List<Documento> documentos = documentoRepository.findByPacienteIdOrderByCriadoEmDesc(pacienteId);
        Map<UUID, Assinatura> assinaturas = assinaturaRepository
                .findByDocumentoIdIn(documentos.stream().map(Documento::getId).toList())
                .stream()
                .collect(Collectors.toMap(Assinatura::getDocumentoId, Function.identity(), (a, b) -> a));
        return documentos.stream()
                .map(d -> {
                    Assinatura a = assinaturas.get(d.getId());
                    return new DocumentoResponse(
                            d.getId(),
                            d.getTipo(),
                            d.getCriadoEm(),
                            a == null ? null : a.getTipoSignatario(),
                            a == null ? null : a.getNomeSignatario(),
                            a == null ? null : a.getMetodo());
                })
                .toList();
    }

    /** Pacientes (dentre os informados) que já têm ficha de cadastro assinada — para o aviso de ficha pendente. */
    @Transactional(readOnly = true)
    public Set<UUID> comFichaAssinada(Collection<UUID> pacienteIds) {
        if (pacienteIds.isEmpty()) {
            return Set.of();
        }
        return documentoRepository.pacientesComDocumento(TipoDocumento.FICHA_CADASTRO, pacienteIds);
    }
}
