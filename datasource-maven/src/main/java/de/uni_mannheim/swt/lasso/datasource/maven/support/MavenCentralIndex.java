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
package de.uni_mannheim.swt.lasso.datasource.maven.support;

import de.uni_mannheim.swt.lasso.index.CandidateQueryResult;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.collect.CandidateResultCollector;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.CandidateQuery;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository interface for Solr Index.
 *
 * @author Marcus Kessel
 */
public class MavenCentralIndex {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenCentralIndex.class);

    private final RandomMavenCentralRepository mavenCentralRepository;

    private final QueryBuilder candidateQueryBuilder;

    public MavenCentralIndex(RandomMavenCentralRepository mavenCentralRepository, QueryBuilder candidateQueryBuilder) {
        this.mavenCentralRepository = mavenCentralRepository;
        this.candidateQueryBuilder = candidateQueryBuilder;
    }

    protected CandidateQueryResult doQuery(CandidateQuery query, int start, int rows, SearchOptions searchOptions, List<SolrQuery.SortClause> orderByClauses) throws IOException {
        CandidateQueryResult result = new CandidateQueryResult();
        StopWatch queryStopWatch = new StopWatch();
        queryStopWatch.start();
        CandidateResultCollector collector = this.mavenCentralRepository.queryForClassCandidates(query, start, rows, false, orderByClauses, searchOptions.getExpandedRows());
        collector.setRowSizeOverflow(searchOptions.getRowSizeOverflow());
        List<CandidateDocument> candidates = collector.collect(searchOptions.getCursorSize(), rows, searchOptions.getCandidateFilter(), searchOptions.getDocumentHandler(), searchOptions.getSortHandler());
        result.setTotal(collector.getTotal());
        queryStopWatch.stop();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Candidates Query execution time for " + rows + " rows (ms) => " + queryStopWatch.getTime());
        }

        result.setCandidates(candidates);
        result.setQuery(query);

        return result;
    }

    protected CandidateQueryResult doRandomQuery(CandidateQuery query, int start, int rows, SearchOptions searchOptions, List<SolrQuery.SortClause> orderByClauses) throws IOException {
        CandidateQueryResult result = new CandidateQueryResult();
        StopWatch queryStopWatch = new StopWatch();
        queryStopWatch.start();
        CandidateResultCollector collector = this.mavenCentralRepository.queryForRandomClassCandidates(query, start, rows, false, orderByClauses, searchOptions.getExpandedRows());
        collector.setRowSizeOverflow(searchOptions.getRowSizeOverflow());
        List<CandidateDocument> candidates = collector.collect(searchOptions.getCursorSize(), rows, searchOptions.getCandidateFilter(), searchOptions.getDocumentHandler(), searchOptions.getSortHandler());
        result.setTotal(collector.getTotal());
        queryStopWatch.stop();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Candidates Query execution time for " + rows + " rows (ms) => " + queryStopWatch.getTime());
        }

        result.setCandidates(candidates);
        result.setQuery(query);
        return result;
    }

    public CandidateQueryResult query(String queryInput, SearchOptions searchOptions,
                                      String[] constraints, int start, int rows, List<SolrQuery.SortClause> orderByClauses) throws IOException {
        // do query
        CandidateQuery candidateQuery = this.candidateQueryBuilder.build(queryInput, new ArrayList<>(Arrays.asList(constraints)), searchOptions);
        CandidateQueryResult result = this.doQuery(candidateQuery, start, rows, searchOptions, orderByClauses);
        result.setTestClassSource(candidateQuery.getTestClassSource());
        result.setTestSubjectName(candidateQuery.getTestSubjectName());
        result.setTestClassName(candidateQuery.getTestClassName());
        result.setMethods(candidateQuery.getMethods());
        result.setTestable(candidateQuery.isTestable());

        // always set is testable from now on
        result.setTestable(true);
        result.setQuery(candidateQuery);

        return result;
    }

    public CandidateQueryResult queryDirectly(String queryInput, SearchOptions searchOptions,
                                      String[] constraints, int start, int rows, List<SolrQuery.SortClause> orderByClauses) throws IOException {
        // do query
        CandidateQuery candidateQuery = this.candidateQueryBuilder.build(queryInput, new ArrayList<>(Arrays.asList(constraints)), searchOptions);

        CandidateQueryResult result = new CandidateQueryResult();
        StopWatch queryStopWatch = new StopWatch();
        queryStopWatch.start();
        SolrDocumentList solrDocumentList = this.mavenCentralRepository.queryForClassCandidatesWithoutCursor(candidateQuery, start, rows, false, orderByClauses, searchOptions.getExpandedRows());

        queryStopWatch.stop();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Candidates Query execution time for " + rows + " rows (ms) => " + queryStopWatch.getTime());
        }

        result.setCandidates(solrDocumentList.stream()
                .map(SolrCandidateDocument::new).collect(Collectors.toList()));
        result.setTotal(solrDocumentList.getNumFound());

        result.setTestClassSource(candidateQuery.getTestClassSource());
        result.setTestSubjectName(candidateQuery.getTestSubjectName());
        result.setTestClassName(candidateQuery.getTestClassName());
        result.setMethods(candidateQuery.getMethods());
        result.setTestable(candidateQuery.isTestable());
        result.setQuery(candidateQuery);

        // always set is testable from now on
        result.setTestable(true);

        return result;
    }

    public CandidateQueryResult randomQuery(String queryInput, SearchOptions searchOptions,
                                            String[] constraints, int start, int rows, List<SolrQuery.SortClause> orderByClauses) throws IOException {
        // do query
        CandidateQuery candidateQuery = this.candidateQueryBuilder.build(queryInput, new ArrayList<>(Arrays.asList(constraints)), searchOptions);
        CandidateQueryResult result = this.doRandomQuery(candidateQuery, start, rows, searchOptions, orderByClauses);
        result.setTestClassSource(candidateQuery.getTestClassSource());
        result.setTestSubjectName(candidateQuery.getTestSubjectName());
        result.setTestClassName(candidateQuery.getTestClassName());
        result.setMethods(candidateQuery.getMethods());
        result.setTestable(candidateQuery.isTestable());
        result.setQuery(candidateQuery);

        // always set is testable from now on
        result.setTestable(true);

        return result;
    }

    public RandomMavenCentralRepository getMavenCentralRepository() {
        return mavenCentralRepository;
    }

    public QueryBuilder getCandidateQueryBuilder() {
        return candidateQueryBuilder;
    }
}
