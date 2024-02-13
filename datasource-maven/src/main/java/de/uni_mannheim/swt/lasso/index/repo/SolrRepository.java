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
package de.uni_mannheim.swt.lasso.index.repo;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.search.Query;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Solr-based respository interface
 * 
 * @author Marcus Kessel
 *
 */
public abstract class SolrRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SolrRepository.class);

    protected final SolrClient solrClient;

    /**
     * Constructor
     * 
     * @param solrClient
     *            {@link SolrClient} instance
     */
    public SolrRepository(SolrClient solrClient) {
        Validate.notNull(solrClient, "SolrClient cannot be null");
        this.solrClient = solrClient;
    }

    /**
     * Query for specific field (single value)
     * 
     * @param query
     *            Solr Query
     * @param field
     *            SolR field
     * @return {@link Object} value instance
     * @throws IOException
     *             Query failed
     */
    protected Object queryForField(String query, String field) throws IOException {
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields(field);

            // execute query
            SolrDocumentList solrDocList = solrClient.query(solrQuery, SolrRequest.METHOD.POST).getResults();

            if (solrDocList.getNumFound() > 1) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Found more than one document, although only single document expected for field " + field
                            + " and query " + query);
                }
            }

            return solrDocList.get(0).getFirstValue(field);
        } catch (Throwable e) {
            throw new IOException("Could not execute field query for field " + field, e);
        }
    }

    /**
     * Query for specific field (single value)
     * 
     * @param query
     *            Solr Query
     * @param field
     *            SolR field
     * @return {@link Collection} of {@link Object} values
     * @throws IOException
     *             Query failed
     */
    protected Collection<Object> queryForFieldValues(String query, String field) throws IOException {
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields(field);

            // execute query
            SolrDocumentList solrDocList = solrClient.query(solrQuery, SolrRequest.METHOD.POST).getResults();

            if (solrDocList.getNumFound() > 1) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Found more than one document, although only single document expected for field " + field
                            + " and query " + query);
                }
            }

            return solrDocList.get(0).getFieldValues(field);
        } catch (Throwable e) {
            throw new IOException("Could not execute field query for field " + field, e);
        }
    }

    /**
     * Build {@link SolrQuery} and escape necessary chars from Lucene
     * {@link Query}.
     * 
     * @param query
     *            Lucene query
     * @param projectedFields
     *            Projected fields
     * @return {@link SolrQuery} instance
     */
    protected SolrQuery buildSolrQuery(Query query, String[] projectedFields) {
        String queryStr = escapeQuery(query.toString());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Escaped Lucene query to \n" + queryStr);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryStr);
        if (ArrayUtils.isNotEmpty(projectedFields)) {
            solrQuery.setFields(projectedFields);
        }

        return solrQuery;
    }

    protected String escapeQuery(String str) {
        // only escape non-ranges! e.g. [4 TO 5] is not escaped!
        return StringUtils.replaceEach(str, new String[] { "[]" },
                new String[] { "\\[\\]" });
    }

    protected List<String> escapeQuery(List<String> queries) {
        return queries.stream().map(this::escapeQuery).collect(Collectors.toList());
    }

    protected String[] escapeQuery(String[] queries) {
        return Arrays.stream(queries).map(this::escapeQuery).toArray(String[]::new);
    }

    /**
     * Utility method that checks for null input before insertion
     * 
     * @param document
     *            {@link SolrInputDocument} instance
     * @param name
     *            Filed name
     * @param value
     *            Field value
     */
    protected static void addIfExists(SolrInputDocument document, String name, Object value) {
        if (value != null) {
            document.addField(name, value);
        }
    }

    /**
     * @return the solrClient
     */
    public SolrClient getSolrClient() {
        return solrClient;
    }
}
