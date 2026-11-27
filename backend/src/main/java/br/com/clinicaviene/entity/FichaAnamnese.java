package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.util.CampoCriptografadoConverter;
import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Ficha de cadastro e saúde assinada pelo paciente (CONTEXTO §5, D-088). Imutável (o banco rejeita UPDATE/DELETE):
 * atualização = nova ficha. Respostas, alertas e acompanhante cifrados; leitura só pelo {@code FichaAnamneseService}.
 */
@Entity
@Immutable
@Table(name = "ficha_anamnese")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FichaAnamnese {

    @Id
    private UUID id;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "modelo_anamnese_id", nullable = false)
    private UUID modeloAnamneseId;

    @Column(name = "convite_id", nullable = false)
    private UUID conviteId;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemAlteracao origem;

    @Column(name = "pelo_responsavel", nullable = false)
    private boolean peloResponsavel;

    /** JSON cifrado: código da pergunta → resposta. */
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(nullable = false)
    private String respostas;

    /** JSON cifrado: códigos das perguntas de alerta clínico respondidas positivamente. */
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(nullable = false)
    private String alertas;

    /** JSON cifrado (opcional): quem ajudou a preencher. */
    @Convert(converter = CampoCriptografadoConverter.class)
    private String acompanhante;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    public FichaAnamnese(
            UUID pacienteId,
            UUID modeloAnamneseId,
            UUID conviteId,
            UUID documentoId,
            OrigemAlteracao origem,
            boolean peloResponsavel,
            String respostas,
            String alertas,
            String acompanhante,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.pacienteId = pacienteId;
        this.modeloAnamneseId = modeloAnamneseId;
        this.conviteId = conviteId;
        this.documentoId = documentoId;
        this.origem = origem;
        this.peloResponsavel = peloResponsavel;
        this.respostas = respostas;
        this.alertas = alertas;
        this.acompanhante = acompanhante;
        this.criadaEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof FichaAnamnese outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return FichaAnamnese.class.hashCode();
    }
}
