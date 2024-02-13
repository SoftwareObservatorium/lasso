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

import de.uni_mannheim.swt.lasso.index.collect.CandidateResultCollector;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.CandidateQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Repository manager.
 * 
 * @author Marcus Kessel
 *
 */
public class MavenCentralRepository extends SolrRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MavenCentralRepository.class);

    public static final String PREFIX = "m_";

    /**
     * {@link SortClause}s used for querying
     */
    private static final List<SortClause> SORT_CLAUSES = Arrays.<SortClause>asList(SortClause.desc("score"),
            SortClause.desc("id"));

    /**
     * Fields which get projected in {@link SolrQuery} excluding source code
     */
    //private static final String[] PROJECTED_FIELDS = new String[] { "score", "*" };
    //private static final String[] PROJECTED_FIELDS = new String[] { "score","id","name_sexact","packagename_sexact","groupId","artifactId","version",/*"content",*/"hash","m_*","content", "methodSignatureParamsOrderedSyntaxFq_ssig", "doctype_s", "type" , "methodOrigSignatureFq_ssigs_sexact", "pid_s", "bytecodename_s", "methodSignatureParamsOrderedKeywordsFq_sigs" };
    public static final String[] PROJECTED_FIELDS = new String[] {
            "score",
//
            "id",
            "name_sexact",
            "packagename_sexact",
            "groupId",
            "artifactId",
            "version",
            "score",
            "content",
            "hash",
            "doctype_s",
            "type",
            "uri",
            "bytecodename_s",
//
            "methodOrigSignatureFq_sigs_exact",
            "methodOrigSignatureFq_ssigs_sexact",
            "superclass_exact",
            "interface_exact",
            "dep_exact",
            //-- FIXME add again "inheritedmethodOrigSignature_sigs_exact",
            "bytecodename_s",
//
            "methodSignatureParamsOrderedKeywordsFq_sigs",
            //
            "methodSignatureParamsOrderedKeywordsFq_ssig",
            //
            "method_fqs",
            "method_fq",
            "m_*",
            "meta_*",
            "classifier_s",
            // owner of methods
            "pid_s",
            // Henrik: mcall
            "mcall_exact",
            "owner"
    };


    /**
     * Constructor
     *
     * @param solrClient {@link SolrClient} instance
     */
    public MavenCentralRepository(SolrClient solrClient) {
        super(solrClient);
    }

    /**
     * Query for {@link CandidateDocument}s using Solr's cursor capability to
     * dynamically fetch next slices.
     *
     * @param query
     *            Lucene query
     * @param start
     *            Start slice at position
     * @param rows
     *            Max. no. of rows to fetch in each page iteration
     * @return {@link CandidateResultCollector} instance
     * @throws IOException
     *             Query failed
     */
    public CandidateResultCollector queryForClassCandidates(Query query, String[] fq, int start, int rows, boolean highlighting)
            throws IOException {
        // build solr query
        SolrQuery solrQuery = buildSolrQuery(query, null);
        solrQuery.setFilterQueries(escapeQuery(fq));
        // start
        solrQuery.setStart(start);

        // needed unique ID needed for cursor functionality
        solrQuery.setSorts(SORT_CLAUSES);
        solrQuery.setFields(PROJECTED_FIELDS);

        // enable highlighting
        if(highlighting) {
            // hl:on
            solrQuery.setHighlight(true);
            solrQuery.set("hl.fl", "methodSignatureParamsOrderedSyntaxKeywordsFq_sigs,methodSignatureParamsOrderedKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedSyntaxKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedKeywordsFq_sigs");
        }

        // expand?
        expand(solrQuery);

        return new CandidateResultCollector(solrClient, solrQuery);
    }

    /**
     * Automatically expand query if collapse field is defined
     *
     * @param solrQuery
     */
    protected void expand(SolrQuery solrQuery) {
        // default expanded rows of 5
        expand(solrQuery, 5);
    }

    /**
     * Automatically expand query if collapse field is defined
     *
     * @param solrQuery
     * @param expandRows
     */
    protected void expand(SolrQuery solrQuery, int expandRows) {
        //
        if(solrQuery.getFilterQueries() == null) {
            return;
        }

        Optional<String> collapseField = Arrays.stream(solrQuery.getFilterQueries())
                .filter(filter -> StringUtils.startsWith(filter.trim(), "{!collapse field="))
                .findAny();

        if(collapseField.isPresent()) {
            solrQuery.setParam("expand", "true");
            // TODO set expand rows
            solrQuery.setParam("expand.rows", String.valueOf(expandRows));
        }
    }

    /**
     * Query for {@link CandidateDocument}s using Solr's cursor capability to
     * dynamically fetch next slices.
     *
     * @param query
     *            Lucene query
     * @param start
     *            Start slice at position
     * @param rows
     *            Max. no. of rows to fetch in each page iteration
     * @return {@link CandidateResultCollector} instance
     * @throws IOException
     *             Query failed
     */
    public CandidateResultCollector queryForClassCandidates(CandidateQuery query, int start, int rows, boolean highlighting, List<SolrQuery.SortClause> orderByClauses, int expandedRows)
            throws IOException {
        // build solr query
        SolrQuery solrQuery = buildSolrQuery(query.getLuceneQuery(), null);
        if(query.getConstraints() != null) {
            solrQuery.setFilterQueries(escapeQuery(query.getConstraints()).toArray(new String[0]));
        }

        // start
        solrQuery.setStart(start);

        // needed unique ID needed for cursor functionality
        List<SolrQuery.SortClause> sortClauses = new LinkedList<>(SORT_CLAUSES);
        if(CollectionUtils.isNotEmpty(orderByClauses)) {
            sortClauses.addAll(orderByClauses);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Sort clauses: {}", sortClauses);
        }

        solrQuery.setSorts(sortClauses);
        solrQuery.setFields(PROJECTED_FIELDS);

        // enable highlighting
        if(highlighting) {
            // hl:on
            solrQuery.setHighlight(true);
            solrQuery.set("hl.fl", "methodSignatureParamsOrderedSyntaxKeywordsFq_sigs,methodSignatureParamsOrderedKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedSyntaxKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedKeywordsFq_sigs");
        }

        // expand?
        expand(solrQuery, expandedRows);

        return new CandidateResultCollector(solrClient, solrQuery);
    }

    /**
     * Query for {@link CandidateDocument}s using Solr's cursor capability to
     * dynamically fetch next slices.
     *
     * @param query
     *            Lucene query
     * @param start
     *            Start slice at position
     * @param rows
     *            Max. no. of rows to fetch in each page iteration
     * @return {@link CandidateResultCollector} instance
     * @throws IOException
     *             Query failed
     */
    public SolrDocumentList queryForClassCandidatesWithoutCursor(CandidateQuery query, int start, int rows, boolean highlighting, List<SortClause> orderByClauses, int expandedRows)
            throws IOException {
        // build solr query
        SolrQuery solrQuery = buildSolrQuery(query.getLuceneQuery(), null);
        if(query.getConstraints() != null) {
            solrQuery.setFilterQueries(escapeQuery(query.getConstraints()).toArray(new String[0]));
        }

        // start
        solrQuery.setStart(start);
        solrQuery.setRows(rows);

//        // needed unique ID needed for cursor functionality
//        List<SolrQuery.SortClause> sortClauses = new LinkedList<>(SORT_CLAUSES);
//        if(CollectionUtils.isNotEmpty(orderByClauses)) {
//            sortClauses.addAll(orderByClauses);
//        }

//        if(LOG.isDebugEnabled()) {
//            LOG.debug("Sort clauses: {}", sortClauses);
//        }

        if(CollectionUtils.isNotEmpty(orderByClauses)) {
            solrQuery.setSorts(orderByClauses);
        } else {
            solrQuery.setSorts(Collections.singletonList(SortClause.desc("score")));
        }

        solrQuery.setFields(PROJECTED_FIELDS);

        // enable highlighting
        if(highlighting) {
            // hl:on
            solrQuery.setHighlight(true);
            solrQuery.set("hl.fl", "methodSignatureParamsOrderedSyntaxKeywordsFq_sigs,methodSignatureParamsOrderedKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedSyntaxKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedKeywordsFq_sigs");
        }

        // FIXME enable re-ranking
//        boolean reRank = false;
//        if(reRank) {
//            solrQuery.set("rq", "{!rerank reRankQuery=$rqq reRankDocs=100 reRankWeight=1}");
//            solrQuery.set("rqq", "(methodSignatureParamsOrderedKeywordsFq_sigs:\"rv_void;mn_<init>;kw_ps0\"~10 methodSignatureParamsOrderedKeywordsFq_sigs:\"rv_java.lang.object;mn_push;pt_java.lang.object;kw_ps1\"~10 methodSignatureParamsOrderedKeywordsFq_sigs:\"rv_java.lang.object;mn_pop;kw_ps0\"~10 methodSignatureParamsOrderedKeywordsFq_sigs:\"rv_java.lang.object;mn_peek;kw_ps0\"~10 methodSignatureParamsOrderedKeywordsFq_sigs:\"rv_int;mn_size;kw_ps0\"~10)");
//        }

        // expand?
        expand(solrQuery, expandedRows);

        //
        try {
            QueryResponse response = solrClient.query(solrQuery, SolrRequest.METHOD.POST);

            return response.getResults();
        } catch (SolrServerException e) {
            throw new IOException("Could not execute json facet query", e);
        }
    }

    /**
     * JSON facet query
     *
     * @param query
     *            Lucene query
     * @param fields Fields
     * @return {@link CandidateResultCollector} instance
     * @throws IOException
     *             Query failed
     *
     * @see <a href="https://lucene.apache.org/solr/guide/7_2/json-facet-api.html">Solr JSON Facet</a>
     */
    public NamedList<Object> queryStatsUsingJsonFacet(CandidateQuery query, List<String> fields)
            throws IOException {
        // build solr query
        SolrQuery solrQuery = buildSolrQuery(query.getLuceneQuery(), null);
        if(query.getConstraints() != null) {
            solrQuery.setFilterQueries(escapeQuery(query.getConstraints()).toArray(new String[0]));
        }

        solrQuery.setRows(0);

        String facet = "%s_%s : \"%s(%s)\"";

        List<String> stats = Arrays.asList("sum", "avg", "min", "max", "unique", "stddev");

        StringBuilder facetQuery = new StringBuilder();
        for(String field : fields) {
            for(String stat : stats) {
                if(facetQuery.length() > 1) {
                    facetQuery.append(',');
                    facetQuery.append('\n');
                }

                facetQuery.append(String.format(facet, field, stat, stat, field));
            }
        }

        solrQuery.add("json.facet", "{"+facetQuery.toString()+"}");

        try {
            QueryResponse response = solrClient.query(solrQuery, SolrRequest.METHOD.POST);

            return (NamedList<Object>) response.getResponse().get("facets");
        } catch (SolrServerException e) {
            throw new IOException("Could not execute json facet query", e);
        }
    }

    public CandidateResultCollector queryForClassCandidates(CandidateQuery query, int start, int rows, boolean highlighting, int expandedRows) throws IOException {
        return queryForClassCandidates(query, start, rows, highlighting, Collections.emptyList(), expandedRows);
    }

    /**
     * Query for class candidate
     *
     * @param candidateId
     *            Candidate ID
     * @return {@link CandidateDocument}, null if no candidate found
     * @throws IOException
     *             Query failed
     */
    public CandidateDocument queryForClassCandidate(String candidateId) throws IOException {
        try {
            // build solr query
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("+id:" + candidateId);
            solrQuery.setStart(0);
            solrQuery.setRows(1);
            solrQuery.setFields(PROJECTED_FIELDS);

            QueryResponse response = solrClient.query(solrQuery, SolrRequest.METHOD.POST);

            if (response.getResults().size() > 0) {
                return new SolrCandidateDocument(response.getResults().get(0));
            } else {
                return null;
            }
        } catch (Throwable e) {
            throw new IOException("Could not execute candidate query " + candidateId, e);
        }
    }

    /**
     * Query for {@link CandidateDocument}s using Solr's cursor capability to
     * dynamically fetch next slices.
     *
     * @param query
     *            Lucene query
     * @param start
     *            Start slice at position
     * @param rows
     *            Max. no. of rows to fetch in each page iteration
     * @return {@link CandidateResultCollector} instance
     * @throws IOException
     *             Query failed
     */
    public CandidateResultCollector queryForClassCandidates(String query, String[] fq, int start, int rows)
            throws IOException {
        // build solr query
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFilterQueries(escapeQuery(fq));
        // start
        solrQuery.setStart(start);
        // needed unique ID needed for cursor functionality
        solrQuery.setSorts(SORT_CLAUSES);
        solrQuery.setFields(PROJECTED_FIELDS);

        return new CandidateResultCollector(solrClient, solrQuery);
    }
}
