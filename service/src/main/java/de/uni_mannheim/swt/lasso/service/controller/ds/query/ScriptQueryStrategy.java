package de.uni_mannheim.swt.lasso.service.controller.ds.query;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.dto.SearchQueryRequest;
import de.uni_mannheim.swt.lasso.core.dto.SearchRequestResponse;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenCodeUnitUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.index.CandidateQueryResult;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import de.uni_mannheim.swt.lasso.srm.SRMManager;
import joinery.DataFrame;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Get results for script.
 *
 * @author Marcus Kessel
 */
public class ScriptQueryStrategy extends QueryStrategy {

    private static final Logger LOG = LoggerFactory
            .getLogger(ScriptQueryStrategy.class);

    public ScriptQueryStrategy(ClusterEngine clusterEngine, LassoConfiguration lassoConfiguration) {
        super(clusterEngine, lassoConfiguration);
    }

    @Override
    public SearchRequestResponse query(SearchQueryRequest request,
                                       String dataSource) throws IOException {
        SearchRequestResponse response = new SearchRequestResponse();
        // determine distinct actions
        Table actions = clusterEngine.getReportRepository().select(request.getExecutionId(), "SELECT distinct action from StepReport");
        response.setActions(actions.stringColumn(0).asList());
        // just reverse order
        Collections.reverse(response.getActions());

        // determine last action
        String lastAction;
        if(StringUtils.isNotBlank(request.getForAction())) {
            lastAction = request.getForAction();
        } else {
            Table actionTable = clusterEngine.getReportRepository().select(request.getExecutionId(), "SELECT action from StepReport order by lastmodified desc");
            lastAction = actionTable.column(0).getString(0);
            LOG.debug("Last action was '{}'", lastAction);
        }

        // get all systems
        Table countTable = clusterEngine.getReportRepository().select(request.getExecutionId(),
                "SELECT count(system) from StepReport where passed = true and action = '"+lastAction+"'");
        long totalSystems = countTable.longColumn(0).get(0);
        //  limit " + request.getStart() + "," + request.getRows()
        // FIXME perf. issue with post-ranking (hen & egg problem)
        Table systemsTable = clusterEngine.getReportRepository().select(request.getExecutionId(),
                "SELECT system,abstraction,datasource from StepReport where passed = true and action = '"+lastAction+"' order by lastmodified");// asc limit " + request.getStart() + "," + request.getRows());

        if(LOG.isInfoEnabled()) {
            LOG.info("oracle filters '{}'", request.getOracleFilters());
        }

        // filter by oracle values
        if(MapUtils.isNotEmpty(request.getOracleFilters())) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Filtering by oracle filters '{}'", request.getOracleFilters());
            }

            try {
                //
                SRMManager srmManager = lassoConfiguration.getService(SRMManager.class);
                // FIXME arena id (let's assume arena "execute" for now)
                DataFrame df = srmManager.getActuationSheets(request.getExecutionId(), "execute", "value", request.getOracleFilters());

                Set<String> filteredSystems = (Set<String>) df.columns().stream()
                        .filter(s -> !StringUtils.equalsAnyIgnoreCase(s.toString(), "statement"))
                        .filter(s -> !StringUtils.startsWithIgnoreCase(s.toString(), "oracle_"))
                        .map(s -> StringUtils.substringBeforeLast(s.toString(), "_"))
                        .collect(Collectors.toSet());

                totalSystems = filteredSystems.size();

                systemsTable = systemsTable.where(systemsTable.stringColumn(0).isIn(filteredSystems));

                if(LOG.isInfoEnabled()) {
                    LOG.info("Systems filtered '{}' '{}'", filteredSystems, systemsTable.print());
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        // select range of systems (paging)
        StringColumn systems = systemsTable.stringColumn(0);

        response.setTotal(totalSystems);
        response.setRows(systems.size());

        // look if there's a rankreport for sorting
        Table rankingTable = null;
        try {
            rankingTable = clusterEngine.getReportRepository().select(request.getExecutionId(), "SELECT system,abstraction,datasource,rankposition from RankReport order by rankposition asc");
        } catch (Throwable e) {
            //
            LOG.warn("Ranking failed", e);
        }

        LinkedHashMap<String, CodeUnit> implementations = systemsTable.stream().map(row -> {
            try {
                String id = row.getString(0);
                String ds = row.getString(2); // datasource used
                String ab = row.getString("abstraction");

                MavenDataSource mavenDataSource = (MavenDataSource) lassoConfiguration.getDataSource(ds);
                SearchOptions searchOptions = new SearchOptions();
                MavenCentralIndex mavenCentralIndex = mavenDataSource.getMavenCentralIndex();

                CandidateQueryResult result = mavenCentralIndex.query("*:*", searchOptions, new String[]{String.format("id:\"%s\"", id)}, 0, 1, Collections.emptyList());
                List<CodeUnit> impls = result.getCandidates().stream().map(c -> {
                    CodeUnit implementation = MavenCodeUnitUtils.toImplementation(((SolrCandidateDocument) c).getSolrDocument());
                    implementation.setDataSource(ds);

                    // copy over methods
                    List<String> methods = new LinkedList<>();
                    if(implementation.getUnitType() == CodeUnit.CodeUnitType.CLASS) {
                        methods.addAll(((SolrCandidateDocument) c).getSolrDocument().getFieldValues("methodOrigSignatureFq_sigs_exact").stream().map(s -> (String)s ).collect(Collectors.toList()));
                    } else {
                        methods.addAll(((SolrCandidateDocument) c).getSolrDocument().getFieldValues("methodOrigSignatureFq_ssigs_sexact").stream().map(s -> (String)s ).collect(Collectors.toList()));
                    }

                    implementation.setMethods(methods);

                    implementation.getMetaData().put("abstraction", ab);

                    return implementation;
                }).collect(Collectors.toList());

                return impls.get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }).filter(Objects::nonNull).collect(Collectors.toMap(CodeUnit::getId,
                i -> i,
                (e1, e2) -> e1,
                LinkedHashMap::new));

        // explicitly sort (insertion order)
        if(rankingTable != null && rankingTable.rowCount() > 0) {
            // rank by RankingReport
            if(LOG.isInfoEnabled()) {
                LOG.info("Ranking by RankReport");
            }

            for(Row row : rankingTable) {
                CodeUnit codeUnit = null;
                try {
                    codeUnit = implementations.get(row.getString("system"));
                    int rank = row.getInt("rankposition");
                    codeUnit.setScore(implementations.size() - rank + 1);
                } catch (Throwable e) {
                    LOG.warn("Could not rank '{}'. Code Unit is '{}'. Impls size '{}'", row.getString("system"), codeUnit, implementations.size());
                }
            }
        } else {
            // rank by NLP score
            if(LOG.isInfoEnabled()) {
                LOG.info("Ranking by NLP score");
            }

            int score = implementations.size();
            for(CodeUnit impl : implementations.values()) {
                impl.setScore(score--);
            }
        }

        LOG.info("Pre-selected '{}' implementations.", implementations.size());

        // paging
        implementations = implementations.entrySet().stream().sorted(Comparator.comparingDouble(e -> e.getValue().getScore() * -1))
                        .skip(request.getStart()).limit(request.getRows()).collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new));

        for(CodeUnit impl : implementations.values()) {
            LOG.info("impl score {}", impl.getScore());
        }

        response.setImplementations(implementations);
        response.setRows(implementations.size());

        LOG.info("Returning '{}' implementations.", implementations.size());

        return response;
    }
}
