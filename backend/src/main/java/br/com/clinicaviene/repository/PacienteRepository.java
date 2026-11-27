package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Paciente;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {

    Optional<Paciente> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);

    @Query("select p from Paciente p where lower(p.nomeCompleto) like lower(concat('%', :nome, '%')) order by p.nomeCompleto")
    List<Paciente> buscarPorNome(@Param("nome") String nome, Pageable pagina);

    List<Paciente> findByDataNascimentoOrderByNomeCompleto(LocalDate dataNascimento, Pageable pagina);

    /** Candidatos a duplicidade de quem não tem CPF (D-053): mesmo nome e data de nascimento. */
    @Query("select p from Paciente p where lower(p.nomeCompleto) = lower(:nome) and p.dataNascimento = :nascimento")
    List<Paciente> mesmoNomeENascimento(@Param("nome") String nome, @Param("nascimento") LocalDate nascimento);
}
