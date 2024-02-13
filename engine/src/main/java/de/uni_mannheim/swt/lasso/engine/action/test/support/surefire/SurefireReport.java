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
package de.uni_mannheim.swt.lasso.engine.action.test.support.surefire;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Surefire record.
 * 
 * @author Marcus Kessel
 *
 */
public class SurefireReport implements Serializable {

    private String filename;

    private String name;
    private double time;
    private int tests;
    private int failures;

    private int errors;

    private int skipped;

    private Map<String, List<SurefireTestCase>> testCases = new LinkedHashMap<>();

    private boolean isStoppedByUserException;

    public void addTestCase(SurefireTestCase testCase) {
        if(!testCases.containsKey(testCase.getClassName())) {
            testCases.put(testCase.getClassName(), new LinkedList<>());
        }

        testCases.get(testCase.getClassName()).add(testCase);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public int getTests() {
        return tests;
    }

    public void setTests(int tests) {
        this.tests = tests;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public Map<String, List<SurefireTestCase>> getTestCases() {
        return testCases;
    }

    public boolean isStoppedByUserException() {
        return isStoppedByUserException;
    }

    public void setStoppedByUserException(boolean stoppedByUserException) {
        isStoppedByUserException = stoppedByUserException;
    }
}
