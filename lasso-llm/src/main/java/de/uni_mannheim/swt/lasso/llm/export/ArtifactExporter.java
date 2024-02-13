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

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Export artifacts to LASSO ecosystem (persist in index and artifact repository).
 *
 * @author Marcus Kessel
 */
public class ArtifactExporter {

    private static final Logger LOG = LoggerFactory
            .getLogger(ArtifactExporter.class);

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
//        String problems = "humaneval-java-reworded";
//        List<String> generators = Arrays.asList(
//                "humaneval-java-codegen-0.2-reworded",
//                "humaneval-java-davinci-0.2-reworded",
//                "humaneval-java-davinci-0.8-reworded",
//                "humaneval-java-incoder-0.2-reworded",
//                "humaneval-java-incoder-0.8-reworded"
//                );

        String solrCore = "multiple-benchmark-23";
        String nexusRepo = "http://lassohp12.informatik.uni-mannheim.de:8081/repository/multiple-benchmarks/";
        String mavenDefaultImage = "maven:3.6.3-openjdk-11";

        boolean deploy = true;
        boolean index = false;

        String problems = "mbpp-java-reworded";
        List<String> generators = Arrays.asList(
                "mbpp-java-codegen-0.2-reworded",
                "mbpp-java-codegen-0.8-reworded",
                "mbpp-java-codegen_deepspeed-0.2-reworded",
                "mbpp-java-davinci-0.2-reworded",
                "mbpp-java-incoder-0.2-reworded",
                "mbpp-java-incoder-0.8-reworded");

        //
        String rawDataPath = "/home/marcus/development/repositories/huggingface/MultiPL-E-raw-data/";

        File m2Home = new File("/home/marcus/development/benchmarks/m2home");
        m2Home.mkdirs(); // must be created beforehand

        File toDir = new File("/home/marcus/development/benchmarks");
        toDir.mkdirs();

        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        //ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemList = multiple.getProblems("/problems/" + problems + ".json");

        for(String generatorId : generators) {
            LOG.info("Processing {}/{}", generatorId, problems);

            int threads = 10;
            ForkJoinPool customThreadPool = new ForkJoinPool(threads);
            customThreadPool.submit(
                    () -> problemList.parallelStream().forEach(problem -> {
                        try {
                            EvalReader evalReader = new EvalReader();
                            Results results = evalReader.getResults(
                                    new File(rawDataPath),
                                    generatorId,
                                    problem.getName());

                            LOG.info("Processing {}/{}/k = {}", generatorId, problem.getName(), results.getResults().size());

                            File projectRoot = new File(new File(new File(toDir, problems), generatorId), problem.getName());
                            projectRoot.mkdirs();

                            //
                            MavenExporter mavenExport = new MavenExporter(projectRoot, Collections.emptyMap());

                            int k = 0;
                            for(ExecutedSolution solution : results.getResults()) {
                                Map<String, String> meta = new HashMap<>();
                                meta.put("benchmark", problems);
                                meta.put("generator", generatorId);
                                meta.put("problem", problem.getName());
                                meta.put("k", String.valueOf(k));

                                // metadata for index
                                String metadataStr = "";
                                if(MapUtils.isNotEmpty(meta)) {
                                    metadataStr = meta.entrySet().stream()
                                            .map(e -> e.getKey() + "," + e.getValue())
                                            .collect(Collectors.joining("|"));
                                }

                                LOG.debug("meta data string '{}'", metadataStr);

                                Map<String, Object> metaData = new HashMap<>();
                                metaData.put("generator", generatorId);
                                metaData.put("k", k++);
                                if(StringUtils.isNotBlank(metadataStr)) {
                                    metaData.put("metaData", metadataStr);
                                }

                                mavenExport.export(problem, solution, metaData);
                            }

                            // create aggregated
                            mavenExport.createAggregatedPom(projectRoot, problem, generatorId);


                            Publisher publisher = new Publisher(m2Home);
                            publisher.setDeploy(deploy);
                            publisher.setSolrCore(solrCore);
                            publisher.setRepoUrl(nexusRepo);
                            publisher.setMavenDefaultImage(mavenDefaultImage);

                            // do package (optionally deploy)
                            publisher.doPackage(projectRoot, problem, generatorId);

                            // do index
                            if(index) {
                                publisher.doAnalyzeAndStore(projectRoot, problem, generatorId);
                            }
                        } catch (Throwable e) {
                            LOG.warn("Problem failed " + problem.getName(), e);
                            //throw new RuntimeException(e);
                        }
                    })).get();

            customThreadPool.shutdown();

//            for(Problem problem : problemList) {
//                EvalReader evalReader = new EvalReader();
//                Results results = evalReader.getResults(
//                        new File(rawDataPath),
//                        generatorId,
//                        problem.getName());
//
//                LOG.info("Processing {}/{}/k = {}", generatorId, problem.getName(), results.getResults().size());
//
//                File projectRoot = new File(new File(new File(toDir, problems), generatorId), problem.getName());
//                projectRoot.mkdirs();
//
//                //
//                MavenExporter mavenExport = new MavenExporter(projectRoot, Collections.emptyMap());
//
//                int k = 0;
//                for(ExecutedSolution solution : results.getResults()) {
//                    Map<String, String> meta = new HashMap<>();
//                    meta.put("benchmark", problems);
//                    meta.put("generator", generatorId);
//                    meta.put("problem", problem.getName());
//                    meta.put("k", String.valueOf(k));
//
//                    // metadata for index
//                    String metadataStr = "";
//                    if(MapUtils.isNotEmpty(meta)) {
//                        metadataStr = meta.entrySet().stream()
//                                .map(e -> e.getKey() + "," + e.getValue())
//                                .collect(Collectors.joining("|"));
//                    }
//
//                    LOG.debug("meta data string '{}'", metadataStr);
//
//                    Map<String, Object> metaData = new HashMap<>();
//                    metaData.put("generator", generatorId);
//                    metaData.put("k", k++);
//                    if(StringUtils.isNotBlank(metadataStr)) {
//                        metaData.put("metaData", metadataStr);
//                    }
//
//                    mavenExport.export(problem, solution, metaData);
//                }
//
//                // create aggregated
//                mavenExport.createAggregatedPom(projectRoot, problem, generatorId);
//
//
//                Publisher publisher = new Publisher(m2Home);
//                publisher.setDeploy(deploy);
//                publisher.setSolrCore(solrCore);
//                publisher.setRepoUrl(nexusRepo);
//                publisher.setMavenDefaultImage(mavenDefaultImage);
//
//                // do package (optionally deploy)
//                publisher.doPackage(projectRoot, problem, generatorId);
//
//                // do index
//                if(index) {
//                    publisher.doAnalyzeAndStore(projectRoot, problem, generatorId);
//                }
//
//                //Thread.sleep(100 * 1000L);
//            }
        }
    }
}
