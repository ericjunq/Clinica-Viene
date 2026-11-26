package br.com.clinicaviene.identidade;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RepositorioUsuarios extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByConselhoAndNumeroRegistroAndUfRegistro(
            ConselhoProfissional conselho, String numeroRegistro, String ufRegistro);

    List<Usuario> findAllByOrderByNomeCompletoAsc();

    List<Usuario> findAllByPerfilNotOrderByNomeCompletoAsc(Perfil perfil);
}
