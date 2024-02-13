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
package de.uni_mannheim.swt.lasso.arena;

import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.task.Amplify;
import de.uni_mannheim.swt.lasso.arena.task.Execute;
import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.cluster.client.ClientArenaJobRepository;
import de.uni_mannheim.swt.lasso.cluster.client.JobStatus;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;


import java.io.File;
import java.io.IOException;

/**
 * Main entry point for Arena executions.
 *
 * @author Marcus Kessel
 */
public class ArenaExecutor {

    private static String mavenRepoUrl = NexusInstance.LASSOHP12_URL;

    private static String BANNER = "  _                    _____    _____    ____                                             \n" +
            " | |          /\\      / ____|  / ____|  / __ \\        /\\                                  \n" +
            " | |         /  \\    | (___   | (___   | |  | |      /  \\     _ __    ___   _ __     __ _ \n" +
            " | |        / /\\ \\    \\___ \\   \\___ \\  | |  | |     / /\\ \\   | '__|  / _ \\ | '_ \\   / _` |\n" +
            " | |____   / ____ \\   ____) |  ____) | | |__| |    / ____ \\  | |    |  __/ | | | | | (_| |\n" +
            " |______| /_/    \\_\\ |_____/  |_____/   \\____/    /_/    \\_\\ |_|     \\___| |_| |_|  \\__,_|";

    public static void main(String[] args) throws ParseException, IOException {
        System.out.println(BANNER);
        System.out.println();

        Options options = createOptions();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);//, System.getProperties());

        // automatically generate the help statement
//        HelpFormatter formatter = new HelpFormatter();
//        formatter.printHelp("arena", options);

        String mode = cmd.getOptionValue("mode", "local");
        System.out.println(String.format("'%s' mode enabled", mode));

        if (StringUtils.equalsIgnoreCase(mode, "local")) {
            // local mode
            try {
                System.out.println("Local mode enabled deprecated");
            } catch (Throwable e) {
                System.err.println("Local Arena failed with error.");
                e.printStackTrace();
            }
        } else if (StringUtils.equalsIgnoreCase(mode, "distributed")) {
            // distributed mode
            String ipAddresses = cmd.getOptionValue("lasso-addresses", "127.0.0.1:10800");

            try (LassoClusterClient clusterClient = new LassoClusterClient(ipAddresses)) {
                // start
                clusterClient.start();

                System.out.println("Established LASSO cluster connection (thin client)");

                // retrieve job
                ClientArenaJobRepository jobRepository = new ClientArenaJobRepository(clusterClient);

                String jobId = cmd.getOptionValue("lasso-job");
                if (StringUtils.isBlank(jobId)) {
                    throw new RuntimeException("Job ID is required, but was blank.");
                }

                System.out.println(String.format("Retrieving job with given id '%s'", jobId));

                ArenaJob arenaJob = jobRepository.get(jobId);

                if (arenaJob == null) {
                    throw new RuntimeException(String.format("Arena Job return is null for '%s'.", jobId));
                }

                // prepare working dir
                String workingDirectory = cmd.getOptionValue("work-dir");

                System.out.println(String.format("Setting up working directory '%s'", workingDirectory));

                File localRepo = new File(workingDirectory, "local-repo");
                if (!localRepo.exists()) {
                    localRepo.mkdirs();
                }

                // repository url
                String repositoryUrl = cmd.getOptionValue("repository-url", mavenRepoUrl);

                System.out.println(String.format("Setting up Maven repository '%s'", repositoryUrl));

                DependencyResolver resolver = new DependencyResolver(repositoryUrl, localRepo.getAbsolutePath());

                String outputDirectory = cmd.getOptionValue("output");

                System.out.println(String.format("Setting output directory to '%s'", outputDirectory));

                // actual preparation of arena

                // TASK
                String task = cmd.getOptionValue("task", null);
                if (StringUtils.isNotBlank(task)) {
                    System.out.println(String.format("Task activated '%s'", task));

                    if (StringUtils.equals(Amplify.class.getSimpleName(), task)) {
                        try {
                            Amplify.execute(cmd, resolver, arenaJob, clusterClient);
                            arenaJob.setStatus(JobStatus.FINISHED);
                        } catch (Throwable e) {
                            e.printStackTrace();

                            arenaJob.setStatus(JobStatus.FAILED);
                        }

                        jobRepository.put(arenaJob.getId(), arenaJob);

                        //
                        return;
                    } else if (StringUtils.equals(Execute.class.getSimpleName(), task)) {
                        try {
                            Execute.execute(cmd, resolver, arenaJob, clusterClient);
                            arenaJob.setStatus(JobStatus.FINISHED);
                        } catch (Throwable e) {
                            e.printStackTrace();

                            arenaJob.setStatus(JobStatus.FAILED);
                        }

                        jobRepository.put(arenaJob.getId(), arenaJob);

                        //
                        return;
                    } else {
                        throw new Exception("Unknown task");
                    }
                }

                System.out.println(String.format("Task ended '%s'", task));
            } catch (Throwable e) {
                System.err.println("Arena Job failed with error.");
                e.printStackTrace();
            }
        }

        // force all (foreign) threads to close (?)
        // FIXME better use custom SecurityManager to also catch "foreign" exit() calls?
        System.out.println("Terminating JVM.");
        System.exit(0);
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "print this message");
        options.addOption("m", "mode", true, "Arena mode, either 'local' or 'distributed'. default is 'local'");
        options.addOption("t", "task", true, "Task (optional)");
        options.addOption("f", "features", true, "Comma-separated list of features to enable: 'mutation' for mutation testing, 'cc' for code coverage measurements or 'dcg' for dynamic call graph creation");
        options.addOption("w", "work-dir", true, "Arena work directory");
        options.addOption("r", "repository-url", true, "Maven repository URL. default '" + mavenRepoUrl + "'");
        options.addOption("t", "threads", true, "Number of parallel executions (i.e threads). Default number of available 'cores' T/2");

        // distributed mode only
        options.addOption("la", "lasso-addresses", true, "if mode is set to 'distributed', at least one LASSO cluster address must be given. default is '127.0.0.1:10800'");
        options.addOption("lj", "lasso-job", true, "LASSO job id");

        // local mode only
        options.addOption("q", "query", true, "MQL-like query");
        options.addOption("qt", "query-type", true, "Query 'class' implementations or 'method' implementations (default 'class')");
        options.addOption("ql", "query-limit", true, "Query limit (i.e number of implementations to retrieve)");
        options.addOption("qf", "query-filter", true, "Comma-separated list of filter constraints (Solr-like)");
        options.addOption("s", "sheets", true, "Comma-separated list of sheets (*.xlsx) or JUnit test classes (*.java)");

        // CUT: fully-qualified class name
        options.addOption("c", "cut", true, "Fully-qualified class name of CUT (required for JUnit test classes)");

        options.addOption("al", "adaptation-limit", true, "Maximum number of adaptations to compute for each implementation");

        options.addOption("gj", "generate-junit", true, "Generate junit classes");
        //options.addOption("oc", "output-csv", true, "Store as CSV in path X");

        options.addOption("o", "output", true, "Output path");

        options.addOption("i", "input", true, "Input path (scans for JUnit test classes)");

        return options;
    }
}
