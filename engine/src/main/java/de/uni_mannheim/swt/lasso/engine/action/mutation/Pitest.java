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
package de.uni_mannheim.swt.lasso.engine.action.mutation;

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.annotations.*;
import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.DefaultMavenActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.project.ProjectHelper;
import de.uni_mannheim.swt.lasso.engine.matcher.TestMatcher;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Mutation Testing with Pitest.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://pitest.org/quickstart/maven/">Pitest Maven Plugin</a>
 */
@LassoAction(desc = "Pitest Mutation Testing, see https://pitest.org/quickstart/maven/")
@Stable
@Tester // handles tests
@Partitioning(max = 1) // only one implementation per execution
public class Pitest extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Pitest.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_pitest.template");

    @LassoInput(desc = "Remove unpacked classes?", optional = true)
    public boolean removeUnpackedClasses = true;

    @Deprecated
    @LassoInput(desc = "Only run passed tests?", optional = true)
    public boolean passedTestsOnly = false;

    @LassoInput(desc = "Add source to report?", optional = true)
    public boolean generateSourceReport = false;

    @LassoInput(desc = "Pitest timeout constant", optional = true)
    public int timeoutConstant = 4000;

    @LassoInput(desc = "Pitest Mutator Group", optional = true)
    public String mutatorGroup = "DEFAULTS";

    @Deprecated
    @LassoInput(desc = "Do not modify tests", optional = true)
    public boolean notModifyTests = false;

    @LassoInput(desc = "Hard kill timeout in seconds for client processes", optional = true)
    public int timeoutClientProcess = 5 * 60;

    /**
     * To prevent timeouts, use one thread only.
     *
     * @param context
     * @return
     */
    @Override
    protected String getMavenParallelThreads(LSLExecutionContext context) {
//        int processors = Runtime.getRuntime().availableProcessors();
//
//        // further reduce since parallel runs may be resource intensive
//        int threads = processors / 3;
//        if(threads < 1) {
//            return "1";
//        } else {
//            return String.valueOf(threads);
//        }

        return "1"; // also required for killing
    }

    /**
     * Determine no of threads used for PIT for one candidate
     *
     * @return
     */
    int getThreadsCount() {
        int processors = Runtime.getRuntime().availableProcessors();

        // further reduce since parallel runs may be resource intensive
        int threads = processors / 2;
        if(threads < 1) {
            return 1;
        } else {
            return threads;
        }
    }

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
                        // Pitest configuration
                        // see https://pitest.org/quickstart/maven/
                        valueMap.put("pitest.targetClasses", implementation.toFQName());

                        valueMap.put("pitest.mutatorGroup", mutatorGroup);

                        valueMap.put("pitest.targetTests", String.format("%s.*", implementation.getPackagename()));

                        // perm path
                        valueMap.put("pitest.permPath", TestAdaptationManager.getPassthroughPathProperty());

                        // for methods only
                        if(implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                            // limit permutations to method signature only
                            valueMap.put("bytecodename", implementation.getBytecodeName());
                        } else {
                            valueMap.put("bytecodename", "");
                        }

                        // set threads
                        valueMap.put("pitest.threads", getThreadsCount());
                    },
                    executable -> {
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

                        // add source like for JaCoCo Action
                        // add source code
                        if(generateSourceReport) {
                            CompilationUnit cutUnit = new CompilationUnit();
                            cutUnit.setPkg(executable.getCode().getPackagename());
                            cutUnit.setName(executable.getCode().getName());

                            if(executable.getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                                if(LOG.isInfoEnabled()) {
                                    LOG.info("Getting source of parent class for {} using {}", executable.getCode().getParentId(), executable.getCode().getDataSource());
                                }

                                String classId = executable.getCode().getParentId();

                                DataSource dataSource = context.getDataSourceMap().get(executable.getCode().getDataSource());
                                try {
                                    if(dataSource instanceof MavenDataSource) {
                                        MavenDataSource ds = (MavenDataSource) dataSource;
                                        MavenQuery mavenQuery = (MavenQuery) ds.createQueryModelForLSL();
                                        LassoContext ctx = new LassoContext();
                                        ctx.setLogger(new SimpleLogger());
                                        mavenQuery.setLasso(ctx);
                                        mavenQuery.queryForClasses("*:*");
                                        mavenQuery.filter("id:\""+ classId +"\"");
                                        mavenQuery.setDirectly(true);
                                        QueryResult queryResult = ds.query(mavenQuery);
                                        if(CollectionUtils.isNotEmpty(queryResult.getImplementations())) {
                                            CodeUnit classImpl = queryResult.getImplementations().get(0);
                                            cutUnit.setSourceCode(classImpl.getContent());
                                        }
                                    }
                                } catch (Throwable e) {
                                    LOG.warn("Could not get class source code for " + executable.getId(), e);
                                }
                            } else {
                                cutUnit.setSourceCode(executable.getCode().getContent());
                            }

                            // write
                            if(StringUtils.isNotBlank(cutUnit.getSourceCode())) {
                                try {
                                    executable.getProject().writeCompilationUnit(cutUnit, false);
                                } catch (Throwable e) {
                                    LOG.warn("Could not write unit for " + executable.getId(), e);
                                }
                            }
                        }

                        return true;
            });
        } else {
            throw new IllegalArgumentException("No ancestor node found");
        }

        Validate.notNull(executables, "Executables are null");

        // set
        setExecutables(executables);

        return testAdaptationManager;
    }

    @Override
    protected DefaultMavenActionExecutionListener createListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        // timeout
        long millis = timeoutClientProcess * 1000L;

        // also fires action listener ..
        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this, millis) {

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
                    LOG.info("Killing pitest process with class name '{}' of id '{}'",
                            implementation.getCode().toFQName(), executableId);

                    PitestProcessKiller.kill9();

                    // also signal stop
                    onMavenEnd();

                    // also clear executables (assuming only one executable)
                    getExecutables().getExecutables().clear();

                    if(LOG.isInfoEnabled()) {
                        LOG.info(String.format("Killed pitest client process for '%s'",
                                executableId));
                    }
                } catch (Throwable e) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn(String.format("Was not able to kill pitest client process for '%s'",
                                executableId), e);
                    }
                }
            }
        };
        testListener.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));
        testListener.setAllowedMojos(Arrays.asList("pitest-maven:mutationCoverage"));

        return testListener;
    }

    @Override
    public Abstraction createAbstraction(LSLExecutionContext context, ActionConfiguration actionConfiguration, AbstractionSpec abstractionSpec) throws IOException {
        return null;
    }

    @Override
    public void postExecute(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, boolean success) {
        //
        //
        if(removeUnpackedClasses) {
            System executable = getExecutables().getExecutable(executableId);
            if(executable.getProject() != null) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Removing unpackaged classes for '{}'", executableId);
                }

                try {
                    FileUtils.deleteDirectory(executable.getProject().getClasses());
                } catch (IOException e) {
                    LOG.warn(String.format("Removing unpackaged classes failed for '%s'", executableId), e);
                }
            }
        }

        if (!success) {
            // remove
            removeExecutableConditionally(actionConfiguration, executableId, "Collection failed");

            return;
        }
    }

    @Override
    public List<RecordCollector> createCollectors() {
        return Arrays.asList(
                new PitestCollector());
    }

    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        mavenDefaultCommand.addAll(
                Arrays.asList(
                        "clean", // clean
                        "compile", // adapter class
                        "test-compile", // tests
                        //"dependency:build-classpath", "-Dmdep.outputFile=classpath", "-Dmdep.localRepoProperty=M2_REPO",
                        "dependency:list", "-DoutputAbsoluteArtifactFilename=true", "-DoutputFile=jarlist",
                        "-DincludeScope=compile", // only unpack dependencies of scope "compile"
                        "-DoutputDirectory=target/classes", // unpack to directory (default build)
                        "-DexcludeGroupIds=de.uni-mannheim.swt.lasso,io.github.classgraph", // ignore those group ids
                        "dependency:unpack-dependencies",
                        //TestAdaptationManager.getPassthroughPathProperty(), // only run best match permutation
                        "-Djacoco.inclNoLocationClasses=true", // instrument all classes (even if no source available)
                        "-Djacoco.inclBootstrapClasses=false", // avoid bootstrap classes
                        "org.pitest:pitest-maven:mutationCoverage",
                        "-DtimeoutConstant=" + timeoutConstant // increase timeout
                ));

        return mavenDefaultCommand;
    }
}
