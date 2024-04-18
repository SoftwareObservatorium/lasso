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
package de.uni_mannheim.swt.lasso.llm.eval;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class Results {

    private String language;
    private String name;

    /*
                System.out.println(nextRecord.get("experiment"));
            System.out.println(nextRecord.get("problem"));
            System.out.println(nextRecord.get("language"));
            System.out.println(nextRecord.get("top_p"));
            System.out.println(nextRecord.get("max_tokens"));
            System.out.println(nextRecord.get("prompt"));
            System.out.println(nextRecord.get("tests"));
     */
    private double top_p;
    private long max_tokens;
    private String tests;
    private String experiment;
    private String problem;
    private String prompt;

    private List<ExecutedSolution> results;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ExecutedSolution> getResults() {
        return results;
    }

    public void setResults(List<ExecutedSolution> results) {
        this.results = results;
    }

    public double getTop_p() {
        return top_p;
    }

    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    public long getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(long max_tokens) {
        this.max_tokens = max_tokens;
    }

    public String getTests() {
        return tests;
    }

    public void setTests(String tests) {
        this.tests = tests;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
