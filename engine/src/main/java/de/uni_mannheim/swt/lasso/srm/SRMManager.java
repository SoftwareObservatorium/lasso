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

import joinery.DataFrame;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class SRMManager {

    private final ClusterSRMRepository clusterSRMRepository;

    public SRMManager(ClusterSRMRepository clusterSRMRepository) {
        this.clusterSRMRepository = clusterSRMRepository;
    }

//    public DataFrame getActuationSheets(String executionId, String arenaId) throws IOException {
//        DataFrame df = clusterSRMRepository.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = 'value' order by sheetid", executionId, arenaId);
//        DataFrame wide = df.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");
//
//        return wide;
//    }

    public DataFrame getActuationSheets(String executionId, String arenaId, String type, Map<String, String> oracleFilters) throws IOException {
        if(StringUtils.isBlank(type)) {
            type = "value";
        }

        DataFrame df = clusterSRMRepository.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ? order by sheetid", executionId, arenaId, type);

        if(MapUtils.isNotEmpty(oracleFilters)) {
            df = df.select((DataFrame.Predicate<String>) row -> {
                String statement = row.get(0);

                if(oracleFilters.containsKey(statement)) {
                    //String systemId = row.get(1);
                    String actualValue = row.get(2);
                    String expectedValue = oracleFilters.get(statement);

                    return isEqual(actualValue, expectedValue);
                }

                return true;
            });
        }

        DataFrame wide = df.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");

        return wide;
    }

    /**
     * FIXME more sophisticated strategies
     *
     * @param expected
     * @param actual
     * @return
     */
    public boolean isEqual(String expected, String actual) {
        return StringUtils.equals(actual, expected);
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
