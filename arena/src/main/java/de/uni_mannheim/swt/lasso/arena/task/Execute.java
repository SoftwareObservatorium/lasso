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
import org.apache.commons.collections4.CollectionUtils;
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
 * Execution task.
 *
 * @author Marcus Kessel
 */
public class Execute extends Task {

    private static final Logger LOG = LoggerFactory
            .getLogger(Execute.class);

    /**
     * Measure JaCoCo code coverage
     */
    private boolean measureJaCoCo = false;

    /**
     * Measure Mutation score using PIT
     */
    private boolean measurePIT = false;

    private boolean writeSequenceRecords;

    public Execute(MavenRepository mavenRepository) {
        super(mavenRepository);

//        this.setMinimizeSequences(false);
//        this.setRemoveCompileErrors(false);
//        this.setRemoveFlakyTests(false);
//
//        this.setBySequenceSpecification(true);
//
//        this.setDropFailedSequences(false);
    }

    public static void execute(CommandLine cmd, DependencyResolver resolver, ArenaJob arenaJob, LassoClusterClient clusterClient) {
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

        Execute execute = new Execute(mavenRepository);

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

        execute.execute(referenceImpls, provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                writer);
    }

    /**
     * Execute all implementations resolved by {@link SheetProvider}.
     *
     * @param sheetProvider
     * @param adaptationStrategy
     * @param limitAdapters
     * @param resultsWriter
     */
    public void execute(SheetProvider sheetProvider, AdaptationStrategy adaptationStrategy, int limitAdapters, CellWriter resultsWriter) {
        execute(sheetProvider.getImplementations(), sheetProvider, adaptationStrategy, limitAdapters, resultsWriter);
    }

    /**
     * Execute
     *
     * @param referenceImpls
     * @param sheetProvider
     * @param adaptationStrategy
     * @param limitAdapters
     * @param resultsWriter
     */
    public void execute(List<String> referenceImpls, SheetProvider sheetProvider, AdaptationStrategy adaptationStrategy, int limitAdapters, CellWriter resultsWriter) {
        if (LOG.isInfoEnabled()) {
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
                                String.format("%s-%s", "execute-executor", inc.getAndIncrement()));
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
            resolvedSheets.forEach(r -> LOG.debug("Resolved sheet => {}", r.toString()));
        }

        // pick a reference implementation (iterate .. round-based)
        List<Callable<Void>> tasks = new ArrayList<>(referenceImpls.size());
        for (String referenceId : referenceImpls) {
            Callable<Void> task = () -> {
                try {
                    DefaultArena arena = new DefaultArena();
                    arena.setName("execute");
                    // we need to establish mapping of REF to ALT
                    arena.setAdaptationStrategy(adaptationStrategy);

                    Map<AdaptedImplementation, SequenceExecutionRecords> resultsMap = run(arena, resolvedSheets, adaptationStrategy, resultsWriter, limitAdapters, referenceId);

                    // check if we want to write sequence records
                    if(isWriteSequenceRecords() && resultsWriter != null) {
                        for(AdaptedImplementation ai : resultsMap.keySet()) {
                            try {
                                resultsWriter.writeSequenceCode(resultsMap.get(ai), arena);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // get successful sequences
                    Map<AdaptedImplementation, SequenceExecutionRecords> filteredBySuccessfulExecution = doDropFailedExecutableSequences(resultsMap);
                    // minimize (i.e drop duplicates)
                    filteredBySuccessfulExecution = doDropDuplicates(filteredBySuccessfulExecution);

                    ClassUnderTest referenceCut = sheetProvider.getPool().getClassUnderTest(referenceId).get();

                    // TODO oracle check
                    //ss.hasOracle()

                    // now do the measurements

                    // FIXME for measurements, we need "clean" executable sequences

                    // ----- original

                    if(measureJaCoCo) {
                        // make cut appear as "fresh" (better to re-fetch)
                        referenceCut.getProject().removeContainer();

                        CandidatePool jacocoPool = new CandidatePool(getMavenRepository(), Arrays.asList(referenceCut));

                        DefaultArena jacocoArena = new DefaultArena();
                        jacocoArena.setName("jacoco");
                        jacocoArena.setAdaptationStrategy(adaptationStrategy);
                        jacocoPool.setContainerFactory(ContainerFactory.jacoco(getScope()));
                        // automatically resolves project-related artifacts
                        jacocoPool.initProjects();

                        //SequenceExecutionRecords jacocoOriginalTests = doExecute(jacocoArena, referenceCut, filteredBySuccessfulExecution, resultsWriter);

                        // ---
                        Map<AdaptedImplementation, SequenceExecutionRecords> jacocoResults = run(jacocoArena, resolvedSheets, adaptationStrategy, resultsWriter, limitAdapters, referenceId);
                    }

                    if(measurePIT) {
                        // PIT
                        // make cut appear as "fresh" (better to re-fetch)
                        referenceCut.getProject().removeContainer();

                        CandidatePool pitPool = new CandidatePool(getMavenRepository(), Arrays.asList(referenceCut));

                        DefaultArena pitArena = new DefaultArena();
                        pitArena.setName("pit");
                        pitArena.setAdaptationStrategy(adaptationStrategy);
                        // automatically resolves project-related artifacts
                        pitPool.initProjects();

                        // create mutants
                        List<ClassUnderTest> mutants = pitArena.createMutants(pitPool,
                                referenceCut, true, true, true, "original"); // init

                        // desirable to run multi-threaded
                        for (ClassUnderTest variant : pitPool.getClassesUnderTest()) {
                            try {
                                SequenceExecutionRecords variantResults = doExecute(pitArena, variant, filteredBySuccessfulExecution, resultsWriter);
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

                    // now export original Junit tests
                    if(isGenerateJUnitTests()) {
                        if (referenceCut.getLocalProject() != null) {
                            for(AdaptedImplementation adaptedImplementation : filteredBySuccessfulExecution.keySet()) {
                                try {
                                    CompilationUnit unit = exportJUnit(referenceCut, filteredBySuccessfulExecution.get(adaptedImplementation).getRecords(), adaptedImplementation.getAdapterId() + "_OriginalTests");

                                    if(isRemoveCompileErrors()) {
                                        // compile
                                        unit = checkCompileErrors(referenceCut, filteredBySuccessfulExecution.get(adaptedImplementation).getRecords(), unit, adaptedImplementation.getAdapterId() + "_OriginalTests");

                                        if(isRemoveFlakyTests()) {
                                            // try to execute and remove failing tests
                                            checkFlakyTests(referenceCut, unit);
                                        }
                                    }
                                } catch (Throwable e) {
                                    LOG.warn("Export of JUnit failed", e);
                                }
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

    public Map<AdaptedImplementation, SequenceExecutionRecords> run(DefaultArena arena, List<ResolvedSheets> resolvedSheetList, AdaptationStrategy adaptationStrategy, CellWriter cellWriter, int limitAdapters, String referenceId) throws IOException {
//        ResolvedSheets reference = resolvedSheetList.stream()
//                .filter(r -> r.getClassUnderTest().getId().equals(referenceId))
//                .findFirst().get();

        Map<AdaptedImplementation, SequenceExecutionRecords> resultsMap = new LinkedHashMap<>();

        resolvedSheetList = resolvedSheetList.stream()
                .filter(r -> r.getClassUnderTest().getId().equals(referenceId))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(resolvedSheetList)) {
            LOG.warn("Failed to find resolved sheets for {}", referenceId);
            return resultsMap;
        }

        ClassUnderTest classUnderTest = resolvedSheetList.get(0).getClassUnderTest();
        InterfaceSpecification specification = resolvedSheetList.get(0).getSpecification();

        List<SequenceSpecification> sheets = new LinkedList<>();
        for(ResolvedSheets resolvedSheets : resolvedSheetList) {
            sheets.addAll(resolvedSheets.getSheets().values());
        }

        // configure
        if(LOG.isInfoEnabled()) {
            LOG.info("setting ignore visibility to '{}'", isIgnoreVisibility());
        }
        sheets.forEach(ss -> ss.setIgnoreVisibility(isIgnoreVisibility()));

        try {
            LOG.debug("using '{}' sheets", sheets.stream().map(s -> s.getName()).collect(Collectors.joining(",")));

            Map<AdaptedImplementation, SequenceExecutionRecords> results = null;
            if (isBySequenceSpecification()) {
                results = arena.executeEachSequenceSpecification(new ArrayList<>(Collections.singletonList(classUnderTest)), specification, sheets, cellWriter, limitAdapters);
            } else {
                results = arena.execute(new ArrayList<>(Collections.singletonList(classUnderTest)), specification, sheets, cellWriter, limitAdapters);
            }

            // TODO change to N again

//            // there is exactly 1 entry
//            if (results.size() == 1) {
//                for (Map.Entry<AdaptedImplementation, SequenceExecutionRecords> entry : results.entrySet()) {
//                    SequenceExecutionRecords executionResults = entry.getValue();
//
//                    resultsMap.put(classUnderTest.getId(), executionResults);
//                }
//            }

            resultsMap.putAll(results);

            LOG.debug(String.format("Got '%s' results for alternative impl. '%s'", results.size(), classUnderTest.getId()));
        } catch (Throwable e) {
            LOG.warn("Failed to run alt sheets on reference for '{}'", classUnderTest.getId());
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

    public boolean isWriteSequenceRecords() {
        return writeSequenceRecords;
    }

    public void setWriteSequenceRecords(boolean writeSequenceRecords) {
        this.writeSequenceRecords = writeSequenceRecords;
    }
}