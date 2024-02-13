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
import org.apache.ignite.IgniteJdbcThinDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 * @author Marcus Kessel
 */
public class JDBC {

    private static final Logger LOG = LoggerFactory
            .getLogger(JDBC.class);

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1";

    private JdbcTemplate jdbcTemplate;

    static {
        try {
            Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public JdbcTemplate getJdbcTemplate() throws SQLException {
        synchronized (JDBC.class) {
            if(jdbcTemplate != null) {
                return jdbcTemplate;
            }

            jdbcTemplate = new JdbcTemplate();
            IgniteJdbcThinDataSource igniteJdbcThinDataSource = new IgniteJdbcThinDataSource();
            String jdbcUri = JDBC_URL;
            igniteJdbcThinDataSource.setUrl(jdbcUri); // connect to schema
            jdbcTemplate.setDataSource(igniteJdbcThinDataSource);

            return jdbcTemplate;
        }
    }

    public Table sqlToTable(String sql, Object ... args) throws IOException {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate();

            return jdbcTemplate.query(sql, resultSet -> {
                return Table.read().db(resultSet);
            }, args);
        } catch(Throwable e) {
            throw new IOException(e);
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
}
