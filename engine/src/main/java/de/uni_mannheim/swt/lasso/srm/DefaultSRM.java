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

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.data.repository.ReportRepository;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.Behaviour;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.srm.SRM;
import de.uni_mannheim.swt.lasso.engine.action.arena.FunctionalSimilarityReport;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.srm.operators.FunctionalSimilarity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import javax.cache.Cache;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default SRM implementation.
 *
 * @author Marcus Kessel
 */
public class DefaultSRM implements SRM {

    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultSRM.class);

    /**
     * Human readable aliases for convenience
     */
    public static final Map<String, String> ATTRIBUTES_ALIASES = new LinkedHashMap<String, String>() {
        {
            put("cc.loc", "IndexMeasurements.M_STATIC_LOC_TD");
            put("cc.branch.total", "IndexMeasurements.M_STATIC_BRANCH_TD");
        }
    };

    private final ClusterEngine clusterEngine;

    public DefaultSRM(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;
    }

    /**
     * Requires functional equivalence.
     *
     * @param behaviour
     * @param map
     * @return
     * @throws IOException
     */
    public List<String> equalTo(Behaviour behaviour, Map<String, ?> map) throws IOException {
        return similarTo(behaviour, 1d, map);
    }

    /**
     * Requires functional similarity.
     *
     * @param behaviour
     * @param map
     * @return
     * @throws IOException
     */
    public List<String> similarTo(Behaviour behaviour, double minimum, Map<String, ?> map) throws IOException {
        FunctionalSimilarity similarity = new FunctionalSimilarity(clusterEngine);

        Map<String, Double> similarityMap = similarity.measure(behaviour, map);

        Map<String, Double> bestMatches = new LinkedHashMap<>();
        for(String fullId : similarityMap.keySet()) {
            String systemId = fullId;
            int adapterId = 0;
            if(StringUtils.contains(fullId, "_")) {
                systemId = StringUtils.substringBeforeLast(fullId, "_");
                adapterId = Integer.valueOf(StringUtils.substringAfterLast(fullId, "_"));
            }

            ReportKey reportKey = new ReportKey((String) map.get("actionName"), (String) map.get("abstractionId"), systemId, "unknown", adapterId);

            Double score = similarityMap.get(fullId);

            FunctionalSimilarityReport similarityReport = new FunctionalSimilarityReport();
            similarityReport.setScore(score);

            clusterEngine.getReportRepository().put((String) map.get("executionId"), reportKey, similarityReport);

            if(!bestMatches.containsKey(systemId)) {
                bestMatches.put(systemId, score);
            } else {
                Double otherScore = bestMatches.get(systemId);
                if(score > otherScore) {
                    bestMatches.put(systemId, score);
                }
            }
        }

        // also compute best match based on similarity (save with -1)
        // FIXME there may be multiple ones ..
        for(String systemId : bestMatches.keySet()) {
            ReportKey reportKey = new ReportKey((String) map.get("actionName"), (String) map.get("abstractionId"), systemId, "unknown", -1);

            Double score = bestMatches.get(systemId);

            FunctionalSimilarityReport similarityReport = new FunctionalSimilarityReport();
            similarityReport.setScore(score);

            clusterEngine.getReportRepository().put((String) map.get("executionId"), reportKey, similarityReport);
        }

        return similarityMap.entrySet().stream().filter(e -> e.getValue() >= minimum).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public void export(String executionId, String actionName, Abstraction abstraction, File csvFile) throws IOException {
        ClusterSRMRepository clusterSRMRepository = clusterEngine.getClusterSRMRepository();

        Table table = clusterSRMRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ? and actionId = ?", executionId, actionName);

        List<String> ids = abstraction.getSystems().stream().map(System::getId).collect(Collectors.toList());

        table = table.where(table.stringColumn("SYSTEMID").isIn(ids));

        if(table != null) {
            table.write().csv(csvFile);

            if(LOG.isInfoEnabled()) {
                LOG.info("Wrote CSV to {}", csvFile);
            }
        } else {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Table was null {}", csvFile);
            }
        }
    }

    /**
     * First attempts to fetch analysis attributes from arena reports.
     * Alternatively, attributes may be fetched from {@link ReportRepository}.
     *
     * @param map
     * @return
     * @throws IOException
     */
    @Override
    public Object getObservation(Map<String, ?> map) throws IOException {
        IgniteCache<CellId, CellValue> cache = clusterEngine.getClusterSRMRepository().getCache();

        String type = (String) map.get("type");
        if(ATTRIBUTES_ALIASES.containsKey(type)) {
            type = ATTRIBUTES_ALIASES.get(type);
        }

        StringBuilder sql = new StringBuilder("executionId = ? AND actionId = ?");

        List<Object> args = new LinkedList<>();
        args.add(map.get("executionId"));
        args.add(map.get("actionName"));

        if (map.containsKey("sequenceId")) {
            sql.append(" AND sheetId = ?");
            args.add(map.get("sequenceId"));
        }
        if (map.containsKey("abstractionId")) {
            sql.append(" AND abstractionId = ?");
            args.add(map.get("abstractionId"));
        }
        if (map.containsKey("row")) {
            sql.append(" AND y = ?"); //
            args.add(map.get("row")); // x starts at 0, statements naturally at 1
        }
        if (map.containsKey("column")) {
            sql.append(" AND x = ?");
            args.add(map.get("column")); // y starts at 0, statements naturally at 1
        }
//        if(StringUtils.isNotBlank(expectedValue)) {
//            sql.append(" AND value = ?");
//            args.add(expectedValue);
//        }
        if (map.containsKey("type")) {
            sql.append(" AND type = ?");
            args.add(type);
        }

        if (map.containsKey("systemId")) {
            sql.append(" AND systemId = ?");
            args.add(map.get("systemId"));
        }

        SqlQuery<CellId, CellValue> query = new SqlQuery<>(CellValue.class, sql.toString());
        query.setArgs(args.toArray());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Formulated query '{}'. Args = '{}'", sql.toString(), Arrays.toString(args.toArray()));
        }

        QueryCursor<Cache.Entry<CellId, CellValue>> cursor = cache.query(query);

        Optional<Cache.Entry<CellId, CellValue>> entryOp = cursor.getAll().stream().findFirst();

        if (entryOp.isPresent()) {
            return entryOp.get().getValue();
        }

        // try (old) ReportRepository
        Object value = getLastReportValue((String) map.get("executionId"),
                (String) map.get("actionName"),
                (String) map.get("abstractionId"),
                (String) map.get("systemId"),
                type);

        return value;
    }

    // FIXME gets last ONE (should be selectable)
    // FIXME should be selectable by actionId
    public Object getLastReportValue(String executionId, String actionId, String abstractionId, String systemId, String qualifiedField) throws IOException {
        // reportName:fieldName
        String[] parts = StringUtils.split(qualifiedField, ".");

        Validate.isTrue(parts.length == 2, "qualifiedField format mismatch: '%s'", qualifiedField);

        String sql = "select " + parts[1] + " from " + parts[0]
                + " WHERE SYSTEM = '"
                + systemId
                + "' AND ABSTRACTION = '" + abstractionId + "'"
                //+ "' AND ACTION = '" + actionId + "'"
                + " order by LASTMODIFIED desc";

        return clusterEngine.getReportRepository().jdbcToRowMapper(executionId, parts[0], sql, (resultSet, i) -> resultSet.getObject(parts[1]))
                .get(0); // FIXME bad ..
    }
}
