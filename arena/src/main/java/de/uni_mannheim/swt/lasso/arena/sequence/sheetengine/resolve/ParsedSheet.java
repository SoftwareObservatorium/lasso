package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ParsedSheet {

    private String name;
    private List<ParsedRow> rows = new ArrayList<>();

    public List<ParsedRow> getRows() {
        return rows;
    }

    public void setRows(List<ParsedRow> rows) {
        this.rows = rows;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParsedCell resolve(String cellRef) {
        // A -> row index (0)
        // number -> cell index (0)
        int[] reference = SheetResolver.resolveCellReference(cellRef);

        return rows.get(reference[0]).getCells().get(reference[1]);
    }
}
