package br.com.clinicaviene.identidade.repository;

import br.com.clinicaviene.identidade.entity.Usuario;
import br.com.clinicaviene.identidade.enums.ConselhoProfissional;
import br.com.clinicaviene.identidade.enums.Perfil;
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
