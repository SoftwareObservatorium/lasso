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
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.SheetSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.HierarchyMemberResolver;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;
import de.uni_mannheim.swt.lasso.ssn.SSNParser;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    private File buildWorkingDirectory;

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

    public static List<Test> parseAll(List<Sheet> sheets) throws IOException {
        SSNParser ssnParser = new SSNParser();

        List<Test> parsedTests = new ArrayList<>(sheets.size());
        for (Sheet sheet : sheets) {
            LOG.debug("SHEET body\n {}", sheet.getBody());

            try {
                // parse body
                ParsedSheet parsedSheet = ssnParser.parseJsonl(sheet);
                // parse signature
                SheetSignature signature = LQLUtils.lqlToSheetSignature(sheet.getSignature());

                LOG.debug("SHEET signature\n {}", signature.toLQL());

                Test test = new Test(signature.getName(), parsedSheet, signature);
                parsedTests.add(test);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return parsedTests;
    }

    public static StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> parseStimulusMatrix(List<Sheet> sheets, List<ClassUnderTest> classesUnderTest, List<SheetInvocation> sheetInvocations) throws IOException {
        // parse sheets
        List<Test> parsedSheets = parseAll(sheets);

        String interfaceLql = sheets.get(0).getInterfaceSpecification();

        // parse interface specification
        // interface
        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(interfaceLql);
        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();
        // set globally for all (important to set same reference!)
        parsedSheets.forEach(s -> s.setInterfaceSpecification(interfaceSpecificationMap.get(faName)));

        StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix = new StimulusResponseMatrix<>();

        for(Test parsedSheet : parsedSheets) {
            List<SheetInvocation> filtered = sheetInvocations.stream().filter(i -> StringUtils.equals(parsedSheet.getName(), i.getName())).toList();

            String baseName = parsedSheet.getName();

            for(int i = 0; i < filtered.size(); i++) {
                SheetInvocation sheetInvocationDto = filtered.get(i);
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

    public StimulusResponseMatrix<Test, AdaptedImplementation, ExecutedInvocations> runSheets(StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // classes under test
        Set<ClassUnderTest> classesUnderTest = stimulusMatrix.getColumns();
        CandidatePool pool = new CandidatePool(getMavenRepository(), new ArrayList<>(classesUnderTest));
        if(buildWorkingDirectory != null) {
            pool.setWorkingDirectory(buildWorkingDirectory);
        }

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

        Set<Test> tests = stimulusMatrix.getRows();
        // take some random test to obtain interface
        Test randomTest = tests.iterator().next();
        // get interface
        InterfaceSpecification interfaceSpecification = randomTest.getInterfaceSpecification();

        for(ClassUnderTest classUnderTest : classesUnderTest) {
            if(!classUnderTest.getProject().isResolved()) {
                LOG.warn("Not testing {}, since it has unresolved dependencies", classUnderTest.getFullId());

                // FIXME signal in SRM

                // skip
                continue;
            }

            List<AdaptedImplementation> adaptedImplementations;
            try {
                adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);
            } catch (Throwable e) {
                LOG.warn("Not testing {}, since no adapters could be identified", classUnderTest.getFullId());
                LOG.warn("Trace", e);

                // skip
                continue;
            }

            for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {

                executionListener.visitBeforeExecution(adaptedImplementation);

                // run all tests for each impl.
                for(Test test : tests) {
                    // prepare executable sheet
                    ParsedSheet parsedSheet = test.getParsedSheet();

                    // test invocation
                    TestInvocation testInvocation = stimulusMatrix.get(test, classUnderTest);

                    // prepare invocations
                    Invocations invocations;
                    try {
                        invocations = interpreter.interpret(test, classUnderTest, testInvocation);
                    } catch (Throwable e) {
                        LOG.warn("SSN Interpreter failed for {}", classUnderTest.getFullId());
                        LOG.warn("Stack", e);

                        // FIXME add result stimulusResponseMatrix

                        continue;
                    }

                    // run
                    ExecutedInvocations executedInvocations;
                    try {
                        executedInvocations = interpreter.run(invocations, adaptedImplementation, executionListener);
                    } catch (Throwable e) {
                        LOG.warn("SSN Test Run failed for {}", classUnderTest.getFullId());
                        LOG.warn("Stack", e);

                        // FIXME add result stimulusResponseMatrix

                        continue;
                    }

                    // add to SRM
                    stimulusResponseMatrix.put(test, adaptedImplementation, executedInvocations);
                }

                executionListener.visitAfterExecution(adaptedImplementation);
            }
        }

        return stimulusResponseMatrix;
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
        // avoid concurrent modifications ...
        // classes under test
        List<ClassUnderTest> classesUnderTest = new ArrayList<>(stimulusMatrix.getColumns());
        // all tests
        List<Test> tests = new ArrayList<>(stimulusMatrix.getRows());
        CandidatePool pool = new CandidatePool(getMavenRepository(), new ArrayList<>(classesUnderTest));
        if(buildWorkingDirectory != null) {
            pool.setWorkingDirectory(buildWorkingDirectory);
        }
        pool.initProjects();

        // create mutants -- simply expand Stimulus Matrix
        for(ClassUnderTest classUnderTest : classesUnderTest) {
            // Pitest
            Pitest pitest = new Pitest(classUnderTest);
            Map<ClassUnderTest, MutationDetails> mutants = createMutants(pool, classUnderTest, pitest, true, "original");

            // add to stimulus matrix as well -- to keep it consistent with the resulting SRM
            for(ClassUnderTest mutant : mutants.keySet()) {
                for(Test test : tests) {
                    stimulusMatrix.put(test, mutant, stimulusMatrix.get(test, classUnderTest));
                }
            }

            // automatically resolves project-related artifacts
            pool.initProjects();
        }

        // run
        return runSheets(stimulusMatrix, limitAdapters, executionListener);
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
            if(buildWorkingDirectory != null) {
                pool.setWorkingDirectory(buildWorkingDirectory);
            }
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

    public File getBuildWorkingDirectory() {
        return buildWorkingDirectory;
    }

    public void setBuildWorkingDirectory(File buildWorkingDirectory) {
        this.buildWorkingDirectory = buildWorkingDirectory;
    }
}
