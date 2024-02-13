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
package de.uni_mannheim.swt.lasso.index;

import de.uni_mannheim.swt.lasso.index.query.lql.builder.CandidateQuery;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;

import java.util.List;

/**
 * A candidate index query result containing {@link CandidateDocument}s.
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateQueryResult {

    private List<CandidateDocument> candidates;

    private List<String> methods;

    private String testSubjectName;
    private String testClassName;
    private String testClassSource;

    private List<String> constraints;

    private boolean testable;
    
    private long total;

    private CandidateQuery query;

    /**
     * @return the testSubjectName
     */
    public String getTestSubjectName() {
        return testSubjectName;
    }

    /**
     * @param testSubjectName
     *            the testSubjectName to set
     */
    public void setTestSubjectName(String testSubjectName) {
        this.testSubjectName = testSubjectName;
    }

    /**
     * @return the testClassSource
     */
    public String getTestClassSource() {
        return testClassSource;
    }

    /**
     * @param testClassSource
     *            the testClassSource to set
     */
    public void setTestClassSource(String testClassSource) {
        this.testClassSource = testClassSource;
    }

    /**
     * @return the constraints
     */
    public List<String> getConstraints() {
        return constraints;
    }

    /**
     * @param constraints
     *            the constraints to set
     */
    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    /**
     * @return the methods
     */
    public List<String> getMethods() {
        return methods;
    }

    /**
     * @param methods
     *            the methods to set
     */
    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    /**
     * @return the testClassName
     */
    public String getTestClassName() {
        return testClassName;
    }

    /**
     * @param testClassName
     *            the testClassName to set
     */
    public void setTestClassName(String testClassName) {
        this.testClassName = testClassName;
    }

    /**
     * @return true if given candidate query supports test-driven search
     */
    public boolean isTestable() {
        return testable;
    }

    /**
     * @param testable
     *            the testable to set
     */
    public void setTestable(boolean testable) {
        this.testable = testable;
    }

    /**
     * @return the rows
     */
    public int getRows() {
        return candidates != null ? candidates.size() : 0;
    }

    public List<CandidateDocument> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<CandidateDocument> candidates) {
        this.candidates = candidates;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public CandidateQuery getQuery() {
        return query;
    }

    public void setQuery(CandidateQuery query) {
        this.query = query;
    }
}
