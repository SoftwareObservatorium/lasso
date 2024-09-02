package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser based on Sequence Sheet Notation (SSN)
 *
 * @author Marcus Kessel
 */
public class SSNParser {

    /**
     * Parse JSONL
     *
     * @param jsonlStr
     * @return
     * @throws IOException
     */
    public ParsedSheet parseJsonl(String jsonlStr) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ParsedSheet parsedSheet = new ParsedSheet();
        try (MappingIterator<JsonNode> it = mapper.readerFor(JsonNode.class)
                .readValues(jsonlStr)) {
            List<JsonNode> rowNodes = it.readAll();

            // read sheet name
            JsonNode firstRowNode = rowNodes.get(0);
            if(firstRowNode.has("sheet")) {
                parsedSheet.setName(firstRowNode.get("sheet").textValue());
            }

            List<ParsedRow> parsedRows = new ArrayList<>(rowNodes.size());
            parsedSheet.setRows(parsedRows);
            for(JsonNode rowNode : rowNodes) {
                ParsedRow parsedRow = processRow(parsedSheet, rowNode);
                parsedRows.add(parsedRow);
            }
        }

        return parsedSheet;
    }

    ParsedRow processRow(ParsedSheet parsedSheet, JsonNode node) throws IOException {
        ParsedRow parsedRow = new ParsedRow(parsedSheet);
        List<ParsedCell> parsedCells = new ArrayList<>();
        parsedRow.setCells(parsedCells);

        JsonNode cells = node.get("cells");
        // assume JSON Object
        if(cells.isObject()) {
            cells.fields().forEachRemaining(e -> {
                parsedCells.add(new ParsedCell(parsedRow, e.getKey(), e.getValue()));
            });
            return parsedRow;
        }

        throw new IOException("No cells found");
    }
}
