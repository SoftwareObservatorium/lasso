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
package de.uni_mannheim.swt.lasso.srm.olap;

import de.uni_mannheim.swt.lasso.srm.JDBC;
import de.uni_mannheim.swt.lasso.srm.SRHRepository;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

/**
 * @author Marcus Kessel
 */
public class ArrowOlapTest {

    @Test
    public void testQueryArrow() throws SQLException {
        JDBC jdbc = new JDBC();

        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ? order by sheetid";
        Object[] args = {"874aad2d-543d-4372-bc6a-22b4a6d3c04e", SRHRepository.ARENA_DEFAULT, SRHRepository.TYPE_VALUE};

        VectorSchemaRoot root = olap.queryArrow(jdbc.getJdbcTemplate(), sql, args);
    }

    @Test
    public void testQueryDuckDB() throws SQLException {
        JDBC jdbc = new JDBC();

        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ?";
        Object[] args = {"874aad2d-543d-4372-bc6a-22b4a6d3c04e", SRHRepository.ARENA_DEFAULT, SRHRepository.TYPE_VALUE};

        olap.queryDuckDB(jdbc.getJdbcTemplate(), sql, args);
    }

    @Test
    public void testQueryDuckDBAllTypes() throws SQLException {
        JDBC jdbc = new JDBC();

        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE, TYPE FROM srm.cellvalue where executionid = ? and arenaid = ?";
        Object[] args = {"874aad2d-543d-4372-bc6a-22b4a6d3c04e", SRHRepository.ARENA_DEFAULT};

        olap.queryDuckDBAllTypes(jdbc.getJdbcTemplate(), sql, args);
    }

    @Test
    public void testWriteDuckDB() throws SQLException {
        JDBC jdbc = new JDBC();

        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ? order by sheetid";
        Object[] args = {"874aad2d-543d-4372-bc6a-22b4a6d3c04e", SRHRepository.ARENA_DEFAULT, SRHRepository.TYPE_VALUE};

        olap.writeParquet(jdbc.getJdbcTemplate(), sql, args);
    }

    @Test
    public void testReadDuckDB() throws SQLException {
        ArrowOlap olap = new ArrowOlap();

        olap.read();
    }
}
