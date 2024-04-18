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
package de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.classloader.Containers;

import de.uni_mannheim.swt.lasso.arena.event.ArenaExecutionListener;

import de.uni_mannheim.swt.lasso.arena.event.DefaultExecutionListener;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;

import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.sequence.ExecutableSequence;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * An execution container which uses JaCoCo to instrument classes and measure code coverage.
 *
 * @author Marcus Kessel
 */
public class JaCoCoContainer extends Container {

    private static final Logger LOG = LoggerFactory
            .getLogger(JaCoCoContainer.class);

    private final LoggerRuntime runtime;
    private final Instrumenter instrumenter;
    private final Scope scope;
    private RuntimeData data;

    /**
     * Creates a new class realm.
     *
     * @param containers           The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     */
    public JaCoCoContainer(Containers containers, String id, ClassLoader baseClassLoader) {
        super(containers, id, baseClassLoader);

        this.runtime = new LoggerRuntime();
        this.instrumenter = new Instrumenter(runtime);
        this.scope = null;
    }

    /**
     * Creates a new class realm.
     *
     * @param containers           The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     * @param scope {@link Scope}
     */
    public JaCoCoContainer(Containers containers, String id, ClassLoader baseClassLoader, Scope scope) {
        super(containers, id, baseClassLoader);

        this.runtime = new LoggerRuntime();
        this.instrumenter = new Instrumenter(runtime);
        this.scope = scope;
    }

    @Override
    protected boolean instrumentClass(String name) {
        return true;
    }

    @Override
    protected byte[] instrumentClassBytes(String name, byte[] bytes) {
        try {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Instrumented class '{}'", name);
            }

            return instrumenter.instrument(bytes, name);
        } catch (IOException e) {
            LOG.warn("Failed to instrument class '{}'", name);
            LOG.warn("Stack trace", e);
        }

        return bytes;
    }

    /**
     * Default behavior is to start measurement before sequence execution and to end it after sequence execution.
     *
     * FIXME make measurement session configurable (i.e when to start/end measurement).
     *
     * @return
     */
    @Override
    public ArenaExecutionListener getArenaExecutionListener() {
        return new DefaultExecutionListener(this) {
            @Override
            public void onBeforeExecution(SequenceExecutionRecords results) {
                super.onBeforeExecution(results);

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Starting JaCoCo measurement");
                }

                try {
                    start();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAfterExecution(SequenceExecutionRecords results) {
                super.onAfterExecution(results);

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Stopping JaCoCo measurement");
                }

                try {
                    CoverageBuilder coverageBuilder = stop();

                    JaCoCoObservation observation = new JaCoCoObservation(coverageBuilder);
                    results.addObservation("jacoco", observation);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onBeforeStatement(SequenceExecutionRecord result, ExecutableSequence executableSequence, int i) {
                super.onBeforeStatement(result, executableSequence, i);
            }

            @Override
            public void onAfterStatement(SequenceExecutionRecord result, ExecutableSequence executableSequence, int i) {
                super.onAfterStatement(result, executableSequence, i);
            }

            @Override
            public void onBeforeSequence(SequenceExecutionRecord result, ExecutableSequence executableSequence) {
                super.onBeforeSequence(result, executableSequence);
            }

            @Override
            public void onAfterSequence(SequenceExecutionRecord result, ExecutableSequence executableSequence) {
                super.onAfterSequence(result, executableSequence);
            }
        };
    }

    /**
     * Need to be called AFTER instrumentation of classes
     *
     * @throws Exception
     */
    public void start() throws Exception {
        if(this.data != null) {
            // already initialized, just reset
            reset();

            return;
        }

        this.data = new RuntimeData();
        runtime.startup(data);
    }

    public void shutdown() {
        runtime.shutdown();
    }

    /**
     * FIXME is this possible?
     *
     */
    public void reset() {
        data.reset(); // XXX right call?
    }

    /**
     * Create filters based on given {@link Scope}.
     *
     * @return
     */
    private IFilter createFilters() {
        List<IFilter> filters = new LinkedList<>();
        // evaluate scope
        if(scope != null && StringUtils.equalsIgnoreCase(scope.getType(), "class")) {
            // method filters
            if(scope.getConfiguration().containsKey("methodBlacklist")) {
                // remove certain methods
                List<String> methodBlacklist = (List<String>) scope.getConfiguration().get("methodBlacklist");
                if(CollectionUtils.isNotEmpty(methodBlacklist)) {
                    filters.add(new MethodFilter(methodBlacklist, false));
                }
            } else if(scope.getConfiguration().containsKey("methodWhitelist")) {
                // only allowed methods
                List<String> methodWhitelist = (List<String>) scope.getConfiguration().get("methodWhitelist");
                if(CollectionUtils.isNotEmpty(methodWhitelist)) {
                    filters.add(new MethodFilter(methodWhitelist, true));
                }
            }

        }

        // default is class scope
        return new JaCoCoFilters(filters);
    }

    public ExecutionDataStore getExecutionDataStore() {
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);

        return executionData;
    }

    public CoverageBuilder stop() throws IOException {
        // At the end of test execution we collect execution data and shutdown
        // the runtime:
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        //runtime.shutdown();

        // Together with the original class definition we can calculate coverage
        // information:
        final CoverageBuilder coverageBuilder = new CoverageBuilder();

        // create filters
        IFilter filter = createFilters();
        // custom analyzer
        final FlexibleAnalyzer analyzer = new FlexibleAnalyzer(executionData, coverageBuilder, filter);

        //
        for(String className : getClasses().keySet()) {
//            // TODO class filters
//            if(scope.getConfiguration().containsKey("pkgWhitelist")) {
//                // should be done on class level
//            } else if(scope.getConfiguration().containsKey("pkgBlacklist")) {
//
//            }

            //
            byte[] bytes = super.loadClassBytes(className);

            analyzer.analyzeClass(bytes, className);
        }

        // FIXME perhaps do XML and/or CSV

        if(getClassUnderTest().getLocalProject() != null) {
            try {
                MavenProject mavenProject = getClassUnderTest().getLocalProject();

                CompilationUnit cutUnit = new CompilationUnit();
                cutUnit.setPkg(getClassUnderTest().getImplementation().getCode().getPackagename());
                cutUnit.setName(getClassUnderTest().getImplementation().getCode().getName());
                cutUnit.setSourceCode(getClassUnderTest().getImplementation().getCode().getContent());
                mavenProject.writeCompilationUnit(cutUnit, false);

                //
                File reportDirectory = new File(mavenProject.getTarget(), String.format("jacoco_%s_%s", getId(), System.currentTimeMillis()));
                File sourceDirectory = mavenProject.getSrcMain();
                createReport(coverageBuilder, executionData, sessionInfos, reportDirectory, sourceDirectory);
            } catch (Throwable e) {
                LOG.warn("generation of HTML report failed", e);
            }
        }

        return coverageBuilder;
    }

    /**
     * directly taken from JaCoCo API Usage Examples
     *
     * @author JaCoCo Project
     *
     * @param coverageBuilder
     * @param executionData
     * @param sessionInfos
     * @param reportDirectory
     * @param sourceDirectory
     * @throws IOException
     */
    private void createReport(CoverageBuilder coverageBuilder, ExecutionDataStore executionData, SessionInfoStore sessionInfos, File reportDirectory, File sourceDirectory)
            throws IOException {
        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter
                .createVisitor(new FileMultiReportOutput(reportDirectory));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(sessionInfos.getInfos(),
                executionData.getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        visitor.visitBundle(coverageBuilder.getBundle(getClassUnderTest().getId() + "_" + getClassUnderTest().getId()),
                new DirectorySourceFileLocator(sourceDirectory, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();
    }
}
