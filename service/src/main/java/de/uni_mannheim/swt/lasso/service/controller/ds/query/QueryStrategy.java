package de.uni_mannheim.swt.lasso.service.controller.ds.query;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.dto.SearchQueryRequest;
import de.uni_mannheim.swt.lasso.core.dto.SearchRequestResponse;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;

import java.io.IOException;

/**
 *
 * @author Marcus Kessel
 */
public abstract class QueryStrategy {

    protected final ClusterEngine clusterEngine;
    protected final LassoConfiguration lassoConfiguration;

    public QueryStrategy(ClusterEngine clusterEngine, LassoConfiguration lassoConfiguration) {
        this.clusterEngine = clusterEngine;
        this.lassoConfiguration = lassoConfiguration;
    }

    public abstract SearchRequestResponse query(SearchQueryRequest request,
                                                String dataSource) throws IOException;
}
