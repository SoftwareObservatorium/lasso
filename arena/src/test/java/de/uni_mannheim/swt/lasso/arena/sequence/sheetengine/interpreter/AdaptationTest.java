package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
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
public class AdaptationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AdaptationTest.class);

    @Test
    public void test_Base64_remote_DefaultAdaptationStrategy() throws IOException, ClassNotFoundException {
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
        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        SSNTestDriver testDriver = new SSNTestDriver();
        // ADAPTATION
        testDriver.setAdaptationStrategy(new DefaultAdaptationStrategy());
        // LIMIT ADAPTEDS
        int limitAdapters = 10;
        // XXX see DefaultAdaptationStrategy.METHOD_MAX

        String mavenRepoUrl = NexusInstance.MAVEN_CENTRAL;
        File localRepo = new File("/tmp/my_repo/local-repo");
        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        testDriver.setMavenRepository(new MavenRepository(resolver));

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test1()", ssnJsonlStr, lql)), Arrays.asList(classUnderTest), Arrays.asList(new SheetInvocation("test1", "")));

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, limitAdapters, visitor);

        assertEquals(5, stimulusResponseMatrix.getTable().columnKeySet().size());

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdapterId(), executedInvocations);
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();

            //
        }

    }
}
