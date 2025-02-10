package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;

import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.testing.generate.random.RandomObjectGenerator;
import de.uni_mannheim.swt.lasso.testing.generate.typeaware.MutatorSettings;
import de.uni_mannheim.swt.lasso.testing.generate.typeaware.TypeAwareMutator;
import examples_new.StackEmptyConstructorExample;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class TestDataGeneratorTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestDataGeneratorTest.class);

    @Test
    public void testMutate() throws IOException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "\\"Hello World!\\""}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;

        Class cutClass = StackEmptyConstructorExample.class;
        ClassUnderTest classUnderTest = CutUtils.createExample(cutClass);

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test()", ssnJsonlStr, lql)), Arrays.asList(classUnderTest), Arrays.asList(new SheetInvocation("test", "")));

        MutatorSettings settings = new MutatorSettings();
        TypeAwareMutator mutator = new TypeAwareMutator(settings);

        TestDataGenerator testGenerator = new TestDataGenerator();

        List<Sheet> generatedSheets = testGenerator.generateData(stimulusMatrix, (parsedCell, parameter) -> mutator.mutateValue(parameter.getValue()));

        assertEquals(1, generatedSheets.size());
    }

    @Test
    public void testRandom() throws IOException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "create", "C2": "java.lang.String", "D2": "\\"Hello World!\\""}}
                {"sheet": "Sheet 1", "header": "Row 3", "cells": {"A3": {}, "B3": "push", "C3": "A1", "D3": "A2"}}
                {"sheet": "Sheet 1", "header": "Row 4", "cells": {"A4": 1, "B4": "size", "C4": "A1"}}
                """;

        String lql = """
                Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """;

        Class cutClass = StackEmptyConstructorExample.class;
        ClassUnderTest classUnderTest = CutUtils.createExample(cutClass);

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test()", ssnJsonlStr, lql)), Arrays.asList(classUnderTest), Arrays.asList(new SheetInvocation("test", "")));

        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();

        TestDataGenerator testGenerator = new TestDataGenerator();

        List<Sheet> generatedSheets = testGenerator.generateData(stimulusMatrix, (parsedCell, parameter) -> randomObjectGenerator.random(parameter.getTargetClass()));

        assertEquals(1, generatedSheets.size());

        System.out.println(generatedSheets.get(0).getBody());
    }

    @Test
    public void testRandom_base64() throws IOException {
        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
                {"sheet": "Sheet 1", "header": "Row 2", "cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
                """;

        String lql = """
                Base64{
                    encode(byte[])->byte[]
                }
                """;

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                Arrays.asList(new Sheet("test()", ssnJsonlStr, lql)), Arrays.asList(classUnderTest), Arrays.asList(new SheetInvocation("test", "")));

        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();

        TestDataGenerator testGenerator = new TestDataGenerator();

        List<Sheet> generatedSheets = testGenerator.generateData(stimulusMatrix, (parsedCell, parameter) -> randomObjectGenerator.random(parameter.getTargetClass()));

        assertEquals(1, generatedSheets.size());

        System.out.println(generatedSheets.get(0).getBody());
    }
}
