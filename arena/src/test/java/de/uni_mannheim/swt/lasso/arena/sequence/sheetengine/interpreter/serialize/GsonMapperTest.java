package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StackEmptyConstructorExample;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class GsonMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(GsonMapperTest.class);

    @Test
    public void test_Stack_empty_constructor() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;

        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
                Arrays.asList(visitor));

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        ExecutedInvocations executedInvocations = testDriver.runSheet(ssnJsonlStr, lql, cutClass, 1, invocationVisitor);

        Sheet<Integer, Integer, String> actuationSheet = visitor.getActuationSheet();
        Sheet<Integer, Integer, String> adaptedActuationSheet = visitor.getAdaptedActuationSheet();

        actuationSheet.debug();
        adaptedActuationSheet.debug();

        assertEquals("""
                0 0 "$CUT@Stack@0"
                0 1 "public Stack()"
                0 2 "Stack"
                1 0 "\\u0027Hello World!\\u0027"
                1 1 "public java.lang.String(java.lang.String)"
                1 2 "java.lang.String"
                1 3 "\\u0027Hello World!\\u0027"
                2 0 "\\u0027Hello World!\\u0027"
                2 1 "java.lang.String Stack.push(java.lang.String)"
                2 2 "$CUT@Stack@0"
                2 3 "\\u0027Hello World!\\u0027"
                3 0 1
                3 1 "int Stack.size()"
                3 2 "$CUT@Stack@0"
                """, actuationSheet.toString());
        assertEquals("""
                0 0 "$CUT@Stack@0"
                0 1 "public de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.examples.StackEmptyConstructorExample()"
                0 2 "Stack"
                1 0 "\\u0027Hello World!\\u0027"
                1 1 "public java.lang.String(java.lang.String)"
                1 2 "java.lang.String"
                1 3 "\\u0027Hello World!\\u0027"
                2 0 "\\u0027Hello World!\\u0027"
                2 1 "public java.lang.Object java.util.Stack.push(java.lang.Object)"
                2 2 "$CUT@Stack@0"
                2 3 "\\u0027Hello World!\\u0027"
                3 0 1
                3 1 "public synchronized int java.util.Vector.size()"
                3 2 "$CUT@Stack@0"
                """, adaptedActuationSheet.toString());
    }
}
