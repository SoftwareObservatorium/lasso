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
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.PassThroughAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.JaCoCoListener;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.srh.SRHWriter;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.arena.task.load.DefaultSheetProvider;
import de.uni_mannheim.swt.lasso.arena.task.load.SheetMatch;
import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.dto.srm.JUnitCodeUnit;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Execution task.
 *
 * @author Marcus Kessel
 */
public class SSNExecute extends Task {

    private static final Logger LOG = LoggerFactory
            .getLogger(SSNExecute.class);

    /**
     * Measure JaCoCo code coverage
     */
    private boolean measureJaCoCo = false;

    /**
     * Measure Mutation score using PIT
     */
    private boolean measurePIT = false;

    private boolean writeSequenceRecords;

    public SSNExecute(MavenRepository mavenRepository) {
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

        java.lang.System.out.println("Found sheets: " + arenaJob.getStimulusSheets());

        String inputDirectory = cmd.getOptionValue("input");
        java.lang.System.out.println(String.format("Setting input directory to '%s'", inputDirectory));
        String outputDirectory = cmd.getOptionValue("output");
        java.lang.System.out.println(String.format("Setting output directory to '%s'", outputDirectory));

        // FIXME ?
        File path = new File(inputDirectory);

        File work = new File(outputDirectory);
        work.mkdirs();

        SSNExecute execute = new SSNExecute(mavenRepository);
        execute.setIgnoreVisibility(arenaJob.isIgnoreVisibility());
        execute.setWriteSequenceRecords(arenaJob.isWriteSequenceRecords());
        execute.setGenerateJUnitTests(arenaJob.isGenerateJUnitTests());

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

        // set scope for measurements
        execute.setScope(arenaJob.getScope());

        // test driver
        SSNTestDriver testDriver = execute.setUpTestDriver(arenaJob);
        // set build directory
        testDriver.setBuildWorkingDirectory(work);

        int adapterLimit = 1;
        if (arenaJob.getMaxPermutations() > 0) {
            adapterLimit = arenaJob.getMaxPermutations();
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("adapterLimit is '{}'", adapterLimit);
        }

        // already set in sheet
        //String interfaceSpecificationStr = arenaJob.getSpecification();
        List<System> implementations = arenaJob.getImplementations();
        List<Sheet> stimulusSheets = arenaJob.getStimulusSheets();

        // SSN sheets
        List<Sheet> ssnSheets = stimulusSheets.stream().filter(Sheet::isSSN).toList();

        // add if any
        List<Sheet> sheets = new LinkedList<>(ssnSheets);

        // check if JUnit classes available
        List<JUnitCodeUnit> junitClasses = stimulusSheets.stream()
                .filter(s -> s.getClass().equals(JUnitCodeUnit.class))
                .map(s->(JUnitCodeUnit) s).toList();
        if(CollectionUtils.isNotEmpty(junitClasses)) {
            try {
                List<Sheet> junitSheets = resolveSheetsFromJUnit(testDriver, junitClasses, implementations, path, arenaJob);
                if(CollectionUtils.isNotEmpty(junitSheets)) {
                    sheets.addAll(junitSheets);
                }
            } catch (Throwable e) {
                // ignore
                e.printStackTrace();
            }
        }

        // do we need to resolve tests and translate to sheets?
        try {
            List<Sheet> translatedSheets = execute.resolveSheetsFromJUnit(testDriver, implementations, path, arenaJob);
            if(CollectionUtils.isNotEmpty(translatedSheets)) {
                sheets.addAll(translatedSheets);
            }
        } catch (Throwable e) {
            // ignore
            e.printStackTrace();
        }

        // execute
        execute.execute(testDriver, clusterClient, sheets, implementations, adapterLimit, arenaJob);
    }

    private SSNTestDriver setUpTestDriver(ArenaJob arenaJob) {
        // driver
        SSNTestDriver testDriver = new SSNTestDriver();
        testDriver.setMavenRepository(getMavenRepository());

        if(LOG.isInfoEnabled()) {
            LOG.info("adapter strategy is '{}'", arenaJob.getAdapterStrategy());
        }

        // set adaptation strategy
        String adaptationStrategy = arenaJob.getAdapterStrategy();
        if (StringUtils.equalsIgnoreCase(adaptationStrategy, DefaultAdaptationStrategy.class.getSimpleName())) {
            testDriver.setAdaptationStrategy(new DefaultAdaptationStrategy());
        } else if (StringUtils.equalsIgnoreCase(adaptationStrategy, PassThroughAdaptationStrategy.class.getSimpleName())) {
            testDriver.setAdaptationStrategy(new PassThroughAdaptationStrategy());
        } else {
            // default
            testDriver.setAdaptationStrategy(new DefaultAdaptationStrategy());
        }

        return testDriver;
    }

    public static List<Sheet> resolveSheetsFromJUnit(SSNTestDriver ssnTestDriver, List<JUnitCodeUnit> jUnitCodeUnits, List<System> implementations, File path, ArenaJob arenaJob) throws IOException {
        // use from FA spec
        InterfaceSpecification interfaceSpecification = LQLUtils.lqlToList(arenaJob.getSpecification()).get(0);
        ClassUnderTest pseudo = CutUtils.createExample(interfaceSpecification.getClassName());

        // create CUTs
        List<ClassUnderTest> classesUnderTest = new LinkedList<>();
        classesUnderTest.add(pseudo);

        for(System implementation : implementations) {
            ClassUnderTest classUnderTest = new ClassUnderTest(implementation);
            classesUnderTest.add(classUnderTest);
        }

        CandidatePool pool = new CandidatePool(ssnTestDriver.getMavenRepository(), classesUnderTest);
        if(ssnTestDriver.getBuildWorkingDirectory() != null) {
            pool.setWorkingDirectory(ssnTestDriver.getBuildWorkingDirectory());
        }

        // init
        pool.initProjects();

        List<Sheet> allStimulusSheets = new LinkedList<>();
        for(JUnitCodeUnit jUnitCodeUnit : jUnitCodeUnits) {
            try {

//                // CREATE PSEUDO CUT
//                ClassUnderTest classUnderTest = TestSupport.createPseudoImplementation(interfaceSpecification.getClassName());

                String testClassSource = jUnitCodeUnit.getCodeUnit().getContent();

                // certain unit tests can only be fully resolved by using its CUT
                ClassUnderTest classUnderTest = pseudo; // pseudo is default
                if(StringUtils.isNotBlank(jUnitCodeUnit.getClassUnderTest())) {
                    // resolve
                    Optional<System> systemOp = implementations.stream().filter(s -> StringUtils.equals(jUnitCodeUnit.getClassUnderTest(), s.getId())).findFirst();

                    if(systemOp.isPresent()) {
                        classUnderTest = classesUnderTest.stream().filter(c -> StringUtils.equals(c.getId(), systemOp.get().getId())).findFirst().get();
                    }
                }

                List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClassSource, classUnderTest, interfaceSpecification, null, jUnitCodeUnit.getTestPrefix());

                if(CollectionUtils.isNotEmpty(stimulusSheets)) {
                    for(Sheet stimulusSheet : stimulusSheets) {
                        java.lang.System.out.println(stimulusSheet.getSignature());
                        java.lang.System.out.println(stimulusSheet.getBody());
                        java.lang.System.out.println("----");

                        // set target specification
                        stimulusSheet.setInterfaceSpecification(arenaJob.getSpecification());
                        //stimulusSheet.setInvocations(new LinkedList<>());
                    }

                    allStimulusSheets.addAll(stimulusSheets);
                }
            } catch (Throwable e) {
                // ignore
                e.printStackTrace();
            }
        }

        return allStimulusSheets;
    }

    public static List<Sheet> resolveSheetsFromJUnit(SSNTestDriver ssnTestDriver, List<System> implementations, File path, ArenaJob arenaJob) {
        // create CUTs
        List<ClassUnderTest> classesUnderTest = new LinkedList<>();
        for(System implementation : implementations) {
            ClassUnderTest classUnderTest = new ClassUnderTest(implementation);
            classesUnderTest.add(classUnderTest);
        }

        CandidatePool pool = new CandidatePool(ssnTestDriver.getMavenRepository(), new ArrayList<>(classesUnderTest));
        if(ssnTestDriver.getBuildWorkingDirectory() != null) {
            pool.setWorkingDirectory(ssnTestDriver.getBuildWorkingDirectory());
        }

        // init
        pool.initProjects();

        Map<String, ClassUnderTest> implsMap = classesUnderTest.stream().collect(Collectors.toMap(i -> i.getId(), i -> i));

        DefaultSheetProvider provider = new DefaultSheetProvider(path, pool, implementations);
        List<SheetMatch> matches = provider.findUnitTests();

        List<Sheet> allStimulusSheets = new LinkedList<>();
        for(SheetMatch sheetMatch : matches) {
            try {
                ClassUnderTest classUnderTest = implsMap.get(sheetMatch.getImplementation());

                String testClassSource = FileUtils.readFileToString(sheetMatch.getFile(), StandardCharsets.UTF_8);

                // spec from CUT
//                JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();
//                InterfaceSpecification parsedSpecification = importJUnitClass.toSpecification(testClassSource, classUnderTest).get(classUnderTest.getClassName());
//                java.lang.System.out.println(parsedSpecification.toLQL());

                // use from FA spec
                InterfaceSpecification interfaceSpecification = LQLUtils.lqlToList(arenaJob.getSpecification()).get(0);

                List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClassSource, classUnderTest, interfaceSpecification, null, "evo");

                if(CollectionUtils.isNotEmpty(stimulusSheets)) {
                    for(Sheet stimulusSheet : stimulusSheets) {
                        java.lang.System.out.println(stimulusSheet.getSignature());
                        java.lang.System.out.println(stimulusSheet.getBody());
                        java.lang.System.out.println("----");

                        // set target specification
                        stimulusSheet.setInterfaceSpecification(arenaJob.getSpecification());
                        //stimulusSheet.setInvocations(new LinkedList<>());
                    }

                    allStimulusSheets.addAll(stimulusSheets);
                }
            } catch (Throwable e) {
                // ignore
                e.printStackTrace();
            }
        }

        return allStimulusSheets;
    }

    // execute
    private void execute(SSNTestDriver testDriver, LassoClusterClient clusterClient, List<Sheet> stimulusSheets, List<System> implementations, int adapterLimit, ArenaJob arenaJob) {
        // some arena id
        String arenaId = UUID.randomUUID().toString();

        // create CUTs
        List<ClassUnderTest> classesUnderTest = new LinkedList<>();
        for(System implementation : implementations) {
            ClassUnderTest classUnderTest = new ClassUnderTest(implementation);
            classesUnderTest.add(classUnderTest);
        }

        // test invocations
        List<SheetInvocation> sheetInvocations = stimulusSheets.stream().flatMap(s -> {
            String sheetName = StringUtils.substringBefore(s.getSignature(), "(");

            List<String> invocations = s.getInvocations();
            if (CollectionUtils.isEmpty(invocations)) {
                // default invocation
                return Arrays.asList(new SheetInvocation(sheetName, "")).stream();
            }

            //
            return invocations.stream().map(i -> new SheetInvocation(sheetName, i));
        }).toList();

        // stimulus matrix
        StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix;
        try {
            stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                    stimulusSheets, classesUnderTest, sheetInvocations);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Parsing stimulus matrix failed", e);
        }

        Validate.isTrue(!(isMeasureJaCoCo() && isMeasurePIT()), "JaCoCo and mutation is not possible at the same time");

        // visitor
        InvocationVisitor invocationVisitor;
        JaCoCoListener jaCoCoListener = new JaCoCoListener();
        if (isMeasureJaCoCo()) {
            invocationVisitor = new CompositeInvocationVisitor(
                    Arrays.asList(jaCoCoListener)); // add jacoco listener

            // set driver
            testDriver.setEnableJaCoCoCoverage(true);
        } else {
            invocationVisitor = new InvocationVisitor();
        }

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix;

        if (isMeasurePIT()) {
            try {
                stimulusResponseMatrix = testDriver.mutateAndRunSheets(stimulusMatrix, adapterLimit, invocationVisitor);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException("mutateAndRunSheets failed", e);
            }
        } else {
            try {
                stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, adapterLimit, invocationVisitor);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException("runSheets failed", e);
            }
        }

        //
        SRHWriter writer = new SRHWriter(clusterClient);

        // all tests
//        for (Test test : stimulusResponseMatrix.getRows()) {
//            // something to do here?
//        }

        // all implementations
        for (AdaptedImplementation adaptedImplementation : stimulusResponseMatrix.getColumns()) {
            // add oracle?s
        }

        // FIXME make configurable
        GsonMapper gsonMapper = new GsonMapper();

        // write
        for (Test test : stimulusResponseMatrix.getRows()) {
            boolean stimulusStored = false;

            // for each test
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> oracleSheet = null;

            for (AdaptedImplementation adaptedImplementation : stimulusResponseMatrix.getColumns()) {
                ExecutedInvocations executedInvocations = stimulusResponseMatrix.get(test, adaptedImplementation);
                LOG.debug("executed invocations\n{}", executedInvocations);

                TestInvocation testInvocation = stimulusMatrix.get(test, adaptedImplementation.getAdaptee());

                if(!stimulusStored) {
                    try {
                        // write stimulus sheet and interface
                        writer.storeStimulusSheet(arenaJob, arenaId, test, testInvocation);
                        stimulusStored = true;
                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(oracleSheet == null) {
                    try {
                        //ExecutedInvocations oracleInvocations = SheetUtils.toOracle(executedInvocations.getInvocations());
                        ExecutedInvocations oracleInvocations = SheetUtils.toOracle(executedInvocations);
                        oracleSheet = SheetUtils.toOracleSheet(oracleInvocations, gsonMapper);

                        //oracleSheet.debug();

                        LOG.info("Storing oracle sheet in SRH");

                        // store actuation sheet
                        writer.storeOracleActuationSheet(arenaJob, arenaId, test, testInvocation, oracleSheet);
                    } catch (Throwable e) {
                        LOG.warn("Oracle sheet failed", e);
                    }
                }

                try {
                    List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = SheetUtils.toSheets(adaptedImplementation, executedInvocations, gsonMapper);

                    //de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);

//                    actuationSheetData.debug();
//                    adaptedActuationSheetData.debug();

//                    LOG.debug("JSON actuationSheet\n{}", actuationSheetData.toJsonl());
//                    LOG.debug("JSON adaptedActuationSheet\n{}", adaptedActuationSheetData.toJsonl());

                    try {
                        LOG.info("Storing sheet in SRH for 'codeUnit {} adapter {} variant {}'", adaptedImplementation.getAdaptee().getId(), adaptedImplementation.getAdapterId(), adaptedImplementation.getAdaptee().getVariantId());

                        // store actuation sheet
                        writer.storeActuationSheet(arenaJob, arenaId, adaptedImplementation, test, testInvocation, adaptedActuationSheetData);
                    } catch (Throwable e) {
                        LOG.warn("Storing sheet in SRH failed", e);
                    }
                } catch (Throwable e) {
                    LOG.warn("execution failed", e);

                    throw new RuntimeException(e);
                }
            }
        }

        if(isMeasureJaCoCo()) {
            // metrics
            for (AdaptedImplementation adaptedImplementation : stimulusResponseMatrix.getColumns()) {
                StimulusResponseMatrix<String, AdaptedImplementation, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object>> jacocoSrm = jaCoCoListener.getStimulusResponseMatrix();
                Set<String> metricIds = jacocoSrm.getRows();

                //int cols = aggregatedMetricSrm.getNumberOfColumns();
                // assume same order
                for (String metricId : metricIds) {
                    de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object> metricSheet = jacocoSrm.get(metricId, adaptedImplementation);
                    //metricSheet.debug();

                    // add to SRH
                    try {
                        LOG.info("Storing metric sheet in SRH");

                        // store
                        writer.storeMetricActuationSheet(arenaJob, arenaId, adaptedImplementation, "jacoco", metricSheet);
                    } catch (Throwable e) {
                        LOG.warn("Storing metric sheet in SRH failed", e);
                    }
                }
            }
        }
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