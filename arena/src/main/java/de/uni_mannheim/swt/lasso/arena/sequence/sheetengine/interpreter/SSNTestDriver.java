package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.Engine;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 *
 * @author Marcus Kessel
 */
public class SSNTestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(SSNTestDriver.class);

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, Class cutClass, int limitAdapters) throws IOException {
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        Engine engine = new Engine();
        Map<String, InterfaceSpecification> interfaceSpecificationMap = engine.lqlToMap(lql);

        SSNInterpreter interpreter = new SSNInterpreter();

        ClassUnderTest classUnderTest = createExample(Stack.class);
        CandidatePool pool = new CandidatePool(mavenRepository, Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        // TODO call with classundertest to set classloader
        Invocations invocations = interpreter.interpret(parsedSheet, interfaceSpecificationMap, classUnderTest);

        AdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();

        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecificationMap.get(faName), classUnderTest, limitAdapters);

        // run
        ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementations.get(0));

        return executedInvocations;
    }

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
}
