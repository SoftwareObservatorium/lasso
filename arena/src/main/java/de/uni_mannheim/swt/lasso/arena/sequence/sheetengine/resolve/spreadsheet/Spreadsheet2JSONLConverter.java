package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.spreadsheet;

import org.apache.poi.ss.usermodel.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Simple converter to convert XLSX Spreadsheets to SSN JSON Bodies.
 *
 * @author Marcus Kessel
 */
public class Spreadsheet2JSONLConverter {

    /**
     * Convert Spreadsheet to JSONL
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static String convert(InputStream in) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            // XXX currently assumes one sheet
            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, Object>> rows = new ArrayList<>();

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Map<String, Object> rowData = new LinkedHashMap<>();

                Iterator<Cell> it = row.cellIterator();
                while (it.hasNext()) {
                    Cell cell = it.next();

                    Object cellValue = getCellValue(cell);

                    rowData.put(cell.getAddress().formatAsString(), cellValue);
                }

                rows.add(rowData);
            }

            ObjectMapper objectMapper = new ObjectMapper();

            StringBuilder jsonl = new StringBuilder();
            for (Map<String, Object> row : rows) {
                Map<String, Map<String, Object>> m = new LinkedHashMap<>();
                m.put("cells", row);
                String json = objectMapper.writeValueAsString(m);

                jsonl.append(json);
                jsonl.append("\n");
            }

            return jsonl.toString();
        }
    }

    /**
     * Convert cell types to corresponding SSN types.
     *
     * @param cell
     * @return
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return new Object();
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
//            case BLANK:
//                return new Object();
            default:
                return new Object();
        }
    }
}

