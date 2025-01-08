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
import java.util.*;

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
            int r = 0;
            for(JsonNode rowNode : rowNodes) {
                ParsedRow parsedRow = processRow(parsedSheet, rowNode, r++);
                parsedRows.add(parsedRow);
            }
        }

//        // interface
//        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(interfaceLql);
//        // FIXME for all CUTs .. here only one
//        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();
//        parsedSheet.setInterfaceSpecification(interfaceSpecificationMap.get(faName));

        return parsedSheet;
    }

    ParsedRow processRow(ParsedSheet parsedSheet, JsonNode node, int rowId) throws IOException {
        ParsedRow parsedRow = new ParsedRow(parsedSheet);
        List<ParsedCell> parsedCells = new ArrayList<>();
        parsedRow.setCells(parsedCells);

        JsonNode cells = node.get("cells");
        // assume JSON Object
        if(cells.isObject()) {
            List<Integer> colIds = new LinkedList<>();

            cells.fields().forEachRemaining(e -> {
                // row/column
                int[] reference = SheetResolver.resolveCellReference(e.getKey());
                // add to row
                parsedCells.add(new ParsedCell(parsedRow, e.getKey(), e.getValue()));
                //parsedCells.add(new ParsedCell(parsedRow, e.getKey(), e.getValue()));

                colIds.add(reference[1]);
            });

            // fill up row with missing cells
            int max = colIds.stream().mapToInt(i -> i).max().getAsInt();
            for(int i = 0; i < max; i++) {
                if(!colIds.contains(i)) {
                    parsedCells.add(i, new ParsedCell(parsedRow, SheetResolver.toColumnLabel(i)+SheetResolver.toRowLabel(rowId), mapper.createObjectNode()));
                }
            }

            // validate
            Validate.notNull(parsedRow.getOutput(), "No output cell defined");
            Validate.notNull(parsedRow.getOperation(), "No operation cell defined");
            Validate.notEmpty(parsedRow.getInputs(), "No input cells defined");

            return parsedRow;
        }

        throw new IOException("No cells found");
    }
}
