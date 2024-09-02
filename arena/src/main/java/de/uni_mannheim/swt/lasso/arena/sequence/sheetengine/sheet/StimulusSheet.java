package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;

import java.util.Map;

/**
 * stimulus sequence sheets only define the invocations to be made on the software component under test when
 * the sheet is executed (i.e., they specify a sequence of statements ready for execution)
 *
 * @author Marcus Kessel
 */
public class StimulusSheet {

    /**
     * The interface specification of a sequence sheet
     */
    private final Map<String, InterfaceSpecification> interfaceSpecifications;

    /**
     * Parse sequence sheet
     */
    private final ParsedSheet parsedSheet;

    public StimulusSheet(Map<String, InterfaceSpecification> interfaceSpecifications, ParsedSheet parsedSheet) {
        this.interfaceSpecifications = interfaceSpecifications;
        this.parsedSheet = parsedSheet;
    }

    public Map<String, InterfaceSpecification> getInterfaceSpecifications() {
        return interfaceSpecifications;
    }

    public String getName() {
        return parsedSheet.getName();
    }

    public ParsedSheet getParsedSheet() {
        return parsedSheet;
    }
}
