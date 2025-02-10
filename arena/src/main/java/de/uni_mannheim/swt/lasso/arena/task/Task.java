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

import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Minimize sequences (i.e drop duplicates).
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
