package de.uni_mannheim.swt.lasso.sheets.service;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.SSNTestDriver;
import de.uni_mannheim.swt.lasso.sheets.service.driver.LocalSimpleTestDriver;
import de.uni_mannheim.swt.lasso.sheets.service.driver.TestDriver;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcus Kessel
 */
public class SheetsManager {

    private static final Logger LOG = LoggerFactory.getLogger(SheetsManager.class);

    private final TestDriver testDriver;

    public SheetsManager(TestDriver testDriver) {
        this.testDriver = testDriver;
    }

    public SheetResponse execute(SheetRequest request, UserInfo userInfo) {
        // FIXME decide which driver
        try {
            SheetResponse sheetResponse = testDriver.execute(request);

            return sheetResponse;
        } catch (Throwable e) {
            LOG.warn("execution failed", e);

            throw new RuntimeException(e);
        }
    }

    public InterfaceSpecificationResponse toLql(ClassUnderTestSpec classUnderTestSpec, UserInfo userInfo) {
        SSNTestDriver testDriver = new SSNTestDriver();

        try {
            InterfaceSpecification interfaceSpecification = testDriver.toLQL(classUnderTestSpec.getClassName(), classUnderTestSpec.getArtifacts());

            InterfaceSpecificationResponse response = new InterfaceSpecificationResponse();
            response.setInterfaceSpecification(interfaceSpecification.toLQL());

            return response;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
