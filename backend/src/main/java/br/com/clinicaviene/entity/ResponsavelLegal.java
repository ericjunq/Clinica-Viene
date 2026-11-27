package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.Parentesco;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Responsável legal do paciente menor (LGPD art. 14). É quem aceita os termos e assina. */
@Entity
@Table(name = "responsavel_legal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResponsavelLegal {

    @Id
    private UUID id;

    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;

    @Column(name = "nome_completo", nullable = false, length = 150, updatable = false)
    private String nomeCompleto;

    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(nullable = false, updatable = false)
    private String cpf;

    @Column(name = "cpf_hash", nullable = false, length = 64, updatable = false)
    private String cpfHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Parentesco parentesco;

    @Column(length = 20)
    private String telefone;

    @Column(length = 254)
    private String email;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public ResponsavelLegal(
            UUID pacienteId,
            String nomeCompleto,
            String cpf,
            String cpfHash,
            Parentesco parentesco,
            String telefone,
            String email,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.pacienteId = pacienteId;
        this.nomeCompleto = nomeCompleto;
        this.cpf = cpf;
        this.cpfHash = cpfHash;
        this.parentesco = parentesco;
        this.telefone = telefone;
        this.email = email;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResponsavelLegal outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return ResponsavelLegal.class.hashCode();
    }
}
