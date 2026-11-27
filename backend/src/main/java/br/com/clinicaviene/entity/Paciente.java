package br.com.clinicaviene.entity;

import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.util.CampoCriptografadoConverter;
import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Paciente (CONTEXTO §4). Nome, CPF e data de nascimento não são editáveis: só mudam por nova ficha
 * conferida e assinada pelo paciente (D-092). Contato e endereço mudam com versão auditada.
 */
@Entity
@Table(name = "paciente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paciente {

    public static final int MAIORIDADE = 18;

    @Id
    private UUID id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    /** CPF cifrado (só dígitos). Opcional para menor; adulto sem CPF só com aprovação do MASTER (D-053). */
    @Convert(converter = CampoCriptografadoConverter.class)
    private String cpf;

    @Column(name = "cpf_hash", length = 64)
    private String cpfHash;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(length = 254)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 8)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 80)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "sem_cpf_justificativa", updatable = false)
    private String semCpfJustificativa;

    @Column(name = "sem_cpf_aprovado_por", updatable = false)
    private UUID semCpfAprovadoPor;

    @Column(name = "sem_cpf_aprovado_em", updatable = false)
    private Instant semCpfAprovadoEm;

    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    public Paciente(
            String nomeCompleto,
            String cpf,
            String cpfHash,
            LocalDate dataNascimento,
            String email,
            String telefone,
            EnderecoDto endereco,
            UUID criadoPor,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.nomeCompleto = nomeCompleto;
        this.cpf = cpf;
        this.cpfHash = cpfHash;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.telefone = telefone;
        aplicarEndereco(endereco);
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /** Adulto sem CPF: só com justificativa e aprovação do MASTER (D-053). */
    public void registrarSemCpf(String justificativa, UUID aprovadoPor, Instant agora) {
        this.semCpfJustificativa = justificativa;
        this.semCpfAprovadoPor = aprovadoPor;
        this.semCpfAprovadoEm = agora;
    }

    /**
     * Nome, CPF e nascimento só mudam por ficha conferida e assinada pelo próprio paciente (D-092) — chamado
     * apenas pelo {@code PacienteService} ao gravar a ficha assinada.
     */
    public void corrigirIdentificacaoPelaFicha(
            String nomeCompleto, String cpf, String cpfHash, LocalDate dataNascimento, Instant agora) {
        this.nomeCompleto = nomeCompleto;
        this.cpf = cpf;
        this.cpfHash = cpfHash;
        this.dataNascimento = dataNascimento;
        this.atualizadoEm = agora;
    }

    public void atualizarContato(String email, String telefone, EnderecoDto endereco, Instant agora) {
        this.email = email;
        this.telefone = telefone;
        aplicarEndereco(endereco);
        this.atualizadoEm = agora;
    }

    public EnderecoDto endereco() {
        return new EnderecoDto(cep, logradouro, numero, complemento, bairro, cidade, uf);
    }

    public int idade(LocalDate hoje) {
        return Period.between(dataNascimento, hoje).getYears();
    }

    public boolean menorDeIdade(LocalDate hoje) {
        return idade(hoje) < MAIORIDADE;
    }

    private void aplicarEndereco(EnderecoDto endereco) {
        EnderecoDto e = endereco == null ? EnderecoDto.vazio() : endereco;
        this.cep = e.cep();
        this.logradouro = e.logradouro();
        this.numero = e.numero();
        this.complemento = e.complemento();
        this.bairro = e.bairro();
        this.cidade = e.cidade();
        this.uf = e.uf();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Paciente outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Paciente.class.hashCode();
    }
}
