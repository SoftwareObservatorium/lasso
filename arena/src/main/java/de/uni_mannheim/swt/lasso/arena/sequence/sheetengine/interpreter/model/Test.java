package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;

/**
 * A test in an SRM
 *
 * @author Marcus Kessel
 */
public class Test {

    private final String name;
    private final ParsedSheet parsedSheet;

    private final SheetSignature signature;
    private InterfaceSpecification interfaceSpecification;

    public Test(String name, ParsedSheet parsedSheet, SheetSignature signature) {
        this.name = name;
        this.parsedSheet = parsedSheet;
        this.signature = signature;
    }

    public Test(String name, Test test) {
        this(name, test.getParsedSheet(), test.getSignature());

        this.interfaceSpecification = test.getInterfaceSpecification();
    }

    public String getName() {
        return name;
    }

    public ParsedSheet getParsedSheet() {
        return parsedSheet;
    }

    public SheetSignature getSignature() {
        return signature;
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(InterfaceSpecification interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }
}
