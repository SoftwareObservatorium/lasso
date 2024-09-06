package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import com.google.common.collect.Table;
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

        Table<Integer, Integer, String> actuationSheet = visitor.getActuationSheet();
        Table<Integer, Integer, String> adaptedActuationSheet = visitor.getAdaptedActuationSheet();

        visitor.debug(actuationSheet);
        System.out.println("-----");
        visitor.debug(adaptedActuationSheet);
    }
}
