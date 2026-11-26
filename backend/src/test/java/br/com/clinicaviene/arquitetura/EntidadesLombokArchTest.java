package br.com.clinicaviene.arquitetura;

import static com.tngtech.archunit.lang.conditions.ArchConditions.be;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.lombok.ClasseComValue;
import fixtures.lombok.EntidadeComAllArgsConstructor;
import fixtures.lombok.EntidadeComData;
import fixtures.lombok.EntidadeComEqualsAndHashCode;
import fixtures.lombok.EntidadeComToString;
import fixtures.lombok.EntidadeCorreta;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regras do Lombok (docs/DECISOES.md D-051).
 *
 * <p>As anotações do Lombok ({@code @Data}, {@code @ToString}, {@code @EqualsAndHashCode},
 * {@code @AllArgsConstructor}, {@code @Value}) têm retenção SOURCE e não existem no bytecode.
 * Como o {@code lombok.config} liga {@code lombok.addLombokGeneratedAnnotation}, todo código
 * gerado recebe {@code @lombok.Generated} (retenção CLASS), e é isso que as regras detectam.
 */
class EntidadesLombokArchTest {

    private static final String GERADO_PELO_LOMBOK = "lombok.Generated";

    /** Pega @Data, @ToString e @EqualsAndHashCode em entidades. */
    static final ArchRule ENTIDADE_SEM_TOSTRING_EQUALS_HASHCODE_DO_LOMBOK = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Entity.class)
            .and()
            .haveNameMatching("toString|equals|hashCode|canEqual")
            .should()
            .beAnnotatedWith(GERADO_PELO_LOMBOK)
            .because("entidades JPA não podem usar @Data, @ToString ou @EqualsAndHashCode;"
                    + " equals/hashCode devem ser escritos à mão, só pelo id (D-051)")
            .allowEmptyShould(true);

    private static final DescribedPredicate<JavaConstructor> COM_PARAMETROS =
            DescribedPredicate.describe("têm parâmetros", c -> !c.getRawParameterTypes().isEmpty());

    /** Pega @AllArgsConstructor (e @RequiredArgsConstructor público) em entidades. */
    static final ArchRule ENTIDADE_SEM_CONSTRUTOR_COM_ARGUMENTOS_DO_LOMBOK = noConstructors()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Entity.class)
            .and()
            .arePublic()
            .and(COM_PARAMETROS)
            .should()
            .beAnnotatedWith(GERADO_PELO_LOMBOK)
            .because("entidades JPA não podem usar @AllArgsConstructor (D-051)")
            .allowEmptyShould(true);

    private static final DescribedPredicate<JavaClass> GERADA_COMO_LOMBOK_VALUE = DescribedPredicate.describe(
            "geradas com @Value do Lombok",
            c -> c.getModifiers().contains(JavaModifier.FINAL)
                    && c.getMethods().stream()
                            .anyMatch(m -> m.getName().equals("equals") && m.isAnnotatedWith(GERADO_PELO_LOMBOK))
                    && c.getFields().stream()
                            .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
                            .allMatch(f -> f.getModifiers().contains(JavaModifier.FINAL)));

    /** Pega @Value em qualquer classe. */
    static final ArchRule SEM_LOMBOK_VALUE = noClasses()
            .should(be(GERADA_COMO_LOMBOK_VALUE))
            .because("@Value é proibido; use record (D-051)")
            .allowEmptyShould(true);

    static final List<ArchRule> REGRAS = List.of(
            ENTIDADE_SEM_TOSTRING_EQUALS_HASHCODE_DO_LOMBOK,
            ENTIDADE_SEM_CONSTRUTOR_COM_ARGUMENTOS_DO_LOMBOK,
            SEM_LOMBOK_VALUE);

    private static final JavaClasses CODIGO_DA_APLICACAO = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.clinicaviene");

    @Test
    void codigoDaAplicacaoRespeitaAsRegrasDoLombok() {
        REGRAS.forEach(regra -> regra.check(CODIGO_DA_APLICACAO));
    }

    // --- prova de que as regras realmente pegam as violações (fixtures em src/test) ---

    @Test
    void detectaDataEmEntidade() {
        assertViola(ENTIDADE_SEM_TOSTRING_EQUALS_HASHCODE_DO_LOMBOK, EntidadeComData.class);
    }

    @Test
    void detectaToStringEmEntidade() {
        assertViola(ENTIDADE_SEM_TOSTRING_EQUALS_HASHCODE_DO_LOMBOK, EntidadeComToString.class);
    }

    @Test
    void detectaEqualsAndHashCodeEmEntidade() {
        assertViola(ENTIDADE_SEM_TOSTRING_EQUALS_HASHCODE_DO_LOMBOK, EntidadeComEqualsAndHashCode.class);
    }

    @Test
    void detectaAllArgsConstructorEmEntidade() {
        assertViola(ENTIDADE_SEM_CONSTRUTOR_COM_ARGUMENTOS_DO_LOMBOK, EntidadeComAllArgsConstructor.class);
    }

    @Test
    void detectaValue() {
        assertViola(SEM_LOMBOK_VALUE, ClasseComValue.class);
    }

    @Test
    void aceitaEntidadeNoPadrao() {
        JavaClasses classes = new ClassFileImporter().importClasses(EntidadeCorreta.class);
        REGRAS.forEach(regra -> assertThatCode(() -> regra.check(classes)).doesNotThrowAnyException());
    }

    private static void assertViola(ArchRule regra, Class<?> classe) {
        JavaClasses classes = new ClassFileImporter().importClasses(classe);
        assertThatThrownBy(() -> regra.check(classes)).isInstanceOf(AssertionError.class);
    }
}
