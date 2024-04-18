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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.llm.eval.ExecutedSolution;
import de.uni_mannheim.swt.lasso.llm.eval.Results;
import de.uni_mannheim.swt.lasso.llm.export.MavenExporter;
import de.uni_mannheim.swt.lasso.llm.export.Publisher;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.cli.DefaultParser;
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
public class MCArtifactExporter {

    private static final Logger LOG = LoggerFactory
            .getLogger(MCArtifactExporter.class);

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, ParseException {
        LOG.info("Args passed '{}'", Arrays.toString(args));

        Options options = createOptions();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // FIXME
        String mavenDefaultImage = "maven:3.6.3-openjdk-17";

        String corpusFile = cmd.getOptionValue("corpus");
        String mcPath = cmd.getOptionValue("mc");
        String mavenHome = cmd.getOptionValue("ma");
        String workDir = cmd.getOptionValue("w");
        int threads = Integer.parseInt(cmd.getOptionValue("t", "10"));
        String mavenThreads = cmd.getOptionValue("mt", "1");
        boolean index = BooleanUtils.toBoolean(cmd.getOptionValue("i", "true"));
        boolean deploy = BooleanUtils.toBoolean(cmd.getOptionValue("d", "true"));

        File file = new File(corpusFile);
        ObjectMapper json = new ObjectMapper();
        ExecutableCorpus executableCorpus = json.readValue(file, ExecutableCorpus.class);

        File m2Home = new File(mavenHome);
        m2Home.mkdirs(); // must be created beforehand

        File toDir = new File(workDir);
        toDir.mkdirs();

        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();

        File dir = new File(mcPath);
        File[] files = dir.listFiles((d, name) -> name.contains(".java"));

        MultiPLECompletions completions = new MultiPLECompletions();

        for (File completitionsFile : files) {
            LOG.info("Processing {}", completitionsFile.getName());

            try {
                String problems = StringUtils.substringBefore(completitionsFile.getName(), ".");

                //List<Results> resultsList = completions.read(completitionsFile).subList(0, 1);
                List<Results> resultsList = completions.read(completitionsFile);

                ForkJoinPool customThreadPool = new ForkJoinPool(threads);
                customThreadPool.submit(
                        () -> resultsList.parallelStream().forEach(results -> {
                            try {
                                LOG.info("Processing {}/{}/k = {}", results.getName(), results.getProblem(), results.getResults().size());

                                File projectRoot = new File(new File(new File(toDir, problems), results.getExperiment()), results.getProblem());
                                projectRoot.mkdirs();

                                //
                                MavenExporter mavenExport = new MavenExporter(projectRoot, executableCorpus);

                                String generatorId = results.getExperiment();
                                Problem problem = new Problem();
                                problem.setName(results.getProblem());

                                int k = 0;
                                for (ExecutedSolution solution : results.getResults()) {
                                    Map<String, String> meta = new HashMap<>();
                                    meta.put("benchmark", problems);
                                    meta.put("experiment", generatorId);
                                    meta.put("generator", generatorId);
                                    meta.put("model", generatorId);
                                    meta.put("problem", problem.getName());
                                    meta.put("tests", problem.getTests());
                                    meta.put("prompt", problem.getPrompt());
                                    meta.put("k", String.valueOf(k));

                                    // metadata for index
                                    String metadataStr = "";
                                    if (MapUtils.isNotEmpty(meta)) {
                                        metadataStr = meta.entrySet().stream()
                                                .map(e -> e.getKey() + "," + e.getValue())
                                                .collect(Collectors.joining("|"));
                                    }

                                    LOG.debug("meta data string '{}'", metadataStr);

                                    Map<String, Object> metaData = new HashMap<>();
                                    metaData.put("generator", generatorId);
                                    metaData.put("k", k++);
                                    if (StringUtils.isNotBlank(metadataStr)) {
                                        metaData.put("metaData", metadataStr);
                                    }

                                    mavenExport.export(problem, solution, metaData);
                                }

                                // create aggregated
                                mavenExport.createAggregatedPom(projectRoot, problem, generatorId);


                                Publisher publisher = new Publisher(executableCorpus, m2Home);
                                publisher.setMavenThreads(mavenThreads);
                                publisher.setDeploy(deploy);
                                publisher.setMavenDefaultImage(mavenDefaultImage);

                                // do package (optionally deploy)
                                publisher.doPackage(projectRoot, problem, generatorId);

                                // do index
                                if (index) {
                                    publisher.doAnalyzeAndStore(projectRoot, problem, generatorId);
                                }

                                // remove
                                boolean remove = true;
                                if(remove) {
                                    boolean deleted = FileUtils.deleteQuietly(projectRoot);
                                    LOG.info("Deleted directory {} => {}", projectRoot.getAbsolutePath(), deleted);
                                }
                            } catch (Throwable e) {
                                LOG.warn("Completions failed " + completitionsFile.getName(), e);
                                //throw new RuntimeException(e);
                            }
                        })).get();

                customThreadPool.shutdown();
            } catch (Throwable e) {
                LOG.warn("Completions failed " + completitionsFile.getName(), e);
            }
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "print this message");
        options.addOption("c", "corpus", true, "path to corpus config file");
        options.addOption("mc", "completions", true, "path to MultiPL-E completions data");
        options.addOption("ma", "mavenhome", true, "path to maven home");
        options.addOption("w", "work", true, "path to work directory");
        options.addOption("mt", "maventhreads", true, "maven threads");
        options.addOption("i", "index", true, "should index?");
        options.addOption("d", "deploy", true, "should deploy");
        options.addOption("t", "threads", true, "worker threads");

        return options;
    }
}
