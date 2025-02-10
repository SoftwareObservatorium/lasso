package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.benchmark.Benchmark;
import de.uni_mannheim.swt.lasso.benchmark.ClasspathBenchmarkLoader;
import de.uni_mannheim.swt.lasso.benchmark.FunctionalAbstraction;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import examples_new.*;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class BenchmarksTest {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarksTest.class);

    @Test
    public void test_HumanEval_checkDictCase() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
            {"cells":{"B1":"create","C1":"Problem"}}
            {"cells":{"A1":"true","B2":"checkDictCase","C2":"A1","D2":"new java.util.HashMap<java.lang.String, java.lang.String>(java.util.Map.of(\\"STATE\\", \\"NC\\", \\"ZIP\\", \\"12345\\"))"}}
            """;

        String lql = """
                Problem {
                  checkDictCase(java.util.HashMap<java.lang.String, java.lang.String>)->boolean
                }
                """;
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = CheckDictCase.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test()", ssnJsonlStr, lql)), Arrays.asList(CutUtils.createExample(cutClass)), Arrays.asList(new SheetInvocation("test", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_db_() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        String problem = "HumanEval_13_greatest_common_divisor";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = HumanEval_13_greatest_common_divisor.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_longest() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        String problem = "HumanEval_12_longest";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = Longest12.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_common() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        String problem = "HumanEval_58_common";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = Common58.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_stringToMd5() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        String problem = "HumanEval_162_string_to_md5";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = StringToMd5162.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_meanAbsoluteDeviation() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        String problem = "HumanEval_4_mean_absolute_deviation";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = Mean4.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_mbpp_104_sort_sublists() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("mbpp-java-reworded");
        String problem = "mbpp_104_sort_sublists";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = mbpp_104_sort_sublists.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_mbpp_120_max_product_tuple() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("mbpp-java-reworded");
        String problem = "mbpp_120_max_product_tuple";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = mbpp_120_max_product_tuple.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_HumanEval_mbpp_250_count_X() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("mbpp-java-reworded");
        String problem = "mbpp_250_count_X";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = mbpp_250_count_X.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_mbpp_262_split_two_parts() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("mbpp-java-reworded");
        String problem = "mbpp_262_split_two_parts";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = mbpp_262_split_two_parts.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void test_mbpp_409_min_product_tuple() throws IOException, ClassNotFoundException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("mbpp-java-reworded");
        String problem = "mbpp_409_min_product_tuple";
        FunctionalAbstraction functionalAbstraction = benchmark.getAbstractions().get(problem);

        List<Sheet> stimulusSheets = functionalAbstraction.getStimulusSheets();
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        System.out.println("INTERFACE SIGANTURE\n" + functionalAbstraction.getLql());

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        Class cutClass = mbpp_409_min_product_tuple.class;

        SSNTestDriver testDriver = new SSNTestDriver();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                stimulusSheets, Arrays.asList(CutUtils.createExample(cutClass)), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations\n{}", executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }
}
