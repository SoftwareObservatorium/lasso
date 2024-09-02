package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author Marcus Kessel
 */
public class ParsedCell {

    private final ParsedRow parsedRow;
    private final String key;
    private final JsonNode node;

    public ParsedCell(ParsedRow parsedRow, String key, JsonNode node) {
        this.parsedRow = parsedRow;
        this.key = key;
        this.node = node;
    }

    public boolean isValueReference() {
        if(isString() && isValidCellReference(getNodeValue().textValue())) {
            return true;
        }

        return false;
    }

    public boolean isString() {
        return getNodeValue().isTextual();
    }

    public boolean isNumber() {
        return getNodeValue().isNumber();
    }

    public JsonNode getNodeValue() {
        return node;
    }

    public static boolean isValidCellReference(String cell) {
        return SheetResolver.isCellReference(cell);
    }

    public ParsedRow getParsedRow() {
        return parsedRow;
    }

    public String getKey() {
        return key;
    }
}
