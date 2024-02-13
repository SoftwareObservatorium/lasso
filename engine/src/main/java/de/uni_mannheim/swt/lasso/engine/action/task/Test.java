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
package de.uni_mannheim.swt.lasso.engine.action.task;

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
import de.uni_mannheim.swt.lasso.engine.action.test.TestUtils;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;
import de.uni_mannheim.swt.lasso.engine.action.test.support.surefire.SurefireCollector;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.project.ProjectHelper;
import de.uni_mannheim.swt.lasso.engine.matcher.TestMatcher;
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Maven surefire test action.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Surefire Test")
@Stable
@Tester
public class Test extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Test.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_testsurefire.template");

    @LassoInput(desc = "Measure Test Coverage?", optional = true)
    public boolean measureTestCoverage = true;

    @LassoInput(desc = "minimum test coverage", optional = false)
    public double minimumTestCoverage = 0d;

    // FIXME
    @LassoInput(desc = "drop failed tests from test class (rewrites test class)", optional = false)
    public boolean dropNonPassingTests = false;

    @LassoInput(desc = "drop non-compilable tests from test class (rewrites test class)", optional = false)
    public boolean dropNonCompilableTests = false;

    @LassoInput(desc = "JUnit test classes", optional = true)
    public Map<String, String> testClasses;

    @Override
    protected MavenProjectManager createManager(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        TestAdaptationManager testAdaptationManager = new TestAdaptationManager(context);

        Systems executables = null;

        ActionNode ancestorNode = context.getExecutionPlan().getAction(actionConfiguration.getDependsOn());

        // check if ancestor action supports testing (i.e handles tests)
        if(context.getActionManager().isTester(ancestorNode.getType())) {
            // existing executables
            final Systems existingExecutables = context.getLassoOperations().getExecutables(
                    context.getExecutionId(), actionConfiguration.getAbstraction().getName(), ancestorNode.getName());
            // reject any not in actionConfiguration
            if(existingExecutables.hasExecutables()) {
                // reject any not part of the "partition block" received by this action
                existingExecutables.getExecutables()
                        .removeIf(executable -> actionConfiguration.getAbstraction().getImplementations().stream().noneMatch(impl -> executable.getId().equals(impl.getId())));
            }

            if(LOG.isDebugEnabled()) {
                existingExecutables.getExecutables().forEach(e -> LOG.debug("Existing for {} {}", ancestorNode.getName(), e.getId()));
            }

            //
            executables = testAdaptationManager.initNew(this,
                    getInstanceId(),
                    actionConfiguration.getAbstraction(),
                    POM_TEMPLATE,
                    (implementation, candidate, valueMap) -> {
                        // for methods only
                        if(implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                            // limit permutations to method signature only
                            valueMap.put("bytecodename", implementation.getBytecodeName());
                        } else {
                            valueMap.put("bytecodename", "");
                        }

                        valueMap.put("candidate.classname", implementation.getName());
                    },
                    executable -> {
                //

                        // TODO transfer project
                        //executable.getImplementation().getWorkerNodeId();

                        System existingExecutable = null;
                        try {
                            existingExecutable = existingExecutables.getExecutable(executable.getId());
                        } catch (Throwable e) {
                            //
                            if(LOG.isWarnEnabled()) {
                                LOG.warn("Did not find existing executable for '{}'", executable.getId());
                                LOG.warn("Exception thrown", e);
                            }

                            return false;
                        }

//                        //
//                        if(existingExecutable.hasExecutionSignatures()) {
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
                        if(remote) {
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
                            if(actionConfiguration.hasIncludeTestsPattern()) {
                                TestMatcher testMatcher = new TestMatcher();
                                List<File> testClasses = testMatcher.findMatches(actionConfiguration.getIncludeTestsPattern(), executable.getProject().getSrcTest());

                                if(CollectionUtils.isEmpty(testClasses)) {
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
                            try {
                                TestUtils.setUpTestClass(executable, testClasses.get(executable.getId()));
                            } catch (Throwable e) {
                                LOG.warn("Setting up test classes for '{}' failed", executable.getId());
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

        return testAdaptationManager;
    }

    @Override
    protected DefaultMavenActionExecutionListener createListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        // also fires action listener ..
        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this);
        testListener.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));
        testListener.setAllowedMojos(Arrays.asList("maven-surefire-plugin:test"));

        return testListener;
    }

    @Override
    public Abstraction createAbstraction(LSLExecutionContext context, ActionConfiguration actionConfiguration, AbstractionSpec abstractionSpec) throws IOException {
        return null;
    }

    @Override
    public void postExecute(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, boolean success) {
        if (!success) {
            // remove
            removeExecutableConditionally(actionConfiguration, executableId, "Collection failed");

            return;
        }

        System executable = getExecutables().getExecutable(executableId);

        // TODO

        if(actionConfiguration.isDropFailed()) {
            // TODO


//            // remove if no report set
//            ReportOperations reportOperations = context.getReportOperations();
//            ReportKey rKey = ReportKey.of(this, getExecutables().getAbstractionName(), executable.getImplementation());
//
//            JaCoCoReport report = null;
//            try {
//                report = reportOperations.get(context.getExecutionId(), rKey, JaCoCoReport.class);
//            } catch (Throwable e) {
//                // no test reports found
//                if (LOG.isWarnEnabled()) {
//                    LOG.warn(String.format("No JaCoCoReport record found for '%s'", executable.getId()));
//                }
//            }
//
//            if(report == null) {
//                boolean removed = getExecutables().remove(executableId);
//                if(removed) {
//                    if(LOG.isDebugEnabled()) {
//                        LOG.debug("Removed executable '{}' from list since no report was found", executableId);
//                    }
//                }
//            } else {
//                // FIXME move outside dropFailed
//
//                // remove if minimum test coverage not reached
//                if(report.getJaCoCoTestCoverage() < minimumTestCoverage) {
//                    boolean removed = getExecutables().remove(executableId);
//                    if(removed) {
//                        if(LOG.isDebugEnabled()) {
//                            LOG.debug("Removed executable '{}' from list since test coverage was to low => min {} vs actual {}",
//                                    executableId, minimumTestCoverage, report.getJaCoCoTestCoverage());
//                        }
//                    }
//                }
//            }
        }
    }

    @Override
    public List<RecordCollector> createCollectors() {
        return Arrays.asList(
                new SurefireCollector(false));
    }

    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        mavenDefaultCommand.addAll(Arrays.asList("test"));

        return mavenDefaultCommand;
    }
}
