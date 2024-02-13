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

import org.apache.ignite.IgniteJdbcThinDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 *
 * @author Marcus Kessel
 */
public class SRMExporter {

    private static final Logger LOG = LoggerFactory
            .getLogger(SRMExporter.class);

    private static final String JDBC_URL = "jdbc:ignite:thin://swt105.informatik.uni-mannheim.de";

    private JdbcTemplate jdbcTemplate;

    static {
        try {
            Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public SRMExporter() {

    }

    public JdbcTemplate getJdbcTemplate() throws SQLException {
        synchronized (SRMExporter.class) {
            if(jdbcTemplate != null) {
                return jdbcTemplate;
            }

            jdbcTemplate = new JdbcTemplate();
            IgniteJdbcThinDataSource igniteJdbcThinDataSource = new IgniteJdbcThinDataSource();
            igniteJdbcThinDataSource.setUrl(JDBC_URL);
            jdbcTemplate.setDataSource(igniteJdbcThinDataSource);

            return jdbcTemplate;
        }
    }

    public Table sqlToTable(String sql, Object ... args) throws IOException {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate();

            return jdbcTemplate.query(sql, resultSet -> {
                return Table.read().db(resultSet, SRMRepository.CACHE_NAME);
            }, args);
        } catch(Throwable e) {
            throw new IOException(e);
        }
    }

    public void export(String executionId, String actionName, File file) throws IOException {
        Table table = sqlToTable("SELECT * FROM \"srm\".CELLVALUE WHERE executionId = ?", executionId);

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

    public void exportAll(String actionName, File file) throws IOException {
        Table table = sqlToTable("SELECT * FROM \"srm\".CELLVALUE");

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

    public static void main(String[] args) throws IOException {
        SRMExporter exporter = new SRMExporter();

        //exporter.export("e02f3324-77e6-4b22-a929-cc7b212f3cd3", "ayham", new File("/tmp/ayham.csv"));

        exporter.exportAll("srm", new File("/tmp/srm.csv"));
    }
}
