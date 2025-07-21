package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.transformer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;

import examples_new.FindIndex;
import examples_new.Modp;
import examples_new.SumCalculator;
import examples_new.SumSquares;
import examples_new.transform.Stack1;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class JUnitToSsnTransformerTest {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitToSsnTransformerTest.class);

    private void debugRun(StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix) throws IOException {
        SSNTestDriver testDriver = new SSNTestDriver();
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());
        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for (Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test test = cell.getRowKey();

            System.out.println("TEST " + test.getParsedSheet().getSheet().getBody());


            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            ExecutedInvocations oracleInvocations = SheetUtils.toOracle(executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> oracleSheet = SheetUtils.toOracleSheet(oracleInvocations, new GsonMapper());

            System.out.println("----------- ORACLE -------");

            oracleSheet.debug();

            System.out.println(oracleSheet.toJsonl());
        }
    }

    private CompilationUnit create(String sourceCode) {
        // --- Solver Configuration ---
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        // The ReflectionTypeSolver lets us find JDK classes.
        typeSolver.add(new ReflectionTypeSolver());

        // Configure JavaParser to use the Symbol Solver.
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration().setSymbolResolver(symbolSolver);

        // --- Transformation ---
        // We now use a configured JavaParser instance, NOT StaticJavaParser.
        JavaParser configuredParser = new JavaParser(parserConfig);
        CompilationUnit cu = configuredParser.parse(sourceCode).getResult().orElseThrow();

        return cu;
    }

    @Test
    public void test_fail_assertThrows() throws IOException {
        String sourceCode = """
// src/test/java/com/example/AssertionPatternsTest.java
package com.example;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AssertionPatternsTest {

    // A simple class to test against.
    static class DataProcessor {
        public int processList(List<String> data) {
            if (data == null) {
                throw new IllegalArgumentException("Data list cannot be null");
            }
            if (data.isEmpty()) {
                fail("Processing an empty list is not allowed.");
            }
            return data.size();
        }
    }

    @Test
    void testAssertThrows() {
        DataProcessor processor = new DataProcessor();

        assertThrows(IllegalArgumentException.class, () -> {
            processor.processList(null);
        });
    }
    
    @Test
    void testFail() {
        DataProcessor processor = new DataProcessor();
        try {
            processor.processList(null);
            fail("blub");
        } catch(Throwable e) {
            // not reached
        }
    }
}
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);
    }

    @Test
    public void test_Stack() throws IOException {
        String sourceCode = """
                // src/test/java/com/example/StackTest.java
                package com.example;
                
                import org.junit.jupiter.api.Test;
                //import java.util.Stack;
                import static org.junit.jupiter.api.Assertions.*;
                
                public class StackTest {
                
                    @Test
                    void testPushAndPop() {
                        Stack<String> stack = new Stack<>();
                        stack.push("Hello");
                        stack.push("World");
                
                        String item1 = stack.pop();
                        assertEquals("World", item1);
                
                        String item2 = stack.pop();
                        assertEquals("Hello", item2);
                
                        assertTrue(stack.isEmpty());
                    }
                }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Stack {
                    push(java.lang.String)->void
                    pop()->java.lang.String
                    isEmpty()->boolean
                }
                """;
        Class cutClass = Stack1.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_Stack_2() throws IOException {
        String sourceCode = """
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StackTest {

    @Test
    void testPeekDoesNotRemove() {
        Stack stack = new Stack();
        Object item = new Object();
        stack.push(item);
        assertEquals(item, stack.peek());
        // Call peek again to ensure it doesn't remove
        assertEquals(item, stack.peek());
        assertEquals(item, stack.pop());
        assertEquals(0, stack.size());
    }
}
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Stack {
                    push(java.lang.Object)->void
                    peek()->java.lang.Object
                    pop()->java.lang.Object
                    size()->int
                }
                """;
        Class cutClass = Stack.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_complex() throws IOException {
        String sourceCode = """
                                import org.junit.jupiter.api.Test;
                                import java.util.ArrayList;
                                import java.util.Base64;
                                import java.util.List;
                                import static org.junit.jupiter.api.Assertions.*;
                
                                public class ComplexTest {
                
                                    @Test
                                    void testStaticAndChainedCalls() {
                                        // Chained static method call
                                        String originalText = "SequenceSheets";
                                        String encodedText = Base64.getEncoder().encodeToString(originalText.getBytes());
                
                                        assertEquals("U2VxdWVuY2VTaGVldHM=", encodedText);
                
                                        // Standard static method call
                                        Base64.Decoder decoder = Base64.getDecoder();
                                        byte[] decodedBytes = decoder.decode(encodedText);
                                        String decodedText = new String(decodedBytes);
                
                                        // Check the decoded result
                                        assertEquals(originalText, decodedText);
                                    }
                
                                    @Test
                                    void testNewAssertions() {
                                        List<String> list = new ArrayList<>();
                
                                        // assertNotNull on a created object
                                        assertNotNull(list);
                
                                        String item = null;
                                        assertNull(item); // this will be skipped as `item` doesn't come from a row
                
                                        list.add("test");
                                        assertFalse(list.isEmpty());
                                    }
                                }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Stack {
                    push(java.lang.String)->void
                    pop()->java.lang.String
                    isEmpty()->boolean
                }
                """;
        Class cutClass = Stack1.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_sumCalc() throws IOException {
        String sourceCode = """
                import org.junit.Test;
                import static org.junit.Assert.assertEquals;
                
                class SumCalculator {
                    public int sum(int a, int b) { return a + b; }
                }
                
                public class SumCalculatorTest {
                
                    @Test
                    public void shouldSumTwoPositiveNumbers() {
                        SumCalculator calculator = new SumCalculator();
                        int result = calculator.sum(5, 10);
                        assertEquals(15, result);
                    }
                
                    @Test
                    public void shouldSumTwoNegativeNumbers() {
                        SumCalculator calculator = new SumCalculator();
                        int result = calculator.sum(-3, -7);
                        assertEquals(-10, result);
                    }
                
                    @Test
                    public void shouldSumPositiveAndNegativeNumber() {
                        SumCalculator calculator = new SumCalculator();
                        int result = calculator.sum(5, -8);
                        assertEquals(-3, result);
                    }
                
                    @Test
                    public void shouldSumZeroAndAnyNumber() {
                        SumCalculator calculator = new SumCalculator();
                        int result = calculator.sum(0, 10);
                        assertEquals(10, result);
                
                        result = calculator.sum(0, -5);
                        assertEquals(-5, result);
                    }
                
                    @Test
                    public void shouldThrowExceptionWhenSummingNullExceptionValues() {
                        SumCalculator calculator = new SumCalculator();
                        try {
                            int result = calculator.sum(Integer.MIN_VALUE, 10);
                            fail("Expected ArithmeticException");
                        } catch (ArithmeticException e) {
                            // expected exception
                        }
                    }
                
                    @Test
                    public void shouldSumLargeNumbers() {
                        SumCalculator calculator = new SumCalculator();
                        long result1 = calculator.sum(1234567890L, 9876543210L);
                        assertEquals(11111111100L, result1);
                
                        long result2 = calculator.sum(-1111111110L, -9999999990L);
                        assertEquals(-11111111110L, result2);
                    }
                }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                SumCalculator {
                    sum(long,long)->long
                }
                """;
        Class cutClass = SumCalculator.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_sumSquares() throws IOException {
        String sourceCode = """
                import org.junit.Test;
                import static org.junit.Assert.assertEquals;
                import java.util.ArrayList;
                import java.util.Arrays;
                
                public class ProblemTest {
                
                    @Test
                    public void testHappyCases() {
                        assertEquals(14l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.0f, (float) 2.0f, (float) 3.0f))));
                        assertEquals(98l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.0f, (float) 4.0f, (float) 9.0f))));
                        assertEquals(84l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.0f, (float) 3.0f, (float) 5.0f, (float) 7.0f))));
                    }
                
                    @Test
                    public void testEdgeCases() {
                        // Complex inputs
                        assertEquals(29l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 1.4f, (float) 4.2f, (float) 0.0f))));
                        assertEquals(6l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) -2.4f, (float) 1.0f, (float) 1.0f))));
                
                        // Corner case inputs
                        assertEquals(0l, Problem.sumSquares(new ArrayList<>()));
                        assertEquals(1l, Problem.sumSquares(new ArrayList<>(Arrays.asList((float) 1.0f))));
                    }
                
                    @Test
                    public void testDifficultInputs() {
                        // Negative numbers
                        assertEquals(8l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) -2.5f, (float) 3.0f))));
                
                        // Zero
                        assertEquals(0l, Problem.sumSquares(new ArrayList<Float>(Arrays.asList((float) 0.0f))));
                    }
                
                    @Test
                    void testSumSquaresSingleElementPositive() {
                        ArrayList<Float> list = new ArrayList<>(Arrays.asList(2.0f));
                        long expected = 4;
                        long actual = Problem.sumSquares(list);
                        assertEquals(expected, actual);
                    }
                }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Problem {
                    sumSquares(java.util.ArrayList<Long>)->long
                }
                """;
        Class cutClass = SumSquares.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_findIndex_minimal() throws IOException {
        String sourceCode = """
                import org.junit.jupiter.api.*;
                import static org.junit.jupiter.api.Assertions.*;
                
                class ProblemTest {
                
                    @Test
                    void testFindIndexHappyCase() {
                        assertEquals(10, Problem.findIndex(2));
                        assertEquals(99, Problem.findIndex(3));
                        assertEquals(4568, Problem.findIndex(5));
                    }
                    }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Problem {
                    findIndex(int)->int
                }
                """;
        Class cutClass = FindIndex.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_findIndex() throws IOException {
        String sourceCode = """
                import org.junit.jupiter.api.*;
                import static org.junit.jupiter.api.Assertions.*;
                
                class ProblemTest {
                
                    @Test
                    void testFindIndexHappyCase() {
                        assertEquals(10, Problem.findIndex(2));
                        assertEquals(99, Problem.findIndex(3));
                        assertEquals(4568, Problem.findIndex(5));
                    }
                
                    @Test
                    void testFindIndexEdgeCases() {
                        // Smallest number with 1 digit is 1
                        assertEquals(1, Problem.findIndex(1));
                
                        // Largest possible output for a 10-digit triangular number (since the smallest 10-digit number is 10^9)
                        long maxN = 3674850; // Solved using T_n >= 10^9
                        assertEquals(maxN, Problem.findIndex(10));
                    }
                
                    @Test
                    void testFindIndexComplexInput() {
                        // Test with a very large number of digits to ensure the function handles edge cases appropriately
                        long digits = 20;
                        long expectedIndex = (long) Math.ceil((Math.sqrt(8 * (long) Math.pow(10, digits - 1) + 1) - 1) / 2);
                        assertEquals(expectedIndex, Problem.findIndex(digits));
                    }
                
                    @Test
                    void testFindIndexCornerCases() {
                        // Test with zero and negative inputs which should throw an exception or return a meaningful value
                        assertThrows(IllegalArgumentException.class, () -> Problem.findIndex(0));
                
                        // Negative input case - throwing IllegalArgumentException as triangular numbers are not defined for non-positive integers
                        assertThrows(IllegalArgumentException.class, () -> Problem.findIndex(-1));
                    }
                }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Problem {
                    findIndex(int)->int
                }
                """;
        Class cutClass = FindIndex.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void test_modp() throws IOException {
        String sourceCode = """
                import org.junit.jupiter.api.*;
                import static org.junit.jupiter.api.Assertions.*;
                
                class ProblemTest {
                
                    @Test
                    void testModpHappyCase1() {
                        assertEquals(3L, Problem.modp(3L, 5L));
                    }
                
                    @Test
                    void testModpHappyCase2() {
                        assertEquals(2L, Problem.modp(1101L, 101L));
                    }
                
                    @Test
                    void testModpHappyCase3() {
                        assertEquals(1L, Problem.modp(0L, 101L));
                    }
                
                    @Test
                    void testModpHappyCase4() {
                        assertEquals(8L, Problem.modp(3L, 11L));
                    }
                
                    @Test
                    void testModpHappyCase5() {
                        assertEquals(1L, Problem.modp(100L, 101L));
                    }
                
                    @Test
                    void testModpEdgeCaseSmallP() {
                        assertThrows(ArithmeticException.class, () -> Problem.modp(2L, 1L));
                    }
                
                    @Test
                    void testModpEdgeCaseNegativeN() {
                        assertEquals(-3 % 5 + 5, Problem.modp(-3L, 5L)); // -3 mod 5 should be 2
                    }
                
                    @Test
                    void testModpEdgeCaseZeroP() {
                        assertThrows(ArithmeticException.class, () -> Problem.modp(2L, 0L));
                    }
                
                    @Test
                    void testModpEdgeCaseLargeNumbers() {
                        assertEquals(1431655765L % 1000000007L, Problem.modp((long) Math.pow(2, 50), 1000000007L));
                    }
                }
                """;

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
                Problem {
                    modp(long,long)->long
                }
                """;
        Class cutClass = Modp.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    // ---

    @Test
    public void testToSequenceSpecification_ArrayStack__javautilStack() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/PseudoStack.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";
        Class cutClass = Stack.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void testToSequenceSpecification_ArrayStack__javautilStack_EvoSuite() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/EvoStack.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";
        Class cutClass = Stack.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void testToSequenceSpecification_ArrayStack__javautilStack_Gemma3_pseudo() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/Stack_Gemma3.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";
        Class cutClass = Stack.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void testToSequenceSpecification_ArrayStack__javautilStack_Gemma3_pseudo_inline() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/Stack_Gemma3_inline.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";
        Class cutClass = Stack.class;

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(CutUtils.createExample(cutClass)), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void testToSequenceSpecification_Base64() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/jsonl/Curated_Base64_0_Test.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
        Base64 {
            Base64(int)
            encode(byte[])->byte[]
            encode(byte[],int,int)->byte[]
            encodeBase64URLSafeString(byte[])->java.lang.String
            encodeBase64String(byte[])->java.lang.String
            isUrlSafe()->boolean
        }""";
        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(classUnderTest), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void testToSequenceSpecification_Base64_all() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/jsonl/Base64_0_Test.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);

        String lql = """
        Base64 {
            encode(byte[])->byte[]
            decode(java.lang.String)->byte[]
        }""";
        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        List<Sheet> sheets = transformer.transform(cu, lql, "myprefix");
        List<SheetInvocation> invocations = sheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                sheets, Arrays.asList(classUnderTest), invocations);

        debugRun(stimulusMatrix);
    }

    @Test
    public void testToSequenceSpecification_GCD__PSEUDO() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/PseudoGCD_GAI.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);
    }

    @Test
    public void testToSequenceSpecification_GCD__PSEUDO_2() throws IOException {
        String sourceCode = FileUtils.readFileToString(new File("sheets/PseudoGCD_GAI_2.java"), StandardCharsets.UTF_8);

        CompilationUnit cu = create(sourceCode);

        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();
        String jsonlOutput = transformer.transform(cu);

        debug(jsonlOutput);
    }

    void debug(String jsonlOutput) {
        System.out.println("--- Generated Sequence Sheet (JSONL) ---");
        System.out.println(jsonlOutput);
        System.out.println("----------------------------------------");
    }
}
