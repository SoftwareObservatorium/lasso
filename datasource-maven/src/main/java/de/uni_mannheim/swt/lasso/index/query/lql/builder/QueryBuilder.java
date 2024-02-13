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

import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy;
import de.uni_mannheim.swt.lasso.lql.parser.LQL;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query builder for queries.
 * 
 * @author Marcus Kessel
 *
 */
public class QueryBuilder {

    private static final Logger LOG = LoggerFactory
            .getLogger(QueryBuilder.class);

    /**
     *
     * @param queryInput
     * @param constraints
     * @param searchOptions
     * @return
     * @throws IOException
     */
    public CandidateQuery build(String queryInput, List<String> constraints, SearchOptions searchOptions)
            throws IOException {
        try {
            // query
            CandidateQuery candidateQuery = new CandidateQuery();

            // handle constraints
            candidateQuery.setConstraints(constraints);

            // query
            LQLLuceneStrategy mQuery = createIDCSQuery(queryInput, searchOptions, candidateQuery);

            // empty
            if (mQuery.isEmpty()) {
                // nothing
                throw new IOException("Query is empty for " + queryInput);
            }

            // is LQL?
            if (mQuery.isLQL()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Found LQL Lucene Query | " + queryInput);
                }

                if(mQuery.getLQLParseResult() != null
                        && mQuery.getLQLParseResult().getInterfaceSpecification() != null) {
//                    if(mQuery.getMqlQueryComponent().getClasses().size() > 1) {
//                        if(LOG.isWarnEnabled()) {
//                            LOG.warn("Found more than one LQL class! Assuming first class as CUT");
//                        }
//                    }
                }
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Found Keyword/Constraint Lucene Query | "
                            + queryInput);
                }
            }

            // currently only one search query
            Query luceneQuery = mQuery.getLuceneQuery();
            candidateQuery.setLuceneQuery(luceneQuery);
            candidateQuery.setLqlQuery(mQuery);

            if (LOG.isInfoEnabled()) {
                LOG.info("Constructed Lucene Query: " + luceneQuery.toString());

                if(CollectionUtils.isNotEmpty(candidateQuery.getConstraints())) {
                    LOG.info("Using Filter queries: "
                            + candidateQuery.getConstraints().stream().collect(Collectors.joining(",")));
                }
            }

            return candidateQuery;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not create result query for " + queryInput, e);
            }

            throw new IOException(
                    "Could not create result query for " + queryInput, e);
        }
    }

    /**
     * Parse query
     *
     * @param queryStr       Query String
     * @param searchOptions
     * @param candidateQuery
     * @return {@link LQLLuceneStrategy} instance
     */
    protected LQLLuceneStrategy createIDCSQuery(String queryStr, SearchOptions searchOptions, CandidateQuery candidateQuery) {
        //
        LQLLuceneStrategy query = createMQLLuceneStategy(searchOptions);

        LQLParseResult parseResult = LQL.parse(queryStr);
//        if(parseResult.hasSystem()) {
//            query.setLQLParseResult(parseResult);
//        }
        query.setLQLParseResult(parseResult);

        // special handling of "*:*" -> classic query, others filter queries
        if(CollectionUtils.isNotEmpty(parseResult.getFilters())) {
            List<String> filters = new LinkedList<>();
            for(String filter : parseResult.getFilters()) {
                if(StringUtils.equals("*:*", filter)) {
                    query.setKeywordsAndConstraints("*:*");
                } else {
                    filters.add(filter);
                }
            }

            if(candidateQuery.getConstraints() != null) {
                candidateQuery.getConstraints().addAll(filters);
            } else {
                candidateQuery.setConstraints(filters);
            }
        }

        return query;
    }

    /**
     * Create corresponding {@link LQLLuceneStrategy}
     *
     * @param searchOptions
     * @return
     */
    public LQLLuceneStrategy createMQLLuceneStategy(SearchOptions searchOptions) {
        Class<? extends LQLLuceneStrategy> mqlLuceneStrategyClass = searchOptions.getStrategy();

        if(mqlLuceneStrategyClass == null) {
            throw new IllegalArgumentException("MQLLuceneStrategy not found => " + searchOptions.getStrategy());
        }

        try {
            LQLLuceneStrategy instance = mqlLuceneStrategyClass.getConstructor(SearchOptions.class).newInstance(searchOptions);

            // set options
            instance.setFullyQualified(searchOptions.isFullyQualified());

            return instance;
        } catch (Throwable e) {
            throw new RuntimeException("Cannot instantiate MQLLuceneStrategy => " + searchOptions.getStrategy(), e);
        }
    }
}
