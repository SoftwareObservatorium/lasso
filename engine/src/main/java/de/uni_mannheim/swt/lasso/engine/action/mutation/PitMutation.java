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

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 *
 * @author Marcus Kessel
 */
public class PitMutation extends LassoReport {

    @QuerySqlField(index = false)
    private boolean detected;
    @QuerySqlField(index = false)
    private String status;
    @QuerySqlField(index = false)
    private int numberOfTestRuns;

    @QuerySqlField(index = false)
    private String sourceFile;
    @QuerySqlField(index = false)
    private String mutatedClass;
    @QuerySqlField(index = false)
    private String mutatedMethod;
    @QuerySqlField(index = false)
    private String methodDescription;
    @QuerySqlField(index = false)
    private int lineNumber;
    @QuerySqlField(index = false)
    private String mutator;
    @QuerySqlField(index = false)
    private int index;
    @QuerySqlField(index = false)
    private int block;
    @QuerySqlField(index = false)
    private String killingTests;
    @QuerySqlField(index = false)
    private int noOfKillingTests;
    @QuerySqlField(index = false)
    private String succeedingTests;
    @QuerySqlField(index = false)
    private int noOfSucceedingTests;
    @QuerySqlField(index = false)
    private String description;

    public boolean isDetected() {
        return detected;
    }

    public void setDetected(boolean detected) {
        this.detected = detected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNumberOfTestRuns() {
        return numberOfTestRuns;
    }

    public void setNumberOfTestRuns(int numberOfTestRuns) {
        this.numberOfTestRuns = numberOfTestRuns;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getMutatedClass() {
        return mutatedClass;
    }

    public void setMutatedClass(String mutatedClass) {
        this.mutatedClass = mutatedClass;
    }

    public String getMutatedMethod() {
        return mutatedMethod;
    }

    public void setMutatedMethod(String mutatedMethod) {
        this.mutatedMethod = mutatedMethod;
    }

    public String getMethodDescription() {
        return methodDescription;
    }

    public void setMethodDescription(String methodDescription) {
        this.methodDescription = methodDescription;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMutator() {
        return mutator;
    }

    public void setMutator(String mutator) {
        this.mutator = mutator;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getBlock() {
        return block;
    }

    public void setBlock(int block) {
        this.block = block;
    }

    public String getKillingTests() {
        return killingTests;
    }

    public void setKillingTests(String killingTests) {
        this.killingTests = killingTests;
    }

    public int getNoOfKillingTests() {
        return noOfKillingTests;
    }

    public void setNoOfKillingTests(int noOfKillingTests) {
        this.noOfKillingTests = noOfKillingTests;
    }

    public String getSucceedingTests() {
        return succeedingTests;
    }

    public void setSucceedingTests(String succeedingTests) {
        this.succeedingTests = succeedingTests;
    }

    public int getNoOfSucceedingTests() {
        return noOfSucceedingTests;
    }

    public void setNoOfSucceedingTests(int noOfSucceedingTests) {
        this.noOfSucceedingTests = noOfSucceedingTests;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
