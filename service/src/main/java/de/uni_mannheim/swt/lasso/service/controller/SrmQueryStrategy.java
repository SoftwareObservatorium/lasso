package de.uni_mannheim.swt.lasso.service.controller;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.data.repository.LassoRepository;
import de.uni_mannheim.swt.lasso.core.dto.AbstractionInfo;
import de.uni_mannheim.swt.lasso.core.dto.SearchSrmQueryRequest;
import de.uni_mannheim.swt.lasso.core.dto.SearchSrmQueryResponse;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.*;

/**
 * Get abstractions for SRMs.
 *
 * @author Marcus Kessel
 */
public class SrmQueryStrategy {

    private static final Logger LOG = LoggerFactory
            .getLogger(SrmQueryStrategy.class);

    private final ClusterEngine clusterEngine;
    private final LassoConfiguration lassoConfiguration;

    public SrmQueryStrategy(ClusterEngine clusterEngine, LassoConfiguration lassoConfiguration) {
        this.clusterEngine = clusterEngine;
        this.lassoConfiguration = lassoConfiguration;
    }

    public SearchSrmQueryResponse query(SearchSrmQueryRequest request) throws IOException {
        SearchSrmQueryResponse response = new SearchSrmQueryResponse();

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

//        // get all systems
//        Table countTable = clusterEngine.getReportRepository().select(request.getExecutionId(),
//                "SELECT count(system) from StepReport where passed = true and action = '"+lastAction+"'");
//        long totalSystems = countTable.longColumn(0).get(0);
//        //  limit " + request.getStart() + "," + request.getRows()
//        // FIXME perf. issue with post-ranking (hen & egg problem)
//        Table systemsTable = clusterEngine.getReportRepository().select(request.getExecutionId(),
//                "SELECT system,abstraction,datasource from StepReport where passed = true and action = '"+lastAction+"' order by lastmodified");// asc limit " + request.getStart() + "," + request.getRows());
//
//        // select range of systems (paging)
//        StringColumn systems = systemsTable.stringColumn(0);

        // get from cache
        LassoRepository lassoRepository = clusterEngine.getLassoRepository();
        Map<String, Systems> abstractions = lassoRepository.getAbstractions(request.getExecutionId(), lastAction);

        List<AbstractionInfo> abstractionInfos = new LinkedList<>();
        response.setAbstractions(abstractionInfos);
        for(String abstraction : abstractions.keySet()) {
            AbstractionInfo abstractionInfo = new AbstractionInfo();
            abstractionInfo.setName(abstraction);
            abstractionInfo.setAction(lastAction);

            Systems abSystems = abstractions.get(abstraction);
            abstractionInfo.setSpecification(abSystems.getSpecification());
            abstractionInfo.setCodeUnits(abSystems.getExecutables().stream().map(System::getCode).toList());

            abstractionInfos.add(abstractionInfo);
        }

        return response;
    }
}
