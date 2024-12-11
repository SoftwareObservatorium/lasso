package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.SheetSignature;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class LQLUtilsTest {

    @Test
    public void test_lqlToList() throws IOException {
        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;

        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(lql);

        assertEquals(1, interfaceSpecificationMap.size());
        assertTrue(interfaceSpecificationMap.containsKey("Stack"));
    }

    @Test
    public void test_lqlToMap() throws IOException {
        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;

        List<InterfaceSpecification> interfaceSpecificationList = LQLUtils.lqlToList(lql);

        assertEquals(1, interfaceSpecificationList.size());
        assertEquals("Stack", interfaceSpecificationList.get(0).getClassName());
    }

    @Test
    public void test_lqlToSheetSignature() throws IOException {
        String methodLql = "testSum(p1=int,p2=int,p3=int)";

        SheetSignature sheetSignature = LQLUtils.lqlToSheetSignature(methodLql);

        assertEquals(3, sheetSignature.getParameterSize());
        assertEquals("testSum", sheetSignature.getName());
        assertEquals(methodLql, sheetSignature.toLQL());
    }
}
