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

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.arena.sequence.compile.JavaCompiler;
import de.uni_mannheim.swt.lasso.arena.sequence.generator.unit.JUnit4Generator;
import de.uni_mannheim.swt.lasso.arena.sequence.minimize.SequenceSpecificationMinimizer;
import de.uni_mannheim.swt.lasso.arena.sequence.unit.JUnitReport;
import de.uni_mannheim.swt.lasso.arena.sequence.unit.JUnitRunner;
import de.uni_mannheim.swt.lasso.arena.writer.CellWriter;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.sequence.Sequence;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract Arena task.
 *
 * @author Marcus Kessel
 */
public abstract class Task {

    private static final Logger LOG = LoggerFactory
            .getLogger(Task.class);

    /**
     * Adapt by sequence specification or by entire set of sequence specifications.
     */
    private boolean bySequenceSpecification = true;

    /**
     * Number of parallel threads
     */
    private int threads = 4;

    /**
     * Remove any sequences (i.e JUnit test methods) which cannot be compiled
     */
    private boolean removeCompileErrors = true;

    /**
     * Remove any sequences (i.e JUnit test methods) for which any assertions fail
     */
    private boolean removeFlakyTests = true;

    /**
     * Minimize sequences (i.e drop duplicates based on {@link randoop.sequence.Sequence}).
     */
    private boolean minimizeSequences = true;

    /**
     * Drop failed executable sequences?
     */
    private boolean dropFailedSequences = true;

    private boolean generateJUnitTests = false;

    /**
     * Task timeout in seconds
     */
    private int taskTimeout = 60 * 60;

    /**
     * Number of attempts to remove compile errors
     */
    private int compileErrorsAttempts = 5;

//    /**
//     * Number of attempts to remove flaky tests
//     */
//    private int flakyTestsAttempts = 5;

    private Scope scope;

    private boolean ignoreVisibility;

    private final MavenRepository mavenRepository;

    public Task(MavenRepository mavenRepository) {
        this.mavenRepository = mavenRepository;
    }

    protected CompilationUnit exportJUnit(ClassUnderTest cut, List<SequenceExecutionRecord> records, String testClassSuffix) throws IOException {
        CompilationUnit unit = new JUnit4Generator().exportJUnit(cut, records, testClassSuffix);

        if(LOG.isInfoEnabled()) {
            LOG.info("Exported test => '{}'", unit.getFQName());
        }

        return unit;
    }

    protected CompilationUnit checkCompileErrors(ClassUnderTest cut, List<SequenceExecutionRecord> records, CompilationUnit unit, String testClassSuffix) throws IOException {
        // how many retries?
        for(int r = 0; r < getCompileErrorsAttempts(); r++) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Checking for compile errors => '{}'. Attempt '{}' of '{}'", unit.getFQName(), r + 1, getCompileErrorsAttempts());
            }

            List<Long> compileErrors = new JavaCompiler().compileAndObtainErrors(cut, unit, mavenRepository.getResolver());
            if(compileErrors.isEmpty()){
                // nothing to do
                break;
            } else {
                LOG.warn("Found compile errors '{}'. Modifying test class", compileErrors);

                unit = new JUnit4Generator().exportJUnit(cut, records, testClassSuffix, compileErrors);
            }
        }

        return unit;
    }

    protected void checkFlakyTests(ClassUnderTest cut, CompilationUnit unit) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Checking for flaky tests => '{}'", unit.getFQName());
        }

        // try to execute
        try {
            JUnitReport report = new JUnitRunner().run(cut, unit, mavenRepository.getResolver());
            if(!report.isPassing()) {
                // write updated unit again
                LOG.warn("Overriding flaky unit '{}'", unit.getFQName());

                // compile again
                List<Long> compileErrors = new JavaCompiler().compileAndObtainErrors(cut, report.getUnit(), mavenRepository.getResolver());
                if(!compileErrors.isEmpty()){
                    // we failed
                }

                // write out anyways?
                MavenProject mavenProject = cut.getLocalProject();
                mavenProject.writeCompilationUnit(report.getUnit(), true);
            } else {
                // we succeeded
            }
        } catch (Throwable e) {
            LOG.warn("Checking for flaky tests failed for '{}'", cut.getId());
            LOG.warn("stack trace", e);
        }
    }

    protected List<SequenceExecutionRecord> minimizeSequences(List<SequenceExecutionRecord> records) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Minimizing sequence records");
        }

        List<SequenceExecutionRecord> minimized = new SequenceSpecificationMinimizer().minimize(records);

        // FIXME store in observations
        int noOfRemoved = records.size() - minimized.size();

        if(LOG.isInfoEnabled()) {
            LOG.info("Removed '{}' sequence records", noOfRemoved);
        }

        return minimized;
    }

    protected SequenceExecutionRecords execute(DefaultArena arena, ClassUnderTest cut, Map<String, SequenceExecutionRecords> originalTests, CellWriter writer) {
        if(isBySequenceSpecification()) {
            return arena.executeEachSequenceSpecification(cut, originalTests, writer);
        } else {
            return arena.execute(cut, originalTests, writer);
        }
    }

    protected SequenceExecutionRecords doExecute(DefaultArena arena, ClassUnderTest cut, Map<AdaptedImplementation, SequenceExecutionRecords> originalTests, CellWriter writer) {
        if(isBySequenceSpecification()) {
            return arena.doExecuteEachSequenceSpecification(cut, originalTests, writer);
        } else {
            return arena.doExecute(cut, originalTests, writer);
        }
    }

//    protected SequenceExecutionRecords dropFailedExecutableSequences(SequenceExecutionRecords executionResults) {
//        Map<String, SequenceExecutionRecords> filteredMap = new LinkedHashMap<>();
//
//        if(!isDropFailedSequences()) {
//            return executionResults;
//        }
//
//        SequenceExecutionRecords filteredResults = new SequenceExecutionRecords(executionResults.getImplementation(), executionResults.getSpecification());
//
//        for (SequenceExecutionRecord executionResult : executionResults.getRecords()) {
//            if (executionResult.getExecutableSequence() != null) {
//                LOG.debug(executionResult.getSequenceSpecification().getName() + " => normally executed = "
//                        + executionResult.getExecutableSequence().isNormalExecution() // if false, then exception thrown??
//                        + ", nonExecutedStatements = "
//                        + executionResult.getExecutableSequence().hasNonExecutedStatements()
//                        + ", hasInvalidBehavior = "
//                        + executionResult.getExecutableSequence().hasInvalidBehavior()
//                        + ", hasFailure = "
//                        + executionResult.getExecutableSequence().hasFailure()
//                );
//
//                if (executionResult.getExecutableSequence().sequence != null) {
//                    // FIXME java.lang.Error: Exception thrown before end of sequence
//                    // also include subsequences
//                    try {
//                        if(executionResult.getExecutableSequence().hasNonExecutedStatements()) {
//                            int i = executionResult.getExecutableSequence().getNonExecutedIndex();
//
//                            LOG.debug("Sequence not executed at {} => {}. Statement {}. Before result {}", i, executionResult.getExecutableSequence().toCodeString(),
//                                    executionResult.getExecutableSequence().sequence.getStatement(i),
//                                    executionResult.getExecutableSequence().getResult(i- 1)
//                            );
//
//                            // FIXME create subsequence
//                            Sequence sub = new Sequence(executionResult.getExecutableSequence().sequence.statements.getSublist(i - 1));
//                            executionResult.getExecutableSequence().sequence = sub;
//                            executionResult.setSequence(sub);
//
//                            LOG.debug("Setting subsequence {}", executionResult.getExecutableSequence().toCodeString());
//
//                        }
//                    } catch (Throwable e) {
//                        e.printStackTrace();
//                    }
//
//                    filteredResults.add(executionResult);
//                } else {
//                    LOG.debug("Missing sequence " + executionResult.getSequenceSpecification().getName());
//                }
//            } else {
//                LOG.debug("Missing executable sequence " + executionResult.getSequenceSpecification().getName());
//            }
//        }
//
//        return filteredResults;
//    }
//
//    protected  SequenceExecutionRecords dropDuplicates(SequenceExecutionRecords executionResults) {
//        if(!isMinimizeSequences()) {
//            return executionResults;
//        }
//
//        // minimize?
//        // here post-processing
//        if(CollectionUtils.isNotEmpty(executionResults.getRecords())) {
//            List<SequenceExecutionRecord> minimized = minimizeSequences(executionResults.getRecords());
//            executionResults.setRecords(minimized);
//
//            // FIXME what about observations? do they need to be filtered?
//        }
//
//        return executionResults;
//    }

    protected Map<AdaptedImplementation, SequenceExecutionRecords> doDropFailedExecutableSequences(Map<AdaptedImplementation, SequenceExecutionRecords> resultsMap) {
        Map<AdaptedImplementation, SequenceExecutionRecords> filteredMap = new LinkedHashMap<>();

        if(!isDropFailedSequences()) {
            filteredMap.putAll(resultsMap);

            return filteredMap;
        }

        for (AdaptedImplementation id : resultsMap.keySet()) {
            SequenceExecutionRecords executionResults = resultsMap.get(id);

            SequenceExecutionRecords filteredResults = new SequenceExecutionRecords(executionResults.getImplementation(), executionResults.getSpecification());
            filteredMap.put(id, filteredResults);

            for (SequenceExecutionRecord executionResult : executionResults.getRecords()) {
                if (executionResult.getExecutableSequence() != null) {
                    LOG.debug(executionResult.getSequenceSpecification().getName() + " => normally executed = "
                            + executionResult.getExecutableSequence().isNormalExecution() // if false, then exception thrown??
                            + ", nonExecutedStatements = "
                            + executionResult.getExecutableSequence().hasNonExecutedStatements()
                            + ", hasInvalidBehavior = "
                            + executionResult.getExecutableSequence().hasInvalidBehavior()
                            + ", hasFailure = "
                            + executionResult.getExecutableSequence().hasFailure()
                    );

                    if (executionResult.getExecutableSequence().sequence != null) {
                        // FIXME java.lang.Error: Exception thrown before end of sequence
                        // also include subsequences
                        try {
                            if(executionResult.getExecutableSequence().hasNonExecutedStatements()) {
                                int i = executionResult.getExecutableSequence().getNonExecutedIndex();

                                LOG.debug("Sequence not executed at {} => {}. Statement {}. Before result {}", i, executionResult.getExecutableSequence().toCodeString(),
                                        executionResult.getExecutableSequence().sequence.getStatement(i),
                                        executionResult.getExecutableSequence().getResult(i- 1)
                                );

                                // FIXME create subsequence
                                Sequence sub = new Sequence(executionResult.getExecutableSequence().sequence.statements.getSublist(i - 1));
                                executionResult.getExecutableSequence().sequence = sub;
                                executionResult.setSequence(sub);

                                LOG.debug("Setting subsequence {}", executionResult.getExecutableSequence().toCodeString());

                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                        filteredResults.add(executionResult);
                    } else {
                        LOG.debug("Missing sequence " + executionResult.getSequenceSpecification().getName());
                    }
                } else {
                    LOG.debug("Missing executable sequence " + executionResult.getSequenceSpecification().getName());
                }
            }
        }

        return filteredMap;
    }

    @Deprecated
    protected Map<String, SequenceExecutionRecords> dropFailedExecutableSequences(Map<String, SequenceExecutionRecords> resultsMap) {
        Map<String, SequenceExecutionRecords> filteredMap = new LinkedHashMap<>();

        if(!isDropFailedSequences()) {
            filteredMap.putAll(resultsMap);

            return filteredMap;
        }

        for (String id : resultsMap.keySet()) {
            SequenceExecutionRecords executionResults = resultsMap.get(id);

            SequenceExecutionRecords filteredResults = new SequenceExecutionRecords(executionResults.getImplementation(), executionResults.getSpecification());
            filteredMap.put(id, filteredResults);

            for (SequenceExecutionRecord executionResult : executionResults.getRecords()) {
                if (executionResult.getExecutableSequence() != null) {
                    LOG.debug(executionResult.getSequenceSpecification().getName() + " => normally executed = "
                            + executionResult.getExecutableSequence().isNormalExecution() // if false, then exception thrown??
                            + ", nonExecutedStatements = "
                            + executionResult.getExecutableSequence().hasNonExecutedStatements()
                            + ", hasInvalidBehavior = "
                            + executionResult.getExecutableSequence().hasInvalidBehavior()
                            + ", hasFailure = "
                            + executionResult.getExecutableSequence().hasFailure()
                    );

                    if (executionResult.getExecutableSequence().sequence != null) {
                        // FIXME java.lang.Error: Exception thrown before end of sequence
                        // also include subsequences
                        try {
                            if(executionResult.getExecutableSequence().hasNonExecutedStatements()) {
                                int i = executionResult.getExecutableSequence().getNonExecutedIndex();

                                LOG.debug("Sequence not executed at {} => {}. Statement {}. Before result {}", i, executionResult.getExecutableSequence().toCodeString(),
                                        executionResult.getExecutableSequence().sequence.getStatement(i),
                                        executionResult.getExecutableSequence().getResult(i- 1)
                                        );

                                // FIXME create subsequence
                                Sequence sub = new Sequence(executionResult.getExecutableSequence().sequence.statements.getSublist(i - 1));
                                executionResult.getExecutableSequence().sequence = sub;
                                executionResult.setSequence(sub);

                                LOG.debug("Setting subsequence {}", executionResult.getExecutableSequence().toCodeString());

                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                        filteredResults.add(executionResult);
                    } else {
                        LOG.debug("Missing sequence " + executionResult.getSequenceSpecification().getName());
                    }
                } else {
                    LOG.debug("Missing executable sequence " + executionResult.getSequenceSpecification().getName());
                }
            }
        }

        return filteredMap;
    }

    @Deprecated
    protected  Map<String, SequenceExecutionRecords> dropDuplicates(Map<String, SequenceExecutionRecords> resultsMap) {
        if(!isMinimizeSequences()) {
            return resultsMap;
        }

        for (String id : resultsMap.keySet()) {
            SequenceExecutionRecords executionResults = resultsMap.get(id);

            // minimize?
            // here post-processing
            if(CollectionUtils.isNotEmpty(executionResults.getRecords())) {
                List<SequenceExecutionRecord> minimized = minimizeSequences(executionResults.getRecords());
                executionResults.setRecords(minimized);

                // FIXME what about observations? do they need to be filtered?
            }
        }

        return resultsMap;
    }

    protected  Map<AdaptedImplementation, SequenceExecutionRecords> doDropDuplicates(Map<AdaptedImplementation, SequenceExecutionRecords> resultsMap) {
        if(!isMinimizeSequences()) {
            return resultsMap;
        }

        for (AdaptedImplementation id : resultsMap.keySet()) {
            SequenceExecutionRecords executionResults = resultsMap.get(id);

            // minimize?
            // here post-processing
            if(CollectionUtils.isNotEmpty(executionResults.getRecords())) {
                List<SequenceExecutionRecord> minimized = minimizeSequences(executionResults.getRecords());
                executionResults.setRecords(minimized);

                // FIXME what about observations? do they need to be filtered?
            }
        }

        return resultsMap;
    }

    public boolean isBySequenceSpecification() {
        return bySequenceSpecification;
    }

    public void setBySequenceSpecification(boolean bySequenceSpecification) {
        this.bySequenceSpecification = bySequenceSpecification;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean isRemoveCompileErrors() {
        return removeCompileErrors;
    }

    public void setRemoveCompileErrors(boolean removeCompileErrors) {
        this.removeCompileErrors = removeCompileErrors;
    }

    public boolean isRemoveFlakyTests() {
        return removeFlakyTests;
    }

    public void setRemoveFlakyTests(boolean removeFlakyTests) {
        this.removeFlakyTests = removeFlakyTests;
    }

    public MavenRepository getMavenRepository() {
        return mavenRepository;
    }

    public boolean isMinimizeSequences() {
        return minimizeSequences;
    }

    public void setMinimizeSequences(boolean minimizeSequences) {
        this.minimizeSequences = minimizeSequences;
    }

    public boolean isDropFailedSequences() {
        return dropFailedSequences;
    }

    public void setDropFailedSequences(boolean dropFailedSequences) {
        this.dropFailedSequences = dropFailedSequences;
    }

    public int getTaskTimeout() {
        return taskTimeout;
    }

    public void setTaskTimeout(int taskTimeout) {
        this.taskTimeout = taskTimeout;
    }

    public int getCompileErrorsAttempts() {
        return compileErrorsAttempts;
    }

    public void setCompileErrorsAttempts(int compileErrorsAttempts) {
        this.compileErrorsAttempts = compileErrorsAttempts;
    }

    public boolean isIgnoreVisibility() {
        return ignoreVisibility;
    }

    public void setIgnoreVisibility(boolean ignoreVisibility) {
        this.ignoreVisibility = ignoreVisibility;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public boolean isGenerateJUnitTests() {
        return generateJUnitTests;
    }

    public void setGenerateJUnitTests(boolean generateJUnitTests) {
        this.generateJUnitTests = generateJUnitTests;
    }
}
