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
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.BshEval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestSSNInterpreter {

    private static final Logger LOG = LoggerFactory.getLogger(TestSSNInterpreter.class);

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test() throws IOException {
        String ssnJsonlStr = "{\"sheet\": \"Sheet 1\", \"header\": \"Row 1\", \"cells\": {\"A1\": null, \"B1\": \"create\", \"C1\": \"Stack\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 2\", \"cells\": {\"A2\": null, \"B2\": \"create\", \"C2\": \"java.lang.String\", \"D2\": \"'Hello World!'\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A3\": null, \"B3\": \"push\", \"C3\": \"A1\", \"D3\": \"A2\"}}\n" +
                "{\"sheet\": \"Sheet 1\", \"header\": \"Row 3\", \"cells\": {\"A4\": null, \"B4\": \"size\", \"C4\": \"A1\"}}\n";

        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";
        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(lql);

        SSNInterpreter interpreter = new SSNInterpreter();

        // TODO call with classundertest to set classloader
        Invocations invocations = interpreter.interpret(parsedSheet, interfaceSpecificationMap);

        LOG.debug("Invocations\n{}", invocations);

        // now take it and adapt! we can directly inject an adapter and delegate dynamically
        // Option 1. Stack { push(..) { delegate.XXX("push", args ...); } }
        // Option 2. just use invocation as a template and directly call adaptee! (like in randoop)

        ClassUnderTest classUnderTest = CutUtils.createExample(Stack.class);
        CandidatePool pool = new CandidatePool(mavenRepository, Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        AdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;

        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecificationMap.get("Stack"), classUnderTest, limitAdapters);

        ExecutionListener executionListener = new ExecutionListener();
        // run
        ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementations.get(0), executionListener);
        LOG.debug("Executed Invocations\n{}", executedInvocations);
    }

    @Test
    public void testLqlToJava() throws IOException, NoSuchMethodException, ClassNotFoundException {
        String lql = "Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}";
        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(lql);

        SSNInterpreter interpreter = new SSNInterpreter();

        Eval eval = new BshEval();
        interpreter.lqlToJava(eval, interfaceSpecificationMap.get("Stack"));

        Class clazz = eval.resolveClass("Stack");
        clazz.getMethods();
        LOG.debug(clazz.getDeclaredConstructor().toString());
        LOG.debug(clazz.getDeclaredConstructor().toGenericString());

        // FIXME we could use the method prefix to ship the ID in order to link to LQL
        // encode ID: __ID__methodName(...) -- or __A_XXX__
        // or SORT: sort LQL to Java, each sig, then sort reflected ones

        LOG.debug("class = {}", clazz);
        //LOG.debug("class = {}",          bsh.getNameSpace().);
    }
}
