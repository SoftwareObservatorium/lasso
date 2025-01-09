package de.uni_mannheim.swt.lasso.sheets.service.driver;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.PitestContainer;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.JaCoCoListener;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class LocalSimpleTestDriver implements TestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSimpleTestDriver.class);

    private final MavenRepository mavenRepository;
    private final GsonMapper gsonMapper = new GsonMapper();

    public LocalSimpleTestDriver(MavenRepository mavenRepository) {
        this.mavenRepository = mavenRepository;
    }

    @Override
    public SheetResponse execute(SheetRequest request) throws IOException {
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
            // artifacts
            String artifact = null;
            if (CollectionUtils.isNotEmpty(cut.getArtifacts())) {
                artifact = cut.getArtifacts().get(0);
            }
            return CutUtils.createExample(cut.getClassName(), artifact);
        }).toList();

        // read invocations
        List<SheetInvocation> sheetInvocations = sheetDtos.stream().flatMap(s -> {
            String sheetName = StringUtils.substringBefore(s.getSignature(), "(");

            List<String> invocations = s.getInvocations();
            if(CollectionUtils.isEmpty(invocations)) {
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

        // code analyzers
        boolean jacoco = false;
        boolean mutation = false;
        if(CollectionUtils.isNotEmpty(request.getAnalyzers())) {
            jacoco = request.getAnalyzers().contains("cc");
            mutation = request.getAnalyzers().contains("mt");
        }

        Validate.isTrue(!(jacoco && mutation), "JaCoCo and mutation is not possible at the same time");

        // visitor
        InvocationVisitor invocationVisitor;
        JaCoCoListener jaCoCoListener = new JaCoCoListener();
        if(jacoco) {
            ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
            invocationVisitor = new CompositeInvocationVisitor(
                    Arrays.asList(visitor, jaCoCoListener)); // add jacoco listener

            // set driver
            testDriver.setEnableJaCoCoCoverage(true);
        } else {
            invocationVisitor = new InvocationVisitor();
        }

        // adapters
        int adapters = 1;

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix;

        if(mutation) {
            stimulusResponseMatrix = testDriver.mutateAndRunSheets(stimulusMatrix, adapters, invocationVisitor);
        } else {
            stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, adapters, invocationVisitor);
        }

        // for each cell
        List<SheetSpec> actuationSheetResults = new ArrayList<>(sheetSpecs.size());
        List<SheetSpec> adaptedActuationSheetResults = new ArrayList<>(sheetSpecs.size());
        List<SheetSpec> metricSheetResults = new LinkedList<>();
        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {

            try {
                ExecutedInvocations executedInvocations = cell.getValue();
                LOG.debug("executed invocations\n{}", executedInvocations);

                ActuationSheet actuationSheet = new ActuationSheet();
                actuationSheet.setAdaptedImplementation(cell.getColumnKey());
                actuationSheet.setExecutedInvocations(executedInvocations);

                List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = actuationSheet.toSheetData(gsonMapper);

                de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
                de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);

                actuationSheetData.debug();
                adaptedActuationSheetData.debug();

                LOG.info("JSON actuationSheet\n{}", actuationSheetData.toJsonl());
                LOG.info("JSON adaptedActuationSheet\n{}", adaptedActuationSheetData.toJsonl());

                ParsedSheet parsedSheet = actuationSheet.getExecutedInvocations().getInvocations().getParsedSheet();

                SheetSpec actuationSheetResult = new SheetSpec();

                // FIXME set more fields
                TestInvocation testInvocation = stimulusMatrix.get(cell.getRowKey(), cell.getColumnKey().getAdaptee());
                String sig = cell.getRowKey().getName() + ":" + parsedSheet.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";

                actuationSheetResult.setSignature(sig);
                actuationSheetResult.setInterfaceSpecification(parsedSheet.getInterfaceSpecification().toLQL());
                actuationSheetResult.setBody(actuationSheetData.toJsonl());
                actuationSheetResult.setImplementation("ABSTRACTION");

                actuationSheetResults.add(actuationSheetResult);

                SheetSpec adaptedActuationSheetResult = new SheetSpec();
                adaptedActuationSheetResult.setSignature(sig);
                adaptedActuationSheetResult.setInterfaceSpecification(parsedSheet.getInterfaceSpecification().toLQL());
                adaptedActuationSheetResult.setBody(adaptedActuationSheetData.toJsonl());

                if(mutation && !actuationSheet.getAdaptedImplementation().getAdaptee().getVariantId().equals("original")) {
                    PitestContainer pitestContainer = (PitestContainer) actuationSheet.getAdaptedImplementation().getAdaptee().getProject().getContainer();
                    LOG.debug("Mutant {}", pitestContainer.getMutant().getDetails());

                    String mutantId = pitestContainer.getMutant().getDetails().getId().toString();

                    AdaptedImplementation adaptedImplementation = actuationSheet.getAdaptedImplementation();
                    adaptedActuationSheetResult.setImplementation(adaptedImplementation.getAdaptee().getFullId() + "|" + adaptedImplementation.getAdapterId() + "|" + mutantId);
                } else {
                    AdaptedImplementation adaptedImplementation = actuationSheet.getAdaptedImplementation();
                    adaptedActuationSheetResult.setImplementation(adaptedImplementation.getAdaptee().getFullId() + "|" + adaptedImplementation.getAdapterId());
                }

                adaptedActuationSheetResults.add(adaptedActuationSheetResult);

                // jacoco reports
                if(jacoco) {
                    StimulusResponseMatrix<String, AdaptedImplementation, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet> jacocoSrm = jaCoCoListener.getStimulusResponseMatrix();
                    Map<String, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet> metricSheets = jacocoSrm.getTable().column(cell.getColumnKey());

                    for(String metricId : metricSheets.keySet()) {
                        de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet metricSheet = metricSheets.get(metricId);

                        SheetSpec metricSheetResult = new SheetSpec();
                        metricSheetResult.setSignature(metricId);
                        metricSheetResult.setInterfaceSpecification(parsedSheet.getInterfaceSpecification().toLQL());
                        metricSheetResult.setBody(metricSheet.toJsonl());
                        metricSheetResult.setImplementation(adaptedActuationSheetResult.getImplementation());
                        metricSheetResults.add(metricSheetResult);
                    }
                }
            } catch (Throwable e) {
                LOG.warn("execution failed", e);

                throw new RuntimeException(e);
            }
        }

        TestResult testResult = new TestResult();
        testResult.setClassUnderTestSpec(null); // FIXME
        testResult.setExecutionId("FIXME"); // FIXME
        testResult.setStatus("SUCCESS"); // FIXME
        testResult.setActuationSheets(actuationSheetResults);
        testResult.setAdaptedActuationSheets(adaptedActuationSheetResults);
        testResult.setMetricSheets(metricSheetResults);

        testResults.add(testResult);

        // TODO create SRMs per metric (cf. slides)
        if(jacoco) {
            // option 1) either via listener or set to invocations .. but here on the level of a set of actuation sheets
            // option 2) return an SRH: collection of SRMs as tables + send queries as part of answers so that the client can query the tabular data -- like a
        }

        SheetResponse sheetResponse = new SheetResponse();
        sheetResponse.setTestResults(testResults);
        sheetResponse.setStatus("SUCCESS"); // FIXME
        sheetResponse.setExecutionId("FIXME"); // FIXME

        return sheetResponse;
    }
}
