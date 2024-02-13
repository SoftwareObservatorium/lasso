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
package de.uni_mannheim.swt.lasso.arena.task;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.task.load.DefaultSheetProvider;
import de.uni_mannheim.swt.lasso.arena.task.load.ResolvedSheets;
import de.uni_mannheim.swt.lasso.arena.task.load.SheetProvider;
import de.uni_mannheim.swt.lasso.arena.writer.CellWriter;
import de.uni_mannheim.swt.lasso.arena.writer.SRMRepositoryWriter;
import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Test amplification task.
 *
 * @author Marcus Kessel
 */
public class Amplify extends Task {

    private static final Logger LOG = LoggerFactory
            .getLogger(Amplify.class);

    /**
     * Measure JaCoCo code coverage
     */
    private boolean measureJaCoCo = false;

    /**
     * Measure Mutation score using PIT
     */
    private boolean measurePIT = false;

    /**
     * (Over)write input test class.
     *
     */
    private boolean writeOriginalTests = false;

    private boolean writeSequenceRecords;

    public Amplify(MavenRepository mavenRepository) {
        super(mavenRepository);
    }

    public static void execute(CommandLine cmd, DependencyResolver resolver, ArenaJob arenaJob, LassoClusterClient clusterClient) {
//        MavenRepository mavenRepository = new MavenRepository(resolver);
//
//        String inputDirectory = cmd.getOptionValue("input");
//        java.lang.System.out.println(String.format("Setting input directory to '%s'", inputDirectory));
//        String outputDirectory = cmd.getOptionValue("output");
//        java.lang.System.out.println(String.format("Setting output directory to '%s'", outputDirectory));
//
//        File path = new File(inputDirectory);
//
//        //DefaultArena arena = new DefaultArena(new ArrayList<>(), mavenRepository);
//        CandidatePool pool = new CandidatePool(mavenRepository);
//        File work = new File(outputDirectory);
//        work.mkdirs();
//        pool.setWorkingDirectory(work);
//        //arena.setAdaptationStrategy(new DefaultAdaptationStrategy());
//        Amplify amplify = new Amplify(mavenRepository);
//
//        //
//        SRMRepositoryWriter writer = new SRMRepositoryWriter(clusterClient, arenaJob);
//        int limitAdapters = arenaJob.getMaxPermutations();
//
//        List<String> referenceImpls;
//        if(arenaJob.isReferenceImplementationOnly()) {
//            // pick by abstraction id
//            Optional<String> refIdOp = arenaJob.getImplementations().stream()
//                    .map(System::getId)
//                    .filter(id -> StringUtils.equals(id, arenaJob.getAbstractionId()))
//                    .findFirst();
//            referenceImpls = new ArrayList<>();
//            referenceImpls.add(
//                    refIdOp.orElseThrow(() -> new IllegalArgumentException("Cannot find reference implementation for abstraction " + arenaJob.getAbstractionId())));
//        } else {
//            // take all
//            referenceImpls = arenaJob.getImplementations().stream()
//                    .map(System::getId)
//                    .collect(Collectors.toList());
//        }
//
//        // options
//        if(cmd.hasOption("features")) {
//            String features = cmd.getOptionValue("features");
//            if (StringUtils.contains(features, ",")) {
//                String[] fArr = StringUtils.split(features, ',');
//
//                LOG.info("found features '{}'", Arrays.toString(fArr));
//
//                amplify.measurePIT = ArrayUtils.contains(fArr, "mutation");
//                amplify.measureJaCoCo = ArrayUtils.contains(fArr, "cc");
//            } else {
//                LOG.info("found single feature '{}'", features);
//
//                amplify.measurePIT = StringUtils.equalsIgnoreCase(features, "mutation");
//                amplify.measureJaCoCo = StringUtils.equalsIgnoreCase(features, "cc");
//            }
//
//            LOG.info("PIT enabled '{}'", amplify.measurePIT);
//            LOG.info("CC enabled '{}'", amplify.measureJaCoCo);
//        }
//
//        DefaultSheetProvider provider = new DefaultSheetProvider(path, pool, arenaJob.getImplementations());
//
//        if(arenaJob.getThreads() > -1) {
//            LOG.info("setting threads to '{}'", arenaJob.getThreads());
//
//            provider.setThreads(arenaJob.getThreads());
//            amplify.setThreads(arenaJob.getThreads());
//        }
//
//        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
//        // in case of method type we need to restrict it to the single method which can be adapted
//        boolean methods = arenaJob.getImplementations().stream().allMatch(i -> i.getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD);
//        if(methods) {
//            adaptationStrategy.addSpecFilter(new ImplementationUnitFilter());
//        }
//
//        // set specification if necessary
//        if(StringUtils.isNotBlank(arenaJob.getSpecification())) {
//            CodeSearch codeSearch = new CodeSearch();
//            try {
//                List<InterfaceSpecification> interfaceSpecifications = codeSearch.fromLQL(arenaJob.getSpecification());
//                InterfaceSpecification interfaceSpecification = interfaceSpecifications.get(0);
//                provider.setInterfaceSpecification(interfaceSpecification);
//            } catch (Throwable e) {
//                LOG.warn("Setting specification failed", e);
//            }
//        }
//
//        // make sure to retrieve all implementations (in case some implementations have no sheet)
//        try {
//            Set<String> allCuts = provider.findCuts();
//            provider.resolveCuts(allCuts);
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//
//        amplify.amplify(referenceImpls, provider,
//                adaptationStrategy,
//                limitAdapters,
//                writer);

        MavenRepository mavenRepository = new MavenRepository(resolver);

        String inputDirectory = cmd.getOptionValue("input");
        java.lang.System.out.println(String.format("Setting input directory to '%s'", inputDirectory));
        String outputDirectory = cmd.getOptionValue("output");
        java.lang.System.out.println(String.format("Setting output directory to '%s'", outputDirectory));

        File path = new File(inputDirectory);

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File(outputDirectory);
        work.mkdirs();
        pool.setWorkingDirectory(work);

        Amplify execute = new Amplify(mavenRepository);

        //
        SRMRepositoryWriter writer = new SRMRepositoryWriter(clusterClient, arenaJob);
        int limitAdapters = arenaJob.getMaxPermutations();

        if(LOG.isInfoEnabled()) {
            LOG.info("Setting limitAdapters to '{}'", limitAdapters);
        }

        execute.setIgnoreVisibility(arenaJob.isIgnoreVisibility());
        execute.setWriteSequenceRecords(arenaJob.isWriteSequenceRecords());

        execute.setGenerateJUnitTests(arenaJob.isGenerateJUnitTests());

        List<String> referenceImpls;
        if (arenaJob.isReferenceImplementationOnly()) {
            // pick by abstraction id
            Optional<String> refIdOp = arenaJob.getImplementations().stream()
                    .map(System::getId)
                    .filter(id -> StringUtils.equals(id, arenaJob.getAbstractionId()))
                    .findFirst();
            referenceImpls = new ArrayList<>();
            referenceImpls.add(
                    refIdOp.orElseThrow(() -> new IllegalArgumentException("Cannot find reference implementation for abstraction " + arenaJob.getAbstractionId())));
        } else {
            // take all
            referenceImpls = arenaJob.getImplementations().stream()
                    .map(System::getId)
                    .collect(Collectors.toList());
        }

        execute.setBySequenceSpecification(arenaJob.isBySequenceSpecification());

        // options
        if(cmd.hasOption("features")) {
            String features = cmd.getOptionValue("features");
            if (StringUtils.contains(features, ",")) {
                String[] fArr = StringUtils.split(features, ',');

                LOG.info("found features '{}'", Arrays.toString(fArr));

                execute.measurePIT = ArrayUtils.contains(fArr, "mutation");
                execute.measureJaCoCo = ArrayUtils.contains(fArr, "cc");
            } else {
                LOG.info("found single feature '{}'", features);

                execute.measurePIT = StringUtils.equalsIgnoreCase(features, "mutation");
                execute.measureJaCoCo = StringUtils.equalsIgnoreCase(features, "cc");
            }

            LOG.info("PIT enabled '{}'", execute.measurePIT);
            LOG.info("CC enabled '{}'", execute.measureJaCoCo);
        }

        DefaultSheetProvider provider = new DefaultSheetProvider(path, pool, arenaJob.getImplementations());

        if(arenaJob.getThreads() > -1) {
            LOG.info("setting threads to '{}'", arenaJob.getThreads());

            provider.setThreads(arenaJob.getThreads());
            execute.setThreads(arenaJob.getThreads());
        }

        // set specification if necessary
        if(StringUtils.isNotBlank(arenaJob.getSpecification())) {
            CodeSearch codeSearch = new CodeSearch();
            try {
                List<InterfaceSpecification> interfaceSpecifications = codeSearch.fromLQL(arenaJob.getSpecification());
                InterfaceSpecification interfaceSpecification = interfaceSpecifications.get(0);
                provider.setInterfaceSpecification(interfaceSpecification);
            } catch (Throwable e) {
                LOG.warn("Setting specification failed", e);
            }
        }

        // set scope for measurements
        execute.setScope(arenaJob.getScope());

        // make sure to retrieve all implementations (in case some implementations have no sheet)
        try {
            Set<String> allCuts = provider.findCuts();
            provider.resolveCuts(allCuts);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        execute.amplify(referenceImpls, provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                writer);
    }

    /**
     * Amplify all implementations resolved by {@link SheetProvider}.
     *
     * @param sheetProvider
     * @param adaptationStrategy
     * @param limitAdapters
     * @param resultsWriter
     */
    public void amplify(SheetProvider sheetProvider, AdaptationStrategy adaptationStrategy, int limitAdapters, CellWriter resultsWriter) {
        amplify(sheetProvider.getImplementations(), sheetProvider, adaptationStrategy, limitAdapters, resultsWriter);
    }

    /**
     * Amplify given list of reference implementations.
     *
     * @param referenceImpls
     * @param sheetProvider
     * @param adaptationStrategy
     * @param limitAdapters
     * @param resultsWriter
     */
    public void amplify(List<String> referenceImpls, SheetProvider sheetProvider, AdaptationStrategy adaptationStrategy, int limitAdapters, CellWriter resultsWriter) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Using '{}' threads for execution", getThreads());
        }

        ExecutorService executor = Executors.newFixedThreadPool(getThreads(),
                new ThreadFactory() {

                    private AtomicInteger inc = new AtomicInteger();

                    /**
                     * Assign a specific names
                     */
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                String.format("%s-%s", "harvest-executor", inc.getAndIncrement()));
                    }

                });

        List<ResolvedSheets> resolvedSheets;
        try {
            resolvedSheets = sheetProvider.resolve();
        } catch (Throwable e) {
            LOG.warn("Could not resolve sheets", e);

            throw new RuntimeException("Could not resolve sheets", e);
        }

        if (LOG.isDebugEnabled()) {
            //resolvedSheets.forEach(r -> LOG.debug("Resolved sheet => {}", r.toString()));
        }

        // pick a reference implementation (iterate .. round-based)
        List<Callable<Void>> tasks = new ArrayList<>(referenceImpls.size());
        for (String referenceId : referenceImpls) {
            Callable<Void> task = () -> {
            try {
                Map<String, SequenceExecutionRecords> resultsMap = run(sheetProvider.getPool(), resolvedSheets, adaptationStrategy, resultsWriter, limitAdapters, referenceId);

                // get successful sequences
                Map<String, SequenceExecutionRecords> filteredBySuccessfulExecution = dropFailedExecutableSequences(resultsMap);
                // minimize (i.e drop duplicates)
                filteredBySuccessfulExecution = dropDuplicates(filteredBySuccessfulExecution);

                Map<String, SequenceExecutionRecords> originalTests = new LinkedHashMap<>();
                originalTests.put(referenceId, filteredBySuccessfulExecution.get(referenceId));

                // now do the measurements

                // ----- original

                // JACOCO
                ClassUnderTest referenceCut = sheetProvider.getPool().getClassUnderTest(referenceId).get();

                if(measureJaCoCo) {
                    // make cut appear as "fresh" (better to re-fetch)
                    referenceCut.getProject().removeContainer();

                    CandidatePool jacocoPool = new CandidatePool(getMavenRepository(), Arrays.asList(referenceCut));

                    DefaultArena jacocoArena = new DefaultArena();
                    jacocoArena.setName("original_jacoco");
                    jacocoArena.setAdaptationStrategy(adaptationStrategy);
                    jacocoPool.setContainerFactory(ContainerFactory.JACOCO_FACTORY);
                    // automatically resolves project-related artifacts
                    jacocoPool.initProjects();

                    SequenceExecutionRecords jacocoOriginalTests = execute(jacocoArena, referenceCut, originalTests, resultsWriter);
                }

                if(measurePIT) {
                    // PIT
                    // make cut appear as "fresh" (better to re-fetch)
                    referenceCut.getProject().removeContainer();

                    CandidatePool pitPool = new CandidatePool(getMavenRepository(), Arrays.asList(referenceCut));

                    DefaultArena pitArena = new DefaultArena();
                    pitArena.setName("original_pit");
                    pitArena.setAdaptationStrategy(adaptationStrategy);
                    // automatically resolves project-related artifacts
                    pitPool.initProjects();

                    // create mutants
                    List<ClassUnderTest> mutants = pitArena.createMutants(pitPool,
                            referenceCut, true, true, true, "original"); // init

                    // desirable to run multi-threaded
                    for (ClassUnderTest variant : pitPool.getClassesUnderTest()) {
                        try {
                            SequenceExecutionRecords variantResults = execute(pitArena, variant, originalTests, resultsWriter);
                        } catch (Throwable e) {
                            LOG.warn("Variant failed " + variant.getFullId());
                            LOG.warn("Stack trace", e);
                        } finally {
                            // remove
                            try {
                                variant.getProject().removeContainer();
                            } catch (Throwable e) {
                                //
                            }
                        }
                    }
                }

                // ----- AMPLIFIED

                if(measureJaCoCo) {
                    // JACOCO
                    // make cut appear as "fresh" (better to re-fetch)
                    referenceCut.getProject().removeContainer();
                    CandidatePool jacocoAmplifiedPool = new CandidatePool(getMavenRepository(), Arrays.asList(referenceCut));

                    DefaultArena jacocoArenaAmplified = new DefaultArena();
                    jacocoArenaAmplified.setName("amplified_jacoco");
                    jacocoAmplifiedPool.setContainerFactory(ContainerFactory.JACOCO_FACTORY);
                    jacocoArenaAmplified.setAdaptationStrategy(adaptationStrategy);
                    // automatically resolves project-related artifacts
                    jacocoAmplifiedPool.initProjects();

                    SequenceExecutionRecords jacocoAmplified = execute(jacocoArenaAmplified, referenceCut, filteredBySuccessfulExecution, resultsWriter);
                }

                // PIT
                if(measurePIT) {
                    // make cut appear as "fresh" (better to re-fetch)
                    referenceCut.getProject().removeContainer();
                    CandidatePool pitAmplifiedPool = new CandidatePool(getMavenRepository(), Arrays.asList(referenceCut));

                    DefaultArena pitArenaAmplified = new DefaultArena();
                    pitArenaAmplified.setName("amplified_pit");
                    pitArenaAmplified.setAdaptationStrategy(adaptationStrategy);
                    // automatically resolves project-related artifacts
                    pitAmplifiedPool.initProjects();

                    // create mutants
                    List<ClassUnderTest> mutantsAmplified = pitArenaAmplified.createMutants(pitAmplifiedPool,
                            referenceCut, true, true, true, "amplified"); // init

                    // desirable to run multi-threaded
                    for (ClassUnderTest variant : pitAmplifiedPool.getClassesUnderTest()) {
                        try {
                            SequenceExecutionRecords variantResults = execute(pitArenaAmplified, variant, filteredBySuccessfulExecution, resultsWriter);
                        } catch (Throwable e) {
                            LOG.warn("Variant failed " + variant.getFullId());
                            LOG.warn("Stack trace", e);
                        } finally {
                            // remove
                            try {
                                variant.getProject().removeContainer();
                            } catch (Throwable e) {
                                //
                            }
                        }
                    }
                }

                // do we want to export original tests?
                if(isWriteOriginalTests()) {
                    // now export original Junit tests
                    if(referenceCut.getLocalProject() != null) {
//                    try {
//                        new JUnit4Generator().exportJUnit(referenceCut, filteredBySuccessfulExecution.get(referenceId).getRecords(), "OriginalTests");
//                    } catch (Throwable e) {
//                        LOG.warn("Export of JUnit failed", e);
//                    }

                        try {
                            CompilationUnit unit = exportJUnit(referenceCut, filteredBySuccessfulExecution.get(referenceId).getRecords(), "OriginalTests");

                            // compile
                            if(isRemoveCompileErrors()) {
                                unit = checkCompileErrors(referenceCut, filteredBySuccessfulExecution.get(referenceId).getRecords(), unit, "OriginalTests");

                                if(isRemoveFlakyTests()) {
                                    // try to execute
                                    checkFlakyTests(referenceCut, unit);
                                }
                            }
                        } catch (Throwable e) {
                            LOG.warn("Export of JUnit failed", e);
                        }
                    }
                }

                // now export amplified Junit tests
                if(isGenerateJUnitTests()) {
                    if(referenceCut.getLocalProject() != null) {
                        List<SequenceExecutionRecord> amplifiedResults = filteredBySuccessfulExecution.values().stream()
                                .flatMap(l -> l.getRecords().stream()).collect(Collectors.toList());

//                    try {
//                        new JUnit4Generator().exportJUnit(referenceCut, amplifiedResults, "AmplifiedTests");
//                    } catch (Throwable e) {
//                        LOG.warn("Export of JUnit failed", e);
//                    }

                        try {
                            CompilationUnit unit = exportJUnit(referenceCut, amplifiedResults, "AmplifiedTests");

                            // compile
                            if(isRemoveCompileErrors()) {
                                unit = checkCompileErrors(referenceCut, amplifiedResults, unit, "AmplifiedTests");

                                if(isRemoveFlakyTests()) {
                                    // try to execute
                                    checkFlakyTests(referenceCut, unit);
                                }
                            }
                        } catch (Throwable e) {
                            LOG.warn("Export of JUnit failed", e);
                        }
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Implementation " + referenceId + " failed");
                LOG.warn("Stack trace", e);
            }

                return null;
            };

            tasks.add(task);
        }

        // execute all
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);

            //executor.invokeAll(tasks, getTaskTimeout() * tasks.size(), TimeUnit.SECONDS);

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
    }

    public Map<String, SequenceExecutionRecords> run(CandidatePool pool, List<ResolvedSheets> resolvedSheetList, AdaptationStrategy adaptationStrategy, CellWriter cellWriter, int limitAdapters, String referenceId) throws IOException {
        ClassUnderTest classUnderTest = pool.getClassUnderTest(referenceId).get();

        Map<String, SequenceExecutionRecords> resultsMap = new LinkedHashMap<>();

        // adapt reference implementation to alternative implementations' sheets
        for (ResolvedSheets alt : resolvedSheetList) {
            //boolean isReferenceSheet = alt.getClassUnderTest().getId().equals(referenceId);

            //
            DefaultArena arena = new DefaultArena();
            arena.setName("execute");
            arena.setAdaptationStrategy(adaptationStrategy);

            try {
                List<SequenceSpecification> sheets = new ArrayList<>(alt.getSheets().values());
                Map<AdaptedImplementation, SequenceExecutionRecords> results = null;
                if(isBySequenceSpecification()) {
                    results = arena.executeEachSequenceSpecification(new ArrayList<>(Collections.singletonList(classUnderTest)), alt.getSpecification(), sheets, cellWriter, limitAdapters);
                } else {
                    results = arena.execute(new ArrayList<>(Collections.singletonList(classUnderTest)), alt.getSpecification(), sheets, cellWriter, limitAdapters);
                }

                // there is exactly 1 entry
                if (results.size() == 1) {
                    for (Map.Entry<AdaptedImplementation, SequenceExecutionRecords> entry : results.entrySet()) {
                        SequenceExecutionRecords executionResults = entry.getValue();

                        resultsMap.put(alt.getClassUnderTest().getId(), executionResults);
                    }
                }

                LOG.debug(String.format("Got '%s' results for alternative impl. '%s'", results.size(), alt.getClassUnderTest().getId()));
            } catch (Throwable e) {
                LOG.warn("Failed to run alt sheets on reference for '{}'", alt.getClassUnderTest().getId());
            }
        }

        return resultsMap;
    }

    public boolean isMeasureJaCoCo() {
        return measureJaCoCo;
    }

    public void setMeasureJaCoCo(boolean measureJaCoCo) {
        this.measureJaCoCo = measureJaCoCo;
    }

    public boolean isMeasurePIT() {
        return measurePIT;
    }

    public void setMeasurePIT(boolean measurePIT) {
        this.measurePIT = measurePIT;
    }

    public boolean isWriteOriginalTests() {
        return writeOriginalTests;
    }

    public void setWriteOriginalTests(boolean writeOriginalTests) {
        this.writeOriginalTests = writeOriginalTests;
    }

    public boolean isWriteSequenceRecords() {
        return writeSequenceRecords;
    }

    public void setWriteSequenceRecords(boolean writeSequenceRecords) {
        this.writeSequenceRecords = writeSequenceRecords;
    }
}
