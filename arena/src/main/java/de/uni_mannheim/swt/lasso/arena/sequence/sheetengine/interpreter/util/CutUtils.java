package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocations;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;

import java.io.File;
import java.util.Collections;
import java.util.UUID;

/**
 * Implementation utilities
 *
 * @author Marcus Kessel
 */
public class CutUtils {

    public static final String EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT = "examples.lasso:examples:1.0.0-SNAPSHOT";

    public static ClassUnderTest createExample(Class<?> exampleClass) {
        String[] uriParts = StringUtils.split(EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT, ":");

        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());
        implementation.setName(exampleClass.getSimpleName());
        implementation.setPackagename(exampleClass.getPackage().getName());
        implementation.setGroupId(uriParts[0]);
        implementation.setArtifactId(uriParts[1]);
        implementation.setVersion(uriParts[2]);
        ClassUnderTest classUnderTest = new ClassUnderTest(new de.uni_mannheim.swt.lasso.core.model.System(implementation));
        //classUnderTest.setPseudo(true);

        // one workaround to avoid resolution of artifacts
        classUnderTest.getProject().setDependencyResult(new DependencyResult(new DependencyRequest()));

        return classUnderTest;
    }

    public static ClassUnderTest createExample(String exampleClass) {
        return createExample(exampleClass, null);
    }

    public static ClassUnderTest createExample(String exampleClass, String artifactUri) {
        String name = StringUtils.substringAfterLast(exampleClass, ".");
        String pkg = StringUtils.substringBeforeLast(exampleClass, ".");

        if(StringUtils.isBlank(artifactUri)) {
            artifactUri = EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT;
        }

        String[] uriParts = StringUtils.split(artifactUri, ":");

        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());
        implementation.setName(name);
        implementation.setPackagename(pkg);
        implementation.setGroupId(uriParts[0]);
        implementation.setArtifactId(uriParts[1]);
        implementation.setVersion(uriParts[2]);
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

    /**
     * For internal testing purposes ...
     *
     * @param classUnderTest
     */
    public static void initializeCutDirty(ClassUnderTest classUnderTest) {
        String mavenRepoUrl = NexusInstance.LOCAL_URL;
        File localRepo = new File("/tmp/lalalamvn/local-repo");

        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        MavenRepository mavenRepository = new MavenRepository(resolver);

        CandidatePool pool = new CandidatePool(mavenRepository, Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();
    }

    public static boolean isMarkerType(Class<?> type) {
        if(type == null) {
            return false;
        }

        return FAMarker.class.equals(type);
    }
}
