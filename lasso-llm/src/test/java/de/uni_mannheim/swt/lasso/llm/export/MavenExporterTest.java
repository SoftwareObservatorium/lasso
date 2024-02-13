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
package de.uni_mannheim.swt.lasso.llm.export;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.llm.eval.EvalReader;
import de.uni_mannheim.swt.lasso.llm.eval.ExecutedSolution;
import de.uni_mannheim.swt.lasso.llm.eval.Results;
import de.uni_mannheim.swt.lasso.llm.problem.MultiPLE;
import de.uni_mannheim.swt.lasso.llm.problem.Problem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class MavenExporterTest {

    /**
     * Can do mvn -X exec:java -Dexec.mainClass="HumanEval_23_strlen.Problem" for double-checking
     *
     * @throws IOException
     */
    @Test
    public void test_single() throws IOException {
        String problemId = "HumanEval_23_strlen";
        String generatorId = "humaneval-java-davinci-0.2-reworded";

        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        //ProblemToSheetParser parse = new ProblemToSheetParser(container);
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/humaneval-java-reworded.json"));

        Problem problem = problemsMap.get(problemId);

        String rawDataPath = "/home/marcus/development/repositories/huggingface/MultiPL-E-raw-data/";
        EvalReader evalReader = new EvalReader();
        Results results = evalReader.getResults(
                new File(rawDataPath),
                generatorId,
                problemId);

        File baseDir = new File("/tmp/llm_" + System.currentTimeMillis());
        baseDir.mkdirs();
        //
        MavenExporter mavenExport = new MavenExporter(baseDir, Collections.emptyMap());

        ExecutedSolution solution = results.getResults().get(0);

        Map<String, Object> metaData = new HashMap<>();
        metaData.put("generator", generatorId);
        metaData.put("k", 0);

        mavenExport.export(problem, solution, metaData);

        // create aggregated
        mavenExport.createAggregatedPom(baseDir, problem, generatorId);
    }

    @Test
    public void test_aggregated() throws IOException {
        String problemId = "HumanEval_23_strlen";
        String generatorId = "humaneval-java-davinci-0.2-reworded";

        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        //ProblemToSheetParser parse = new ProblemToSheetParser(container);
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/humaneval-java-reworded.json"));

        Problem problem = problemsMap.get(problemId);

        String rawDataPath = "/home/marcus/development/repositories/huggingface/MultiPL-E-raw-data/";
        EvalReader evalReader = new EvalReader();
        Results results = evalReader.getResults(
                new File(rawDataPath),
                generatorId,
                problemId);

        File baseDir = new File("/tmp/llm_" + System.currentTimeMillis());
        baseDir.mkdirs();
        //
        MavenExporter mavenExport = new MavenExporter(baseDir, Collections.emptyMap());

        int k = 0;
        for(ExecutedSolution solution : results.getResults()) {

            Map<String, Object> metaData = new HashMap<>();
            metaData.put("generator", generatorId);
            metaData.put("k", k++);

            mavenExport.export(problem, solution, metaData);
        }

        // create aggregated
        mavenExport.createAggregatedPom(baseDir, problem, generatorId);
    }
}
