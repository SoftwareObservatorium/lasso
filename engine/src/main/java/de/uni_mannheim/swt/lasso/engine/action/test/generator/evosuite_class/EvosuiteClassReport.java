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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.evosuite_class;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
@Deprecated
public class EvosuiteClassReport extends LassoReport {

    private String extractedTestClassSource;

    private List<String> testClassFiles;

    @QuerySqlField(index = false)
    private String testClasses;

    @QuerySqlField(index = false)
    private String TARGET_CLASS;
    @QuerySqlField(index = false)
    private String criterion;
    @QuerySqlField(index = false)
    private double Coverage;
    @QuerySqlField(index = false)
    private double Total_Goals;
    @QuerySqlField(index = false)
    private double Covered_Goals;
    @QuerySqlField(index = false)
    private double Classpath_Classes;
    @QuerySqlField(index = false)
    private double Analyzed_Classes;
    @QuerySqlField(index = false)
    private double Total_Branches;
    @QuerySqlField(index = false)
    private double Covered_Branches;
    @QuerySqlField(index = false)
    private double Lines;
    @QuerySqlField(index = false)
    private double Covered_Lines;
    @QuerySqlField(index = false)
    private double Total_Methods;
    @QuerySqlField(index = false)
    private double Covered_Methods;
    @QuerySqlField(index = false)
    private double Branchless_Methods;
    @QuerySqlField(index = false)
    private double Mutants;
    @QuerySqlField(index = false)
    private double Statements_Executed;
    @QuerySqlField(index = false)
    private double Tests_Executed;
    @QuerySqlField(index = false)
    private double Size;
    @QuerySqlField(index = false)
    private double Length;
    @QuerySqlField(index = false)
    private double Total_Time;

    @QuerySqlField(index = false)
    private int filteredSize;

    public String getTARGET_CLASS() {
        return TARGET_CLASS;
    }

    public void setTARGET_CLASS(String TARGET_CLASS) {
        this.TARGET_CLASS = TARGET_CLASS;
    }

    public String getCriterion() {
        return criterion;
    }

    public void setCriterion(String criterion) {
        this.criterion = criterion;
    }

    public double getCoverage() {
        return Coverage;
    }

    public void setCoverage(double coverage) {
        Coverage = coverage;
    }

    public double getTotal_Goals() {
        return Total_Goals;
    }

    public void setTotal_Goals(double total_Goals) {
        Total_Goals = total_Goals;
    }

    public double getCovered_Goals() {
        return Covered_Goals;
    }

    public void setCovered_Goals(double covered_Goals) {
        Covered_Goals = covered_Goals;
    }

    public double getClasspath_Classes() {
        return Classpath_Classes;
    }

    public void setClasspath_Classes(double classpath_Classes) {
        Classpath_Classes = classpath_Classes;
    }

    public double getAnalyzed_Classes() {
        return Analyzed_Classes;
    }

    public void setAnalyzed_Classes(double analyzed_Classes) {
        Analyzed_Classes = analyzed_Classes;
    }

    public double getTotal_Branches() {
        return Total_Branches;
    }

    public void setTotal_Branches(double total_Branches) {
        Total_Branches = total_Branches;
    }

    public double getCovered_Branches() {
        return Covered_Branches;
    }

    public void setCovered_Branches(double covered_Branches) {
        Covered_Branches = covered_Branches;
    }

    public double getLines() {
        return Lines;
    }

    public void setLines(double lines) {
        Lines = lines;
    }

    public double getCovered_Lines() {
        return Covered_Lines;
    }

    public void setCovered_Lines(double covered_Lines) {
        Covered_Lines = covered_Lines;
    }

    public double getTotal_Methods() {
        return Total_Methods;
    }

    public void setTotal_Methods(double total_Methods) {
        Total_Methods = total_Methods;
    }

    public double getCovered_Methods() {
        return Covered_Methods;
    }

    public void setCovered_Methods(double covered_Methods) {
        Covered_Methods = covered_Methods;
    }

    public double getBranchless_Methods() {
        return Branchless_Methods;
    }

    public void setBranchless_Methods(double branchless_Methods) {
        Branchless_Methods = branchless_Methods;
    }

    public double getMutants() {
        return Mutants;
    }

    public void setMutants(double mutants) {
        Mutants = mutants;
    }

    public double getStatements_Executed() {
        return Statements_Executed;
    }

    public void setStatements_Executed(double statements_Executed) {
        Statements_Executed = statements_Executed;
    }

    public double getTests_Executed() {
        return Tests_Executed;
    }

    public void setTests_Executed(double tests_Executed) {
        Tests_Executed = tests_Executed;
    }

    public double getSize() {
        return Size;
    }

    public void setSize(double size) {
        Size = size;
    }

    public double getLength() {
        return Length;
    }

    public void setLength(double length) {
        Length = length;
    }

    public double getTotal_Time() {
        return Total_Time;
    }

    public void setTotal_Time(double total_Time) {
        Total_Time = total_Time;
    }

    public String getTestClasses() {
        return testClasses;
    }

    public void setTestClasses(String testClasses) {
        this.testClasses = testClasses;
    }

    public List<String> getTestClassFiles() {
        return testClassFiles;
    }

    public void setTestClassFiles(List<String> testClassFiles) {
        this.testClassFiles = testClassFiles;
    }

    public String getExtractedTestClassSource() {
        return extractedTestClassSource;
    }

    public void setExtractedTestClassSource(String extractedTestClassSource) {
        this.extractedTestClassSource = extractedTestClassSource;
    }

    public int getFilteredSize() {
        return filteredSize;
    }

    public void setFilteredSize(int filteredSize) {
        this.filteredSize = filteredSize;
    }
}
