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
package de.uni_mannheim.swt.lasso.srm;

import de.uni_mannheim.swt.lasso.srm.operators.FunctionalCorrectness;
import joinery.DataFrame;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simplified SRM operations.
 *
 * @author Marcus Kessel
 */
public class SRMManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(SRMManager.class);

    private final ClusterSRMRepository clusterSRMRepository;
    private final FunctionalCorrectness correctness;

    public SRMManager(ClusterSRMRepository clusterSRMRepository, FunctionalCorrectness correctness) {
        this.clusterSRMRepository = clusterSRMRepository;
        this.correctness = correctness;
    }

    /**
     * Get actuation sheets based on given oracle value filters.
     *
     * @param executionId
     * @param arenaId
     * @param type
     * @param oracleFilters
     * @return
     * @throws IOException
     */
    public DataFrame getActuationSheets(String executionId, String arenaId, String type, Map<String, String> oracleFilters) throws IOException {
        if(StringUtils.isBlank(type)) {
            type = "value";
        }

        DataFrame df = clusterSRMRepository.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ? order by sheetid", executionId, arenaId, type);

        DataFrame wide = df.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");

        // filter by oracle
        wide = this.filterByOracleValues(wide, oracleFilters);

        return wide;
    }

    /**
     * Filter systems by given oracle filters.
     *
     * @param wide
     * @param oracleFilters
     * @return
     */
    protected DataFrame filterByOracleValues(DataFrame wide, Map<String, String> oracleFilters) {
        if(MapUtils.isNotEmpty(oracleFilters)) {
            Set<String> drop = new HashSet<>();
            for(Object column : wide.columns()) {
                String colName = (String) column;
                if(!StringUtils.equalsAnyIgnoreCase(colName, "STATEMENT", "VALUE")) {
                    boolean equivalent = true;
                    for(String stmt : oracleFilters.keySet()) {
                        String actual = (String) wide.get(stmt, colName);
                        String expected = oracleFilters.get(stmt);
                        equivalent = correctness.assertStringEquals(stmt, actual, expected);

                        if(!equivalent) {
                            break;
                        }
                    }

                    if(!equivalent) {
                        drop.add(colName);
                    }
                }
            }

            // drop
            wide = wide.drop(drop.toArray());
        }

        return wide;
    }

    public DataFrame getActuationSheets(String executionId, String arenaId, String[] types) throws IOException {
        // FIXME unsafe
        String typesIn = Arrays.stream(types).map(t -> StringUtils.wrap(t, "'")).collect(Collectors.joining(","));

        DataFrame df = clusterSRMRepository.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type in ("+typesIn+") order by sheetid", executionId, arenaId);
        DataFrame wide = df.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");

        return wide;
    }

    public DataFrame getActuationSheetsForSystem(String executionId, String arenaId, String systemId, String type) throws IOException {
        if(StringUtils.isBlank(type)) {
            type = "value";
        }

        DataFrame df = clusterSRMRepository.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and systemid = ? and type = ? order by sheetid", executionId, arenaId, systemId, type);
        DataFrame wide = df.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");

        return wide;
    }
}
