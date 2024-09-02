package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve;

/**
 *
 * @author Marcus Kessel
 */
public class SheetResolver {

    public static boolean isCellReference(String cellReference) {
        if (!cellReference.matches("[A-Z]+[0-9]+")) {
            return false;
        }

        return true;
    }

    /**
     * Resolves a cell reference into a pair of integers representing the row and column indices.
     *
     * @param cellReference The cell reference to resolve (e.g. "A1", "B3", etc.)
     * @return An array containing the row index and column index, respectively
     */
    public static int[] resolveCellReference(String cellReference) {
        if (!cellReference.matches("[A-Z]+[0-9]+")) {
            throw new IllegalArgumentException("Invalid cell reference: " + cellReference);
        }

        String colStr = cellReference.replaceAll("[0-9]+", "");
        String rowStr = cellReference.replaceAll("[A-Z]+", "");

        int col = getColIndex(colStr);
        int row = Integer.parseInt(rowStr);

        return new int[] {row - 1, col - 1};
    }

    private static int getColIndex(String columnStr) {
        long result = 0;
        for (int i = 0; i < columnStr.length(); i++) {
            char c = Character.toUpperCase(columnStr.charAt(i));
            if (c >= 'A' && c <= 'Z') {
                result *= 26;
                result += c - 'A' + 1;
            } else {
                throw new IllegalArgumentException("Invalid Excel column reference: " + columnStr);
            }
        }
        return (int) result;
    }
}
