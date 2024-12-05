package de.uni_mannheim.swt.lasso.sheets.service.driver;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class LocalSimpleTestDriver implements TestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSimpleTestDriver.class);

    @Override
    public SheetResponse execute(SheetRequest request) throws IOException {
        List<TestResult> testResults = new ArrayList<>(request.getClassesUnderTest().size());

        List<SheetSpec> sheetSpecs = request.getSheets();
        String interfaceSpecification = sheetSpecs.get(0).getInterfaceSpecification();

        // parse stimulus sheets
        List<ParsedSheet> parsedSheets = SSNTestDriver.parseSheets(sheetSpecs.stream().map(SheetSpec::getBody).collect(Collectors.toList()));

        // for each CUT
        for(ClassUnderTestSpec classUnderTestSpec : request.getClassesUnderTest()) {

            SSNTestDriver testDriver = new SSNTestDriver();
            //ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
            GsonMapper gsonMapper = new GsonMapper();
            InvocationVisitor visitor = new InvocationVisitor();

            List<SheetSpec> actuationSheetResults = new ArrayList<>(sheetSpecs.size());
            List<SheetSpec> adaptedActuationSheetResults = new ArrayList<>(sheetSpecs.size());

            List<ActuationSheet> actuationSheets = testDriver.runSheet(parsedSheets, interfaceSpecification, classUnderTestSpec.getClassName(), classUnderTestSpec.getArtifacts(), 1, visitor);

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

                    actuationSheetResults.add(actuationSheetResult);

                    SheetSpec adaptedActuationSheetResult = new SheetSpec();
                    adaptedActuationSheetResult.setName(actuationSheet.getExecutedInvocations().getInvocations().getParsedSheet().getName());
                    adaptedActuationSheetResult.setInterfaceSpecification(interfaceSpecification);
                    adaptedActuationSheetResult.setBody(adaptedActuationSheetData.toJsonl());

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

        SheetResponse sheetResponse = new SheetResponse();
        sheetResponse.setTestResults(testResults);
        sheetResponse.setStatus("SUCCESS"); // FIXME
        sheetResponse.setExecutionId("FIXME"); // FIXME

        return sheetResponse;
    }
}
