/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.llm.problem;

import org.junit.Test;
//import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.uni_mannheim.swt.lasso.llm.eval.Results;

/**
 *
 * @author Marcus Kessel
 */
public class MultiPLECompletionsTest {

    @Test
    public void testListAll() {
        File dir = new File("/home/marcus/development/repositories/huggingface/MultiPL-E-completions/data");
        File[] files = dir.listFiles((d, name) -> name.contains(".java"));

        MultiPLECompletions multiPLECompletions = new MultiPLECompletions();
        Arrays.stream(files).forEach(file -> multiPLECompletions.read(file));
    }

    @Test
    public void testHumanEvalProblems() throws IOException {
        MultiPLECompletions multiPLECompletions = new MultiPLECompletions();

        File dir = new File("/home/marcus/development/repositories/huggingface/MultiPL-E-completions/data");
        File parquet = new File(dir, "humaneval.java.davinci.0.2.reworded-00000-of-00001-2f05972f87ef7add.parquet");

        List<Results> resultsList = multiPLECompletions.read(parquet);

        System.out.println(resultsList.size());
    }

    @Test
    public void testHumanEvalProblems_bug() throws IOException {
        MultiPLECompletions multiPLECompletions = new MultiPLECompletions();

        File dir = new File("/home/marcus/development/repositories/huggingface/MultiPL-E-completions/data");
        File parquet = new File(dir, "humaneval.java.StarCoder2_7b_16k.0.2.reworded-00000-of-00001.parquet");

        List<Results> resultsList = multiPLECompletions.read(parquet);

        System.out.println(resultsList.size());
    }
}
