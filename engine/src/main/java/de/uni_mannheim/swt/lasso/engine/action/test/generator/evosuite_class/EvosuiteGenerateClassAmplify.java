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
import de.uni_mannheim.swt.lasso.engine.project.ProjectHelper;
import de.uni_mannheim.swt.lasso.engine.matcher.TestMatcher;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
// FIXME merge with EvosuiteGenerateClass
public class EvosuiteGenerateClassAmplify extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(EvosuiteGenerateClassAmplify.class);

    public static final String REPORT = "EvosuiteGenerateClass";

    private static final String POM_TEMPLATE_EVOSUITE =
            Mavenizer.getPomTemplate("/mavenizer/pom_evosuite_generate_class.template");

    @LassoInput(desc = "Evosuite search budget in seconds", optional = true)
    public int searchBudget = 60;
    @LassoInput(desc = "Evosuite stopping criterion", optional = true)
    public String stoppingCondition = "MaxTime";

    @LassoInput(desc = "Implementation specific time budget in seconds", optional = true)
    public Map<String, Integer> timeBudgetProvider;

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

    @LassoInput(desc = "Ignore missing reports", optional = true)
    public boolean ignoreMissingReport = false;

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

        final Systems existingExecutables;
        if(amplify) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Amplification enabled.");
            }

            ActionNode ancestorNode = context.getExecutionPlan().getAction(actionConfiguration.getDependsOn());

            // check if ancestor action supports testing (i.e handles tests)
            if (context.getActionManager().isTester(ancestorNode.getType())) {
                // existing executables
                existingExecutables = context.getLassoOperations().getExecutables(
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
            } else {
                existingExecutables = null;
            }
        } else {
            existingExecutables = null;
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

                    int timeBudget = getTimeBudgetFor(implementation);

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
                    if(existingExecutables != null) {


                        // TODO transfer project
                        //executable.getImplementation().getWorkerNodeId();

                        System existingExecutable = null;
                        try {
                            existingExecutable = existingExecutables.getExecutable(executable.getId());
                        } catch (Throwable e) {
                            //
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Did not find existing executable for '{}'", executable.getId());
                                LOG.warn("Exception thrown", e);
                            }

                            return false;
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
    public int getTimeBudgetFor(CodeUnit implementation) {
        if(MapUtils.isEmpty(timeBudgetProvider)) {
            return searchBudget;
        }

        return timeBudgetProvider.getOrDefault(implementation.getId(), timeBudgetProviderDefault);
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
        long timeout = searchBudget * 4 * 1000L;

        if(repetitions > 1) {
            timeout *= repetitions;
        }

        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this, timeout) {

            @Override
            public void onProjectFailed(String time, String executableId) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("PROJECT FAILED FOR '{}'", executableId);
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
                if(LOG.isDebugEnabled()) {
                    LOG.debug("EVOSUITE GENERATION FAILED FOR '{}'. Reason => {}", executableId, cause);
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
    }

    @Override
    public List<RecordCollector> createCollectors() {
        EvosuiteClassCollector evosuiteClassCollector = new EvosuiteClassCollector(true, REPORT);
        evosuiteClassCollector.setIgnoreMissingReport(ignoreMissingReport);

        return Arrays.asList(
                evosuiteClassCollector);
    }

    void doReportCause(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, EvosuiteExecutionReport evosuiteExecutionReport) {
        // FIXME gets overridden if we don't use different permId each time
        try {
            Optional<System> implementationOptional = actionConfiguration.getAbstraction().getImplementations().stream().filter(i -> i.getId().equals(executableId)).findFirst();
            ReportKey reportKey = ReportKey.of(EvosuiteGenerateClassAmplify.this,
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
