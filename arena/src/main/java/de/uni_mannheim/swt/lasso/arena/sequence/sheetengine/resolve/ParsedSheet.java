package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.SheetSignature;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ParsedSheet {

    private SheetSignature signature;
    private List<ParsedRow> rows = new ArrayList<>();

    private InterfaceSpecification interfaceSpecification;

    public List<ParsedRow> getRows() {
        return rows;
    }

    public void setRows(List<ParsedRow> rows) {
        this.rows = rows;
    }

    public String getName() {
        return signature.getName();
    }

    public ParsedCell resolve(String cellRef) {
        // A -> row index (0)
        // number -> cell index (0)
        int[] reference = SheetResolver.resolveCellReference(cellRef);

        return rows.get(reference[0]).getCells().get(reference[1]);
    }

    public SheetSignature getSignature() {
        return signature;
    }

    public void setSignature(SheetSignature signature) {
        this.signature = signature;
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(InterfaceSpecification interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }
}
