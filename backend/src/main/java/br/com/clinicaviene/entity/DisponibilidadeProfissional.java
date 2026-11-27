package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Horário fixo semanal do profissional num consultório, em hora local da clínica (D-106). Encerrar = definir o fim
 * da vigência; o histórico fica.
 */
@Entity
@Table(name = "disponibilidade_profissional")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DisponibilidadeProfissional {

    @Id
    private UUID id;

    @Column(name = "profissional_id", nullable = false, updatable = false)
    private UUID profissionalId;

    @Column(name = "consultorio_id", nullable = false, updatable = false)
    private UUID consultorioId;

    /** ISO: 1 = segunda … 7 = domingo. */
    @Column(name = "dia_semana", nullable = false, updatable = false)
    private short diaSemana;

    @Column(name = "hora_inicio", nullable = false, updatable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false, updatable = false)
    private LocalTime horaFim;

    @Column(name = "vigencia_inicio", nullable = false, updatable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public DisponibilidadeProfissional(
            UUID profissionalId,
            UUID consultorioId,
            DayOfWeek dia,
            LocalTime horaInicio,
            LocalTime horaFim,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            UUID criadoPor,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.profissionalId = profissionalId;
        this.consultorioId = consultorioId;
        this.diaSemana = (short) dia.getValue();
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.vigenciaInicio = vigenciaInicio;
        this.vigenciaFim = vigenciaFim;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public DayOfWeek dia() {
        return DayOfWeek.of(diaSemana);
    }

    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(vigenciaInicio) && (vigenciaFim == null || !data.isAfter(vigenciaFim));
    }

    /** O horário deixa de valer depois de {@code ultimoDia} (inclusive). */
    public void encerrar(LocalDate ultimoDia) {
        this.vigenciaFim = ultimoDia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof DisponibilidadeProfissional outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return DisponibilidadeProfissional.class.hashCode();
    }
}
