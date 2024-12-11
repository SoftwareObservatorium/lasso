package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;

/**
 * A test in an SRM
 *
 * @author Marcus Kessel
 */
public class Test {

    private String name;
    private ParsedSheet parsedSheet;

    public Test(String name, ParsedSheet parsedSheet) {
        this.name = name;
        this.parsedSheet = parsedSheet;
    }

    public String getName() {
        return name;
    }

    public ParsedSheet getParsedSheet() {
        return parsedSheet;
    }
}
