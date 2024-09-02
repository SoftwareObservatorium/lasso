package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.ActuationSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.AdapterSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.StimulusSheet;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class EngineTest {

    private static final Logger LOG = LoggerFactory.getLogger(EngineTest.class);

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_stack() throws IOException {
        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"push\", \"C2\": \"Hello World!\"}}\n" +
                "{\"sheet\": \"Sheet 2\", \"header\": \"Row 1\", \"cells\": {\"A3\": 2, \"B3\": \"size\", \"C3\": null}}";

        Engine engine = new Engine();

        ClassUnderTest classUnderTest = createExample(Stack.class);
        CandidatePool pool = new CandidatePool(mavenRepository, Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        AdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;

        // stage 1: obtain StimulusSheet
        StimulusSheet stimulusSheet = engine.createStimulusSheetFromLQL(lql, ssnJsonlStr);

        // stage 2: obtain (adapted) implementation
        List<AdaptedImplementation> adaptedImplementations = engine.createAdapters(stimulusSheet, classUnderTest, adaptationStrategy, limitAdapters);

        // stage 3: obtain (adapted) implementation
        List<AdapterSheet> adapterSheets = engine.resolveSheets(stimulusSheet, adaptedImplementations);

        // stage 4: execute each adapted implementation to obtain actuation sheets
        ExecutionListener executionListener = new ExecutionListener();
        List<ActuationSheet> actuationSheets = engine.execute(adapterSheets, executionListener);

        // debug
        for(ActuationSheet actuationSheet : actuationSheets) {
            // print
            LOG.debug(Objects.toString(actuationSheet));
        }
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
