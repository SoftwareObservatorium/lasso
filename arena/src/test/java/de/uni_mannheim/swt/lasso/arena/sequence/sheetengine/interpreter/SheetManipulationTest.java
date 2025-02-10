package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class SheetManipulationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SheetManipulationTest.class);

    @Test
    public void test_sheet() {
        int col = 0;
        Sheet<Integer, Integer, String> sheet1 = new Sheet<>();
        sheet1.put(0, col, "A");
        sheet1.put(1, col, "B");
        sheet1.put(2, col, "C");

        Sheet<Integer, Integer, String> sheet2 = new Sheet<>();
        sheet2.put(0, col, "C");
        sheet2.put(1, col, "B");
        sheet2.put(2, col, "A");

        Sheet<Integer, Integer, String> sheet3 = new Sheet<>();
        sheet3.put(0, col, "A");
        sheet3.put(1, col, "B");
        sheet3.put(2, col, "C");

        assertNotEquals(sheet1, sheet2);
        assertNotEquals(sheet2, sheet3);
        assertEquals(sheet1, sheet3);

        assertFalse(sheet1.isEquivalentColumn(sheet2, col));
        assertFalse(sheet2.isEquivalentColumn(sheet3, col));
        assertTrue(sheet1.isEquivalentColumn(sheet3, col));
    }
}
