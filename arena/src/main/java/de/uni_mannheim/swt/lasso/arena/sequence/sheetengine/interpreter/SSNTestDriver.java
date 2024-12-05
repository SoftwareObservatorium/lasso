package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.Pitest;
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

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.apache.commons.collections4.CollectionUtils;
import org.pitest.mutationtest.engine.MutationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

    private boolean enableJaCoCoCoverage;

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

        // FIXME do it over all sequence sheets
        if(isEnableJaCoCoCoverage()) {
            // set scope
            Scope scope = new Scope();
            scope.setType("class");
//            List<String> pkgWhitelist = new ArrayList<>();
//            pkgWhitelist.add("de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples");
//            scope.addConfiguration("pkgWhitelist", (Serializable) pkgWhitelist);

            ContainerFactory containerFactory = ContainerFactory.jacoco(scope);
            pool.setContainerFactory(containerFactory);
        }

        // automatically resolves project-related artifacts
        pool.initProjects();

        Invocations invocations = interpreter.interpret(parsedSheet, interfaceSpecificationMap, classUnderTest);

        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();

        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecificationMap.get(faName), classUnderTest, limitAdapters);

        // FIXME move to global
        executionListener.visitBeforeExecution(adaptedImplementations.get(0));

        // run
        ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementations.get(0), executionListener);

        // FIXME move to global
        executionListener.visitAfterExecution(adaptedImplementations.get(0));

        return executedInvocations;
    }

    public Map<ClassUnderTest, ExecutedInvocations> runSheetAndMutate(String ssnJsonlStr, String lql, ClassUnderTest classUnderTest, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(lql);

        SSNInterpreter interpreter = new SSNInterpreter();

        CandidatePool pool = new CandidatePool(getMavenRepository(), Collections.singletonList(classUnderTest));
        pool.initProjects();

        // create mutants
        // Pitest
        Pitest pitest = new Pitest(classUnderTest);
        Map<ClassUnderTest, MutationDetails> mutants = createMutants(pool, classUnderTest, pitest, true, "original");

        // automatically resolves project-related artifacts
        pool.initProjects();

        Map<ClassUnderTest, ExecutedInvocations> results = new LinkedHashMap<>(pool.getClassesUnderTest().size());
        for(ClassUnderTest variant : pool.getClassesUnderTest()) {
            Invocations invocations = interpreter.interpret(parsedSheet, interfaceSpecificationMap, classUnderTest);

            // FIXME for all CUTs .. here only one
            String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();

            List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecificationMap.get(faName), classUnderTest, limitAdapters);

            // FIXME move to global
            executionListener.visitBeforeExecution(adaptedImplementations.get(0));

            // run
            ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementations.get(0), executionListener);

            // FIXME move to global
            executionListener.visitAfterExecution(adaptedImplementations.get(0));

            results.put(variant, executedInvocations);

            // save details
            if(mutants.containsKey(variant)) {
                MutationDetails mutationDetails = mutants.get(variant);
                // FIXME mutant data
            }
        }

        return results;
    }

    public Map<ClassUnderTest, MutationDetails> createMutants(CandidatePool pool, ClassUnderTest classUnderTest, Pitest pitest, boolean generateReport, String reportSuffix) {
        try {
            List<MutationDetails> mutationDetails = pitest.findMutations();

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Generated '%s' mutants for implementation '%s' (%s)",
                        mutationDetails.size(),
                        classUnderTest.getId(),
                        classUnderTest.getClassName()));
            }

            Map<ClassUnderTest, MutationDetails> mutants = new LinkedHashMap<>(mutationDetails.size());

            for (int m = 0; m < mutationDetails.size(); m++) {
                MutationDetails md = mutationDetails.get(m);

                if (classUnderTest.getImplementation() != null && classUnderTest.getImplementation().getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                    // FIXME restrict mutants to method implementation
                    //md.getMethod();
                }
                // generate mutant
                ClassUnderTest mutant = pitest.generateMutant(
                        String.valueOf(m),
                        md,
                        pool.getMavenRepository().getResolver());

                mutants.put(mutant, md);

                // add mutant to the arena
                pool.addClass(mutant);
            }

            if (generateReport) {
                if (classUnderTest.getLocalProject() != null) {
                    pitest.generateReport(reportSuffix);
                }
            }

            return mutants;
        } catch (Throwable e) {
            throw new RuntimeException("Could not create mutants", e);
        }
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

    public boolean isEnableJaCoCoCoverage() {
        return enableJaCoCoCoverage;
    }

    public void setEnableJaCoCoCoverage(boolean enableJaCoCoCoverage) {
        this.enableJaCoCoCoverage = enableJaCoCoCoverage;
    }
}
