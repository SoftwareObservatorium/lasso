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
import joinery.DataFrame;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Marcus Kessel
 */
public class ArrowOlapTest {

    String executionId = "aeb524b9-cfe7-4d2b-afa4-503ef9079f45";

    String sqlAllTypes = "SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID, '_', VARIANTID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ?";
    String sqlByType = sqlAllTypes + " and type = ?";

    @Test
    public void testQueryDuckDB() throws SQLException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sqlByType)) {
            preparedStatement.setString(1, executionId);
            preparedStatement.setString(2, SRHRepository.TYPE_VALUE);

            DataFrame dataFrame = olap.queryDuckDB(preparedStatement, false);
            System.out.println(dataFrame.toString());
        }
    }

    @Test
    public void testQueryDuckDBAllTypes_prepared() throws SQLException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sqlAllTypes)) {
            preparedStatement.setString(1, executionId);

            DataFrame dataFrame = olap.queryDuckDB(preparedStatement, false);
            System.out.println(dataFrame.toString());
        }
    }

    @Test
    public void testWriteDuckDB() throws SQLException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = sqlByType + "order by sheetid";
        String path = "/tmp/blub_type.parquet";

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);
            preparedStatement.setString(2, SRHRepository.TYPE_VALUE);

            olap.writeParquet(preparedStatement, path);
        }
    }

    @Test
    @Disabled
    public void testWriteAll() throws SQLException {
        //JDBC.JDBC_URL = "jdbc:ignite:thin://lassohp1.informatik.uni-mannheim.de";
        JDBC jdbc = new JDBC();

        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT * FROM srm.cellvalue where executionid = ?";

        String path = "/tmp/blub_all.parquet";

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);

            olap.sqlToParquet(preparedStatement, path);
        }
    }

    @Test
    public void testReadDuckDB() throws SQLException {
        ArrowOlap olap = new ArrowOlap();
        String path = "/tmp/blub_all.parquet";
        DataFrame dataFrame = olap.readParquetAsDataFrame(path);
        System.out.println(dataFrame);
    }
}
