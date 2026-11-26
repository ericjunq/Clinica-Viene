package br.com.clinicaviene.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cria o usuário DEV na primeira subida, só com o banco sem nenhum usuário (D-048, D-067). O DEV então
 * cria o MASTER pelo sistema. Com usuários no banco, não faz nada.
 */
@Component
@EnableConfigurationProperties(PropriedadesDevInicial.class)
class InicializadorDev implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDev.class);

    private final RepositorioUsuarios repositorio;
    private final ServicoUsuarios servico;
    private final PropriedadesDevInicial propriedades;

    InicializadorDev(RepositorioUsuarios repositorio, ServicoUsuarios servico, PropriedadesDevInicial propriedades) {
        this.repositorio = repositorio;
        this.servico = servico;
        this.propriedades = propriedades;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (repositorio.count() > 0) {
            return;
        }
        if (!propriedades.completas()) {
            log.warn("Nenhum usuário cadastrado. Defina DEV_INICIAL_CPF, DEV_INICIAL_NOME, DEV_INICIAL_EMAIL e "
                    + "DEV_INICIAL_SENHA para criar o usuário DEV inicial.");
            return;
        }
        Usuario dev = servico.criarDevInicial(new DadosNovoUsuario(
                propriedades.nome(),
                propriedades.cpf(),
                propriedades.email(),
                null,
                Perfil.DEV,
                null,
                null,
                null,
                null,
                propriedades.senha()));
        log.info("Usuário DEV inicial criado (id {}). Troca de senha e cadastro de 2FA obrigatórios no primeiro login.",
                dev.getId());
    }
}
