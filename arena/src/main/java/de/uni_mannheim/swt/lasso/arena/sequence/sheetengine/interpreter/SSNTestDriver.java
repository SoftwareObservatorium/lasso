package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.PassThroughAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.HierarchyMemberResolver;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * SSN test driver.
 *
 * @author Marcus Kessel
 */
public class SSNTestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(SSNTestDriver.class);

    /**
     * Dependency resolution
     */
    private MavenRepository mavenRepository;

    /**
     * {@link AdaptationStrategy}
     */
    private AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();

    public MavenRepository getMavenRepository() {
        // FIXME update
        if(mavenRepository == null) {
            String mavenRepoUrl = NexusInstance.LOCAL_URL;
            File localRepo = new File("/tmp/lalalamvn/local-repo");

            DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
            this.mavenRepository = new MavenRepository(resolver);
        }

        return mavenRepository;
    }

    public void setMavenRepository(MavenRepository mavenRepository) {
        this.mavenRepository = mavenRepository;
    }

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, Class cutClass, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // FIXME artifacts
        return runSheet(ssnJsonlStr, lql, CutUtils.createExample(cutClass), limitAdapters, executionListener);
    }

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, String cutClass, List<String> artifacts, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // artifacts
        String artifact = null;
        if(CollectionUtils.isNotEmpty(artifacts)) {
            artifact = artifacts.get(0);
        }

        return runSheet(ssnJsonlStr, lql, CutUtils.createExample(cutClass, artifact), limitAdapters, executionListener);
    }

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, ClassUnderTest classUnderTest, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(lql);

        SSNInterpreter interpreter = new SSNInterpreter();

        CandidatePool pool = new CandidatePool(getMavenRepository(), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        // TODO call with classundertest to set classloader
        Invocations invocations = interpreter.interpret(parsedSheet, interfaceSpecificationMap, classUnderTest);

        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();

        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecificationMap.get(faName), classUnderTest, limitAdapters);

        // run
        ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementations.get(0), executionListener);

        return executedInvocations;
    }

    public InterfaceSpecification toLQL(String className, List<String> artifacts) {
        // artifacts
        String artifact = null;
        if(CollectionUtils.isNotEmpty(artifacts)) {
            artifact = artifacts.get(0);
        }

        try {
            ClassUnderTest classUnderTest = CutUtils.createExample(className, artifact);
            CandidatePool pool = new CandidatePool(getMavenRepository(), Collections.singletonList(classUnderTest));
            // automatically resolves project-related artifacts
            pool.initProjects();

            Class<?> clazz = classUnderTest.loadClass();

            InterfaceSpecification interfaceSpecification = new InterfaceSpecification();
            interfaceSpecification.setClassName(clazz.getSimpleName()); // use simple name and not fully-qualified one

            List<MethodSignature> constructors = new LinkedList<>();
            Arrays.stream(clazz.getDeclaredConstructors()).forEach(c -> {
                constructors.add(new ReflectionConstructorSignature(c));
            });
            interfaceSpecification.setConstructors(constructors);

            List<MethodSignature> methods = new LinkedList<>();
            // also detect all inherited, non-overridden super methods
            HierarchyMemberResolver.getAllDeclaredMethods(clazz).forEach(m -> {
                methods.add(new ReflectionMethodSignature(m));
            });

            interfaceSpecification.setMethods(methods);

            return interfaceSpecification;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public AdaptationStrategy getAdaptationStrategy() {
        return adaptationStrategy;
    }

    public void setAdaptationStrategy(AdaptationStrategy adaptationStrategy) {
        this.adaptationStrategy = adaptationStrategy;
    }
}
