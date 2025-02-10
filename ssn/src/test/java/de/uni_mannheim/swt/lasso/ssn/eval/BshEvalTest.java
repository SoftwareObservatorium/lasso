package de.uni_mannheim.swt.lasso.ssn.eval;

import de.uni_mannheim.swt.lasso.ssn.ParsedCell;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;
import de.uni_mannheim.swt.lasso.ssn.SSNParser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author Marcus Kessel
 */
public class BshEvalTest {

    @Test
    public void test_remove_generics() {
        // works with Snapshot 3.0
        BshEval bshEval = new BshEval();
        assertEquals("java.util.ArrayList", bshEval.removeGenerics("java.util.ArrayList<java.lang.String>")); // single
        assertEquals("java.util.ArrayList", bshEval.removeGenerics("java.util.ArrayList<java.util.ArrayList<java.lang.String>>")); // nested
        assertEquals("HashMap", bshEval.removeGenerics("HashMap<ArrayList<java.lang.String>, ArrayList<java.lang.String>>")); // nested

        assertEquals("""
/* LQL */
class Problem{
Problem(){}
java.util.ArrayList sortSublists(java.util.ArrayList arg0){}
}
                """, bshEval.removeGenerics("""
/* LQL */
class Problem{
Problem(){}
java.util.ArrayList<java.util.ArrayList<java.lang.String>> sortSublists(java.util.ArrayList<java.util.ArrayList<java.lang.String>> arg0){}
}
                """));
    }

    @Test
    public void test_nested_generics() throws EvalException {
        // works with Snapshot 3.0
        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval("new java.util.ArrayList<java.util.ArrayList<java.lang.String>>()");

        assertTrue(obj instanceof ArrayList);
        assertNotNull(obj);

        System.out.println(obj);
    }

    @Test
    public void test_generics() throws EvalException {
        // works with Snapshot 3.0
        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval("new java.util.HashMap<String, String>()");

        assertTrue(obj instanceof HashMap);
        assertNotNull(obj);

        System.out.println(obj);
    }

    @Test
    public void test_list_of_tuples() throws EvalException {
        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval("new java.util.ArrayList<org.javatuples.Pair<java.lang.Long, java.lang.Long>>(java.util.Arrays.asList((org.javatuples.Pair<Long, Long>) org.javatuples.Pair.with(2l, 7l), (org.javatuples.Pair<Long, Long>) org.javatuples.Pair.with(2l, 6l), (org.javatuples.Pair<Long, Long>) org.javatuples.Pair.with(1l, 8l), (org.javatuples.Pair<Long, Long>) org.javatuples.Pair.with(4l, 9l)))");

        assertTrue(obj instanceof ArrayList);
        assertNotNull(obj);

        System.out.println(obj);
    }

    @Test
    public void test_string_array() throws EvalException {
        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval("new String[]{\"Hello\", \"World\"}");

        assertTrue(obj instanceof String[]);
        assertNotNull(obj);

        System.out.println(obj);
    }

    @Test
    public void test_bytearray() throws IOException, EvalException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
                """;
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        ParsedCell cell = parsedSheet.resolve("D2");

        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval(cell.getNodeValue().asText());

        assertTrue(obj instanceof byte[]);
        assertNotNull(obj);

        System.out.println(obj);
    }

    @Test
    public void test_map() throws IOException, EvalException {
        @Language("jsonl")
        String ssnJsonlStr = """
            {"cells":{"B1":"create","C1":"Problem"}}
            {"cells":{"A1":"true","B2":"checkDictCase","C2":"A1","D2":"new java.util.HashMap<String, String>(java.util.Map.of(\\"STATE\\", \\"NC\\", \\"ZIP\\", \\"12345\\"))"}}
            """;
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        ParsedCell cell = parsedSheet.resolve("D2");

        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval(cell.getNodeValue().asText());

        System.out.println(obj);
    }
}
