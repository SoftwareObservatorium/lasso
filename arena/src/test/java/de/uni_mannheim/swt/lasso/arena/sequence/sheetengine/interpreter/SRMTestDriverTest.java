package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto.SheetDto;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto.SheetInvocationDto;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapperVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;

import examples_new.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class SRMTestDriverTest {

    private static final Logger LOG = LoggerFactory.getLogger(SRMTestDriverTest.class);

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
        ObjectMapperVisitor visitor = createVisitor();

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new SheetDto("test()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocationDto("test", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            visitor.getActuationSheet().debug();
            visitor.getAdaptedActuationSheet().debug();
            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(4, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(0).getTargetClass());
            assertEquals(0, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("java.lang.String"), invocations.getInvocation(1).getTargetClass());
            assertEquals(1, invocations.getInvocation(1).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(2).getTargetClass());
            assertEquals(1, invocations.getInvocation(2).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(3).getTargetClass());
            assertEquals(0, invocations.getInvocation(3).getParameters().size());
            // test oracle values (first column)
            assertTrue(invocations.getInvocation(0).getExpectedOutput().isUndefined());
            assertTrue(invocations.getInvocation(1).getExpectedOutput().isUndefined());
            assertTrue(invocations.getInvocation(2).getExpectedOutput().isUndefined());
            assertFalse(invocations.getInvocation(3).getExpectedOutput().isUndefined());
            assertEquals("1", invocations.getInvocation(3).getExpectedOutput().getExpression());

            assertEquals(4, executedInvocations.getSequence().size());
        }
    }

    private ObjectMapperVisitor createVisitor() {
        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
//        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
//                Arrays.asList(visitor));

        return visitor;
    }
}
