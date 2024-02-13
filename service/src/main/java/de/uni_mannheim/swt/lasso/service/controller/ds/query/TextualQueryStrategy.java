package de.uni_mannheim.swt.lasso.service.controller.ds.query;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.dto.SearchQueryRequest;
import de.uni_mannheim.swt.lasso.core.dto.SearchRequestResponse;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenCodeUnitUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.index.CandidateQueryResult;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Get results for textual code search.
 *
 * @author Marcus Kessel
 */
public class TextualQueryStrategy extends QueryStrategy {

    private static final Logger LOG = LoggerFactory
            .getLogger(TextualQueryStrategy.class);

    public TextualQueryStrategy(ClusterEngine clusterEngine, LassoConfiguration lassoConfiguration) {
        super(clusterEngine, lassoConfiguration);
    }

    @Override
    public SearchRequestResponse query(SearchQueryRequest request,
                                       String dataSource) throws IOException {
        SearchRequestResponse response = new SearchRequestResponse();

        // classic search
        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(MavenQuery.queryStrategies.get(request.getStrategy()));

        MavenDataSource mavenDataSource = (MavenDataSource) lassoConfiguration.getDataSource(dataSource);

        MavenCentralIndex mavenCentralIndex = mavenDataSource.getMavenCentralIndex();

        Map<String, CodeUnit> implementations = null;
        try {
            List<SolrQuery.SortClause> sortClauses = new LinkedList<>();
            if(CollectionUtils.isNotEmpty(request.getSortBy())) {
                sortClauses = request.getSortBy().stream().map(s -> {
                    String[] arr = s.split(" ");
                    if(StringUtils.equalsIgnoreCase(arr[1], "desc")) {
                        return SolrQuery.SortClause.desc(arr[0]);
                    } else {
                        return SolrQuery.SortClause.desc(arr[0]);
                    }
                }).collect(Collectors.toList());
            }

            CandidateQueryResult result = mavenCentralIndex.queryDirectly(request.getQuery(),
                    searchOptions,
                    request.getFilters().stream().toArray(String[]::new),
                    request.getStart(),
                    request.getRows(),
                    sortClauses);
            response.setTotal(result.getTotal());
            response.setRows(result.getRows());

            implementations = result.getCandidates().stream().map(c -> {
                CodeUnit implementation = MavenCodeUnitUtils.toImplementation(((SolrCandidateDocument) c).getSolrDocument());
                implementation.setDataSource(dataSource);

                // copy over methods
                List<String> methods = new LinkedList<>();
                if(implementation.getUnitType() == CodeUnit.CodeUnitType.CLASS) {
                    methods.addAll(((SolrCandidateDocument) c).getSolrDocument().getFieldValues("methodOrigSignatureFq_sigs_exact").stream().map(s -> (String)s ).collect(Collectors.toList()));
                } else {
                    methods.addAll(((SolrCandidateDocument) c).getSolrDocument().getFieldValues("methodOrigSignatureFq_ssigs_sexact").stream().map(s -> (String)s ).collect(Collectors.toList()));
                }

                implementation.setMethods(methods);

                return implementation;
            }).collect(Collectors.toMap(CodeUnit::getId,
                    i -> i,
                    (e1, e2) -> e1,
                    LinkedHashMap::new));

            response.setImplementations(implementations);
        } catch (IOException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Textual code search failed", e);
            }
        }

        return response;
    }
}
