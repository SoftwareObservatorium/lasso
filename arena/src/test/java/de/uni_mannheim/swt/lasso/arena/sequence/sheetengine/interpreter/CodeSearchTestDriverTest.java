package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class CodeSearchTestDriverTest {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSearchTestDriverTest.class);

    SolrInstance solrInstance = new SolrInstance("mavencentral2023", "solr", "Y2E5ZjgzMGV", "http://lassohp10.informatik.uni-mannheim.de:8983/solr/mavencentral2023/");

    public MavenRepository mavenRepository() {
        // FIXME change maven repo
        String mavenRepoUrl = NexusInstance.MAVEN_CENTRAL;
        File localRepo = new File("/tmp/my_repo/local-repo");
        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        return new MavenRepository(resolver);
    }

    @Test
    public void test_getClassesDirectly() throws IOException {
        CodeSearch codeSearch = new CodeSearch(solrInstance);

        String mql = "Stack{\n" +
                "Stack(int)\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int" +
                "}";

        List<ClassUnderTest> classesUnderTest = codeSearch.queryForClassesDirectly(mql, 10, "class"); // retrieve classes

        for(ClassUnderTest cut : classesUnderTest) {
            System.out.println(ToStringBuilder.reflectionToString(cut.getImplementation().getCode()));
        }
    }

    @Test
    public void test_Stack_empty_constructor() throws IOException, ClassNotFoundException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
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

        // use code search
        CodeSearch codeSearch = new CodeSearch(solrInstance);
        // retrieved classes
        List<ClassUnderTest> classesUnderTest = codeSearch.queryForClassesDirectly(lql, 10, "class"); // retrieve classes

        SSNTestDriver testDriver = new SSNTestDriver();
        // set resolvable repo
        testDriver.setMavenRepository(mavenRepository());
        // set permutator
        testDriver.setAdaptationStrategy(new DefaultAdaptationStrategy());

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test()", ssnJsonlStr, lql)), classesUnderTest, Arrays.asList(new SheetInvocation("test", "")));

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
}
