package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;

import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.PitestContainer;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
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

import java.io.File;
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

    @Test
    public void test_Stack_empty_constructor_PARAMETERIZED() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "?p1"}}
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
                Arrays.asList(new SheetDto("test(p1=java.lang.String)", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocationDto("test", "\"Hello World!\""), new SheetInvocationDto("test", "\"I'm a robot\"")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        assertEquals(2, stimulusResponseMatrix.getTable().size());

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);

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

            ActuationSheet actuationSheet = new ActuationSheet();
            actuationSheet.setExecutedInvocations(cell.getValue());
            actuationSheet.setAdaptedImplementation(cell.getColumnKey());
            actuationSheet.toSheetData(new GsonMapper()).get(1).debug();
        }
    }

    @Test
    public void test_Stack_empty_constructor_PARAMETERIZED_input_output() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "?p1"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": "?p2", "B4": "size", "C4": "A1"}}
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
                Arrays.asList(new SheetDto("test(p1=java.lang.String,p2=int)", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocationDto("test", "\"Hello World!\",1"), new SheetInvocationDto("test", "\"I'm a robot\",1")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        assertEquals(2, stimulusResponseMatrix.getTable().size());

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);

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
            //assertEquals("1", invocations.getInvocation(3).getExpectedOutput().getExpression());

            assertEquals(4, executedInvocations.getSequence().size());

            ActuationSheet actuationSheet = new ActuationSheet();
            actuationSheet.setExecutedInvocations(cell.getValue());
            actuationSheet.setAdaptedImplementation(cell.getColumnKey());
            actuationSheet.toSheetData(new GsonMapper()).get(1).debug();
        }
    }

    @Test
    public void test_BoundedQueue_Mutation_Coverage() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "BoundedQueue", "D1": 10}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'Hello World!'"}}
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
        //ObjectMapperVisitor visitor = createVisitor();

        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
                Arrays.asList(visitor));

        Class cutClass = BoundedQueue.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new SheetDto("test()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocationDto("test", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.mutateAndRunSheets(stimulusMatrix, 1, invocationVisitor);

        assertEquals(29, stimulusResponseMatrix.getTable().size()); // original + 28 mutants

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            visitor.getActuationSheet().debug();
            visitor.getAdaptedActuationSheet().debug();
            Invocations invocations = executedInvocations.getInvocations();

            if(cell.getColumnKey().getAdaptee().getVariantId().equals("original")) {
                // original impl.
            } else {
                PitestContainer pitestContainer = (PitestContainer) cell.getColumnKey().getAdaptee().getProject().getContainer();
                LOG.debug("Mutant {}", pitestContainer.getMutant().getDetails());
            }

            //assertEquals(29, actuationSheets.stream().map(a -> a.getAdaptedImplementation().getAdaptee()).collect(Collectors.toSet()).size()); // original + 28 mutants
        }
    }

    @Test
    public void test_BoundedQueue_multiple_tests() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr1 = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "BoundedQueue", "D1": 10}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'Hello World!'"}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": false, "B3": "isEmpty", "C3": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": false, "B4": "isFull", "C4": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 5", "cells": {"A5": "D2", "B5": "deQueue", "C5": "A1"}}
                {"sheet": "Sheet 1", "header": "Row 6", "cells": {"A6": true, "B6": "isEmpty", "C6": "A1"}}
                """;
        String ssnJsonlStr2 = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "BoundedQueue", "D1": 5}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A2": {}, "B2": "enQueue", "C2": "A1", "D2": "'aaaaa'"}}
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
        //ObjectMapperVisitor visitor = createVisitor();

        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
                Arrays.asList(visitor));

        Class cutClass = BoundedQueue.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new SheetDto("test1()", ssnJsonlStr1, lql), new SheetDto("test2()", ssnJsonlStr2, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocationDto("test1", ""), new SheetInvocationDto("test2", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, invocationVisitor);

        //assertEquals(29, stimulusResponseMatrix.getTable().size());

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            visitor.getActuationSheet().debug();
            visitor.getAdaptedActuationSheet().debug();
            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_Base64_remote() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
                """;

        String lql = """
                Base64{
                    encode(byte[])->byte[]
                }
                """;
        ObjectMapperVisitor visitor = createVisitor();

        SSNTestDriver testDriver = new SSNTestDriver();
        String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
        File localRepo = new File("/tmp/my_repo/local-repo");
        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        testDriver.setMavenRepository(new MavenRepository(resolver));

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.createExample("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new SheetDto("test1()", ssnJsonlStr, lql)), Arrays.asList(classUnderTest), Arrays.asList(new SheetInvocationDto("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        //assertEquals(29, stimulusResponseMatrix.getTable().size());

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            visitor.getActuationSheet().debug();
            visitor.getAdaptedActuationSheet().debug();
            Invocations invocations = executedInvocations.getInvocations();
        }

    }

    @Test
    public void test_Base64_MutationCoverage() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
                """;

        String lql = """
                Base64{
                    encode(byte[])->byte[]
                }
                """;
        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
                Arrays.asList(visitor));

        SSNTestDriver testDriver = new SSNTestDriver();
        String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
        File localRepo = new File("/tmp/my_repo/local-repo");
        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        testDriver.setMavenRepository(new MavenRepository(resolver));

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.createExample("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new SheetDto("test1()", ssnJsonlStr, lql)), Arrays.asList(classUnderTest), Arrays.asList(new SheetInvocationDto("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.mutateAndRunSheets(stimulusMatrix, 1, invocationVisitor);

        //assertEquals(29, stimulusResponseMatrix.getTable().size());

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            visitor.getActuationSheet().debug();
            visitor.getAdaptedActuationSheet().debug();
            Invocations invocations = executedInvocations.getInvocations();
        }

    }

    private ObjectMapperVisitor createVisitor() {
        ObjectMapperVisitor visitor = new ObjectMapperVisitor(new GsonMapper());
//        InvocationVisitor invocationVisitor = new CompositeInvocationVisitor(
//                Arrays.asList(visitor));

        return visitor;
    }
}
