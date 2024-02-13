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
package de.uni_mannheim.swt.lasso.index.query.lql.builder;

import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;

import java.util.List;

/**
 * A candidate query
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateQuery {

    private Query luceneQuery;
    private String testClassSource;
    private List<String> methods;
    private String testSubjectName;
    private String testClassName;

    private List<String> constraints;

    private LQLLuceneStrategy lqlQuery;

    /**
     * @return the luceneQuery
     */
    public Query getLuceneQuery() {
        return luceneQuery;
    }

    /**
     * @param luceneQuery
     *            the luceneQuery to set
     */
    public void setLuceneQuery(Query luceneQuery) {
        this.luceneQuery = luceneQuery;
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
     * @return true if given query supports test-driven search (simple check if
     *         test class source exists)
     */
    public boolean isTestable() {
        return StringUtils.isNotBlank(getTestClassSource());
    }

    public LQLLuceneStrategy getLqlQuery() {
        return lqlQuery;
    }

    public void setLqlQuery(LQLLuceneStrategy lqlQuery) {
        this.lqlQuery = lqlQuery;
    }
}
