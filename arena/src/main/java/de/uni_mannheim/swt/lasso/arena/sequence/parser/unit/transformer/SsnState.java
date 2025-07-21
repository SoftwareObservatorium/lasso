package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.transformer;
import com.google.gson.Gson;
import com.github.javaparser.ast.expr.Expression;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * State during transformation
 *
 * @author Marcus Kessel
 */
public class SsnState {
    public int getCurrentRow() {
        return currentRow;
    }

    private int currentRow = 0;
    // Maps a Java variable name to its SSN cell reference (e.g., "stack" -> "A1")
    private final Map<String, String> varToCellMap = new HashMap<>();
    // Maps a Java variable to a literal value (e.g., "expected" -> "\"Hello\"")
    private final Map<String, String> varToValueMap = new HashMap<>();
    // Stores the cells for each row. Key is row number.
    private final Map<Integer, Map<String, Object>> rows = new LinkedHashMap<>();

    public int newRow() {
        currentRow++;
        rows.put(currentRow, new LinkedHashMap<>());
        // By default, the output cell is empty
        addCell(currentRow, "A", Map.of()); // compliant with {"A1": {}} for empty cells
        return currentRow;
    }

    public void addCell(int rowNum, String col, Object value) {
        String cellKey = col + rowNum;
        rows.computeIfAbsent(rowNum, k -> new LinkedHashMap<>()).put(cellKey, value);
    }

    // Updates the 'A' column of a given cell reference (e.g., "A2")
    public void updateCell(String cellRef, Object newValue) {
        if (cellRef == null || cellRef.length() < 2) return;

        char col = cellRef.charAt(0);
        int rowNum = Integer.parseInt(cellRef.substring(1));

        if (rows.containsKey(rowNum)) {
            rows.get(rowNum).put(cellRef, newValue);
        }
    }

    public void mapVariableToCell(String varName, String cell) {
        this.varToCellMap.put(varName, cell);
    }

    public void mapVariableToValue(String varName, String value) {
        this.varToValueMap.put(varName, value);
    }

    public String getCellForVariable(String varName) {
        return this.varToCellMap.get(varName);
    }

    /**
     * retrieve a variable's literal value, if it has one.
     *
     * @param varName
     * @return
     */
    public String getValueForVariable(String varName) {
        return this.varToValueMap.get(varName);
    }

    public String resolveExpression(Expression expr) {
        String exprStr = expr.toString();
        String cell = getCellForVariable(exprStr);
        if (cell != null) {
            return cell;
        }
        String value = getValueForVariable(exprStr);
        if (value != null) {
            return value;
        }
        return exprStr;
    }

    public String toJsonL() {
        Gson gson = new Gson();
        return rows.values().stream()
                .map(row -> {
                    Map<String, Map<String, Object>> wrapper = new HashMap<>();
                    wrapper.put("cells", row);
                    return gson.toJson(wrapper);
                })
                .collect(Collectors.joining("\n"));
    }
}
