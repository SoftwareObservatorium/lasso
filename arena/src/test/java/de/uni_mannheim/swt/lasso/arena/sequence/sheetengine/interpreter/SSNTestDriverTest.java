package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.JaCoCoListener;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import examples_new.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class SSNTestDriverTest {

    private static final Logger LOG = LoggerFactory.getLogger(SSNTestDriverTest.class);

    @Test
    public void testToLql() {
        Class cutClass = CompositeNodeExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        InterfaceSpecification interfaceSpecification = testDriver.toLQL(cutClass.getCanonicalName(), null);

        String lql = interfaceSpecification.toLQL();
        LOG.debug("LQL\n{}", lql);

        assertEquals("""
                CompositeNodeExample{
                	CompositeNodeExample(java.lang.String)
                	getName()->java.lang.String
                	getParent()->examples_new.CompositeNodeExample
                	setName(java.lang.String)->void
                	setParent(examples_new.CompositeNodeExample)->void
                	finalize()->void
                	wait(long,int)->void
                	wait()->void
                	wait(long)->void
                	equals(java.lang.Object)->boolean
                	toString()->java.lang.String
                	hashCode()->int
                	getClass()->java.lang.Class
                	clone()->java.lang.Object
                	notify()->void
                	notifyAll()->void
                }""", lql);
    }

    /**
     * Multi-object
     *
     * <code>
     *     CUT = Stack (empty constructor)
     *     java.lang.String
     * </code>
     *
     */
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
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);

            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

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

    /**
     * Multi-object
     *
     * <code>
     *     CUT = Stack (non-empty constructor)
     *     java.lang.String
     * </code>
     *
     */
    @Test
    public void test_Stack_nonempty_constructor() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack", "D1": 10}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    Stack(int)
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = StackNonEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(4, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(0).getTargetClass());
            assertEquals(1, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("java.lang.String"), invocations.getInvocation(1).getTargetClass());
            assertEquals(1, invocations.getInvocation(1).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(2).getTargetClass());
            assertEquals(1, invocations.getInvocation(2).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(3).getTargetClass());
            assertEquals(0, invocations.getInvocation(3).getParameters().size());

            assertEquals(4, executedInvocations.getSequence().size());
        }
    }

    /**
     * Special commands in SSN
     *
     * <code>
     *     $create - create instance
     *     $eval - evaluate code expression
     * </code>
     *
     * @throws IOException
     */
    @Test
    public void test_Stack_code() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "$eval", "C2": "Arrays.toString(new char[]{'a', 'b'})"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = StackEmptyConstructorExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(4, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(0).getTargetClass());
            assertEquals(0, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("java.lang.String"), invocations.getInvocation(1).getTargetClass());
            assertEquals(0, invocations.getInvocation(1).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(2).getTargetClass());
            assertEquals(1, invocations.getInvocation(2).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Stack"), invocations.getInvocation(3).getTargetClass());
            assertEquals(0, invocations.getInvocation(3).getParameters().size());

            assertEquals(4, executedInvocations.getSequence().size());
        }
    }

    /**
     * A CUT class with a static method.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void test_static() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Singleton"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "sum", "C2": "A1", "D2": 2, "E2": 3}}
                """;

        String lql = """
                Singleton {
                    sum(int,int)->int
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = StaticMethodExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(2, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(0).getTargetClass());
            assertEquals(0, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(1).getTargetClass());

            assertEquals(2, executedInvocations.getSequence().size());
        }
    }

//    /**
//     * A CUT class with a static method.
//     *
//     * @throws IOException
//     * @throws ClassNotFoundException
//     */
//    @Test
//    public void test_static_nocreate() throws IOException, ClassNotFoundException {
//        @Language("jsonl")
//        String ssnJsonlStr = """
//                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "sum", "C2": "Singleton", "D2": 2, "E2": 3}}
//                """;
//
//        String lql = """
//                Singleton {
//                    sum(int,int)->int
//                }
//                """;
//        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());
//
//        Class cutClass = StaticMethodExample.class;
//
//        SSNTestDriver testDriver = new SSNTestDriver();
//        // SM
//        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
//                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));
//
//        // SRM
//        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);
//
//        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
//            ExecutedInvocations executedInvocations = cell.getValue();
//
//            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
//            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
//            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
//            actuationSheetData.debug();
//            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
//            adaptedActuationSheetData.debug();
//
//            Invocations invocations = executedInvocations.getInvocations();
//
//            assertEquals(1, invocations.getSequence().size());
//
//            assertEquals(1, executedInvocations.getSequence().size());
//        }
//    }

    /**
     * A CUT class with a static method and non-visible constructor.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void test_static_invisible() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Singleton"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "sum", "C2": "A1", "D2": 2, "E2": 3}}
                """;

        String lql = """
                Singleton {
                    sum(int,int)->int
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = InvisibleStaticMethodExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(2, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(0).getTargetClass());
            assertEquals(0, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Singleton"), invocations.getInvocation(1).getTargetClass());

            assertEquals(2, executedInvocations.getSequence().size());
        }
    }

    @Test
    public void test_composite_node() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Node", "D1": "'node1'"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "Node", "D2": "'node2'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "setParent", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": "A2", "B4": "getParent", "C4": "A1"}}
                """;

        String lql = """
                Node {
                    Node(java.lang.String)
                    setParent(Node)->void
                    getParent()->Node
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = CompositeNodeExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(4, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(0).getTargetClass());
            assertEquals(1, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(1).getTargetClass());
            assertEquals(1, invocations.getInvocation(1).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(2).getTargetClass());
            assertEquals(1, invocations.getInvocation(2).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(3).getTargetClass());
            assertEquals(0, invocations.getInvocation(3).getParameters().size());
            // test oracle values (first column)
            assertTrue(invocations.getInvocation(0).getExpectedOutput().isUndefined());
            assertTrue(invocations.getInvocation(1).getExpectedOutput().isUndefined());
            assertTrue(invocations.getInvocation(2).getExpectedOutput().isUndefined());
            assertFalse(invocations.getInvocation(3).getExpectedOutput().isUndefined());
            assertEquals("A2", invocations.getInvocation(3).getExpectedOutput().getExpression());

            assertEquals(4, executedInvocations.getSequence().size());
        }
    }

    @Test
    public void test_composite_node_self() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Node", "D1": "'node1'"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "setParent", "C2": "A1", "D2": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": "A1", "B3": "getParent", "C3": "A1"}}
                """;

        String lql = """
                Node {
                    Node(java.lang.String)
                    setParent(Node)->void
                    getParent()->Node
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = CompositeNodeExample.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            assertEquals(3, invocations.getSequence().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(0).getTargetClass());
            assertEquals(1, invocations.getInvocation(0).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(1).getTargetClass());
            assertEquals(1, invocations.getInvocation(1).getParameters().size());
            assertEquals(invocations.getEval().resolveClass("Node"), invocations.getInvocation(2).getTargetClass());
            assertEquals(0, invocations.getInvocation(2).getParameters().size());
            // test oracle values (first column)
            assertTrue(invocations.getInvocation(0).getExpectedOutput().isUndefined());
            assertTrue(invocations.getInvocation(1).getExpectedOutput().isUndefined());
            assertFalse(invocations.getInvocation(2).getExpectedOutput().isUndefined());
            assertEquals("A1", invocations.getInvocation(2).getExpectedOutput().getExpression());

            assertEquals(3, executedInvocations.getSequence().size());
        }
    }

    /**
     * JaCoCo Coverage
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void test_BoundedQueue_JaCoCo_Coverage() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "BoundedQueue", "D1": 10}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": false, "B3": "isEmpty", "C3": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": false, "B4": "isFull", "C4": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 5", "cells": {"A5": "D2", "B5": "deQueue", "C5": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 6", "cells": {"A6": true, "B6": "isEmpty", "C6": "A1"}}
                """;

        String lql = """
                BoundedQueue {
                    BoundedQueue(int)
                    enQueue(java.lang.Object)->void
                    deQueue()->java.lang.Object
                    isEmpty()->boolean
                    isFull()->boolean
                }
                """;
        JaCoCoListener jaCoCoListener = new JaCoCoListener();
        InvocationVisitor visitor = new CompositeInvocationVisitor(
                Arrays.asList(jaCoCoListener)); // add jacoco listener

        Class cutClass = BoundedQueue.class;

        SSNTestDriver testDriver = new SSNTestDriver();
        testDriver.setEnableJaCoCoCoverage(true);

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }

        jaCoCoListener.getStimulusResponseMatrix().debug();
    }
}
