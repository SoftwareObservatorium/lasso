package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocations;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;

import java.util.UUID;

/**
 * Implementation utilities
 *
 * @author Marcus Kessel
 */
public class CutUtils {

    public static ClassUnderTest createExample(Class<?> exampleClass) {
        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());
        implementation.setName(exampleClass.getSimpleName());
        implementation.setPackagename(exampleClass.getPackage().getName());
        implementation.setGroupId("examples.lasso");
        implementation.setArtifactId("examples");
        implementation.setVersion("1.0.0-SNAPSHOT");
        ClassUnderTest classUnderTest = new ClassUnderTest(new de.uni_mannheim.swt.lasso.core.model.System(implementation));
        //classUnderTest.setPseudo(true);

        // one workaround to avoid resolution of artifacts
        classUnderTest.getProject().setDependencyResult(new DependencyResult(new DependencyRequest()));

        return classUnderTest;
    }

    public static boolean isFaCut(Invocations invocations, Class targetClass) {
        return invocations.getInterfaceSpecifications().containsKey(targetClass.getCanonicalName());
    }

    public static boolean isCut(ClassUnderTest classUnderTest, Class targetClass) throws ClassNotFoundException {
        return classUnderTest.loadClass().equals(targetClass);
    }
}
