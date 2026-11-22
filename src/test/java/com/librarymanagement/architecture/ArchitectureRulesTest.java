package com.librarymanagement.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.librarymanagement", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureRulesTest {

    @ArchTest
    public static final ArchRule controllersMustNotAccessRepositoriesDirectly =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .or().areAnnotatedWith(Controller.class)
                    .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
                    .because("Controllers must remain thin and delegate data persistence through application services");

    @ArchTest
    public static final ArchRule controllersShouldBeNamedProperly =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("REST API controllers must follow consistent naming conventions");

    @ArchTest
    public static final ArchRule repositoriesShouldBeNamedProperly =
            classes()
                    .that().areAnnotatedWith(Repository.class)
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Data repositories must follow consistent naming conventions");
}
