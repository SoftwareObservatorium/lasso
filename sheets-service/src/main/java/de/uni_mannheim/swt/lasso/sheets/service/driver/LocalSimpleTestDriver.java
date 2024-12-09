package de.uni_mannheim.swt.lasso.sheets.service.driver;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.PitestContainer;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.JaCoCoListener;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        String interfaceSpecification = sheetSpecs.get(0).getInterfaceSpecification();

        // parse stimulus sheets
        List<ParsedSheet> parsedSheets = SSNTestDriver.parseSheets(sheetSpecs.stream().map(SheetSpec::getBody).collect(Collectors.toList()));

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
        if(jacoco) {
            ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
            invocationVisitor = new CompositeInvocationVisitor(
                    Arrays.asList(visitor, new JaCoCoListener())); // add jacoco listener

            // set driver
            testDriver.setEnableJaCoCoCoverage(true);
        } else {
            invocationVisitor = new InvocationVisitor();
        }

        // adapters
        int adapters = 1;

        // for each CUT
        for(ClassUnderTestSpec classUnderTestSpec : request.getClassesUnderTest()) {
            List<SheetSpec> actuationSheetResults = new ArrayList<>(sheetSpecs.size());
            List<SheetSpec> adaptedActuationSheetResults = new ArrayList<>(sheetSpecs.size());

            List<ActuationSheet> actuationSheets;
            if(mutation) {
                actuationSheets = testDriver.mutateAndRunSheets(parsedSheets, interfaceSpecification, classUnderTestSpec.getClassName(), classUnderTestSpec.getArtifacts(), adapters, invocationVisitor);
            } else {
                actuationSheets = testDriver.runSheet(parsedSheets, interfaceSpecification, classUnderTestSpec.getClassName(), classUnderTestSpec.getArtifacts(), adapters, invocationVisitor);
            }

            for(ActuationSheet actuationSheet : actuationSheets) {
                try {
                    ExecutedInvocations executedInvocations = actuationSheet.getExecutedInvocations();
                    LOG.debug("executed invocations\n{}", executedInvocations);

                    List<Sheet<Integer, Integer, String>> sheets = actuationSheet.toSheetData(gsonMapper);

                    Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
                    Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);

                    actuationSheetData.debug();
                    adaptedActuationSheetData.debug();

                    LOG.info("JSON actuationSheet\n{}", actuationSheetData.toJsonl());
                    LOG.info("JSON adaptedActuationSheet\n{}", adaptedActuationSheetData.toJsonl());

                    SheetSpec actuationSheetResult = new SheetSpec();
                    actuationSheetResult.setName(actuationSheet.getExecutedInvocations().getInvocations().getParsedSheet().getName());
                    actuationSheetResult.setInterfaceSpecification(interfaceSpecification);
                    actuationSheetResult.setBody(actuationSheetData.toJsonl());
                    actuationSheetResult.setImplementation("ABSTRACTION");

                    actuationSheetResults.add(actuationSheetResult);

                    SheetSpec adaptedActuationSheetResult = new SheetSpec();
                    adaptedActuationSheetResult.setName(actuationSheet.getExecutedInvocations().getInvocations().getParsedSheet().getName());
                    adaptedActuationSheetResult.setInterfaceSpecification(interfaceSpecification);
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
                } catch (Throwable e) {
                    LOG.warn("execution failed", e);

                    throw new RuntimeException(e);
                }
            }

            TestResult testResult = new TestResult();
            testResult.setClassUnderTestSpec(classUnderTestSpec);
            testResult.setExecutionId("FIXME"); // FIXME
            testResult.setStatus("SUCCESS"); // FIXME
            testResult.setActuationSheets(actuationSheetResults);
            testResult.setAdaptedActuationSheets(adaptedActuationSheetResults);

            testResults.add(testResult);
        }

        // TODO create SRMs per metric (cf. slides)
        if(jacoco) {
            // option 1) either via listener or set to invocations .. but here on the level of a set of actuation sheets
            // option 2) return an SRH: collection of SRMs as tables + send queries as part of answers so that the client can query the tabular data
        }

        SheetResponse sheetResponse = new SheetResponse();
        sheetResponse.setTestResults(testResults);
        sheetResponse.setStatus("SUCCESS"); // FIXME
        sheetResponse.setExecutionId("FIXME"); // FIXME

        return sheetResponse;
    }
}
