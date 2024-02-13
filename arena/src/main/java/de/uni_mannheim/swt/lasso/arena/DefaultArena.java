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

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;

import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.Pitest;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.adaptation.OriginalImplementation;
import de.uni_mannheim.swt.lasso.arena.writer.CellWriter;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.pitest.mutationtest.engine.MutationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.*;
import randoop.main.GenTests;
import randoop.operation.TypedClassOperation;
import randoop.reflection.OmitMethodsPredicate;
import randoop.reflection.VisibilityPredicate;
import randoop.sequence.ExecutableSequence;
import randoop.sequence.Sequence;
import randoop.test.ExpectedExceptionCheckGen;
import randoop.test.CustomTestCheckGenerator;
import randoop.test.RegressionCaptureGenerator;
import randoop.types.Type;
import randoop.util.MultiMap;
import randoop.util.ReflectionExecutor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default arena implementation.
 *
 * @author Marcus Kessel
 */
public class DefaultArena extends Arena {

    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultArena.class);

    // randoop internals
    private static MultiMap<Type, TypedClassOperation> sideEffectFreeMethodsByType =
            GenTests.readSideEffectFreeMethods();

    private int threads = Runtime.getRuntime().availableProcessors() / 2;

    private AdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

    /**
     * Default settings for Randoop Reflection execution
     */
    static {
        setExecutionTimeout(10 * 1000 /* 10 secs*/);
    }

    public static void setExecutionTimeout(int millis) {
        //
        LOG.info("Setting Randoop's reflection execution timeout to '{}' millis", millis);
        ReflectionExecutor.usethreads = true; // must be enabled
        ReflectionExecutor.call_timeout = millis;
    }

    public DefaultArena() {

    }

    /**
     * Execute a (sub)list of {@link ClassUnderTest} part of the arena.
     * <p>
     * Adapts per {@link SequenceSpecification}.
     *
     * @param classUnderTestList
     * @param specification
     * @param sheets
     * @param cellWriter
     * @param limitAdapters
     * @return
     */
    public Map<AdaptedImplementation, SequenceExecutionRecords> executeEachSequenceSpecification(List<ClassUnderTest> classUnderTestList, InterfaceSpecification specification, List<SequenceSpecification> sheets, CellWriter cellWriter, int limitAdapters) {
        Map<AdaptedImplementation, SequenceExecutionRecords> results = new LinkedHashMap<>();

        if (LOG.isInfoEnabled()) {
            LOG.info("Using '{}' threads for execution", threads);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads,
                new ThreadFactory() {

                    private AtomicInteger inc = new AtomicInteger();

                    /**
                     * Assign a specific names
                     */
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                String.format("%s-%s", "arena-executor", inc.getAndIncrement()));
                    }

                });

        List<Callable<Void>> tasks = new ArrayList<>(classUnderTestList.size());
        for (ClassUnderTest cut : classUnderTestList) {
            Callable<Void> task = () -> {
                //List<AdaptedImplementation> adaptedImplementations = adapt(cut, specification, limitAdapters);

                //for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                // call underlying engine
                Container container = cut.getProject().getContainer();

                CutExecutionListener executionVisitor = new CutExecutionListener(container.getArenaExecutionListener());

                SequenceExecutionRecords executionResults = new SequenceExecutionRecords(new OriginalImplementation(cut), specification);
                results.put(executionResults.getImplementation(), executionResults);

                // fire
                try {
                    container.getArenaExecutionListener().onBeforeExecution(executionResults);
                } catch (Throwable e) {
                    LOG.warn("before execution listener failed", e);
                }

                int seq = 0;
                for (SequenceSpecification sheet : sheets) {
                    InterfaceSpecification interfaceSpecification = sheet.toInterfaceSpecification();

                    // ss does not use CUT
                    if(interfaceSpecification.isEmpty()) {
                        if(LOG.isWarnEnabled()) {
                            LOG.warn("SequenceSpecification does not define CUT => '{}'", sheet.getName());
                        }

                        continue;
                    }

                    List<AdaptedImplementation> adaptedImplementations = adapt(cut, interfaceSpecification, limitAdapters);

                    // FIXME NPE

                    for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                        boolean failed = false;

                        SequenceExecutionRecord record;

                        Sequence s = null;
                        try {
                            record = sheet.instantiate(interfaceSpecification, adaptedImplementation);

                            s = record.getSequence();
                        } catch (Throwable e) {
                            LOG.warn("Creating sequence failed '{}'", sheet.getName());
                            LOG.warn("Stack trace", e);

                            record = new SequenceExecutionRecord(sheet, interfaceSpecification, adaptedImplementation);
                            record.setExecutableSequence(null);

                            failed = true;
                        }

                        executionResults.add(record);

                        if (!failed) {
                            LOG.debug("--- Sequence ---");
                            LOG.debug(s.toCodeString());
                            LOG.debug("--- Sequence ---");

                            // execute
                            ExecutableSequence executableSequence = null;
                            try {
                                executableSequence = execute(s, executionVisitor); // add own visitor
                            } catch (Throwable e) {
                                LOG.warn("Executing sequence failed '{}'", sheet.getName());
                                LOG.warn("Stack trace", e);
                            }

                            record.setExecutableSequence(executableSequence);

                            // write results
                            if (cellWriter != null) {
                                try {
                                    cellWriter.writeExecutedSequence(record, this);
                                } catch (Throwable e) {
                                    LOG.warn("Writing executed sequence failed", e);
                                }
                            }

                            // write observations
                            if (cellWriter != null) {
                                try {
                                    cellWriter.writeObservations(record, this);
                                } catch (Throwable e) {
                                    LOG.warn("Writing observations failed", e);
                                }
                            }

                            seq++;
                        }
                    }
                }

                // fire
                try {
                    container.getArenaExecutionListener().onAfterExecution(executionResults);
                } catch (Throwable e) {
                    LOG.warn("after execution listener failed", e);
                }

                // write suite level observations
                if (cellWriter != null) {
                    try {
                        cellWriter.writeObservations(executionResults, this);
                    } catch (Throwable e) {
                        LOG.warn("Writing observations failed", e);
                    }
                }
                //}

                return null;
            };

            tasks.add(task);
        }

        // execute all
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);

            futures.stream().forEach(f -> {
                try {
                    f.get();
                } catch (Throwable e) {
                    LOG.warn("Task failed", e);
                }
            });
        } catch (InterruptedException e) {
            LOG.warn("Executor failed", e);
        } finally {
            executor.shutdown();
        }

        return results;
    }

    /**
     * Execute a (sub)list of {@link ClassUnderTest} part of the arena.
     *
     * @param classUnderTestList
     * @param specification
     * @param sheets
     * @param cellWriter
     * @param limitAdapters
     * @return
     */
    public Map<AdaptedImplementation, SequenceExecutionRecords> execute(List<ClassUnderTest> classUnderTestList, InterfaceSpecification specification, List<SequenceSpecification> sheets, CellWriter cellWriter, int limitAdapters) {
        Map<AdaptedImplementation, SequenceExecutionRecords> results = new LinkedHashMap<>();

        if (LOG.isInfoEnabled()) {
            LOG.info("Using '{}' threads for execution", threads);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads,
                new ThreadFactory() {

                    private AtomicInteger inc = new AtomicInteger();

                    /**
                     * Assign a specific names
                     */
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                String.format("%s-%s", "arena-executor", inc.getAndIncrement()));
                    }

                });

        List<Callable<Void>> tasks = new ArrayList<>(classUnderTestList.size());
        for (ClassUnderTest cut : classUnderTestList) {
            Callable<Void> task = () -> {
                List<AdaptedImplementation> adaptedImplementations = adapt(cut, specification, limitAdapters);

                try {
                    cellWriter.writeAdapters(specification, cut, adaptedImplementations);
                } catch (Throwable e) {
                    LOG.warn("writing adapters failed", e);
                }

                LOG.debug("Computed '{}' for system '{}'", adaptedImplementations.size(), cut.getId());

                for (AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                    //

                    // call underlying engine
                    Container container = cut.getProject().getContainer();

                    SequenceExecutionRecords executionResults = new SequenceExecutionRecords(adaptedImplementation, specification);

                    results.put(adaptedImplementation, executionResults);

                    CutExecutionListener executionVisitor = new CutExecutionListener(container.getArenaExecutionListener());

                    // fire
                    try {
                        container.getArenaExecutionListener().onBeforeExecution(executionResults);
                    } catch (Throwable e) {
                        LOG.warn("before execution listener failed", e);
                    }

                    int seq = 0;
                    for (SequenceSpecification sheet : sheets) {
                        boolean failed = false;

                        SequenceExecutionRecord record;

                        Sequence s = null;
                        try {
                            record = sheet.instantiate(specification, adaptedImplementation);

                            s = record.getSequence();
                        } catch (Throwable e) {
                            LOG.warn("Creating sequence failed '{}'", sheet.getName());
                            LOG.warn("Stack trace", e);

                            record = new SequenceExecutionRecord(sheet, specification, adaptedImplementation);
                            record.setExecutableSequence(null);

                            failed = true;
                        }

                        executionResults.add(record);

                        if (!failed) {
                            LOG.debug("--- Sequence ---");
                            LOG.debug(s.toCodeString());
                            LOG.debug("--- Sequence ---");

                            // execute
                            ExecutableSequence executableSequence = null;
                            try {
                                executableSequence = execute(s, executionVisitor); // add own visitor
                            } catch (Throwable e) {
                                LOG.warn("Executing sequence failed '{}'", sheet.getName());
                                LOG.warn("Stack trace", e);
                            }

                            record.setExecutableSequence(executableSequence);

                            // write results
                            if (cellWriter != null) {
                                try {
                                    cellWriter.writeExecutedSequence(record, this);
                                } catch (Throwable e) {
                                   LOG.warn("writing executed sequence failed", e);
                                }
                            }

                            // write observations
                            if (cellWriter != null) {
                                try {
                                    cellWriter.writeObservations(record, this);
                                } catch (Throwable e) {
                                    LOG.warn("writing observations failed", e);
                                }
                            }

                            seq++;
                        }
                    }

                    // fire
                    try {
                        container.getArenaExecutionListener().onAfterExecution(executionResults);
                    } catch (Throwable e) {
                        LOG.warn("after execution listener failed", e);
                    }

                    // write suite level observations
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeObservations(executionResults, this);
                        } catch (Throwable e) {
                            LOG.warn("writing observations failed", e);
                        }
                    }
                }

                return null;
            };

            tasks.add(task);
        }

        // execute all
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);

            futures.stream().forEach(f -> {
                try {
                    f.get();
                } catch (Throwable e) {
                    LOG.warn("Task failed", e);
                }
            });
        } catch (InterruptedException e) {
            LOG.warn("Executor failed", e);
        } finally {
            executor.shutdown();
        }

        return results;
    }

    public List<AdaptedImplementation> adapt(ClassUnderTest cut, InterfaceSpecification specification, int limitAdapters) {
        List<AdaptedImplementation> adaptedImplementations = null;
        try {
            adaptedImplementations = adaptationStrategy.adapt(specification, cut, limitAdapters);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not adapt '{}'", cut.getId());
                LOG.warn("Stack trace", e);
            }

            // continue
            return null;
        }

        if(LOG.isDebugEnabled()) {
            for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                LOG.debug("adapted implementation '{}'", adaptedImplementation);
            }
        }

        return adaptedImplementations;
    }

    public List<ClassUnderTest> createMutants(CandidatePool pool, ClassUnderTest classUnderTest, boolean addToArena, boolean initialize, boolean generateReport, String reportSuffix) {
        try {
            // Pitest
            Pitest pitest = new Pitest(classUnderTest);

            List<MutationDetails> mutationDetails = pitest.findMutations();

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Generated '%s' mutants for implementation '%s' (%s)",
                        mutationDetails.size(),
                        classUnderTest.getId(),
                        classUnderTest.getClassName()));
            }

            List<ClassUnderTest> mutants = new ArrayList<>(mutationDetails.size());

            for (int m = 0; m < mutationDetails.size(); m++) {
                MutationDetails md = mutationDetails.get(m);

                if (classUnderTest.getImplementation() != null && classUnderTest.getImplementation().getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                    // FIXME restrict mutants to method implementation
                    //md.getMethod();
                }
                // generate mutant
                ClassUnderTest mutant = pitest.generateMutant(
                        String.valueOf(m),
                        md,
                        pool.getMavenRepository().getResolver());

                mutants.add(mutant);
            }

            // add mutants to arena
            if (addToArena) {
                pool.addClasses(mutants);
            }

            if (initialize) {
                pool.initProjects();
            }

            if (generateReport) {
                if (classUnderTest.getLocalProject() != null) {
                    pitest.generateReport(reportSuffix);
                }
            }

            return mutants;
        } catch (Throwable e) {
            throw new RuntimeException("Could not create mutants", e);
        }
    }

    /**
     * Re-run existing results.
     *
     * @param cut
     * @param existingResults
     * @param cellWriter
     * @return
     */
    public SequenceExecutionRecords executeEachSequenceSpecification(ClassUnderTest cut, Map<String, SequenceExecutionRecords> existingResults, CellWriter cellWriter) {
        // call underlying engine
        Container container = cut.getProject().getContainer();

        // FIXME set specification
        SequenceExecutionRecords executionResults = new SequenceExecutionRecords(new OriginalImplementation(cut), null);

        // fire
        try {
            container.getArenaExecutionListener().onBeforeExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("before execution listener failed", e);
        }

        CutExecutionListener executionVisitor = new CutExecutionListener(container.getArenaExecutionListener());

        int seq = 0;
        for (String sheetSetId : existingResults.keySet()) {
            SequenceExecutionRecords sheetSet = existingResults.get(sheetSetId);

            for (SequenceExecutionRecord sheet : sheetSet.getRecords()) {
                InterfaceSpecification specification = sheet.getSequenceSpecification().toInterfaceSpecification();

                // ss does not use CUT
                if(specification.isEmpty()) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("SequenceSpecification does not define CUT => '{}'", sheet.getSequenceSpecification().getName());
                    }

                    continue;
                }

                // FIXME copy adapter instead of doing perms again
                List<AdaptedImplementation> adaptedImplementations = adapt(cut, specification, 1);
                AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);
                //new PermutatorAdaptedImplementation(cut, null);

                boolean failed = false;

                SequenceExecutionRecord record;
                Sequence s = null;
                try {
                    record = sheet.getSequenceSpecification().instantiate(specification, adaptedImplementation);

                    s = record.getSequence();
                } catch (Throwable e) {
                    LOG.warn("Creating sequence failed '{}'", sheet.getSequenceSpecification().getName());
                    LOG.warn("Stack trace", e);

                    record = new SequenceExecutionRecord(sheet.getSequenceSpecification(), specification, adaptedImplementation);
                    record.setExecutableSequence(null);

                    failed = true;
                }

                executionVisitor.setCurrent(record);

                if (!failed) {
                    LOG.debug("--- Sequence ---");
                    LOG.debug(s.toCodeString());
                    LOG.debug("--- Sequence ---");

                    // execute
                    ExecutableSequence executableSequence = null;
                    try {
                        executableSequence = execute(s, executionVisitor); // add own visitor
                    } catch (Throwable e) {
                        LOG.warn("Executing sequence failed '{}'", sheet.getSequenceSpecification().getName());
                        LOG.warn("Stack trace", e);
                    }

                    record.setExecutableSequence(executableSequence);

                    // write results
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeExecutedSequence(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing executed sequence failed", e);
                        }
                    }

                    // write observations
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeObservations(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing observations failed", e);
                        }
                    }

                    seq++;
                }
            }
        }

        // fire
        try {
            container.getArenaExecutionListener().onAfterExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("after execution listener failed", e);
        }

        // write suite level observations
        if (cellWriter != null) {
            try {
                cellWriter.writeObservations(executionResults, this);
            } catch (Throwable e) {
                LOG.warn("writing observations failed", e);
            }
        }

        return executionResults;
    }

    /**
     * Re-run existing results.
     *
     * @param cut
     * @param existingResults
     * @param cellWriter
     * @return
     */
    public SequenceExecutionRecords doExecuteEachSequenceSpecification(ClassUnderTest cut, Map<AdaptedImplementation, SequenceExecutionRecords> existingResults, CellWriter cellWriter) {
        // call underlying engine
        Container container = cut.getProject().getContainer();

        // FIXME set specification
        SequenceExecutionRecords executionResults = new SequenceExecutionRecords(new OriginalImplementation(cut), null);

        // fire
        try {
            container.getArenaExecutionListener().onBeforeExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("before execution listener failed", e);
        }

        CutExecutionListener executionVisitor = new CutExecutionListener(container.getArenaExecutionListener());

        int seq = 0;
        for (AdaptedImplementation adaptedImplementation : existingResults.keySet()) {
            SequenceExecutionRecords sheetSet = existingResults.get(adaptedImplementation);

            for (SequenceExecutionRecord sheet : sheetSet.getRecords()) {
                InterfaceSpecification specification = sheet.getSequenceSpecification().toInterfaceSpecification();

                // ss does not use CUT
                if(specification.isEmpty()) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("SequenceSpecification does not define CUT => '{}'", sheet.getSequenceSpecification().getName());
                    }

                    continue;
                }

                boolean failed = false;

                SequenceExecutionRecord record;
                Sequence s = null;
                try {
                    record = sheet.getSequenceSpecification().instantiate(specification, adaptedImplementation);

                    s = record.getSequence();
                } catch (Throwable e) {
                    LOG.warn("Creating sequence failed '{}'", sheet.getSequenceSpecification().getName());
                    LOG.warn("Stack trace", e);

                    record = new SequenceExecutionRecord(sheet.getSequenceSpecification(), specification, adaptedImplementation);
                    record.setExecutableSequence(null);

                    failed = true;
                }

                executionVisitor.setCurrent(record);

                if (!failed) {
                    LOG.debug("--- Sequence ---");
                    LOG.debug(s.toCodeString());
                    LOG.debug("--- Sequence ---");

                    // execute
                    ExecutableSequence executableSequence = null;
                    try {
                        executableSequence = execute(s, executionVisitor); // add own visitor
                    } catch (Throwable e) {
                        LOG.warn("Executing sequence failed '{}'", sheet.getSequenceSpecification().getName());
                        LOG.warn("Stack trace", e);
                    }

                    record.setExecutableSequence(executableSequence);

                    // write results
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeExecutedSequence(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing executed sequence failed", e);
                        }
                    }

                    // write observations
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeObservations(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing observations failed", e);
                        }
                    }

                    seq++;
                }
            }
        }

        // fire
        try {
            container.getArenaExecutionListener().onAfterExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("after execution listener failed", e);
        }

        // write suite level observations
        if (cellWriter != null) {
            try {
                cellWriter.writeObservations(executionResults, this);
            } catch (Throwable e) {
                LOG.warn("writing observations failed", e);
            }
        }

        return executionResults;
    }

    /**
     * Re-run existing results.
     *
     * @param cut
     * @param existingResults
     * @param cellWriter
     * @return
     */
    public SequenceExecutionRecords execute(ClassUnderTest cut, Map<String, SequenceExecutionRecords> existingResults, CellWriter cellWriter) {
        // call underlying engine
        Container container = cut.getProject().getContainer();

        // FIXME set specification
        SequenceExecutionRecords executionResults = new SequenceExecutionRecords(new OriginalImplementation(cut), null);

        // fire
        try {
            container.getArenaExecutionListener().onBeforeExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("ArenaExecutionListener failed", e);
        }

        CutExecutionListener executionVisitor = new CutExecutionListener(container.getArenaExecutionListener());

        int seq = 0;
        for (String sheetSetId : existingResults.keySet()) {
            SequenceExecutionRecords sheetSet = existingResults.get(sheetSetId);

            InterfaceSpecification specification = sheetSet.getSpecification();

            // FIXME copy adapter instead of doing perms again
            List<AdaptedImplementation> adaptedImplementations = adapt(cut, specification, 1);
            AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);
            //new PermutatorAdaptedImplementation(cut, null);

            for (SequenceExecutionRecord sheet : sheetSet.getRecords()) {
                boolean failed = false;

                SequenceExecutionRecord record;
                Sequence s = null;
                try {
                    record = sheet.getSequenceSpecification().instantiate(specification, adaptedImplementation);

                    s = record.getSequence();
                } catch (Throwable e) {
                    LOG.warn("Creating sequence failed '{}'", sheet.getSequenceSpecification().getName());
                    LOG.warn("Stack trace", e);

                    record = new SequenceExecutionRecord(sheet.getSequenceSpecification(), specification, adaptedImplementation);
                    record.setExecutableSequence(null);

                    failed = true;
                }

                executionVisitor.setCurrent(record);

                if (!failed) {
                    LOG.debug("--- Sequence ---");
                    LOG.debug(s.toCodeString());
                    LOG.debug("--- Sequence ---");

                    // execute
                    ExecutableSequence executableSequence = null;
                    try {
                        executableSequence = execute(s, executionVisitor); // add own visitor
                    } catch (Throwable e) {
                        LOG.warn("Executing sequence failed '{}'", sheet.getSequenceSpecification().getName());
                        LOG.warn("Stack trace", e);
                    }

                    record.setExecutableSequence(executableSequence);

                    // write results
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeExecutedSequence(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing executed sequence failed", e);
                        }
                    }

                    // write observations
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeObservations(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing observations failed", e);
                        }
                    }

                    seq++;
                }
            }
        }

        // fire
        try {
            container.getArenaExecutionListener().onAfterExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("ArenaExecutionListener failed", e);
        }

        // write suite level observations
        if (cellWriter != null) {
            try {
                cellWriter.writeObservations(executionResults, this);
            } catch (Throwable e) {
                LOG.warn("writing observations failed", e);
            }
        }

        return executionResults;
    }

    /**
     * Re-run existing results.
     *
     * @param cut
     * @param existingResults
     * @param cellWriter
     * @return
     */
    public SequenceExecutionRecords doExecute(ClassUnderTest cut, Map<AdaptedImplementation, SequenceExecutionRecords> existingResults, CellWriter cellWriter) {
        // call underlying engine
        Container container = cut.getProject().getContainer();

        // FIXME set specification
        SequenceExecutionRecords executionResults = new SequenceExecutionRecords(new OriginalImplementation(cut), null);

        // fire
        try {
            container.getArenaExecutionListener().onBeforeExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("ArenaExecutionListener failed", e);
        }

        CutExecutionListener executionVisitor = new CutExecutionListener(container.getArenaExecutionListener());

        int seq = 0;
        for (AdaptedImplementation adaptedImplementation : existingResults.keySet()) {
            SequenceExecutionRecords sheetSet = existingResults.get(adaptedImplementation);

            InterfaceSpecification specification = sheetSet.getSpecification();

            for (SequenceExecutionRecord sheet : sheetSet.getRecords()) {
                boolean failed = false;

                SequenceExecutionRecord record;
                Sequence s = null;
                try {
                    record = sheet.getSequenceSpecification().instantiate(specification, adaptedImplementation);

                    s = record.getSequence();
                } catch (Throwable e) {
                    LOG.warn("Creating sequence failed '{}'", sheet.getSequenceSpecification().getName());
                    LOG.warn("Stack trace", e);

                    record = new SequenceExecutionRecord(sheet.getSequenceSpecification(), specification, adaptedImplementation);
                    record.setExecutableSequence(null);

                    failed = true;
                }

                executionVisitor.setCurrent(record);

                if (!failed) {
                    LOG.debug("--- Sequence ---");
                    LOG.debug(s.toCodeString());
                    LOG.debug("--- Sequence ---");

                    // execute
                    ExecutableSequence executableSequence = null;
                    try {
                        executableSequence = execute(s, executionVisitor); // add own visitor
                    } catch (Throwable e) {
                        LOG.warn("Executing sequence failed '{}'", sheet.getSequenceSpecification().getName());
                        LOG.warn("Stack trace", e);
                    }

                    record.setExecutableSequence(executableSequence);

                    // write results
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeExecutedSequence(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing executed sequence failed", e);
                        }
                    }

                    // write observations
                    if (cellWriter != null) {
                        try {
                            cellWriter.writeObservations(record, this);
                        } catch (Throwable e) {
                            LOG.warn("writing observations failed", e);
                        }
                    }

                    seq++;
                }
            }
        }

        // fire
        try {
            container.getArenaExecutionListener().onAfterExecution(executionResults);
        } catch (Throwable e) {
            LOG.warn("ArenaExecutionListener failed", e);
        }

        // write suite level observations
        if (cellWriter != null) {
            try {
                cellWriter.writeObservations(executionResults, this);
            } catch (Throwable e) {
                LOG.warn("writing observations failed", e);
            }
        }

        return executionResults;
    }

    /**
     * Executed sequence using Randoop's execution engine.
     *
     * @param sequence
     * @param executionVisitor
     * @return
     */
    public ExecutableSequence execute(Sequence sequence, ExecutionVisitor executionVisitor) {
        VisibilityPredicate visibility = VisibilityPredicate.IS_NOT_PRIVATE;

        ExpectedExceptionCheckGen expectation = new ExpectedExceptionCheckGen(visibility);

        CustomTestCheckGenerator regressionVisitor =
                new CustomTestCheckGenerator(
                        expectation,
                        sideEffectFreeMethodsByType,
                        visibility,
                        OmitMethodsPredicate.NO_OMISSION,
                        true);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing sequence '{}'", sequence.toCodeString());
        }

        ExecutableSequence es = new ExecutableSequence(sequence);
        try {
            es.execute(executionVisitor, regressionVisitor);
        } catch (Error e) {
            LOG.warn("Execution of sequence failed '{}'", sequence.toCodeString());
            LOG.warn("Stack trace", e);

//            for(int i = 0; i < es.size(); i++) {
//                ExecutionOutcome outcome =  es.getResult(i);
//                LOG.warn(outcome.toString());
//            }
        }

        return es;
    }

    @Deprecated
    public List<ExecutableSequence> executeSequences(List<Sequence> sequences, ExecutionVisitor executionVisitor) {
        VisibilityPredicate visibility = VisibilityPredicate.IS_NOT_PRIVATE;

        ExpectedExceptionCheckGen expectation = new ExpectedExceptionCheckGen(visibility);

        RegressionCaptureGenerator regressionVisitor =
                new RegressionCaptureGenerator(
                        expectation,
                        sideEffectFreeMethodsByType,
                        visibility,
                        OmitMethodsPredicate.NO_OMISSION,
                        true);

        List<ExecutableSequence> executableSequences = new LinkedList<>();
        for (Sequence sequence : sequences) {
            ExecutableSequence es = new ExecutableSequence(sequence);
            try {
                es.execute(executionVisitor, regressionVisitor);

                executableSequences.add(es);
            } catch (Throwable e) {
                e.printStackTrace();

//                for(int i = 0; i < es.size(); i++) {
//                    ExecutionOutcome outcome =  es.getResult(i);
//                    System.out.println(outcome);
//                }
            }
        }

        return executableSequences;
    }

    public AdaptationStrategy getAdaptationStrategy() {
        return adaptationStrategy;
    }

    public void setAdaptationStrategy(AdaptationStrategy adaptationStrategy) {
        this.adaptationStrategy = adaptationStrategy;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
}
