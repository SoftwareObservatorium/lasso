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
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto.SheetDto;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto.SheetInvocationDto;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.HierarchyMemberResolver;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.pitest.mutationtest.engine.MutationDetails;
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

    private boolean enableJaCoCoCoverage;

    public MavenRepository getMavenRepository() {
        // FIXME update
        if (mavenRepository == null) {
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

    public static List<ParsedSheet> parseAll(List<SheetDto> sheets) throws IOException {
        SSNParser ssnParser = new SSNParser();

        List<ParsedSheet> parsedSheets = new ArrayList<>(sheets.size());
        for (SheetDto sheet : sheets) {
            LOG.debug("JSONL body\n {}", sheet.getBody());

            ParsedSheet parsedSheet = ssnParser.parseJsonl(sheet.getBody(), sheet.getSignature(), sheet.getInterfaceSpecification());
            parsedSheets.add(parsedSheet);
        }

        return parsedSheets;
    }

    public static StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> parseStimulusMatrix(List<SheetDto> sheets, List<ClassUnderTest> classesUnderTest, List<SheetInvocationDto> sheetInvocations) throws IOException {
        // parse sheets
        List<ParsedSheet> parsedSheets = parseAll(sheets);

        String interfaceLql = sheets.get(0).getInterfaceSpecification();

        // parse interface specification
        // interface
        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(interfaceLql);
        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();
        // set globally for all (important to set same reference!)
        parsedSheets.forEach(s -> s.setInterfaceSpecification(interfaceSpecificationMap.get(faName)));

        StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix = new StimulusResponseMatrix<>();

        for(ParsedSheet parsedSheet : parsedSheets) {
            List<SheetInvocationDto> filtered = sheetInvocations.stream().filter(i -> StringUtils.equals(parsedSheet.getName(), i.getName())).toList();

            String baseName = parsedSheet.getName();

            for(int i = 0; i < filtered.size(); i++) {
                SheetInvocationDto sheetInvocationDto = filtered.get(i);
                TestInvocation testInvocation = new TestInvocation(sheetInvocationDto.getName(), sheetInvocationDto.getInvocation());

                String testName = baseName + "_" + i;

                Test test = new Test(testName, parsedSheet);

                for(ClassUnderTest classUnderTest : classesUnderTest) {
                    stimulusMatrix.put(test, classUnderTest, testInvocation);
                }
            }
        }

        return stimulusMatrix;
    }

    @Deprecated
    public List<ActuationSheet> runSheets(List<ParsedSheet> parsedSheets, Class cutClass, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // FIXME artifacts
        return runSheets(parsedSheets, CutUtils.createExample(cutClass), limitAdapters, executionListener);
    }


    @Deprecated
    public List<ActuationSheet> runSheet(List<ParsedSheet> parsedSheets, String cutClass, List<String> artifacts, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // artifacts
        String artifact = null;
        if (CollectionUtils.isNotEmpty(artifacts)) {
            artifact = artifacts.get(0);
        }

        return runSheets(parsedSheets, CutUtils.createExample(cutClass, artifact), limitAdapters, executionListener);
    }

    public StimulusResponseMatrix<Test, AdaptedImplementation, ExecutedInvocations> runSheets(StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // classes under test
        Set<ClassUnderTest> classesUnderTest = stimulusMatrix.getTable().columnKeySet();
        CandidatePool pool = new CandidatePool(getMavenRepository(), new ArrayList<>(classesUnderTest));

        if (isEnableJaCoCoCoverage()) {
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

        SSNInterpreter interpreter = new SSNInterpreter();

        // SRM
        StimulusResponseMatrix<Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = new StimulusResponseMatrix<>();

        for(ClassUnderTest classUnderTest : classesUnderTest) {
            Map<Test, TestInvocation> testInvocationMap = stimulusMatrix.getTable().column(classUnderTest);
            // take some random test to get interface
            Test randomTest = testInvocationMap.keySet().iterator().next();

            // get interface
            ParsedSheet randomSheet = randomTest.getParsedSheet();
            InterfaceSpecification interfaceSpecification = randomSheet.getInterfaceSpecification();

            List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

            for(Map.Entry<Test, TestInvocation> testInvocation : testInvocationMap.entrySet()) {
                // prepare executable sheet
                Test test = testInvocation.getKey();
                ParsedSheet parsedSheet = test.getParsedSheet();
                Invocations invocations = interpreter.interpret(parsedSheet, classUnderTest, testInvocation.getValue());

                for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                    executionListener.visitBeforeExecution(adaptedImplementation);

                    // run
                    ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementation, executionListener);

                    executionListener.visitAfterExecution(adaptedImplementation);

                    // add to SRM
                    stimulusResponseMatrix.put(test, adaptedImplementation, executedInvocations);
                }
            }
        }

        return stimulusResponseMatrix;
    }

    @Deprecated
    public List<ActuationSheet> runSheets(List<ParsedSheet> parsedSheets, ClassUnderTest classUnderTest, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        CandidatePool pool = new CandidatePool(getMavenRepository(), Collections.singletonList(classUnderTest));

        if (isEnableJaCoCoCoverage()) {
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

        SSNInterpreter interpreter = new SSNInterpreter();

        // get interface
        InterfaceSpecification interfaceSpecification = parsedSheets.get(0).getInterfaceSpecification();
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

        // prepare executable sheets
        List<Invocations> invocationsList = new ArrayList<>(parsedSheets.size());
        for (ParsedSheet parsedSheet : parsedSheets) {
            Invocations invocations = interpreter.interpret(parsedSheet, classUnderTest);
            invocationsList.add(invocations);
        }

        List<ActuationSheet> actuationSheets = new LinkedList<>();
        for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {
            executionListener.visitBeforeExecution(adaptedImplementation);

            // run
            for (Invocations invocations : invocationsList) {
                ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementation, executionListener);

                ActuationSheet actuationSheet = new ActuationSheet();
                actuationSheet.setAdaptedImplementation(adaptedImplementation);
                actuationSheet.setExecutedInvocations(executedInvocations);

                actuationSheets.add(actuationSheet);
            }

            executionListener.visitAfterExecution(adaptedImplementation);
        }

        return actuationSheets;
    }

    /**
     *
     * @param stimulusMatrix is modified, since mutant implementations are added!
     * @param limitAdapters
     * @param executionListener
     * @return
     * @throws IOException
     */
    public StimulusResponseMatrix<Test, AdaptedImplementation, ExecutedInvocations> mutateAndRunSheets(StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // classes under test
        Set<ClassUnderTest> classesUnderTest = stimulusMatrix.getTable().columnKeySet();
        CandidatePool pool = new CandidatePool(getMavenRepository(), new ArrayList<>(classesUnderTest));
        pool.initProjects();

        // create mutants -- simply expand Stimulus Matrix
        for(ClassUnderTest classUnderTest : classesUnderTest) {
            // Pitest
            Pitest pitest = new Pitest(classUnderTest);
            Map<ClassUnderTest, MutationDetails> mutants = createMutants(pool, classUnderTest, pitest, true, "original");

            // add to stimulus matrix as well -- to keep it consistent with the resulting SRM
            for(ClassUnderTest mutant : mutants.keySet()) {
                Map<Test, TestInvocation> map = stimulusMatrix.getTable().column(classUnderTest);
                map.entrySet().forEach(e -> stimulusMatrix.put(e.getKey(), mutant, e.getValue()));
            }

            // automatically resolves project-related artifacts
            pool.initProjects();
        }

        // run
        return runSheets(stimulusMatrix, limitAdapters, executionListener);

//        SSNInterpreter interpreter = new SSNInterpreter();
//
//        // SRM
//        StimulusResponseMatrix<Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = new StimulusResponseMatrix<>();
//
//        for(ClassUnderTest classUnderTest : classesUnderTest) {
//            for (ClassUnderTest variant : pool.getClassesUnderTest()) {
//                Map<Test, TestInvocation> testInvocationMap = stimulusMatrix.getTable().column(classUnderTest);
//                // take some random test to get interface
//                Test randomTest = testInvocationMap.keySet().iterator().next();
//
//                // get interface
//                ParsedSheet randomSheet = randomTest.getParsedSheet();
//                InterfaceSpecification interfaceSpecification = randomSheet.getInterfaceSpecification();
//
//                List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, variant, limitAdapters);
//
//                for(Map.Entry<Test, TestInvocation> testInvocation : testInvocationMap.entrySet()) {
//                    // prepare executable sheet
//                    Test test = testInvocation.getKey();
//                    ParsedSheet parsedSheet = test.getParsedSheet();
//                    Invocations invocations = interpreter.interpret(parsedSheet, variant, testInvocation.getValue());
//
//                    for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {
//                        executionListener.visitBeforeExecution(adaptedImplementation);
//
//                        // run
//                        ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementation, executionListener);
//
//                        executionListener.visitAfterExecution(adaptedImplementation);
//
//                        // add to SRM
//                        stimulusResponseMatrix.put(test, adaptedImplementation, executedInvocations);
//                    }
//                }
//            }
//        }
//
//        return stimulusResponseMatrix;
    }


    @Deprecated
    public List<ActuationSheet> mutateAndRunSheets(List<ParsedSheet> parsedSheets, ClassUnderTest classUnderTest, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        CandidatePool pool = new CandidatePool(getMavenRepository(), Collections.singletonList(classUnderTest));
        pool.initProjects();

        // create mutants
        // Pitest
        Pitest pitest = new Pitest(classUnderTest);
        Map<ClassUnderTest, MutationDetails> mutants = createMutants(pool, classUnderTest, pitest, true, "original");

        // automatically resolves project-related artifacts
        pool.initProjects();

        SSNInterpreter interpreter = new SSNInterpreter();

        List<ActuationSheet> actuationSheets = new LinkedList<>();
        for (ClassUnderTest variant : pool.getClassesUnderTest()) {

            // prepare executable sheets
            List<Invocations> invocationsList = new ArrayList<>(parsedSheets.size());
            for (ParsedSheet parsedSheet : parsedSheets) {
                Invocations invocations = interpreter.interpret(parsedSheet, variant);
                invocationsList.add(invocations);
            }

            InterfaceSpecification interfaceSpecification = parsedSheets.get(0).getInterfaceSpecification();
            List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, variant, limitAdapters);

            for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                executionListener.visitBeforeExecution(adaptedImplementation);

                // run
                for (Invocations invocations : invocationsList) {
                    ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementation, executionListener);

                    ActuationSheet actuationSheet = new ActuationSheet();
                    actuationSheet.setAdaptedImplementation(adaptedImplementation);
                    actuationSheet.setExecutedInvocations(executedInvocations);

                    actuationSheets.add(actuationSheet);
                }

                executionListener.visitAfterExecution(adaptedImplementation);
            }
        }

        return actuationSheets;
    }


    @Deprecated
    public List<ActuationSheet> mutateAndRunSheets(List<ParsedSheet> parsedSheets, String cutClass, List<String> artifacts, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // artifacts
        String artifact = null;
        if (CollectionUtils.isNotEmpty(artifacts)) {
            artifact = artifacts.get(0);
        }

        return mutateAndRunSheets(parsedSheets, CutUtils.createExample(cutClass, artifact), limitAdapters, executionListener);
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
        if (CollectionUtils.isNotEmpty(artifacts)) {
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
