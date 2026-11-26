package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByConselhoAndNumeroRegistroAndUfRegistro(
            ConselhoProfissional conselho, String numeroRegistro, String ufRegistro);

    List<Usuario> findAllByOrderByNomeCompletoAsc();

    List<Usuario> findAllByPerfilNotOrderByNomeCompletoAsc(Perfil perfil);
}
