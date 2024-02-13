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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.evosuite_class;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Tester;
import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.DefaultMavenActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Evosuite class-level action.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://github.com/EvoSuite/evosuite/blob/master/client/src/main/java/org/evosuite/Properties.java">Properties</a>
 */
@LassoAction(desc = "Run Evosuite Test Generation")
@Stable
@Tester // handles tests
public class EvosuiteGenerateClass extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(EvosuiteGenerateClass.class);

    public static final String REPORT = "EvosuiteGenerateClass";

    private static final String POM_TEMPLATE_EVOSUITE =
            Mavenizer.getPomTemplate("/mavenizer/pom_evosuite_generate_class.template");

    @LassoInput(desc = "Evosuite search budget in seconds", optional = true)
    public int searchBudget = 60;
    @LassoInput(desc = "Evosuite stopping criterion", optional = true)
    public String stoppingCondition = "MaxTime";

    @LassoInput(desc = "Hard kill timeout in seconds for evosuite client processes (overrides timeoutMultiplier)", optional = true)
    public int timeoutClientProcess = -1;

    @LassoInput(desc = "Grace period before evosuite client process is killed. timeoutMultiplier times searchBudget", optional = true)
    public int timeoutMultiplier = 3;

    @LassoInput(desc = "Implementation specific time budget in seconds (by implementation)", optional = true)
    public Map<String, Integer> timeBudgetProviderByImpl;
    @LassoInput(desc = "Implementation specific time budget in seconds (by abstraction)", optional = true)
    public Map<String, Integer> timeBudgetProviderByAbstraction;

    @LassoInput(desc = "timeBudgetProvider fallback if implementation is missing", optional = true)
    public int timeBudgetProviderDefault = 120;

    @LassoInput(desc = "Evosuite criteria", optional = true)
    public String criteria = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH";

    /** see org.evosuite.statistics.RuntimeVariable.CONSTANTS */
    @LassoInput(desc = "Evosuite report output columns", optional = true)
    //public String outputVariables = "TARGET_CLASS,criterion,Coverage,Total_Goals,Covered_Goals,Classpath_Classes,Analyzed_Classes,Total_Branches,Covered_Branches,Lines,Covered_Lines,Total_Methods,Covered_Methods,Branchless_Methods,Statements_Executed,Tests_Executed,Size,Length,Total_Time";
    public String outputVariables = "configuration_id,TARGET_CLASS,criterion,Coverage,Total_Goals,Covered_Goals,Lines,Covered_Lines,LineCoverage,Statements_Executed,Total_Branches,Covered_Branches,BranchCoverage,CBranchCoverage,Total_Methods,Covered_Methods,Mutants,WeakMutationScore,MutationScore,Size,Result_Size,Length,Result_Length,Total_Time";
    @LassoInput(desc = "Repeat execution of EvoSuite N times", optional = true)
    public int repetitions = 1;

    @LassoInput(desc = "Implementation specific number of repetitions", optional = true)
    public Map<String, Integer> repetitionsProvider;

    @LassoInput(desc = "repetitionsProvider fallback if implementation is missing", optional = true)
    public int repetitionsProviderDefault = 1;

    @LassoInput(desc = "Generate assertions", optional = true)
    public boolean assertions = true;

    @LassoInput(desc = "Generate tests for given method signature (if method and NOT class signature)", optional = true)
    public boolean methodOnly = false;

    @LassoInput(desc = "Amplification mode", optional = true)
    public boolean amplify = false;

    @LassoInput(desc = "Amplification strategy", optional = true)
    public String amplificationStrategy = AmplificationStategy.EVOSUITE_MULTI_CRITERIA_COVERAGE.name();

    @LassoInput(desc = "Ignore missing reports", optional = true)
    public boolean ignoreMissingReport = false;

    @LassoInput(desc = "EvoSuite version", optional = true)
    public String version = "1.2.0";

//    @LassoInput(desc = "stop after total time budget of X is consumed (applies to entire cluster of worker nodes)", optional = true)
//    public int stopAfterTotalTimeBudget = -1;

    @LassoInput(desc = "stop after X successful classes (applies to entire cluster of worker nodes)", optional = true)
    public int stopAfter = -1;

    @LassoInput(desc = "Remove those executables without generated tests", optional = true)
    public boolean cleanExecutables = true;

    /**
     * @author Marcus Kessel
     */
    public enum AmplificationStategy {
        EVOSUITE_MULTI_CRITERIA_COVERAGE,
        EVOSUITE_FIRST,
        EVOSUITE_BRANCH,
        EVOSUITE_CBRANCH,
        EVOSUITE_LINE,
        EVOSUITE_MUTATION
    }

    /**
     * To prevent too much memory consumption, use a limited set of threads.
     *
     * @param context
     * @return
     */
    @Override
    protected String getMavenParallelThreads(LSLExecutionContext context) {
        String mavenThreads = super.getMavenParallelThreads(context);
        try {
            int threads = Integer.parseInt(mavenThreads);

            threads /= 2;
            if(threads < 1) {
                return "1";
            } else {
                return String.valueOf(threads);
            }
        } catch (NumberFormatException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Cannot parse threads '{}'", mavenThreads);
            }

            return mavenThreads;
        }
    }

    @Override
    protected MavenProjectManager createManager(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        TestAdaptationManager testAdaptationManager = new TestAdaptationManager(context);

        Systems evosuiteExecutables;
        AmplificationStategy strategy;
        if(amplify && StringUtils.isNotBlank(amplificationStrategy)) {
            strategy = AmplificationStategy.valueOf(amplificationStrategy);

            if(LOG.isInfoEnabled()) {
                LOG.info("Found amplification strategy {}. Fetching executables from EvoSuite run.", strategy);
            }

            ActionNode ancestorNode = context.getExecutionPlan().getAction(actionConfiguration.getDependsOn());
            evosuiteExecutables = context.getLassoOperations().getExecutables(
                    context.getExecutionId(), actionConfiguration.getAbstraction().getName(), ancestorNode.getName());
        } else {
            evosuiteExecutables = null;
            strategy = null;
        }

        Systems executables = testAdaptationManager.initNew(this,
                getInstanceId(),
                actionConfiguration.getAbstraction(),
                POM_TEMPLATE_EVOSUITE,
                (implementation, candidate, valueMap) -> {
                    // for methods only
                    if(implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                        // limit permutations to method signature only
                        valueMap.put("bytecodename", implementation.getBytecodeName());
                    } else {
                        valueMap.put("bytecodename", "");
                    }

                    // class
                    valueMap.put("lassoClass", implementation.toFQName());
                    // how many times
                    int noOfRepetitions = getRepetitionsFor(implementation);
                    valueMap.put("lassoRepetitions", String.valueOf(noOfRepetitions));

                    int timeBudget = getTimeBudgetFor(actionConfiguration.getAbstraction(), implementation);

                    // evosuite arguments
                    String evosuiteExtraArgs = String.format(
                            "-Dshow_progress=false -Dsearch_budget=%s -Dstopping_condition=%s -Doutput_variables=%s -DTARGET_CLASS=%s -Djunit_tests=%s -Djunit_suffix=%s -Dtest_scaffolding=%s -Dinline=true -Dassertions=%s -Dignore_missing_statistics=true -Dtest_comments=true -Duse_separate_classloader=false",
                            timeBudget, stoppingCondition, outputVariables, implementation.toFQName(), true, "Test", false, assertions);

                    String extraArgs = evosuiteExtraArgs;
                    if(methodOnly && implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                        // tell which method (see https://github.com/EvoSuite/evosuite/issues/209)
                        String bcMethodName = StringUtils.substringAfterLast(implementation.getBytecodeName(), ".");

                        // -Dtarget_method_list=
                        extraArgs = String.format("%s -Dtarget_method_list=%s", evosuiteExtraArgs, bcMethodName);
                    }

                    valueMap.put("evosuiteExtraArgs", extraArgs);
                },
                executable -> {
                    // amplification mode, put tests in src/test/java + we need to compile as well!
                    if(evosuiteExecutables != null) {
                        System existingExecutable = null;
                        try {
                            existingExecutable = evosuiteExecutables.getExecutable(executable.getId());
                        } catch (Throwable e) {
                            //
                            if(LOG.isWarnEnabled()) {
                                LOG.warn("Did not find existing executable for '{}'", executable.getId());
                            }

                            return false;
                        }

                        Workspace workspace = context.getWorkspace();
                        Collection<File> testClasses = workspace.listFilesRecursively(
                                existingExecutable.getProject(), EvosuiteClassCollector.EVOSUITE_TESTS, "java");

                        if(CollectionUtils.isEmpty(testClasses)) {
                            LOG.warn("No test classes found for " + executable.getId());

                            return false;
                        }

                        ReportOperations reportOperations = context.getReportOperations();

                        final AtomicInteger index = new AtomicInteger(-1);
                        switch (strategy) {
                            case EVOSUITE_FIRST: {
                                // first one = index 0
                                index.set(0);

                                break;
                            }
                            case EVOSUITE_LINE: {
                                // select best test generation based on LINECOVERAGE
                                try {
                                    // FIXME limit to action = getName()
                                    Table table = reportOperations.select(context.getExecutionId(),
                                            "SELECT PERMID, LINECOVERAGE from EVOSUITEGENERATECLASS where IMPLEMENTATION = '"
                                                    + executable.getId()
                                                    + "' ORDER BY LINECOVERAGE DESC LIMIT 1");
                                    index.set(table.intColumn("PERMID").getInt(0) - 1); // -1
                                } catch (Throwable e) {
                                    LOG.warn("Failed to select best LINECOVERAGE for " + executable.getId(), e);
                                }

                                break;
                            }
                            case EVOSUITE_BRANCH: {
                                // select best test generation based on BRANCHCOVERAGE
                                try {
                                    Table table = reportOperations.select(context.getExecutionId(),
                                            "SELECT PERMID, BRANCHCOVERAGE from EVOSUITEGENERATECLASS where IMPLEMENTATION = '"
                                                    + executable.getId()
                                                    + "' ORDER BY BRANCHCOVERAGE DESC LIMIT 1");
                                    index.set(table.intColumn("PERMID").getInt(0) - 1); // -1
                                } catch (Throwable e) {
                                    LOG.warn("Failed to select best BRANCHCOVERAGE for " + executable.getId(), e);
                                }

                                break;
                            }
                            case EVOSUITE_CBRANCH: {
                                // select best test generation based on CBRANCHCOVERAGE
                                try {
                                    Table table = reportOperations.select(context.getExecutionId(),
                                            "SELECT PERMID, CBRANCHCOVERAGE from EVOSUITEGENERATECLASS where IMPLEMENTATION = '"
                                                    + executable.getId()
                                                    + "' ORDER BY CBRANCHCOVERAGE DESC LIMIT 1");
                                    index.set(table.intColumn("PERMID").getInt(0) - 1); // -1
                                } catch (Throwable e) {
                                    LOG.warn("Failed to select best CBRANCHCOVERAGE for " + executable.getId(), e);
                                }

                                break;
                            }
                            case EVOSUITE_MUTATION: {
                                // select best test generation based on WEAKMUTATIONSCORE
                                try {
                                    Table table = reportOperations.select(context.getExecutionId(),
                                            "SELECT PERMID, WEAKMUTATIONSCORE from EVOSUITEGENERATECLASS where IMPLEMENTATION = '"
                                                    + executable.getId()
                                                    + "' ORDER BY WEAKMUTATIONSCORE DESC LIMIT 1");
                                    index.set(table.intColumn("PERMID").getInt(0) - 1); // -1
                                } catch (Throwable e) {
                                    LOG.warn("Failed to select best WEAKMUTATIONSCORE for " + executable.getId(), e);
                                }

                                break;
                            }
                            case EVOSUITE_MULTI_CRITERIA_COVERAGE: {
                                // select best test generation based on COVERAGE
                                try {
                                    Table table = reportOperations.select(context.getExecutionId(),
                                            "SELECT PERMID, COVERAGE from EVOSUITEGENERATECLASS where IMPLEMENTATION = '"
                                                    + executable.getId()
                                                    + "' ORDER BY COVERAGE DESC LIMIT 1");
                                    index.set(table.intColumn("PERMID").getInt(0) - 1); // -1
                                } catch (Throwable e) {
                                    LOG.warn("Failed to select best COVERAGE for " + executable.getId(), e);
                                }

                                break;
                            }
                            default: throw new UnsupportedOperationException("Unknown strategy " + strategy);
                        }

                        // OK?
                        if(index.get() < 0) {
                            //
                            if(LOG.isWarnEnabled()) {
                                LOG.warn("Wasn't able to obtain test generation run for {} for strategy {}", executable.getId(), strategy);
                            }

                            return false;
                        } else {
                            if(LOG.isInfoEnabled()) {
                                LOG.info("Identified best test generation run with index = {}", index.get());
                            }
                        }

                        Optional<File> testClassOp = testClasses.stream()
                                .filter(testClass -> StringUtils.endsWith(testClass.getName(), String.format("_%s_Test.java", index.get())))
                                .findFirst();

                        if(!testClassOp.isPresent()) {
                            if(LOG.isWarnEnabled()) {
                                LOG.warn("Did not find evosuite test class for {} for strategy {}", executable.getId(), strategy);
                            }

                            return false;
                        }

                        // copy over testClass
                        try {
                            File testClass = testClassOp.get();

                            File evoSuiteDir = new File(existingExecutable.getProject().getBaseDir(), EvosuiteClassCollector.EVOSUITE_TESTS);

                            // rewrite path
                            File srcTestPkg = new File(StringUtils.replace(testClass.getParentFile().getAbsolutePath(), evoSuiteDir.getAbsolutePath(), executable.getProject().getSrcTest().getAbsolutePath()));
                            if(LOG.isInfoEnabled()) {
                                LOG.info("Creating src test pkg {}", srcTestPkg.getAbsolutePath());
                            }

                            srcTestPkg.mkdirs();

                            File to = new File(srcTestPkg, testClass.getName());
                            FileUtils.copyFile(testClass, to);

                            if(LOG.isInfoEnabled()) {
                                LOG.info("Copied test class to {}", to);
                            }
                        } catch (Throwable e) {
                            if(LOG.isWarnEnabled()) {
                                LOG.warn("Copying test class failed for {} for strategy {} and index {}", executable.getId(), strategy, index.get());
                            }

                            return false;
                        }
                    }


                    return true;
                });

        // set
        setExecutables(executables);

        if(getExecutables().hasExecutables()) {
            // write values report
            // publish schema
            try {
                ReportOperations reportOperations = context.getReportOperations();

                String[] variable = StringUtils.split(outputVariables, ",");
                reportOperations.newValuesReport(context.getExecutionId(), REPORT, Arrays.stream(variable).collect(Collectors.toMap(String::toUpperCase, v -> "java.lang.Double")));
            } catch (Throwable e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Failed to create report class", e);
                }
            }
        }

        return testAdaptationManager;
    }

    /**
     * Get time budget for given {@link CodeUnit}
     *
     * @param implementation
     * @return
     */
    public int getTimeBudgetFor(Abstraction abstraction, CodeUnit implementation) {
        // check by implementation
        if(MapUtils.isEmpty(timeBudgetProviderByImpl)) {
            // check by abstraction
            if(MapUtils.isEmpty(timeBudgetProviderByAbstraction)) {
                // fallback to default, since no custom budget defined
                return searchBudget;
            } else {
                // abstraction specific budget
                return timeBudgetProviderByAbstraction.getOrDefault(abstraction.getName(), timeBudgetProviderDefault);
            }
        }

        // implementation specific budget
        return timeBudgetProviderByImpl.getOrDefault(implementation.getId(), timeBudgetProviderDefault);
    }

    /**
     * Get repetitions for given {@link CodeUnit}
     *
     * @param implementation
     * @return
     */
    public int getRepetitionsFor(CodeUnit implementation) {
        if(MapUtils.isEmpty(repetitionsProvider)) {
            return repetitions;
        }

        return repetitionsProvider.getOrDefault(implementation.getId(), repetitionsProviderDefault);
    }

    private String readCauseFromLog(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId) {
        File logFile = getLogFile(actionConfiguration, context.getWorkspace());
        if(!logFile.canRead()) {
            return "n/a";
        }

        StringBuilder sb = new StringBuilder();
        try(LineIterator lineIterator = FileUtils.lineIterator(logFile)) {
            while(lineIterator.hasNext()) {
                String line = lineIterator.nextLine();

                if(StringUtils.contains(line, "[MASTER]")) {
                    sb.append(StringUtils.trimToEmpty(StringUtils.substringAfterLast(line, "]")));
                    sb.append("\n");
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    @Override
    protected DefaultMavenActionExecutionListener createListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        // also fires action listener ..

        // set individual timeout: 4 times higher to prevent process (KILLER) to terminate prematurely
        long timeout;
        if(timeoutClientProcess > -1) {
            LOG.info("Using hard timeout for evosuite client processes '{} secs'", timeoutClientProcess);

            timeout = timeoutClientProcess * 1000L;
        } else {
            timeout = searchBudget * timeoutMultiplier * 1000L;
        }

        if(repetitions > 1) {
            timeout *= repetitions;
        }

        LOG.info("Setting timeout for evosuite client processes to '{}' millis", timeout);

        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this, timeout) {

            /**
             * Custom killing for EvoSuite client processes.
             *
             * @param executableId
             */
            @Override
            protected void onTimeoutReached(String executableId) {
                System implementation = getExecutables().getExecutable(executableId);

                // KILL process
                try {
                    LOG.info("Killing evosuite client process with class name '{}' of id '{}'",
                            implementation.getCode().toFQName(), executableId);

                    EvoProcessKiller.kill9(implementation.getCode().toFQName());

                    if(LOG.isInfoEnabled()) {
                        LOG.info(String.format("Killed EvoSuite client process for '%s'",
                                executableId));
                    }
                } catch (Throwable e) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn(String.format("Was not able to kill EvoSuite client process for '%s'",
                                executableId), e);
                    }
                }
            }

            @Override
            public void onProjectFailed(String time, String executableId) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("PROJECT FAILED FOR '{}'", executableId);
                }

                EvosuiteExecutionReport evosuiteExecutionReport = new EvosuiteExecutionReport();
                evosuiteExecutionReport.setProjectFailed(true);
                evosuiteExecutionReport.setCause("");

                // ReportOperations
                doReportCause(context, actionConfiguration, executableId, evosuiteExecutionReport);

                super.onProjectFailed(time, executableId);
            }

            @Override
            public void onMojoFailed(String time, String executableId, String mojo, String cause) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("EVOSUITE GENERATION FAILED FOR '{}'. Reason => {}", executableId, cause);
                }

                EvosuiteExecutionReport evosuiteExecutionReport = new EvosuiteExecutionReport();
                evosuiteExecutionReport.setMojoFailed(true);
                evosuiteExecutionReport.setCause(cause);

                // ReportOperations
                doReportCause(context, actionConfiguration, executableId, evosuiteExecutionReport);

                super.onMojoFailed(time, executableId, mojo, cause);
            }
        };
        testListener.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));
        testListener.setAllowedMojos(Arrays.asList("evosuite-maven-plugin:generate"));

        return testListener;
    }

    @Override
    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        mavenDefaultCommand.addAll(Arrays.asList(
                "test-compile", // target/test-classes is required for amplification mode
                "evosuite:generate",
                // + criterion
                String.format("-Dcriterion=%s", criteria)
        ));

        return mavenDefaultCommand;
    }

    @Override
    public void postExecute(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, boolean success) {
        // check if tests exist
        System executable = getExecutables().getExecutable(executableId);
        int size = executable.getProject()
                .getFiles(executable.getProject().getSrcTest(), "java")
                .size();
//        if(size < 1) {
//            getExecutables().getExecutables().remove(executable); // drop
//
//            if(LOG.isWarnEnabled()) {
//                LOG.warn("Dropping executable '{}', since it has no tests", executable.getId());
//            }
//        }

        if (!success) {
            // report why failed
            EvosuiteExecutionReport evosuiteExecutionReport = new EvosuiteExecutionReport();
            evosuiteExecutionReport.setEvosuiteFailed(true);
            evosuiteExecutionReport.setCause("");

            // read cause
            try {
                evosuiteExecutionReport.setCause(readCauseFromLog(context, actionConfiguration, executableId));
            }catch (Throwable e) {
                e.printStackTrace();
            }

            // ReportOperations
            doReportCause(context, actionConfiguration, executableId, evosuiteExecutionReport);

            // remove
            removeExecutableConditionally(actionConfiguration, executableId, "Collection failed");
        }

        long durationInSecs = this.testListener.getDurations().get(executableId);

        LOG.info("Executable took '{}' seconds", durationInSecs);

        // ended prematurely and NO tests were generated
        if(durationInSecs < 10 && size < 1) {
            // consider it to be a fail

            // allow more impls
            LOG.warn("Executable finished prematurely with no tests '{}'", executable.getId());
        } else {
            //
            if(stopAfter > 0) {
                // get overall time consumed
                long totalDuration = this.testListener.getDurations().values().stream().mapToLong(l -> l).sum();

                // cap
                long maxDuration = (long) stopAfter * searchBudget;
                if(LOG.isInfoEnabled()) {
                    LOG.info("Total duration vs max duration => {} vs {}", totalDuration, maxDuration);
                }

                // stopAfter X successful implementations
                ClusterEngine clusterEngine = context.getConfiguration().getService(ClusterEngine.class);

                // use distributed set to collect number of successes
                IgniteSet<String> set = clusterEngine.getIgnite().set(
                        String.format("%s.%s.%s.implementations", context.getExecutionId(), getName(), actionConfiguration.getAbstraction().getName()),
                        new CollectionConfiguration());
                set.add(executableId); // add

                if(set.size() >= stopAfter) {
                    if(LOG.isInfoEnabled()) {
                        LOG.info("Reached maximum number of successful executables. Stopping action now.");
                    }

                    // broadcast event (engines takes care of killing us and other worker nodes)
                    context.getEventManager().fireKillAction(context.getExecutionId(), getName());

                    // TODO remove set(?)
                }
            }
        }

//        else {
//            // success
//
//            // get overall time consumed
//            long totalDuration = this.testListener.getDurations().values().stream().mapToLong(l -> l).sum();
//
//            // cap
//            long maxDuration = (long) stopAfter * searchBudget;
//            if(LOG.isInfoEnabled()) {
//                LOG.info("Total duration vs max duration => {} vs {}", totalDuration, maxDuration);
//            }
//
//            // FIXME or we could check individual duration per class .. if less than < X .. do another class
//
//            // FIXME when does an executable fail? based on budget or simply retry until we get to stopAfter?
//            if(stopAfter > 0) {
//                // stopAfter X successful implementations
//                ClusterEngine clusterEngine = context.getConfiguration().getService(ClusterEngine.class);
//
//                // use distributed set to collect number of successes
//                IgniteSet<String> set = clusterEngine.getIgnite().set(
//                        String.format("%s.%s.%s.implementations", context.getExecutionId(), getName(), actionConfiguration.getAbstraction().getName()),
//                        new CollectionConfiguration());
//                set.add(executableId); // add
//
//                if(set.size() >= stopAfter) {
//                    if(LOG.isInfoEnabled()) {
//                        LOG.info("Reached maximum number of successful executables. Stopping action now.");
//                    }
//
//                    // broadcast event (engines takes care of killing us and other worker nodes)
//                    context.getEventManager().fireKillAction(context.getExecutionId(), getName());
//
//                    // TODO remove set(?)
//                }
//            }
//        }
    }

    /**
     * Clean list of executables.
     */
    @Override
    protected void onClose() {
        if(LOG.isInfoEnabled()) {
            LOG.info("Cleaning up.");
        }

        Systems executables = getExecutables();
        if(!executables.hasExecutables()) {
            return;
        }

        // FIXME does not work when "amplify = true"
        if(cleanExecutables) {
            synchronized (executables.getExecutables()) {
                Iterator<System> it = executables.getExecutables().iterator();
                while(it.hasNext()) {
                    System executable = it.next();
                    int size = executable.getProject()
                            .getFiles(executable.getProject().getSrcTest(), "java")
                            .size();
                    if(size < 1) {
                        it.remove(); // drop

                        if(LOG.isWarnEnabled()) {
                            LOG.warn("Dropping executable '{}', since it has no tests", executable.getId());
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<RecordCollector> createCollectors() {
        EvosuiteClassCollector evosuiteClassCollector = new EvosuiteClassCollector(!amplify, REPORT);
        evosuiteClassCollector.setIgnoreMissingReport(ignoreMissingReport);

        return Arrays.asList(
                evosuiteClassCollector);
    }

    void doReportCause(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, EvosuiteExecutionReport evosuiteExecutionReport) {
        // FIXME gets overridden if we don't use different permId each time
        try {
            Optional<System> implementationOptional = actionConfiguration.getAbstraction().getImplementations().stream().filter(i -> i.getId().equals(executableId)).findFirst();
            ReportKey reportKey = ReportKey.of(EvosuiteGenerateClass.this,
                    actionConfiguration.getAbstraction().getName(), implementationOptional.get());

            // check if already exists
            EvosuiteExecutionReport report = context.getReportOperations().get(context.getExecutionId(), reportKey, EvosuiteExecutionReport.class);
            if(report == null) {
                context.getReportOperations().put(context.getExecutionId(), reportKey, evosuiteExecutionReport);
            }
        } catch (Throwable e) {
            LOG.warn("EvosuiteExecutionReport failed", e);
        }
    }
}
