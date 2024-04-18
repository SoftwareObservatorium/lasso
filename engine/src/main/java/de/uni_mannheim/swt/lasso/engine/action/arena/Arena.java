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
package de.uni_mannheim.swt.lasso.engine.action.arena;

import de.uni_mannheim.swt.lasso.benchmark.Benchmark;
import de.uni_mannheim.swt.lasso.benchmark.FunctionalAbstraction;
import de.uni_mannheim.swt.lasso.benchmark.Test2XSLX;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.cluster.client.ClusterArenaJobRepository;
import de.uni_mannheim.swt.lasso.cluster.client.JobStatus;
import de.uni_mannheim.swt.lasso.cluster.data.repository.ExecKey;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Tester;
import de.uni_mannheim.swt.lasso.engine.action.test.TestUtils;
import de.uni_mannheim.swt.lasso.engine.action.test.generator.evosuite_class.EvoSuite;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;
import de.uni_mannheim.swt.lasso.engine.action.utils.SequenceUtils;
import de.uni_mannheim.swt.lasso.engine.adaptation.SystemAdapterReport;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.engine.environment.ArenaExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.project.ProjectHelper;
import de.uni_mannheim.swt.lasso.engine.matcher.TestMatcher;
import de.uni_mannheim.swt.lasso.sandbox.container.support.ArenaContainer;
import de.uni_mannheim.swt.lasso.srm.ClusterSRMRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import javax.cache.Cache;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arena action.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Arena action")
@Stable
@Tester
public class Arena extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Arena.class);

    protected static final String ARENA_LOG_TXT = "arena_log.txt";

    public static String POM_TEMPLATE = "pom.template";

    @LassoInput(desc = "Specification (MQL)", optional = false)
    public String specification = "";

    @LassoInput(desc = "Enable features", optional = true)
    public List<String> features = new LinkedList<>();

    @LassoInput(desc = "Task to execute in arena (default 'Execute')", optional = false)
    public String task = "";

    @LassoInput(desc = "Process reference implementation only", optional = true)
    public boolean referenceImplementationOnly = false;

    // TODO remove, use sequences instead
    @Deprecated
    @LassoInput(desc = "Sheets (deprecated, use 'sequences' instead)", optional = true)
    public Map<String, Object> sheets;

    @LassoInput(desc = "Provide Sequence Sheets", optional = true)
    public Map<String, Object> sequences;

    @LassoInput(desc = "Provide JUnit test classes (currently mutually exclusive to 'sequences')", optional = true)
    public Map<String, String> testClasses;

    @LassoInput(desc = "Use Sequences provided by benchmark with given id", optional = true)
    public String benchmark;
    @LassoInput(desc = "Use Sequences provided by benchmark", optional = true)
    public boolean noTestsFromBenchmark;

    @LassoInput(desc = "Obtain stored Sequences from the following actions", optional = true)
    public List<String> sequenceActions = Collections.emptyList();

    @LassoInput(desc = "Mine tests from given action (abstraction name is assumed to be the same, adds all tests to all systems)", optional = true)
    public String populateTestsFromAction = null;

    @LassoInput(desc = "Fully-qualified class name of CUT", optional = false)
    public String cut = "";

    @LassoInput(desc = "max. no. adaptations allowed", optional = true)
    public int maxAdaptations = 1;

    @LassoInput(desc = "generate JUnit tests?", optional = true)
    public boolean generateJUnitTests = false;

    @LassoInput(desc = "export SRM as CSV?", optional = true)
    public boolean exportCsv = false;

    @LassoInput(desc = "Ignore visibility (access to constructors/methods)", optional = true)
    public boolean ignoreVisibility = true;

    @LassoInput(desc = "Write sequence records to SRM", optional = true)
    public boolean writeSequenceRecords = true;

    @LassoInput(desc = "Arena Container timeout in millis", optional = true)
    public long containerTimeout = 5 * 60 * 60 * 1000L;

//    @LassoInput(desc = "Maven Repository URL", optional = true)
//    public String mavenRepository;

    /**
     * Adapt by sequence specification or by entire set of sequence specifications.
     */
    @LassoInput(desc = "adapt by sequence specification (true) or by all (false)", optional = true)
    private boolean adaptBySequenceSpecification = false;

    private ArenaExecutionEnvironment arenaExecutionEnvironment;

    /**
     * scope-aware measurements
     */
    private Scope scope;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        // set scope
        scope = actionConfiguration.getScope();

        TestAdaptationManager testAdaptationManager = new TestAdaptationManager(context);

        Systems executables = null;

        // found reference implementation
        System referenceImpl;
        if(LassoUtils.isValidReferenceImplementation(actionConfiguration.getAbstraction())) {
            referenceImpl = getEvoSuiteReferenceImplementation(context, LassoUtils.getReferenceImplementationFromAlternatives(actionConfiguration.getAbstraction()));

            LOG.info("Found reference implementation '{}'", referenceImpl);

            // add to abstraction
            actionConfiguration.getAbstraction().getSystems().add(referenceImpl);
        } else {
            referenceImpl = null;
        }

        final Systems fromAbstraction;
        if(StringUtils.isNotBlank(populateTestsFromAction)) {
            fromAbstraction = context.getLassoOperations().getExecutables(
                    context.getExecutionId(), actionConfiguration.getAbstraction().getName(), populateTestsFromAction);
        } else {
            fromAbstraction = null;
        }

        ActionNode ancestorNode = context.getExecutionPlan().getAction(actionConfiguration.getDependsOn());


        // FIXME for now, set existing to null if fromAbstraction set

        // check if ancestor action supports testing (i.e handles tests)
        if (fromAbstraction == null && context.getActionManager().isTester(ancestorNode.getType())) {
            // existing executables
            final Systems existingExecutables = context.getLassoOperations().getExecutables(
                    context.getExecutionId(), actionConfiguration.getAbstraction().getName(), ancestorNode.getName());
            // reject any not in actionConfiguration
            if(existingExecutables.hasExecutables()) {
                // reject any not part of the "partition block" received by this action
                existingExecutables.getExecutables()
                        .removeIf(executable -> actionConfiguration.getAbstraction().getImplementations().stream().noneMatch(impl -> executable.getId().equals(impl.getId())));
            }

            if (LOG.isDebugEnabled()) {
                existingExecutables.getExecutables().forEach(e -> LOG.debug("Existing for {} {}", ancestorNode.getName(), e.getId()));
            }

            //
            executables = testAdaptationManager.initNew(this,
                    getInstanceId(),
                    actionConfiguration.getAbstraction(),
                    POM_TEMPLATE,
                    (system, candidate, valueMap) -> {
                        // for methods only
                        if (system.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                            // limit permutations to method signature only
                            valueMap.put("bytecodename", system.getBytecodeName());
                        } else {
                            valueMap.put("bytecodename", "");
                        }
                    },
                    executable -> {
//                        // reject alt impls
//                        if(referenceImplementationOnly && !StringUtils.equals(executable.getId(), actionConfiguration.getAbstraction().getName())) {
//                            if(LOG.isWarnEnabled()) {
//                                LOG.warn("Rejecting '{}', since it is not a reference implementation.", executable.getId());
//                            }
//
//                            return false;
//                        }

                        // TODO transfer project
                        //executable.getImplementation().getWorkerNodeId();

                        // copy from previous "Tester"
                        // TODO we don't need it anymore, since System.getProject ships with it
                        System existingExecutable = null;
                        try {
                            existingExecutable = existingExecutables.getExecutable(executable.getId());
                        } catch (Throwable e) {
                            //
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Did not find existing executable for '{}'", executable.getId());
                                LOG.warn("Exception thrown", e);
                            }

                            // check if it is the ref impl.
                            if(referenceImpl != null && executable.getId().equals(referenceImpl.getId())) {
                                existingExecutable = referenceImpl;
                            } else {
                                return false;
                            }
                        }

//                        //
//                        if (existingExecutable.hasExecutionSignatures()) {
//                            // copy over best match json
//                            try {
//                                TestAdaptationManager.copyBestMatchReport(existingExecutable, executable, context);
//                            } catch (Throwable e) {
//                                LOG.warn("Stack trace:", e);
//                            }
//
//                            // set best match in model
//                            try {
//                                executable.setExecutionSignatures(Arrays.asList(AdaptationUtils.getBestMatches(existingExecutable)));
//                            } catch (Throwable e) {
//                                LOG.warn("Stack trace:", e);
//                            }
//                        }

                        // if remote project
                        boolean remote = true;
                        if (remote) {
                            LOG.info("Copying tests from executable '{}'", executable.getId());
                            try {
                                ProjectHelper.copyTestsFromRemote(context, actionConfiguration, executable, existingExecutable);
                            } catch (Throwable e) {
                                LOG.warn("Stack trace:", e);

                                return false;
                            }
                        } else {
                            // local project

                            // copy adapter class source + test class source (only in case of EvoSuite)
                            try {
                                executable.getProject().copySrcFrom(existingExecutable.getProject(), true);
                            } catch (Throwable e) {
                                LOG.warn("Stack trace:", e);
                            }

                            // check test pattern
                            if (actionConfiguration.hasIncludeTestsPattern()) {
                                TestMatcher testMatcher = new TestMatcher();
                                List<File> testClasses = testMatcher.findMatches(actionConfiguration.getIncludeTestsPattern(), executable.getProject().getSrcTest());

                                if (CollectionUtils.isEmpty(testClasses)) {
                                    LOG.warn("No test classes matched for pattern '{}' on executable '{}'", actionConfiguration.getIncludeTestsPattern(), executable.getId());

                                    return false;
                                }

                                List<File> ftbr = testMatcher
                                        .findMismatches(actionConfiguration.getIncludeTestsPattern(), executable.getProject().getSrcTest());
                                ftbr.stream().peek(f -> LOG.debug("deleting test class mismatch '{}'. Pattern '{}'", f, actionConfiguration.getIncludeTestsPattern()))
                                        .forEach(FileUtils::deleteQuietly);
                            }
                        }

                        return true;
                    });
        } else {
            // empty projects
            executables = testAdaptationManager.initNew(this,
                    getInstanceId(),
                    actionConfiguration.getAbstraction(),
                    POM_TEMPLATE,
                    (implementation, candidate, valueMap) -> {
                        // for methods only
                        if (implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                            // limit permutations to method signature only
                            valueMap.put("bytecodename", implementation.getBytecodeName());
                        } else {
                            valueMap.put("bytecodename", "");
                        }
                    },
                    executable -> {
                        // read test classes
                        if (MapUtils.isNotEmpty(testClasses) && testClasses.containsKey(executable.getId())) {
                            LOG.info("Copying tests from testClasses map for executable '{}'", executable.getId());
                            try {
                                TestUtils.setUpTestClass(executable, testClasses.get(executable.getId()));
                            } catch (Throwable e) {
                                LOG.warn("Setting up test classes for '{}' failed", executable.getId());
                                LOG.warn("Stack trace:", e);

                                return false;
                            }
                        }

                        // read from given systems
                        if(fromAbstraction != null) {
                            LOG.info("Populating tests from given abstraction '{}' from action '{}'", fromAbstraction.getAbstractionName(), populateTestsFromAction);

                            for(System system : fromAbstraction.getExecutables()) {
                                try {
                                    ProjectHelper.copyTestsFromRemoteWithPostfix(context, actionConfiguration, executable, system);
                                } catch (Throwable e) {
                                    LOG.warn("Stack trace:", e);

                                    //return false;
                                }
                            }
                        }

                        // copy from reference impl.
                        if(referenceImpl != null && referenceImpl.getId().equals(executable.getId())) {
                            LOG.info("Copying tests from executable '{}'", executable.getId());
                            try {
                                ProjectHelper.copyTestsFromRemote(context, actionConfiguration, executable, referenceImpl);
                            } catch (Throwable e) {
                                LOG.warn("Stack trace:", e);

                                return false;
                            }
                        }

                        return true;
                    });
        }

        Validate.notNull(executables, "Executables are null");

        // set
        setExecutables(executables);

        if(executables.getExecutables().size() < 1) {
            LOG.warn("No executables to process. Returning ...");

            return;
        }

        // set additional sheets?
        processSheets(context, actionConfiguration, executables);

        // create job
        ArenaJob job = createJob(context, actionConfiguration, executables);
        // put
        ClusterEngine clusterEngine = context.getConfiguration().getService(ClusterEngine.class);
        ClusterArenaJobRepository jobRepository = clusterEngine.getArenaJobRepository();
        jobRepository.put(job.getId(), job);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();

        // also make sure to init report caches

        try {
            // for storing adaptations
            clusterEngine.getOrCreateReportCache(context.getExecutionId(), SystemAdapterReport.class);
        } catch (Throwable e) {
            LOG.warn("Setting up caches failed", e);
        }

        // executable corpus
        ExecutableCorpus corpus = context.getConfiguration().getExecutableCorpus();

        // args passed to arena
        List<String> args = new ArrayList<>(Arrays.asList(
                "java",
                "-Xmx4096m", // memory
                "-XX:+IgnoreUnrecognizedVMOptions", // issue #417
                "-Dsun.misc.URLClassPath.disableJarChecking=true", // prevents classloading problems with java11
                // required for Java >= 17
                "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED",
                "--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED",
                "--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                // -- end Java >= 17
                "-jar",
                "/var/arena/support/arena-1.0.0-SNAPSHOT.jar",
                "--mode", "distributed",
                "--lasso-addresses", "127.0.0.1:10800",
                "--lasso-job", job.getId(),
                "--work-dir", "/var/arena",
                //"--generate-junit", "/var/arena",
                //"--output-csv", "/var/arena/mycsv_"+System.currentTimeMillis()+".csv",
                "--input", ArenaContainer.WD_DEFAULT,
                "--output", ArenaContainer.WD_DEFAULT,
                "--repository-url", corpus.getArtifactRepository().getUrl(),
                "&>" + ARENA_LOG_TXT
        ));

        if (CollectionUtils.isNotEmpty(features)) {
            args.add("--features");
            args.add(features.stream().collect(Collectors.joining(",")));
        }

        if (StringUtils.isNotBlank(task)) {
            args.add("--task");
            args.add(task);
        }

        // set default commands
        Environment environment = actionConfiguration.getProfile().getEnvironment();
        if (CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        environment.getCommandArgsList().add(args);

        ArenaProjectManager manager = new ArenaProjectManager(context);
        arenaExecutionEnvironment = manager.createExecutionEnvironment(this, actionConfiguration, environment);
        // set container timeout
        if(LOG.isInfoEnabled()) {
            LOG.info("Setting arena container timeout to '{}'", containerTimeout);
        }
        arenaExecutionEnvironment.setExecutionTimeout(containerTimeout);

        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        executionEnvironmentManager.run(arenaExecutionEnvironment);

        // collect data

//        // show logs
//        if(LOG.isDebugEnabled()) {
//            LOG.debug("process stdout => '{}'", arenaExecutionEnvironment.getLogs());
//        }

        // check job status
        ArenaJob updatedJob = jobRepository.get(job.getId());

        if (LOG.isInfoEnabled()) {
            LOG.info("Arena job status is '{}'", updatedJob.getStatus());
        }

        if (updatedJob.getStatus() == JobStatus.FINISHED) {
            LOG.info("Arena job finished successfully");
        } else {
            LOG.warn("Arena job failed");
        }

//        // FIXME do filtering on failed ones
//        List<Executable> executableList = actionConfiguration.getAbstraction().getImplementations().stream()
//                .map(impl -> {
//                    Executable executable = new Executable();
//                    executable.setImplementation(impl);
//                    return executable;
//                }).collect(Collectors.toList());
//
//        // set setExecutables() to be compliant with other actions
//        Executables executables = new Executables();
//        executables.setExecutables(executableList);
//        executables.setAbstractionName(actionConfiguration.getAbstraction().getName());
//        executables.setActionInstanceId(getInstanceId());
//        setExecutables(executables);

        // collect sequences
        collectSequences(context, executables);

        //
        if (exportCsv) {
            LOG.info("Exporting SRMs to CSV");

            try {
                Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", context.getExecutionId());

                File projectRoot = context.getWorkspace().getRoot(getInstanceId(), actionConfiguration.getAbstraction());

                table.write().csv(new File(projectRoot, "srm.csv"));
            } catch (Throwable e) {
                LOG.warn("Export CSV failed", e);
            }
        }

        // FIXME do filtering of executables after execution
        // either signaling or timestamps of files etc.

        // FIXME set "sequences"
    }

    private System getEvoSuiteReferenceImplementation(LSLExecutionContext context, String refImplId) {
        ActionNode evosuiteNode = context.getExecutionPlan().getAncestor(this, EvoSuite.class);
        Cache.Entry<ExecKey, System> executableEntry = null;
        try {
            executableEntry = context.getLassoOperations().getExecutableFromAction(context.getExecutionId(), evosuiteNode.getName(), refImplId);
        } catch (Throwable e) {
            //throw new RuntimeException(e);

            // try other actions
            List<ActionNode> actionNodes = context.getExecutionPlan().queryActionsByType(EvoSuite.class);

            for(ActionNode node : actionNodes) {
                LOG.debug("Trying action node '{}'", node.getName());
                try {
                    executableEntry = context.getLassoOperations().getExecutableFromAction(context.getExecutionId(), node.getName(), refImplId);
                    if(executableEntry != null) {
                        break;
                    }
                } catch (Exception ex) {
                    //throw new RuntimeException(e);
                }
            }
        }

        return executableEntry.getValue();
    }

    private void processSheets(LSLExecutionContext context, ActionConfiguration actionConfiguration, Systems executables) {
        //
        if(sequences != null) {
            LOG.warn("Overriding sheets with sequences");
            sheets = sequences;
        }

        // write manual sheets
        if (MapUtils.isNotEmpty(sheets)) {
            SequenceUtils.toXlsx(sheets, this, executables);
        }

        // benchmark set?
        if(benchmark != null) {
            LOG.info("Trying to load sequences from benchmark '{}' using abstraction '{}'", benchmark, executables.getAbstractionName());
            Benchmark b = context.getBenchmarkManager().load(benchmark);

            FunctionalAbstraction ab = b.getAbstractions().get(executables.getAbstractionName());

            if(!noTestsFromBenchmark) {
                Test2XSLX test2XSLX = new Test2XSLX();

                int s = 0;
                for(de.uni_mannheim.swt.lasso.benchmark.Sequence sequence : ab.getSequences()) {
                    String name = sequence.getId() + "_" + s;
                    LOG.info("Writing sheet '{}'", name);

                    Sequence seq = new Sequence();
                    seq.setName(name);
                    seq.setId(getName() + "_" + name);
                    seq.setActionId(getName());
                    executables.addSequence(seq);

                    try {
                        XSSFSheet xssfSheet = test2XSLX.createSheet(sequence, name);

                        for (System executable : executables.getExecutables()) {
                            try {
                                LOG.info("Writing sheet '{}' for '{}'", name, executable.getId());

                                test2XSLX.write(executable, xssfSheet, name);
                            } catch (Throwable e) {
                                LOG.warn("Failed to write sheet for '{}'", executable.getId());
                                LOG.warn("stack trace", e);
                            }
                        }
                    } catch (Throwable e) {
                        LOG.warn("Failed to read sheet '{}'", name);
                        LOG.warn("stack trace", e);
                    }

                    s++;
                }
            }

            // also set specification from benchmark
            specification = ab.getLql();

            LOG.info("Specification set from benchmark '{}' to '{}'", benchmark, specification);
        }

        // generated sequences available? (TestGen actions)
        if(CollectionUtils.isNotEmpty(sequenceActions)) {
            List<de.uni_mannheim.swt.lasso.benchmark.Sequence> collectedSequences =
                    SequenceUtils.collectSequences(context, executables, sequenceActions);
            // write sequences locally for each candidate
            SequenceUtils.writeSequences2Xlsx(this, getExecutables(), collectedSequences, executables.getSpecification());
        }
    }

    private void collectSequences(LSLExecutionContext context, Systems executables) {
        //
        for (System executable : executables.getExecutables()) {
            // make tests available in filesystem
            List<File> testClasses = executable.getProject().getFiles(executable.getProject().getSrcTest(), "java");
            testClasses.forEach(file -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Writing test to remote filesystem '{}'", file.getAbsolutePath());
                }

                try {
                    context.getLassoFileSystem().write(file.getAbsolutePath(), file);
                } catch (Throwable e) {
                    LOG.warn("Failed to write test '{}'", file.getAbsolutePath());
                    LOG.warn("Stack trace:", e);
                }
            });
        }
    }

    private ArenaJob createJob(LSLExecutionContext context, ActionConfiguration actionConfiguration, Systems executables) {
        ArenaJob job = new ArenaJob();
        job.setId(String.format("%s_%s", getInstanceId(), UUID.randomUUID()));
        job.setExecutionId(context.getExecutionId());
        job.setWorkerNode(context.getWorkerNodeId());
        job.setAbstractionId(actionConfiguration.getAbstraction().getName());
        job.setActionId(getName());
        job.setBySequenceSpecification(adaptBySequenceSpecification);
        job.setImplementations(executables.getExecutables());

        if(StringUtils.isBlank(specification) && actionConfiguration.getAbstraction().getSpecification() != null) {
            if(actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification() != null) {
                LOG.debug("Found specification in abstraction '{}'", actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification());

                job.setSpecification(actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification().getLqlQuery());
            }
        } else {
            job.setSpecification(specification); // FIXME validate specification
        }

        //job.setSheets(sheets);
        if (StringUtils.isNotBlank(cut)) {
            job.setCut(cut);
        }

        job.getConfiguration().put("perms", maxAdaptations);
        job.getConfiguration().put("generateTests", generateJUnitTests);
        job.getConfiguration().put("ignoreVisibility", ignoreVisibility);
        job.getConfiguration().put("writeSequenceRecords", writeSequenceRecords);

        // set scope
        job.setScope(scope);

        // amplify
        job.setReferenceImplementationOnly(referenceImplementationOnly);

        // set threads
        int threads = Runtime.getRuntime().availableProcessors() / 2;

        LOG.debug("Setting threads to '{}'", threads);

        job.setThreads(threads);

        return job;
    }

    /**
     * Stop execution environment NOW.
     */
    public void stopNow() {
        if (arenaExecutionEnvironment != null) {
            try {
                arenaExecutionEnvironment.kill();
            } catch (Throwable e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Killing Arena execution environment failed", e);
                }
            }
        }
    }
}
