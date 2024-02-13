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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.randoop;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.*;
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

import org.apache.ignite.IgniteSet;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Randoop (based on randoop-maven-plugin).
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Run Randoop Test Generation")
@Stable
@Tester // handles tests
public class Randoop extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Randoop.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_randoop_generate_class.template");

    @LassoInput(desc = "search budget in seconds", optional = true)
    public int searchBudget = 60;

    @LassoInput(desc = "Hard kill timeout in seconds for evosuite client processes (overrides timeoutMultiplier)", optional = true)
    public int timeoutClientProcess = -1;

    @LassoInput(desc = "Grace period before randoop client process is killed. timeoutMultiplier times searchBudget", optional = true)
    public int timeoutMultiplier = 3;

    @LassoInput(desc = "stop after X successful classes (applies to entire cluster of worker nodes)", optional = true)
    public int stopAfter = -1;

    @LassoInput(desc = "Remove those executables without generated tests", optional = true)
    public boolean cleanExecutables = true;

    @LassoInput(desc = "randoop command line arguments", optional = true)
    public List<String> args = new LinkedList<>();

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

        Systems executables = testAdaptationManager.initNew(this,
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

                    // class
                    valueMap.put("lassoClass", implementation.toFQName());
                    valueMap.put("lassoPackageName", implementation.getPackagename());

                    StringBuilder argsStr = new StringBuilder();
                    for(String arg : args) {
                        // <extraArg>--output-limit=100</extraArg>
                        argsStr.append("<extraArg>");
                        argsStr.append(arg);
                        argsStr.append("</extraArg>");
                    }

                    valueMap.put("randoopArgs", argsStr.toString());
                },
                executable -> {
                    return true;
                });

        // set
        setExecutables(executables);

        return testAdaptationManager;
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

//        if(repetitions > 1) {
//            timeout *= repetitions;
//        }

        LOG.info("Setting timeout for evosuite client processes to '{}' millis", timeout);

        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this, timeout);
//        {
//
//            /**
//             * Custom killing for EvoSuite client processes.
//             *
//             * @param executableId
//             */
//            @Override
//            protected void onTimeoutReached(String executableId) {
//                System implementation = getExecutables().getExecutable(executableId);
//
//                // KILL process
//                try {
//                    LOG.info("Killing evosuite client process with class name '{}' of id '{}'",
//                            implementation.getCode().toFQName(), executableId);
//
//                    EvoProcessKiller.kill9(implementation.getCode().toFQName());
//
//                    if(LOG.isInfoEnabled()) {
//                        LOG.info(String.format("Killed EvoSuite client process for '%s'",
//                                executableId));
//                    }
//                } catch (Throwable e) {
//                    if(LOG.isWarnEnabled()) {
//                        LOG.warn(String.format("Was not able to kill EvoSuite client process for '%s'",
//                                executableId), e);
//                    }
//                }
//            }
//
//            @Override
//            public void onProjectFailed(String time, String executableId) {
//                if(LOG.isWarnEnabled()) {
//                    LOG.warn("PROJECT FAILED FOR '{}'", executableId);
//                }
//
//                EvosuiteExecutionReport evosuiteExecutionReport = new EvosuiteExecutionReport();
//                evosuiteExecutionReport.setProjectFailed(true);
//                evosuiteExecutionReport.setCause("");
//
//                // ReportOperations
//                doReportCause(context, actionConfiguration, executableId, evosuiteExecutionReport);
//
//                super.onProjectFailed(time, executableId);
//            }
//
//            @Override
//            public void onMojoFailed(String time, String executableId, String mojo, String cause) {
//                if(LOG.isWarnEnabled()) {
//                    LOG.warn("EVOSUITE GENERATION FAILED FOR '{}'. Reason => {}", executableId, cause);
//                }
//
//                EvosuiteExecutionReport evosuiteExecutionReport = new EvosuiteExecutionReport();
//                evosuiteExecutionReport.setMojoFailed(true);
//                evosuiteExecutionReport.setCause(cause);
//
//                // ReportOperations
//                doReportCause(context, actionConfiguration, executableId, evosuiteExecutionReport);
//
//                super.onMojoFailed(time, executableId, mojo, cause);
//            }
//        };
        testListener.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));
        testListener.setAllowedMojos(Arrays.asList("randoop-maven-plugin:generate"));

        return testListener;
    }

    @Override
    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        mavenDefaultCommand.addAll(Arrays.asList(
                "test-compile", // target/test-classes is required for amplification mode
                "randoop:generate"
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
            // ReportOperations
            //doReportCause(context, actionConfiguration, executableId, evosuiteExecutionReport);

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
        RandoopClassCollector randoopClassCollector = new RandoopClassCollector(true);

        return Arrays.asList(
                randoopClassCollector);
    }
}
