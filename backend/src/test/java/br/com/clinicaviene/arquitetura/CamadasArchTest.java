package br.com.clinicaviene.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.repository.FichaAnamneseRepository;
import br.com.clinicaviene.service.FichaAnamneseService;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import fixtures.arquitetura.OutroServicoComFichaAnamnese;
import fixtures.arquitetura.controller.ControllerComRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regras de camadas (D-078, D-105): dado de saúde só passa pelo service do assunto, que confere a permissão e grava a
 * auditoria; controllers só delegam a services.
 */
class CamadasArchTest {

    /** A anamnese (respostas de saúde) só é lida/gravada pelo FichaAnamneseService. */
    static final ArchRule ANAMNESE_SO_PELO_SERVICE = noClasses()
            .that()
            .doNotHaveFullyQualifiedName(FichaAnamneseService.class.getName())
            .and()
            .doNotHaveFullyQualifiedName(FichaAnamneseRepository.class.getName())
            .should()
            .dependOnClassesThat()
            .areAssignableTo(FichaAnamneseRepository.class)
            .because("dado de saúde só passa pelo service do assunto, que confere a permissão e registra a"
                    + " auditoria (D-078, D-088)");

    /** Controller é fino: converte e delega ao service, nunca acessa repository. */
    static final ArchRule CONTROLLER_SEM_REPOSITORY = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("controllers só delegam a services, que aplicam regras, permissões e auditoria (D-078)");

    static final List<ArchRule> REGRAS = List.of(ANAMNESE_SO_PELO_SERVICE, CONTROLLER_SEM_REPOSITORY);

    private static final JavaClasses CODIGO_DA_APLICACAO = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.clinicaviene");

    @Test
    void codigoDaAplicacaoRespeitaAsCamadas() {
        REGRAS.forEach(regra -> regra.check(CODIGO_DA_APLICACAO));
    }

    // --- prova de que as regras realmente pegam as violações (fixtures em src/test) ---

    @Test
    void detectaOutraClasseLendoAAnamnese() {
        JavaClasses classes = new ClassFileImporter().importClasses(OutroServicoComFichaAnamnese.class);
        assertThatThrownBy(() -> ANAMNESE_SO_PELO_SERVICE.check(classes)).isInstanceOf(AssertionError.class);
    }

    @Test
    void detectaControllerComRepository() {
        JavaClasses classes = new ClassFileImporter().importClasses(ControllerComRepository.class);
        assertThatThrownBy(() -> CONTROLLER_SEM_REPOSITORY.check(classes)).isInstanceOf(AssertionError.class);
    }
}
