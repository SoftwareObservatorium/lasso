package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedCell;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 *
 * @author Marcus Kessel
 */
public class BshEvalTest {

    @Test
    public void test_bytearray() throws IOException, EvalException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "new byte[]{72,101,108,108,111,32,87,111,114,108,100,33}"}}
                """;
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr, "test()", "Base64{encode(byte[])->byte[]}");

        ParsedCell cell = parsedSheet.resolve("D2");

        //assertTrue(cell.getNodeValue().isArray());

        //System.out.println(cell.);


        BshEval bshEval = new BshEval();
        Object obj = bshEval.eval("\"Hello World!\".getBytes()");

        System.out.println(obj);
    }
}
