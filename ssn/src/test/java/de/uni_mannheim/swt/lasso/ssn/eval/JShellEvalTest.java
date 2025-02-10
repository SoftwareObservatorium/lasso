package de.uni_mannheim.swt.lasso.ssn.eval;

import de.uni_mannheim.swt.lasso.ssn.ParsedCell;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;
import de.uni_mannheim.swt.lasso.ssn.SSNParser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 *
 * @author Marcus Kessel
 */
public class JShellEvalTest {

    @Test
    public void test_bytearray() throws IOException, EvalException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
                """;
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        ParsedCell cell = parsedSheet.resolve("D2");

        JShellEval eval = new JShellEval();
        eval.setClassLoader(Thread.currentThread().getContextClassLoader());
        Object obj = eval.eval(cell.getNodeValue().asText());

        assertNotNull(obj);
        assertTrue(obj instanceof byte[]);

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

        JShellEval eval = new JShellEval();
        Object obj = eval.eval(cell.getNodeValue().asText());

        assertNotNull(obj);
        assertTrue(obj instanceof HashMap);

        System.out.println(obj);
    }
}
