package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ParsedRow {

    private final ParsedSheet parsedSheet;

    private List<ParsedCell> cells;

    public ParsedRow(ParsedSheet parsedSheet) {
        this.parsedSheet = parsedSheet;
    }

    public List<ParsedCell> getCells() {
        return cells;
    }

    public void setCells(List<ParsedCell> cells) {
        this.cells = cells;
    }

    public ParsedCell getOutput() {
        return cells.get(0);
    }

    public ParsedCell getOperation() {
        return cells.get(1);
    }

    public List<ParsedCell> getInputs() {
        return cells.subList(2, cells.size());
    }

    public ParsedSheet getParsedSheet() {
        return parsedSheet;
    }
}
