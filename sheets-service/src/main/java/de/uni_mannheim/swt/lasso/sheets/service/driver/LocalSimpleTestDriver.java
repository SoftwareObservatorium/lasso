package de.uni_mannheim.swt.lasso.sheets.service.driver;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.PitestContainer;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.PassThroughAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.JaCoCoListener;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;
import de.uni_mannheim.swt.lasso.sheets.service.cut.CodeGeneration;
import de.uni_mannheim.swt.lasso.sheets.service.cut.SheetGeneration;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;

import de.uni_mannheim.swt.lasso.sheets.service.srh.InMemorySRH;
import de.uni_mannheim.swt.lasso.testing.generate.random.RandomObjectGenerator;
import de.uni_mannheim.swt.lasso.testing.generate.typeaware.MutatorSettings;
import de.uni_mannheim.swt.lasso.testing.generate.typeaware.TypeAwareMutator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Marcus Kessel
 */
public class LocalSimpleTestDriver implements TestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSimpleTestDriver.class);

    private final MavenRepository mavenRepository;
    private final GsonMapper gsonMapper = new GsonMapper();
    private final CodeGeneration codeGeneration;

    private InMemorySRH inMemorySRH;

    TestDataGenerator testGenerator = new TestDataGenerator();
    TypeAwareMutator typeAwareMutator = new TypeAwareMutator(new MutatorSettings());
    RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();

    private final SheetGeneration sheetGeneration;

    public LocalSimpleTestDriver(MavenRepository mavenRepository, InMemorySRH inMemorySRH, CodeGeneration codeGeneration, SheetGeneration sheetGeneration) {
        this.mavenRepository = mavenRepository;
        this.inMemorySRH = inMemorySRH;
        this.codeGeneration = codeGeneration;
        this.sheetGeneration = sheetGeneration;
    }

    @Override
    public SheetResponse execute(SheetRequest request) throws IOException {
        // FIXME
        String executionId = UUID.randomUUID().toString();

        List<TestResult> testResults = new ArrayList<>(request.getClassesUnderTest().size());

        List<SheetSpec> sheetSpecs = request.getSheets();

        // sheets
        List<Sheet> sheetDtos = sheetSpecs.stream().map(s -> {
            Sheet sheet = new Sheet(s.getSignature(), s.getBody(), s.getInterfaceSpecification());
            sheet.setInvocations(s.getInvocations());
            return sheet;
        }).toList();

        // cuts
        List<ClassUnderTest> classesUnderTest = request.getClassesUnderTest().stream().map(cut -> {
            // generated code?
            if(StringUtils.isNotBlank(cut.getCodeGenerationId())) {
                try {
                    return codeGeneration.toClassUnderTest(cut.getCodeGenerationId());
                } catch (Throwable e) {
                    LOG.warn("Could not find generated code candidate {}", cut.getCodeGenerationId());
                    LOG.warn("Stack", e);

                    return null;
                }
            }

            // code search?
            if(cut.getCodeUnit() != null) {
                LOG.debug("Found code search candidate: {}", cut.getCodeUnit().getId());

                ClassUnderTest classUnderTest = new ClassUnderTest(new de.uni_mannheim.swt.lasso.core.model.System(cut.getCodeUnit()));

                return classUnderTest;
            }

            // artifacts
            String artifact = null;
            // FIXME artifacts
            if (CollectionUtils.isNotEmpty(cut.getArtifacts())) {
                artifact = cut.getArtifacts().get(0);
            }

            if (StringUtils.isNotBlank(artifact)) {
                // resolve remotely
                return CutUtils.resolve(cut.getClassName(), artifact);
            }

            return CutUtils.createExample(cut.getClassName(), artifact);
        }).toList();

        // read invocations
        List<SheetInvocation> sheetInvocations = sheetDtos.stream().flatMap(s -> {
            String sheetName = StringUtils.substringBefore(s.getSignature(), "(");

            List<String> invocations = s.getInvocations();
            if (CollectionUtils.isEmpty(invocations)) {
                // default invocation
                return Arrays.asList(new SheetInvocation(sheetName, "")).stream();
            }

            //
            return invocations.stream().map(i -> new SheetInvocation(sheetName, i));
        }).toList();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheetDtos, classesUnderTest, sheetInvocations);

        // driver
        SSNTestDriver testDriver = new SSNTestDriver();
        testDriver.setMavenRepository(mavenRepository);

        // set adaptation strategy
        String adaptationStrategy = request.getAdaptationStrategy();
        if (StringUtils.equalsIgnoreCase(adaptationStrategy, DefaultAdaptationStrategy.class.getSimpleName())) {
            testDriver.setAdaptationStrategy(new DefaultAdaptationStrategy());
        } else if (StringUtils.equalsIgnoreCase(adaptationStrategy, PassThroughAdaptationStrategy.class.getSimpleName())) {
            testDriver.setAdaptationStrategy(new PassThroughAdaptationStrategy());
        } else {
            testDriver.setAdaptationStrategy(new PassThroughAdaptationStrategy());
        }
        int adapterLimit = 1;
        if (request.getAdapterLimit() > 0) {
            adapterLimit = request.getAdapterLimit();
        }

        // code analyzers
        boolean jacoco = false;
        boolean mutation = false;
        if (CollectionUtils.isNotEmpty(request.getAnalyzers())) {
            jacoco = request.getAnalyzers().contains("cc");
            mutation = request.getAnalyzers().contains("mt");
        }

        Validate.isTrue(!(jacoco && mutation), "JaCoCo and mutation is not possible at the same time");

        // visitor
        InvocationVisitor invocationVisitor;
        JaCoCoListener jaCoCoListener = new JaCoCoListener();
        if (jacoco) {
            invocationVisitor = new CompositeInvocationVisitor(
                    Arrays.asList(jaCoCoListener)); // add jacoco listener

            // set driver
            testDriver.setEnableJaCoCoCoverage(true);
        } else {
            invocationVisitor = new InvocationVisitor();
        }

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix;

        if (mutation) {
            stimulusResponseMatrix = testDriver.mutateAndRunSheets(stimulusMatrix, adapterLimit, invocationVisitor);
        } else {
            stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, adapterLimit, invocationVisitor);
        }

        // for each cell
        List<SheetSpec> actuationSheetResults = new ArrayList<>(sheetSpecs.size());
        List<SheetSpec> adaptedActuationSheetResults = new ArrayList<>(sheetSpecs.size());
        List<SheetSpec> oracleSheetResults = new LinkedList<>();
        List<SheetSpec> metricSheetResults = new LinkedList<>();
        List<SheetSpec> srmViewResults = new LinkedList<>();

        // create big SRM based on output column (we can identify unique clusters afterwards ..)
        int targetColumn = 0;
        Map<Test, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> outputSrm = new LinkedHashMap<>();
        //de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object> aggregatedMetricSrm = new de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<>();

        //StimulusResponseMatrix<ExecutedInvocation, AdaptedImplementation, String> testStatementsSrm = new StimulusResponseMatrix<>();

        // tests are sorted by insertion order by default

        List<SheetSpec> executedTests = new LinkedList<>();

        for (Test test : stimulusResponseMatrix.getRows()) {
            // FIXME make copy of original one
            SheetSpec executedTest = new SheetSpec();

            AdaptedImplementation adaptedImplementation = stimulusResponseMatrix.getColumns().iterator().next();
            TestInvocation testInvocation = stimulusMatrix.get(test, adaptedImplementation.getAdaptee());
            String testSig = test.getName() + ":" + test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";

            executedTest.setSignature(testSig);
            executedTest.setBody(test.getParsedSheet().getSheet().getBody());
            executedTest.setInvocations(Arrays.asList(testInvocation.getInvocationExpression()));

            executedTests.add(executedTest);

        }

        // collect all executed impls.
        List<ClassUnderTestSpec> executedImpls = new LinkedList<>();
        for (AdaptedImplementation adaptedImplementation : stimulusResponseMatrix.getColumns()) {
            // FIXME copy of original one
            ClassUnderTestSpec executedImpl = new ClassUnderTestSpec();

            if (mutation && !adaptedImplementation.getAdaptee().getVariantId().equals("original")) {
                PitestContainer pitestContainer = (PitestContainer) adaptedImplementation.getAdaptee().getProject().getContainer();
                LOG.debug("Mutant {}", pitestContainer.getMutant().getDetails());

                String mutantId = pitestContainer.getMutant().getDetails().getId().toString();

                executedImpl.setId(adaptedImplementation.getAdaptee().getFullId() + "_" + adaptedImplementation.getAdapterId() + "_" + mutantId);
            } else {
                executedImpl.setId(adaptedImplementation.getAdaptee().getFullId() + "_" + adaptedImplementation.getAdapterId());
            }

            executedImpl.setClassName(adaptedImplementation.getAdaptee().getClassName());
            //executedImpl.setArtifacts(Arrays.asList(adaptedImplementation.getAdaptee().toUri()));

            executedImpl.setCodeUnit(adaptedImplementation.getAdaptee().getImplementation().getCode());

            executedImpls.add(executedImpl);
        }

        // add oracle
        ClassUnderTestSpec oracle = new ClassUnderTestSpec();
        oracle.setId("oracle");
        oracle.setClassName("Oracle");
        executedImpls.add(oracle);


        for (Test test : stimulusResponseMatrix.getRows()) {
            boolean stimulusStored = false;

            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> summaryOutputSheet = new de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<>();
            outputSrm.put(test, summaryOutputSheet);

            // for each test
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> oracleSheet = null;

            // FIXME sort to put originals first? (can be multiple cuts ..)
            for (AdaptedImplementation adaptedImplementation : stimulusResponseMatrix.getColumns()) {
                ExecutedInvocations executedInvocations = stimulusResponseMatrix.get(test, adaptedImplementation);
                LOG.debug("executed invocations\n{}", executedInvocations);

                ParsedSheet parsedSheet = executedInvocations.getInvocations().getParsedSheet();
                TestInvocation testInvocation = stimulusMatrix.get(test, adaptedImplementation.getAdaptee());
                String testSig = test.getName() + ":" + test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";

                if(!stimulusStored) {
                    try {
                        // write stimulus sheet and interface
                        inMemorySRH.storeStimulusSheet(executionId, testSig, test, testInvocation);
                        stimulusStored = true;
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

                if(oracleSheet == null) {
                    try {
                        // FIXME merge resolved parameters etc. (get rid of "null" entries)
                        ExecutedInvocations oracleInvocations = SheetUtils.toOracle(executedInvocations.getInvocations());
                        oracleSheet = SheetUtils.toOracleSheet(oracleInvocations, gsonMapper);

                        oracleSheet.debug();

                        LOG.info("Storing oracle sheet in SRH");
                        inMemorySRH.storeOracleSheet(executionId, testSig, oracleSheet);

                        SheetSpec oracleSheetResult = new SheetSpec();
                        oracleSheetResult.setSignature(testSig);
                        oracleSheetResult.setInterfaceSpecification(test.getInterfaceSpecification().toLQL());
                        oracleSheetResult.setBody(oracleSheet.toJsonl());
                        oracleSheetResult.setImplementationId("oracle");

                        oracleSheetResults.add(oracleSheetResult);
                    } catch (Throwable e) {
                        LOG.warn("Oracle sheet failed", e);
                    }

                    // add to adapted + spec
                    SheetSpec oracleSheetResult = new SheetSpec();
                    oracleSheetResult.setSignature(testSig);
                    oracleSheetResult.setInterfaceSpecification(test.getInterfaceSpecification().toLQL());
                    oracleSheetResult.setBody(oracleSheet.toJsonl());
                    oracleSheetResult.setImplementationId("oracle");

                    actuationSheetResults.add(oracleSheetResult);
                    adaptedActuationSheetResults.add(oracleSheetResult);
                }


                try {
                    List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = SheetUtils.toSheets(adaptedImplementation, executedInvocations, gsonMapper);

                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);

                    // FIXME old? add to output summary
                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> outputSheet = actuationSheetData.getColumnAsSheet(targetColumn, 0);
                    int cols = summaryOutputSheet.getNumberOfColumns();
                    for(int row : outputSheet.getRows()) {
                        for(int col : outputSheet.getColumns()) {
                            summaryOutputSheet.put(row, cols, outputSheet.get(row, col));
                        }
                    }

//                    // add to statements SRM
//                    for(int row : outputSheet.getRows()) {
//                        for(int col : outputSheet.getColumns()) {
//                            testStatementsSrm.put(executedInvocations.getExecutedInvocation(row), adaptedImplementation, outputSheet.get(row, col));
//                        }
//                    }

                    actuationSheetData.debug();
                    adaptedActuationSheetData.debug();

                    LOG.info("JSON actuationSheet\n{}", actuationSheetData.toJsonl());
                    LOG.info("JSON adaptedActuationSheet\n{}", adaptedActuationSheetData.toJsonl());

                    try {
                        LOG.info("Storing sheet in SRH");
                        inMemorySRH.storeSheet(executionId, adaptedImplementation, testSig, adaptedActuationSheetData);
                    } catch (Throwable e) {
                        LOG.warn("Storing sheet in SRH failed", e);
                    }

                    SheetSpec actuationSheetResult = new SheetSpec();

                    // FIXME set more fields
                    actuationSheetResult.setSignature(testSig);
                    actuationSheetResult.setInterfaceSpecification(test.getInterfaceSpecification().toLQL());
                    actuationSheetResult.setBody(actuationSheetData.toJsonl());
                    //actuationSheetResult.setImplementationId("ABSTRACTION");

                    actuationSheetResults.add(actuationSheetResult);

                    SheetSpec adaptedActuationSheetResult = new SheetSpec();
                    adaptedActuationSheetResult.setSignature(testSig);
                    adaptedActuationSheetResult.setInterfaceSpecification(test.getInterfaceSpecification().toLQL());
                    adaptedActuationSheetResult.setBody(adaptedActuationSheetData.toJsonl());

                    if (mutation && !adaptedImplementation.getAdaptee().getVariantId().equals("original")) {
                        PitestContainer pitestContainer = (PitestContainer) adaptedImplementation.getAdaptee().getProject().getContainer();
                        LOG.debug("Mutant {}", pitestContainer.getMutant().getDetails());

                        String mutantId = pitestContainer.getMutant().getDetails().getId().toString();

                        adaptedActuationSheetResult.setImplementationId(adaptedImplementation.getAdaptee().getFullId() + "_" + adaptedImplementation.getAdapterId() + "_" + mutantId);
                    } else {
                        adaptedActuationSheetResult.setImplementationId(adaptedImplementation.getAdaptee().getFullId() + "_" + adaptedImplementation.getAdapterId());
                    }

                    actuationSheetResult.setImplementationId(adaptedActuationSheetResult.getImplementationId());

                    adaptedActuationSheetResults.add(adaptedActuationSheetResult);
                } catch (Throwable e) {
                    LOG.warn("execution failed", e);

                    throw new RuntimeException(e);
                }
            }
        }

        if(jacoco) {
            // metrics
            for (AdaptedImplementation adaptedImplementation : stimulusResponseMatrix.getColumns()) {
                StimulusResponseMatrix<String, AdaptedImplementation, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object>> jacocoSrm = jaCoCoListener.getStimulusResponseMatrix();
                Set<String> metricIds = jacocoSrm.getRows();

                //int cols = aggregatedMetricSrm.getNumberOfColumns();
                // assume same order
                for (String metricId : metricIds) {
                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object> metricSheet = jacocoSrm.get(metricId, adaptedImplementation);

//                    for(int row : metricSheet.getRows()) {
//                        for(int col : metricSheet.getColumns()) {
//                            aggregatedMetricSrm.put(row, cols, metricSheet.get(row, col));
//                        }
//                    }

                    SheetSpec metricSheetResult = new SheetSpec();
                    metricSheetResult.setSignature(metricId);
                    metricSheetResult.setInterfaceSpecification("n/a");
                    metricSheetResult.setBody(metricSheet.toJsonl());

                    if (mutation && !adaptedImplementation.getAdaptee().getVariantId().equals("original")) {
                        PitestContainer pitestContainer = (PitestContainer) adaptedImplementation.getAdaptee().getProject().getContainer();
                        LOG.debug("Mutant {}", pitestContainer.getMutant().getDetails());

                        String mutantId = pitestContainer.getMutant().getDetails().getId().toString();

                        metricSheetResult.setImplementationId(adaptedImplementation.getAdaptee().getFullId() + "_" + adaptedImplementation.getAdapterId() + "_" + mutantId);
                    } else {
                        metricSheetResult.setImplementationId(adaptedImplementation.getAdaptee().getFullId() + "_" + adaptedImplementation.getAdapterId());
                    }

                    metricSheetResults.add(metricSheetResult);

                    metricSheet.debug();

                    // add to SRH
                    try {
                        LOG.info("Storing metric sheet in SRH");
                        inMemorySRH.storeMetricSheet(executionId, adaptedImplementation, metricId, metricSheet);
                    } catch (Throwable e) {
                        LOG.warn("Storing sheet in SRH failed", e);
                    }
                }
            }

//            // JACOCO srm view
//            SheetSpec metricSrmResult = new SheetSpec();
//            metricSrmResult.setSignature("JaCoCo SRM");
//            metricSrmResult.setInterfaceSpecification("n/a");
//            metricSrmResult.setBody(aggregatedMetricSrm.toJsonl());
//            metricSrmResult.setImplementation("all");
//            srmViewResults.add(metricSrmResult);
        }

        // output SRM view
        de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> aggregatedSummarySheet = new de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<>();

        for(Test test : outputSrm.keySet()) {
            int r = aggregatedSummarySheet.getNumberOfRows();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> outputSheet = outputSrm.get(test);
            for(int row : outputSheet.getRows()) {
                for(int col : outputSheet.getColumns()) {
                    aggregatedSummarySheet.put(row + r, col, outputSheet.get(row, col));
                }
            }
        }

        //testStatementsSrm.debug();

        SheetSpec outputSrmResult = new SheetSpec();
        outputSrmResult.setSignature("Output SRM");
        outputSrmResult.setInterfaceSpecification("n/a");
        outputSrmResult.setBody(aggregatedSummarySheet.toJsonl());
        outputSrmResult.setImplementationId("all");
        srmViewResults.add(outputSrmResult);

        // equivalence
        boolean equivalenceSheets = true;
        if(equivalenceSheets) {
            // (all tests)
            // ALL TESTS: original implementation
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> originalImpl = aggregatedSummarySheet.getColumnAsSheet(0, 0);

            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Boolean> equivalenceMatrix = new de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<>();
            // iterate over all remaining impls (i.e., alt impls)
            for(int c = 0; c < aggregatedSummarySheet.getNumberOfColumns(); c++) {
                de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> variant = aggregatedSummarySheet.getColumnAsSheet(c, 0);

                boolean equivalent = originalImpl.isEquivalentColumn(variant, 0);
                equivalenceMatrix.put(0, c, equivalent);
            }

            SheetSpec allEquivalenceResult = new SheetSpec();
            allEquivalenceResult.setSignature("All Tests Equivalence to First Implementation");
            allEquivalenceResult.setInterfaceSpecification("n/a");
            allEquivalenceResult.setBody(equivalenceMatrix.toJsonl());
            allEquivalenceResult.setImplementationId("all");
            srmViewResults.add(allEquivalenceResult);

            // for each test
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Boolean> equivalenceMatrixByTest = new de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<>();
            int r = 0;
            for(Test test : outputSrm.keySet()) {
                de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> outputSheet = outputSrm.get(test);
                de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> originalImplByTest = outputSheet.getColumnAsSheet(0, 0);

                // iterate over all remaining impls (i.e., alt impls)
                for(int c = 0; c < outputSheet.getNumberOfColumns(); c++) {
                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> variant = outputSheet.getColumnAsSheet(c, 0);

                    boolean equivalent = originalImplByTest.isEquivalentColumn(variant, 0);
                    equivalenceMatrixByTest.put(r, c, equivalent);
                }

                r++;
            }

            SheetSpec byTestEquivalenceResult = new SheetSpec();
            byTestEquivalenceResult.setSignature("By Test Equivalence to First Implementation");
            byTestEquivalenceResult.setInterfaceSpecification("n/a");
            byTestEquivalenceResult.setBody(equivalenceMatrixByTest.toJsonl());
            byTestEquivalenceResult.setImplementationId("all");
            srmViewResults.add(byTestEquivalenceResult);
        }

        TestResult testResult = new TestResult();
        testResult.setExecutionId(executionId); // FIXME
        testResult.setStatus("SUCCESS"); // FIXME

        testResult.setExecutedTests(executedTests);
        testResult.setExecutedImplementations(executedImpls);

        testResult.setActuationSheets(actuationSheetResults);
        testResult.setAdaptedActuationSheets(adaptedActuationSheetResults);
        testResult.setMetricSheets(metricSheetResults);
        testResult.setOracleSheets(oracleSheetResults);

        testResult.setSrmViews(srmViewResults);

        testResults.add(testResult);

        SheetResponse sheetResponse = new SheetResponse();
        sheetResponse.setTestResults(testResults);
        sheetResponse.setStatus("SUCCESS"); // FIXME
        sheetResponse.setExecutionId(executionId); // FIXME

        return sheetResponse;
    }

    @Override
    public SheetGenerationResponse generateSheets(SheetGenerationRequest request) throws IOException {
        // FIXME
        if(StringUtils.equalsIgnoreCase(request.getTestGenerator(), "genai")) {
            String model = "llama3.1:latest";

            SheetSpec sheetSpec = request.getSheets().get(0);

//            String prompt = """
//generate a test for the functionality described by the following interface specification
//```lql
//${spec}```
//. return the test in the jsonl format used by the following example:
//```
//{"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
//{"cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
//```
//The example represents a test as spreadsheet cells. the first column is the output column, the second column the operation column, the third column the class that offers the operation, and finally the remaining columns are the input parameters to the operation.
//                """;

            String prompt = """
generate a unit test for the functionality described by the following interface specification:
```
${spec}```. return the test in the jsonl format used by the following example:
{"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
{"cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
```
The example represents a unit test as  a spreadsheet. Each line is represents a method call. the first column of the method invocation is the output column, the second column the method name column, the third column the class that offers the method, and finally the remaining columns are the input parameter values to the operation. Delimit each line by a new line operator. Do not generate comments.
                """;

            String spec = sheetSpec.getInterfaceSpecification();
            String nPrompt = StringUtils.replace(prompt, "${spec}", spec);

            LOG.debug("Sheet prompt\n{}", nPrompt);

            SheetSpec genSheet = sheetGeneration.promptOllama(model, nPrompt);
            SheetGenerationResponse response = new SheetGenerationResponse();
            response.setSheets(Arrays.asList(genSheet));
            return response;
        }


        List<SheetSpec> sheetSpecs = request.getSheets();

        // sheets
        List<Sheet> sheetDtos = sheetSpecs.stream().map(s -> {
            Sheet sheet = new Sheet(s.getSignature(), s.getBody(), s.getInterfaceSpecification());
            sheet.setInvocations(s.getInvocations());
            return sheet;
        }).toList();

        // cuts
        List<ClassUnderTest> classesUnderTest = request.getClassesUnderTest().stream().map(cut -> {
            // generated code?
            if (StringUtils.isNotBlank(cut.getCodeGenerationId())) {
                try {
                    return codeGeneration.toClassUnderTest(cut.getCodeGenerationId());
                } catch (Throwable e) {
                    LOG.warn("Could not find generated code candidate {}", cut.getCodeGenerationId());
                    LOG.warn("Stack", e);

                    return null;
                }
            }

            // artifacts
            String artifact = null;
            // FIXME artifacts
            if (CollectionUtils.isNotEmpty(cut.getArtifacts())) {
                artifact = cut.getArtifacts().get(0);
            }

            if (StringUtils.isNotBlank(artifact)) {
                // resolve remotely
                return CutUtils.resolve(cut.getClassName(), artifact);
            }

            return CutUtils.createExample(cut.getClassName(), artifact);
        }).toList();

        // read invocations
        List<SheetInvocation> sheetInvocations = sheetDtos.stream().flatMap(s -> {
            String sheetName = StringUtils.substringBefore(s.getSignature(), "(");

            List<String> invocations = s.getInvocations();
            if (CollectionUtils.isEmpty(invocations)) {
                // default invocation
                return Arrays.asList(new SheetInvocation(sheetName, "")).stream();
            }

            //
            return invocations.stream().map(i -> new SheetInvocation(sheetName, i));
        }).toList();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheetDtos, classesUnderTest, sheetInvocations);

        String tg = request.getTestGenerator();
        TestDataGenerator.Generator generator;
        if(StringUtils.equalsIgnoreCase(tg, "mutate")) {
            // type aware
            generator = (parsedCell, parameter) -> typeAwareMutator.mutateValue(parameter.getValue());
        } else {
            // random
            generator = (parsedCell, parameter) -> randomObjectGenerator.random(parameter.getTargetClass());
        }

        List<Sheet> generatedSheets = testGenerator.generateData(stimulusMatrix, generator);

        SheetGenerationResponse response = new SheetGenerationResponse();
        response.setSheets(generatedSheets.stream().map(s -> {
            SheetSpec sheetSpec = new SheetSpec();
            sheetSpec.setSignature(s.getSignature());
            sheetSpec.setInterfaceSpecification(s.getInterfaceSpecification());
            sheetSpec.setBody(s.getBody());
            //sheetSpec.setImplementation("ABSTRACTION");

            return sheetSpec;
        }).toList());

        return response;
    }
}
