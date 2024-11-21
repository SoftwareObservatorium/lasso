package de.uni_mannheim.swt.lasso.sheets.service.driver;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.SSNTestDriver;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class LocalSimpleTestDriver implements TestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSimpleTestDriver.class);

    @Override
    public SheetResponse execute(SheetRequest request) throws IOException {
        List<TestResult> testResults = new ArrayList<>(request.getClassesUnderTest().size());

        // for each CUT
        for(ClassUnderTestSpec classUnderTestSpec : request.getClassesUnderTest()) {
            List<SheetSpec> sheetSpecs = request.getSheets();

            SSNTestDriver testDriver = new SSNTestDriver();
            ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());

            List<SheetSpec> actuationSheetResults = new ArrayList<>(sheetSpecs.size());
            List<SheetSpec> adaptedActuationSheetResults = new ArrayList<>(sheetSpecs.size());

            for(SheetSpec sheetSpec : sheetSpecs) {
                try {
                    ExecutedInvocations executedInvocations = testDriver.runSheet(sheetSpec.getBody(), sheetSpec.getInterfaceSpecification(), classUnderTestSpec.getClassName(), classUnderTestSpec.getArtifacts(), 1, visitor);

                    LOG.debug("executed invocations\n{}", executedInvocations);

                    Sheet<Integer, Integer, String> actuationSheet = visitor.getActuationSheet();
                    Sheet<Integer, Integer, String> adaptedActuationSheet = visitor.getAdaptedActuationSheet();

                    actuationSheet.debug();
                    adaptedActuationSheet.debug();

                    LOG.info("JSON actuationSheet\n{}", actuationSheet.toJsonl());
                    LOG.info("JSON adaptedActuationSheet\n{}", adaptedActuationSheet.toJsonl());

                    SheetSpec actuationSheetResult = new SheetSpec();
                    actuationSheetResult.setName(sheetSpec.getName());
                    actuationSheetResult.setInterfaceSpecification(sheetSpec.getInterfaceSpecification());
                    actuationSheetResult.setBody(actuationSheet.toJsonl());

                    actuationSheetResults.add(actuationSheetResult);

                    SheetSpec adaptedActuationSheetResult = new SheetSpec();
                    adaptedActuationSheetResult.setName(sheetSpec.getName());
                    adaptedActuationSheetResult.setInterfaceSpecification(sheetSpec.getInterfaceSpecification());
                    adaptedActuationSheetResult.setBody(adaptedActuationSheet.toJsonl());

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
