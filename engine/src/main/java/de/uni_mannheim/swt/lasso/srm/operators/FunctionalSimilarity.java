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
package de.uni_mannheim.swt.lasso.srm.operators;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.model.Behaviour;

import joinery.DataFrame;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Determines Functional similarity of N systems based on their exhibited behaviour in terms of outputs (as part of actuation sheets).
 *
 * @author Marcus Kessel
 */
public class FunctionalSimilarity {

    private static final Logger LOG = LoggerFactory
            .getLogger(FunctionalSimilarity.class);

    private final ClusterEngine clusterEngine;

    public FunctionalSimilarity(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;
    }

    /**
     * Measure degree of functional similarity (see {@link #similarity(List, List)}.
     * 
     * @param behaviour the desired behaviour
     * @param map Properties
     * @return
     * @throws IOException
     */
    public Map<String, Double> measure(Behaviour behaviour, Map<String, ?> map) throws IOException {
        String oracleId = null;
        // manually-specified oracle
        if (behaviour.isManualOracle()) {
            oracleId = "oracle_oracle";
        }

        // executable specification
        if (!behaviour.isManualOracle() && behaviour.getPseudoOracle() != null) {
            oracleId = behaviour.getPseudoOracle().getId();
        }

        Validate.notNull(oracleId, "(Pseudo) oracle is null");

        // 1. retrieve observational records by sheet
        // 2. determine statements in each sheet: remove UUIDs in sheet names and concat X,Y coordinates

        // FIXME arenaid should be passed as parameter
        DataFrame df = clusterEngine.getClusterSRMRepository().sqlToDataFrame(
                "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement," +
                        " CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and actionId = ? and ARENAID = 'execute' and type = 'value' order by sheetid",
                map.get("executionId"), map.get("actionName"));

        // pivot - widen (statements as rows and systems as columns)
        DataFrame wide_statement = df.pivot("STATEMENT", "SYSTEMID", "VALUE");

        // TODO we have to determine the "best match" if ref impl is used
        // for manual oracle, it is "oracle_oracle"

        // select oracle column
        List oracle = wide_statement.col(oracleId);

        // calculate pair-wise similarities between oracle and alternative system
        Map<String, Double> similarities = new LinkedHashMap<>();
        for (Object column : wide_statement.drop(0).drop(oracleId).columns()) {
            List alternative = wide_statement.col(column);

            double sim = similarity(oracle, alternative);

            if (LOG.isDebugEnabled()) {
                LOG.debug("SIM oracle '{}' vs '{}' => {}", oracleId, column, sim);
            }

            similarities.put(Objects.toString(column), sim);
        }

        return similarities;
    }

    /**
     * Jaccard'ish (Intersection over Union), but for (ordered) lists
     *
     * @param one
     * @param two
     * @return
     */
    private double similarity(List one, List two) {
        int total = one.size();
        int matches = 0;
        for (int i = 0; i < total; i++) {
            // special handling for oracle: _INSTANCE_ vs _NA_ in oracle
            if(two.get(i) instanceof String && StringUtils.equals((String) two.get(i), "_INSTANCE_") &&
                    one.get(i) instanceof String && StringUtils.equals((String) one.get(i), "_NA_")
            ) {
                matches++;
                continue;
            }

            if (Objects.equals(one.get(i), two.get(i))) {
                matches++;
            }
        }

        return (double) matches / total;
    }

    /**
     * Transform output value.
     *
     * @param value
     * @return
     */
    private Object transformOutput(Object value) {
        // TODO more sophisticated adaptation strategies for values
        return value;
    }
}
