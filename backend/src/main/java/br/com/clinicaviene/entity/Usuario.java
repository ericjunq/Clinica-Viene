package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusUsuario;
import br.com.clinicaviene.util.CampoCriptografadoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Usuário interno (CONTEXTO §2, §12). Nunca é apagado: é inativado (D-068). */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    /** CPF cifrado com AES-GCM (D-052, D-061). Só dígitos. */
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(nullable = false)
    private String cpf;

    /** HMAC-SHA256 do CPF: unicidade e busca no login. */
    @Column(name = "cpf_hash", nullable = false, length = 64, updatable = false)
    private String cpfHash;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusUsuario status;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_atuacao", length = 30)
    private AreaAtuacao areaAtuacao;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ConselhoProfissional conselho;

    @Column(name = "numero_registro", length = 20)
    private String numeroRegistro;

    @Column(name = "uf_registro", length = 2)
    private String ufRegistro;

    @Getter(AccessLevel.NONE)
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    /** Senha provisória: troca obrigatória no próximo login. */
    @Column(name = "trocar_senha", nullable = false)
    private boolean trocarSenha;

    /** Segredo do TOTP, cifrado (D-072). */
    @Getter(AccessLevel.NONE)
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(name = "totp_segredo")
    private String totpSegredo;

    @Column(name = "totp_ativo", nullable = false)
    private boolean totpAtivo;

    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    public Usuario(NovoUsuarioDto dados, String cpfHash, String senhaHash, UUID criadoPor, Instant agora) {
        this.id = IdUtils.novoId();
        this.nomeCompleto = dados.nomeCompleto();
        this.cpf = dados.cpf();
        this.cpfHash = cpfHash;
        this.email = dados.email();
        this.telefone = dados.telefone();
        this.perfil = dados.perfil();
        this.status = StatusUsuario.ATIVO;
        this.areaAtuacao = dados.areaAtuacao();
        this.conselho = dados.conselho();
        this.numeroRegistro = dados.numeroRegistro();
        this.ufRegistro = dados.ufRegistro();
        this.senhaHash = senhaHash;
        this.trocarSenha = true;
        this.totpAtivo = false;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public boolean ativo() {
        return status == StatusUsuario.ATIVO;
    }

    public String senhaHash() {
        return senhaHash;
    }

    public String totpSegredo() {
        return totpSegredo;
    }

    public void alterarStatus(StatusUsuario novoStatus, Instant agora) {
        this.status = novoStatus;
        this.atualizadoEm = agora;
    }

    /** Dados editáveis (D-086). CPF não muda: identifica a pessoa e é a base do login. */
    public void atualizarDados(
            String nomeCompleto,
            String email,
            String telefone,
            AreaAtuacao areaAtuacao,
            ConselhoProfissional conselho,
            String numeroRegistro,
            String ufRegistro,
            Instant agora) {
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.telefone = telefone;
        this.areaAtuacao = areaAtuacao;
        this.conselho = conselho;
        this.numeroRegistro = numeroRegistro;
        this.ufRegistro = ufRegistro;
        this.atualizadoEm = agora;
    }

    /** Muda o perfil junto com o registro profissional (obrigatório só para PROFISSIONAL — D-086). */
    public void alterarPerfil(
            Perfil novoPerfil,
            AreaAtuacao areaAtuacao,
            ConselhoProfissional conselho,
            String numeroRegistro,
            String ufRegistro,
            Instant agora) {
        this.perfil = novoPerfil;
        this.areaAtuacao = areaAtuacao;
        this.conselho = conselho;
        this.numeroRegistro = numeroRegistro;
        this.ufRegistro = ufRegistro;
        this.atualizadoEm = agora;
    }

    public void definirSenha(String novoHash, boolean provisoria, Instant agora) {
        this.senhaHash = novoHash;
        this.trocarSenha = provisoria;
        this.atualizadoEm = agora;
    }

    public void ativarTotp(String segredo, Instant agora) {
        this.totpSegredo = segredo;
        this.totpAtivo = true;
        this.atualizadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }
}
