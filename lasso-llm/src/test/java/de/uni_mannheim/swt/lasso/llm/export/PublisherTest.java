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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.llm.eval.EvalReader;
import de.uni_mannheim.swt.lasso.llm.eval.ExecutedSolution;
import de.uni_mannheim.swt.lasso.llm.eval.Results;
import de.uni_mannheim.swt.lasso.llm.problem.MultiPLE;
import de.uni_mannheim.swt.lasso.llm.problem.Problem;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
//import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author marcus Kessel
 */
public class PublisherTest {

    @Test
    public void test_single_k() throws IOException {
        File file = new File("../doc/lasso_config/corpus.json");
        ObjectMapper json = new ObjectMapper();
        ExecutableCorpus executableCorpus = json.readValue(file, ExecutableCorpus.class);
        executableCorpus.getArtifactRepository().setPass("0a37a6b9-a79b-4b06-a928-adfec6bc0ddc");

        String problemId = "HumanEval_23_strlen";
        String generatorId = "humaneval-java-davinci-0.2-reworded";
        int k = 0;

        String mavenDefaultImage = "maven:3.6.3-openjdk-17";

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
        MavenExporter mavenExport = new MavenExporter(baseDir, executableCorpus);

        ExecutedSolution solution = results.getResults().get(k);

        // metadata for index
        Map<String, String> meta = new HashMap<>();
        meta.put("benchmark", "humaneval-java-reworded");
        meta.put("problem", problem.getName());
        meta.put("generator", generatorId);
        meta.put("k", String.valueOf(k));

        String metadataStr = "";
        if(MapUtils.isNotEmpty(meta)) {
            metadataStr = meta.entrySet().stream()
                    .map(e -> e.getKey() + "," + e.getValue())
                    .collect(Collectors.joining("|"));
        }

        Map<String, Object> metaData = new HashMap<>();
        metaData.put("generator", generatorId);
        metaData.put("k", k);
        if(StringUtils.isNotBlank(metadataStr)) {
            metaData.put("metaData", metadataStr);
        }

        mavenExport.export(problem, solution, metaData);

        // create aggregated
        mavenExport.createAggregatedPom(baseDir, problem, generatorId);

        // cut
        File m2Home = new File("/tmp/lasso_m2home");
        m2Home.mkdirs(); // must be created beforehand
        Publisher publisher = new Publisher(executableCorpus, m2Home);
        publisher.setDeploy(true); // do deploy
        publisher.setMavenDefaultImage(mavenDefaultImage);

        // do package (optionally deploy)
        publisher.doPackage(baseDir, problem, generatorId);

        // do index
        publisher.doAnalyzeAndStore(baseDir, problem, generatorId);
    }
}
