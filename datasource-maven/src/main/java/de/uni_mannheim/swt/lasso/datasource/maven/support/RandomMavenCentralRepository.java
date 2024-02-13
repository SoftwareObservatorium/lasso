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

import java.io.IOException;
import java.util.*;

import de.uni_mannheim.swt.lasso.index.collect.CandidateResultCollector;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.CandidateQuery;
import de.uni_mannheim.swt.lasso.index.repo.MavenCentralRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Random queries using SolR's "random_XXX" field for sorting.
 * 
 * @author Marcus Kessel
 *
 */
public class RandomMavenCentralRepository extends MavenCentralRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RandomMavenCentralRepository.class);

    private static final List<SolrQuery.SortClause> SORT_CLAUSES = Arrays.asList(SolrQuery.SortClause.desc("score"), SolrQuery.SortClause.desc("id"));

    //private static final String[] PROJECTED_FIELDS = new String[]{"score", "id", "name_sexact", "packagename_sexact", "groupId", "artifactId", "version", "score", "content", "hash", "doctype_s", "type", "uri", "bytecodename_s", "methodOrigSignature_sigs_exact", "superclass_exact", "interface_exact", "dep_exact", "inheritedmethodOrigSignature_sigs_exact", "bytecodename_s", "methodSignatureParamsOrderedKeywordsFq_sigs", "methodSignatureParamsOrderedKeywordsFq_ssig", "m_*", "meta_*", "classifier_s", "method_fqs", "method_fq"};

    public RandomMavenCentralRepository(SolrClient solrClient) {
        super(solrClient);
    }

    public CandidateResultCollector queryForRandomClassCandidates(CandidateQuery query, int start, int rows, boolean highlighting, List<SolrQuery.SortClause> orderByClauses, int expandedRows) throws IOException {
        SolrQuery solrQuery = this.buildSolrQuery(query.getLuceneQuery(), null);
        if (query.getConstraints() != null) {
            solrQuery.setFilterQueries((String[])escapeQuery(query.getConstraints()).toArray(new String[0]));
        }

        SplittableRandom random = new SplittableRandom();
        long seed = random.nextLong(0L, Long.MAX_VALUE);

        List<SolrQuery.SortClause> sortClauses = new ArrayList<>();
        // order seems to matter!
        sortClauses.add(SolrQuery.SortClause.desc(String.format("random_%s", seed)));
        sortClauses.addAll(SORT_CLAUSES);

        if(CollectionUtils.isNotEmpty(orderByClauses)) {
            sortClauses.addAll(orderByClauses);
        }

        solrQuery.setStart(start);
        solrQuery.setSorts(sortClauses);
        solrQuery.setFields(PROJECTED_FIELDS);
        if (highlighting) {
            solrQuery.setHighlight(true);
            solrQuery.set("hl.fl", new String[]{"methodSignatureParamsOrderedSyntaxKeywordsFq_sigs,methodSignatureParamsOrderedKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedSyntaxKeywordsFq_sigs,inheritedmethodSignatureParamsOrderedKeywordsFq_sigs"});
        }

        //
        this.expand(solrQuery, expandedRows);

        return new CandidateResultCollector(this.solrClient, solrQuery);
    }

    public CandidateResultCollector queryForRandomClassCandidates(CandidateQuery query, int start, int rows, boolean highlighting, int expandedRows) throws IOException {
        return queryForRandomClassCandidates(query, start, rows, highlighting, Collections.emptyList(), expandedRows);
    }

    public static void main(String[] args) {
        SplittableRandom random = new SplittableRandom();
        long seed = random.nextLong(0L, Long.MAX_VALUE);
        System.out.println(seed);
    }
}
