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
package de.uni_mannheim.swt.lasso.engine.action.test.minimize;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 *
 * @author Marcus Kessel
 */
@Deprecated
public class TestReport extends LassoReport {

    @QuerySqlField(index = false)
    private int tests;
    @QuerySqlField(index = false)
    private int errors;
    @QuerySqlField(index = false)
    private int failures;
    @QuerySqlField(index = false)
    private int skipped;
    @QuerySqlField(index = false)
    private double testTime;

    public int getTests() {
        return tests;
    }

    public void setTests(int tests) {
        this.tests = tests;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public double getTestTime() {
        return testTime;
    }

    public void setTestTime(double testTime) {
        this.testTime = testTime;
    }
}
