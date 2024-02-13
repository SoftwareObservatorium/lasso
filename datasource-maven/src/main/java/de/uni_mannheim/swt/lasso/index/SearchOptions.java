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

import de.uni_mannheim.swt.lasso.index.collect.CandidateResultCollector;
import de.uni_mannheim.swt.lasso.index.filter.CandidateFilter;
import de.uni_mannheim.swt.lasso.index.query.lql.old.LQL2LuceneClassQuery;
import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;

import java.util.Comparator;

/**
 * Search options
 *
 * @author Marcus Kessel
 */
public class SearchOptions {

    private int expandedRows = 5;

    private boolean fullyQualified = true;

    private String keywordsAndConstraints;

    @Deprecated
    private CandidateFilter candidateFilter;
    @Deprecated
    private CandidateResultCollector.DocumentHandler documentHandler;
    @Deprecated
    private Comparator<CandidateDocument> sortHandler;

    private int cursorSize = 1000;

    /**
     * Fetch 30% more than required (this is a good option to increase efficiency in post filters)
     */
    private double rowSizeOverflow = 1.3;

    /**
     * Include {@link CandidateDocument#getInheritedMethods()} for search ?
     */
    private boolean includeInheritedMethods;

    private boolean experimentalQueryExpansion;
    private boolean experimentalClustering;

    // new
    private int expandMethodNames = 5;

    private Class<? extends LQLLuceneStrategy> strategy = LQL2LuceneClassQuery.class;

    public CandidateFilter getCandidateFilter() {
        return candidateFilter;
    }

    public void setCandidateFilter(CandidateFilter candidateFilter) {
        this.candidateFilter = candidateFilter;
    }

    public CandidateResultCollector.DocumentHandler getDocumentHandler() {
        return documentHandler;
    }

    public void setDocumentHandler(CandidateResultCollector.DocumentHandler documentHandler) {
        this.documentHandler = documentHandler;
    }

    public boolean isExperimentalQueryExpansion() {
        return experimentalQueryExpansion;
    }

    public void setExperimentalQueryExpansion(boolean experimentalQueryExpansion) {
        this.experimentalQueryExpansion = experimentalQueryExpansion;
    }

    public boolean isExperimentalClustering() {
        return experimentalClustering;
    }

    public void setExperimentalClustering(boolean experimentalClustering) {
        this.experimentalClustering = experimentalClustering;
    }

    public Comparator<CandidateDocument> getSortHandler() {
        return sortHandler;
    }

    public void setSortHandler(Comparator<CandidateDocument> sortHandler) {
        this.sortHandler = sortHandler;
    }

    public int getCursorSize() {
        return cursorSize;
    }

    public void setCursorSize(int cursorSize) {
        this.cursorSize = cursorSize;
    }

    public double getRowSizeOverflow() {
        return rowSizeOverflow;
    }

    public void setRowSizeOverflow(double rowSizeOverflow) {
        this.rowSizeOverflow = rowSizeOverflow;
    }

    public boolean isFullyQualified() {
        return fullyQualified;
    }

    public void setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
    }

    public String getKeywordsAndConstraints() {
        return keywordsAndConstraints;
    }

    public void setKeywordsAndConstraints(String keywordsAndConstraints) {
        this.keywordsAndConstraints = keywordsAndConstraints;
    }

    public int getExpandedRows() {
        return expandedRows;
    }

    public void setExpandedRows(int expandedRows) {
        this.expandedRows = expandedRows;
    }

    public boolean isIncludeInheritedMethods() {
        return includeInheritedMethods;
    }

    public void setIncludeInheritedMethods(boolean includeInheritedMethods) {
        this.includeInheritedMethods = includeInheritedMethods;
    }

    public Class<? extends LQLLuceneStrategy> getStrategy() {
        return strategy;
    }

    public void setStrategy(Class<? extends LQLLuceneStrategy> strategy) {
        this.strategy = strategy;
    }

    public int getExpandMethodNames() {
        return expandMethodNames;
    }

    public void setExpandMethodNames(int expandMethodNames) {
        this.expandMethodNames = expandMethodNames;
    }
}
