package de.uni_mannheim.swt.lasso.ssn;

import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ParsedSheet {

    private List<ParsedRow> rows = new ArrayList<>();

    private Sheet sheet;

    public List<ParsedRow> getRows() {
        return rows;
    }

    public void setRows(List<ParsedRow> rows) {
        this.rows = rows;
    }

    public ParsedCell resolve(String cellRef) {
        // A -> row index (0)
        // number -> cell index (0)
        int[] reference = SheetResolver.resolveCellReference(cellRef);

        return rows.get(reference[0]).getCells().get(reference[1]);
    }

    public Sheet getSheet() {
        return sheet;
    }

    public void setSheet(Sheet sheet) {
        this.sheet = sheet;
    }
}
