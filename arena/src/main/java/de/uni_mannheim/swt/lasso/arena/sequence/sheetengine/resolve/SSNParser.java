package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.SheetSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto.SheetDto;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser based on Sequence Sheet Notation (SSN) using JSONL
 *
 * @author Marcus Kessel
 */
public class SSNParser {

    private final ObjectMapper mapper;

    public SSNParser() {
        this(createObjectMapper());
    }

    public SSNParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ParsedSheet parseJsonl(SheetDto sheetDto) throws IOException {
        return parseJsonl(sheetDto.getSignature(), sheetDto.getBody(), sheetDto.getInterfaceSpecification());
    }

    /**
     * Parse sheet data.
     *
     * @param bodyJsonl
     * @param signatureLql
     * @param interfaceLql
     * @return
     * @throws IOException
     */
    public ParsedSheet parseJsonl(String bodyJsonl, String signatureLql, String interfaceLql) throws IOException {
        ParsedSheet parsedSheet = new ParsedSheet();

        // parse signature
        Validate.notBlank(signatureLql, "Signature must be not blank");

        SheetSignature signature = LQLUtils.lqlToSheetSignature(signatureLql);
        parsedSheet.setSignature(signature);

        // parse body
        try (MappingIterator<JsonNode> it = mapper.readerFor(JsonNode.class)
                .readValues(bodyJsonl)) {
            List<JsonNode> rowNodes = it.readAll();

            // read sheet name
//            JsonNode firstRowNode = rowNodes.get(0);
//            if(firstRowNode.has("sheet")) {
//                parsedSheet.setName(firstRowNode.get("sheet").textValue());
//            }

            List<ParsedRow> parsedRows = new ArrayList<>(rowNodes.size());
            parsedSheet.setRows(parsedRows);
            for(JsonNode rowNode : rowNodes) {
                ParsedRow parsedRow = processRow(parsedSheet, rowNode);
                parsedRows.add(parsedRow);
            }
        }

        // interface
        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(interfaceLql);
        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();
        parsedSheet.setInterfaceSpecification(interfaceSpecificationMap.get(faName));

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
