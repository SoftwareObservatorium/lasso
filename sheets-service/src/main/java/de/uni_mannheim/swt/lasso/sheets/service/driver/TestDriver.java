package de.uni_mannheim.swt.lasso.sheets.service.driver;

import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetGenerationRequest;
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetGenerationResponse;
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetRequest;
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetResponse;

import java.io.IOException;

/**
 *
 * @author Marcus Kessel
 */
public interface TestDriver {

    SheetResponse execute(SheetRequest request) throws IOException;

    SheetGenerationResponse generateSheets(SheetGenerationRequest request) throws IOException;
}
