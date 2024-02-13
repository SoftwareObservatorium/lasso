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
package de.uni_mannheim.swt.lasso.cluster.data.repository;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteJdbcThinDataSource;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * Repository for structured reports based on implementations of LassoReport.
 *
 * @author Marcus Kessel
 */
public class ReportRepository implements ReportOperations {

    private static final Logger LOG = LoggerFactory
            .getLogger(ReportRepository.class);

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1/%s_%s";

    static {
        try {
            Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final ClusterEngine clusterEngine;

    private Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();

    public ReportRepository(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;
    }

    public JdbcTemplate getJdbcTemplate(String executionId, String tableName) throws SQLException {
        synchronized (jdbcTemplateMap) {
            String jdbcUri = String.format(JDBC_URL, executionId, tableName);
            if(jdbcTemplateMap.containsKey(jdbcUri)) {
                return jdbcTemplateMap.get(jdbcUri);
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate();
            IgniteJdbcThinDataSource igniteJdbcThinDataSource = new IgniteJdbcThinDataSource();
            igniteJdbcThinDataSource.setUrl(jdbcUri);
            jdbcTemplate.setDataSource(igniteJdbcThinDataSource);

            jdbcTemplateMap.put(jdbcUri, jdbcTemplate);

            return jdbcTemplate;
        }
    }

    @Override
    public void put(String executionId, ReportKey key, LassoReport report) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("PUT '{}' '{}' '{}'", executionId, key, report);
        }

        Validate.notNull(report);

        IgniteCache cache = clusterEngine.getOrCreateReportCache(executionId, report.getClass());

        cache.put(key, report);
    }

    @Override
    public <T extends LassoReport> void remove(String executionId, ReportKey key, Class<T> reportType) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("REMOVE '{}' '{}' '{}'", executionId, key, reportType);
        }

        Validate.notNull(reportType);

        IgniteCache cache = clusterEngine.getOrCreateReportCache(executionId, reportType);

        cache.remove(key);
    }

    @Override
    public void newValuesReport(String executionId, String reportName, Map<String, String> valueTypes) {
        IgniteCache<ReportKey, BinaryObject> valuesCache = clusterEngine.getOrCreateBinaryReportCache(executionId, reportName, valueTypes).withKeepBinary();
    }

    @Override
    public void putValues(String executionId, ReportKey key, String reportName, Map<String, ?> values) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("PUT RAW VALUES '{}' '{}' '{}' '{}'", executionId, key, reportName, values);
        }

        Validate.notNull(values);

        BinaryObjectBuilder builder = clusterEngine.getIgnite().binary().builder(reportName);

        // key
        builder.setField("action", key.getAction());
        builder.setField("dataSource", key.getDataSource());
        builder.setField("system", key.getSystem());
        builder.setField("abstraction", key.getAbstraction());
        builder.setField("permId", key.getPermId());
        builder.setField("LASTMODIFIED", new Date());

        values.forEach((k,v) -> {
            builder.setField(k, v);
        });

        IgniteCache cache = clusterEngine.getIgnite().cache(clusterEngine.toCacheName(executionId, reportName)).withKeepBinary();

        cache.put(key, builder.build());
    }

    @Override
    public void putValues(String executionId, String action, Abstraction abstraction, System system, String reportName, Map<String, ?> values) {
        putValues(executionId, ReportKey.of(action, abstraction.getName(), system), reportName, values);
    }

    @Override
    public <T extends LassoReport> T get(String executionId, ReportKey key, Class<T> reportType) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}' '{}'", executionId, key, reportType);
        }

        if(reportType == null) {
            return null;
        }

        IgniteCache cache = clusterEngine.getOrCreateReportCache(executionId, reportType);

        if(cache == null) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Cache not found for '{}'", reportType);
            }

            return null;
        }

        return (T) cache.get(key);
    }

    /**
     * Get latest report (independent of action).
     *
     * @param executionId
     * @param key
     * @param reportType
     * @param <T>
     * @return
     */
    public <T extends LassoReport> T getFirst(String executionId, ReportKey key, Class<T> reportType) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}' '{}'", executionId, key, reportType);
        }

        if(reportType == null) {
            return null;
        }

        IgniteCache cache = clusterEngine.getOrCreateReportCache(executionId, reportType);

        if(cache == null) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Cache not found for '{}'", reportType);
            }

            return null;
        }

        // fields query
        String query = "select ACTION,ABSTRACTION,SYSTEM, DATASOURCE,PERMID,LASTMODIFIED from "
                + reportType.getSimpleName()
                + " WHERE SYSTEM = '" + key.getSystem()  + "'"
                + "AND ABSTRACTION = '" + key.getAbstraction() + "'"
                + "AND PERMID = '" + key.getPermId() + "'"
                + " order by LASTMODIFIED asc";

        Iterator<List<?>> iterator = null;
        try {
            iterator = query(executionId, query);
        } catch (IOException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("SQL query failed for '{}'", query);
                LOG.warn("Exception", e);
            }

            return null;
        }

        // only first entry is of interest
        if(iterator.hasNext()) {
            List row = iterator.next();
            ReportKey nKey = ReportKey.of((String) row.get(0), (String) row.get(1), (String) row.get(2), (String) row.get(3), (int) row.get(4));

            return (T) cache.get(nKey);
        }

        return null;
    }

    /**
     * Get latest report (independent of action).
     *
     * @param executionId
     * @param key
     * @param reportType
     * @param <T>
     * @return
     */
    public <T extends LassoReport> T getLast(String executionId, ReportKey key, Class<T> reportType) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}' '{}'", executionId, key, reportType);
        }

        if(reportType == null) {
            return null;
        }

        IgniteCache cache = clusterEngine.getOrCreateReportCache(executionId, reportType);

        if(cache == null) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Cache not found for '{}'", reportType);
            }

            return null;
        }

        // fields query
        String query = "select ACTION,ABSTRACTION,SYSTEM, DATASOURCE,PERMID,LASTMODIFIED from "
                + reportType.getSimpleName()
                + " WHERE SYSTEM = '" + key.getSystem()  + "'"
                + "AND ABSTRACTION = '" + key.getAbstraction() + "'"
                + "AND PERMID = '" + key.getPermId() + "'"
                + " order by LASTMODIFIED desc";

        Iterator<List<?>> iterator = null;
        try {
            iterator = query(executionId, query);
        } catch (IOException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("SQL query failed for '{}'", query);
                LOG.warn("Exception", e);
            }

            return null;
        }

        // only first entry is of interest
        if(iterator.hasNext()) {
            List row = iterator.next();
            ReportKey nKey = ReportKey.of((String) row.get(0), (String) row.get(1), (String) row.get(2), (String) row.get(3), (int) row.get(4));

            return (T) cache.get(nKey);
        }

        return null;
    }

    @Override
    public Table reportToTable(String executionId, Class<? extends LassoReport> reportType) throws IOException {
        return reportToTable(executionId, reportType.getSimpleName());
    }

    @Override
    public Table reportToTable(String executionId, String tableName) throws IOException {
        return jdbcToTable(executionId, tableName, String.format("select * from %s", tableName));
    }

    public Table reportToTable(String executionId, String tableName, String actionName) throws IOException {
        return jdbcToTable(executionId, tableName, String.format("select * from %s where action = '%s'", tableName, actionName));
    }

    @Override
    public void export(Table table, File file) throws IOException {
        // write CSV

        if(table != null) {
            table.write().csv(file);

            if(LOG.isInfoEnabled()) {
                LOG.info("Wrote CSV to {}", file);
            }
        } else {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Table was null {}", file);
            }
        }
    }

    @Override
    public void export(String executionId, String actionName, Class<? extends LassoReport> reportType, File file) throws IOException {
        export(reportToTable(executionId, reportType), file);
    }

    @Override
    public void export(String executionId, String actionName, File file) throws IOException {
        exportEach(executionId, actionName, file);
    }

    @Override
    public Table getValues(String executionId, Abstraction abstraction, String qualifiedField) throws IOException {
        // reportName:fieldName
        String[] parts = StringUtils.split(qualifiedField, ".");

        Validate.isTrue(parts.length == 2, "qualifiedField format mismatch: '%s'", qualifiedField);

        Table table = jdbcToTable(executionId, parts[0], String.format("select SYSTEM,PERMID,%s from %s where ABSTRACTION = '%s'", parts[1], parts[0], abstraction.getName()));;

        // rename column
        table.column(parts[1]).setName(qualifiedField);

        return table;
    }

    @Override
    public Table select(String executionId, String sql) throws IOException {
        return sql(executionId, sql);
    }

    @Override
    public Class<? extends LassoReport> toLassoReportClass(String executionId, String reportId) {
        // FIXME NPE if no report registered
        Set<String> caches = clusterEngine.getReportCaches(executionId);

        Optional<? extends Class<?>> op = caches.stream()
                .filter(cache -> StringUtils.endsWith(cache.toLowerCase(), "." + reportId.toLowerCase()))
                .map(cache -> StringUtils.substringAfter(cache, "_"))
                .map(reportClassName -> {
                    try {
                        return ClassUtils.getClass(reportClassName);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).findFirst();

        return (Class<? extends LassoReport>) op.orElse(null);
    }

    /**
     * Export each table.
     *
     * @param executionId
     * @param actionName
     * @param file
     */
    private void exportEach(String executionId, String actionName, File file) {
        Set<String> caches = clusterEngine.getReportCaches(executionId);

        // select all tables modified by action
        caches.stream().forEach(c -> {
            IgniteCache cache = clusterEngine.getIgnite().cache(c);

            CacheConfiguration ccfg = (CacheConfiguration) cache.getConfiguration(CacheConfiguration.class);
            Collection<QueryEntity> entities = ccfg.getQueryEntities();

            Optional<QueryEntity> entityOp = entities.stream().findFirst();
            // one-to-one mapping here

            String tableName = entityOp.get().getTableName();

            String sql = "select distinct ACTION from " + tableName;

            try {
                List<String> actionNames = jdbcToRowMapper(executionId, tableName, sql, (resultSet, i) -> resultSet.getString("ACTION"));

                if(actionNames.contains(actionName)) {
                    Table table = reportToTable(executionId, entityOp.get().getTableName(), actionName);
                    export(table, new File(file, String.format("%s_%s.csv", entityOp.get().getTableName(), actionName)));
                }
            } catch (IOException e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Query table '{}' failed for action '{}'", tableName, actionName);
                }
            }
        });
    }

    public List<String> getTables(String executionId) {
        Set<String> caches = clusterEngine.getReportCaches(executionId);

        // all tables from all caches
        return caches.stream().map(c -> {
            IgniteCache cache = clusterEngine.getIgnite().cache(c);

            CacheConfiguration ccfg = (CacheConfiguration) cache.getConfiguration(CacheConfiguration.class);
            Collection<QueryEntity> entities = ccfg.getQueryEntities();

            Optional<QueryEntity> entityOp = entities.stream().findFirst();
            return entityOp.get().getTableName();
        }).collect(Collectors.toList());
    }

    public Table reportToTable(String executionId) throws IOException {
        Set<String> caches = clusterEngine.getReportCaches(executionId);

        // all tables from all caches
        Table[] tables = caches.stream().map(c -> {
            IgniteCache cache = clusterEngine.getIgnite().cache(c);

            CacheConfiguration ccfg = (CacheConfiguration) cache.getConfiguration(CacheConfiguration.class);
            Collection<QueryEntity> entities = ccfg.getQueryEntities();

            Optional<QueryEntity> entityOp = entities.stream().findFirst();
            // one-to-one mapping here

            try {
                return reportToTable(executionId, entityOp.get().getTableName());
            } catch (IOException e) {
                throw new RuntimeException("Export to table failed", e);
            }
        }).toArray(Table[]::new);

        if(ArrayUtils.isEmpty(tables)) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("no tables to export");
            }

            return null;
        }

        // remove first
        Table table = tables[0];
        // any other table?
        if(tables.length > 1) {
            table = table.joinOn("ABSTRACTION", "SYSTEM", "PERMID")
                    .fullOuter(ArrayUtils.subarray(tables,1, tables.length));
        } else {
            //
            if(LOG.isWarnEnabled()) {
                LOG.warn("no join necessary");
            }
        }

        return table;
    }

    public Iterator<List<?>> query(String executionId, String sql) throws IOException {
        //
        SqlFieldsQuery query = new SqlFieldsQuery(sql);

        // determine table name
        String tableName = findTable(sql);

        IgniteCache cache = findCache(executionId, tableName);

        FieldsQueryCursor<List<?>> cursor = cache.query(query);

        Iterator<List<?>> iterator = cursor.iterator();

        return iterator;
    }

    public List<Integer> getPermutationIds(String executionId, ReportKey key, Class<? extends LassoReport> reportType) throws IOException {
        String sql = "select PERMID from " + reportType.getSimpleName()
                + " WHERE SYSTEM = '"
                + key.getSystem()
                + "' AND ABSTRACTION = '" + key.getAbstraction() + "'";
        // FIXME
                //+ "' AND ACTION = '" + key.getAction() + "'";

        return jdbcToRowMapper(executionId, reportType.getSimpleName(), sql, (resultSet, i) -> resultSet.getInt("PERMID"));
    }

    public Table sql(String executionId, String sql) throws IOException {
        // determine table name
        String tableName = findTable(sql);

        return jdbcToTable(executionId, tableName, sql);
    }

    private Table jdbcToTable(String executionId, String tableName, String sql) throws IOException {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(executionId, tableName);

            return jdbcTemplate.query(sql, resultSet -> {
                return Table.read().db(resultSet, tableName);
            });
        } catch(Throwable e) {
            throw new IOException(e);
        }
    }

    public  <T> List<T> jdbcToRowMapper(String executionId, String tableName, String sql, RowMapper<T> rowMapper) throws IOException {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(executionId, tableName);

            return jdbcTemplate.query(sql, rowMapper);
        } catch(Throwable e) {
            throw new IOException(e);
        }
    }

    private String findTable(String sql) {
        // determine table name
        final String tableName;
        String from = StringUtils.substringAfter(sql.toLowerCase(), "from ");
        if(StringUtils.contains(from, " ")) {
            tableName = StringUtils.substringBefore(from, " ").trim();
        } else {
            tableName = from.trim();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Found table '{}'", tableName);
        }

        return tableName;
    }

    private IgniteCache findCache(String executionId, String tableName) throws IOException {
        Optional<IgniteCache> cacheOp = clusterEngine.getReportCaches(executionId).stream().map(c -> {
            IgniteCache cache = clusterEngine.getIgnite().cache(c);

            CacheConfiguration ccfg = (CacheConfiguration) cache.getConfiguration(CacheConfiguration.class);
            Collection<QueryEntity> entities = ccfg.getQueryEntities();

            Optional<QueryEntity> entityOp = entities.stream().findFirst();
            // one-to-one mapping here

            // ignore case
            if(StringUtils.equalsIgnoreCase(tableName, entityOp.get().getTableName())) {
                return cache;
            } else {
                return null;
            }
        }).filter(c -> c != null).findFirst();

        return cacheOp.orElseThrow(() -> new IOException(String.format("Could not find cache '%s'", tableName)));
    }
}
