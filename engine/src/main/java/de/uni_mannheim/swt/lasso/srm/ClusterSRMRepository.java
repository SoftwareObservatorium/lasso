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
import joinery.DataFrame;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteJdbcThinDataSource;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class ClusterSRMRepository implements SRMRepository {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClusterSRMRepository.class);

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1/%s";

    private JdbcTemplate jdbcTemplate;

    private final ClusterEngine clusterEngine;

    private IgniteCache<CellId, CellValue> cache;

    static {
        try {
            Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ClusterSRMRepository(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;

        initCaches();
    }

    protected void initCaches() {
        CacheConfiguration<CellId, CellValue> implCacheConfig =
                new CacheConfiguration<>(CACHE_NAME);
        implCacheConfig.setIndexedTypes(CellId.class, CellValue.class);
        implCacheConfig.setSqlSchema(CACHE_NAME); // without quotes

        this.cache = this.clusterEngine.getIgnite().getOrCreateCache(implCacheConfig);

    }

    public JdbcTemplate getJdbcTemplate() throws SQLException {
        synchronized (ClusterSRMRepository.class) {
            if(jdbcTemplate != null) {
                return jdbcTemplate;
            }

            jdbcTemplate = new JdbcTemplate();
            IgniteJdbcThinDataSource igniteJdbcThinDataSource = new IgniteJdbcThinDataSource();
            String jdbcUri = String.format(JDBC_URL, CACHE_NAME);
            igniteJdbcThinDataSource.setUrl(jdbcUri); // connect to schema
            jdbcTemplate.setDataSource(igniteJdbcThinDataSource);

            return jdbcTemplate;
        }
    }

    @Override
    public void putAll(Map<CellId, CellValue> cells) {
        cache.putAll(cells);
    }

    @Override
    public void put(CellId id, CellValue value) {
        cache.put(id, value);
    }

    @Override
    public CellValue get(CellId id) {
        return cache.get(id);
    }

    @Override
    public void remove(CellId id) {
        cache.remove(id);
    }

    @Override
    public void clear() {
        if(cache != null) {
            cache.clear();
        }
    }

    public Table sqlToTable(String sql, Object ... args) throws IOException {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate();

            return jdbcTemplate.query(sql, resultSet -> {
                return Table.read().db(resultSet, CACHE_NAME);
            }, args);
        } catch(Throwable e) {
            throw new IOException(e);
        }
    }

    public void export(String executionId, String actionName, File file) throws IOException {
        Table table = sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", executionId);

        // write CSV
        File csvFile = new File(file.getParentFile(), String.format("%s_%s", actionName, file.getName()));

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

    public DataFrame sqlToDataFrame(String sql, Object ... args) throws IOException {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate();

            return jdbcTemplate.query(sql, resultSet -> {
                return DataFrame.readSql(resultSet);
            }, args);
        } catch(Throwable e) {
            throw new IOException(e);
        }
    }

    public IgniteCache<CellId, CellValue> getCache() {
        return cache;
    }

    public ClusterEngine getClusterEngine() {
        return clusterEngine;
    }
}
