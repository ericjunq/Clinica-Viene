package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusUsuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByConselhoAndNumeroRegistroAndUfRegistroAndIdNot(
            ConselhoProfissional conselho, String numeroRegistro, String ufRegistro, UUID id);

    boolean existsByConselhoAndNumeroRegistroAndUfRegistro(
            ConselhoProfissional conselho, String numeroRegistro, String ufRegistro);

    List<Usuario> findAllByOrderByNomeCompletoAsc();

    List<Usuario> findAllByPerfilNotOrderByNomeCompletoAsc(Perfil perfil);

    List<Usuario> findAllByPerfilAndStatusOrderByNomeCompletoAsc(Perfil perfil, StatusUsuario status);
}
