package de.uni_mannheim.swt.lasso.sheets.service;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.SSNTestDriver;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class SheetsManager {

    private static final Logger LOG = LoggerFactory.getLogger(SheetsManager.class);

    public SheetResponse execute(SheetRequest request, UserInfo userInfo) {
        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());

        ClassUnderTestSpec classUnderTestSpec = request.getClassUnderTest();

        List<SheetSpec> sheetSpecs = request.getSheets();

        // FIXME support for more sheets
        SheetSpec sheetSpec = sheetSpecs.get(0);

        SSNTestDriver testDriver = new SSNTestDriver();

        try {
            ExecutedInvocations executedInvocations = testDriver.runSheet(sheetSpec.getBody(), sheetSpec.getInterfaceSpecification(), classUnderTestSpec.getClassName(), 1, visitor);

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

            SheetSpec adaptedActuationSheetResult = new SheetSpec();
            adaptedActuationSheetResult.setName(sheetSpec.getName());
            adaptedActuationSheetResult.setInterfaceSpecification(sheetSpec.getInterfaceSpecification());
            adaptedActuationSheetResult.setBody(adaptedActuationSheet.toJsonl());

            SheetResponse sheetResponse = new SheetResponse();
            sheetResponse.setStatus("SUCCESS"); // FIXME
            sheetResponse.setExecutionId("FIXME"); // FIXME
            sheetResponse.setActuationSheets(Arrays.asList(actuationSheetResult));
            sheetResponse.setAdaptedActuationSheets(Arrays.asList(adaptedActuationSheetResult));

            return sheetResponse;
        } catch (Throwable e) {
            LOG.warn("execution failed", e);

            throw new RuntimeException(e);
        }
    }
}
