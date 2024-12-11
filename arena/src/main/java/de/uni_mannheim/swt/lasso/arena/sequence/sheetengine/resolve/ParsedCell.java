package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

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
        return isString() && isValidCellReference(getNodeValue().textValue());
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

    public boolean isTestParameter() {
        return isString() && StringUtils.startsWith(getNodeValue().textValue(), "?");
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
