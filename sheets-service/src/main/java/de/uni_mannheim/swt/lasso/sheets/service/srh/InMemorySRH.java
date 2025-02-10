package de.uni_mannheim.swt.lasso.sheets.service.srh;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 */
public class InMemorySRH {

    private static final Logger LOG = LoggerFactory
            .getLogger(InMemorySRH.class);

    private DuckDBConnection connection;

    public void initialize() throws SQLException {
        LOG.info("Creating duck connection");
        DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
        this.connection = conn;

        // create table
        createTable();
    }

    public void createTable() throws SQLException {
        // create a table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE CELLVALUE(
                        -- LSL Script execution ID
                        EXECUTIONID VARCHAR,
                        -- LSL Action ID
                        ACTIONID VARCHAR,
                        -- Abstraction container ID
                        ABSTRACTIONID VARCHAR,
                        -- Arena Execution ID
                        ARENAID VARCHAR,
                        -- Sequence Sheet ID (typically name of the test)
                        SHEETID VARCHAR,
                        -- ID of the code as it appears in the code index
                        SYSTEMID VARCHAR,
                        -- A variant of the code depicted by the SYSTEMID above (e.g., mutant code)
                        VARIANTID VARCHAR,
                        -- ID of a particular adapter for the code depicted by the SYSTEMID above as generated part of the adaptation process
                        ADAPTERID VARCHAR,
                        -- Sequence coordinate X (i.e., part of a statement, >= 0)
                        X INT NOT NULL,
                        -- Sequence coordinate Y (i.e., statement, >= 0)
                        Y INT NOT NULL,
                        -- Observation type (e.g., 'value' for output, 'input_value' for input etc.)
                        TYPE VARCHAR,
                        -- Observation value (serialized)
                        VALUE VARCHAR,
                        -- Raw observation value (unserialized)
                        RAWVALUE VARCHAR,
                        -- Object type of observation value
                        VALUETYPE VARCHAR,
                        -- Timestamp of observation
                        LASTMODIFIED TIMESTAMP,
                        -- Execution time (i.e., for observation values)
                        EXECUTIONTIME BIGINT
                    )
                    """);

            LOG.info("Table created");
        }
    }

    public void storeSheet(String executionId, AdaptedImplementation adaptedImplementation, String testSig, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> sheet) throws SQLException {
        try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO CELLVALUE (EXECUTIONID, SYSTEMID, SHEETID, X, Y, TYPE, VALUE, VARIANTID, ADAPTERID, LASTMODIFIED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            for (Table.Cell<Integer, Integer, String> cell : sheet.getCells()) {
                insertStmt.setString(1, executionId);
                insertStmt.setString(2, adaptedImplementation.getAdaptee().getId());
                insertStmt.setString(3, testSig);
                insertStmt.setInt(4, cell.getColumnKey());
                insertStmt.setInt(5, cell.getRowKey());

                // column
                String col = "input_value";
                if(cell.getColumnKey() == 0) {
                    col = "value";
                } else if(cell.getColumnKey() == 1) {
                    col = "op";
                } else if(cell.getColumnKey() == 2) {
                    col = "service";
                }

                insertStmt.setString(6, col);
                insertStmt.setString(7, cell.getValue());
                insertStmt.setString(8, adaptedImplementation.getAdaptee().getVariantId());
                insertStmt.setString(9, adaptedImplementation.getAdapterId());
                insertStmt.setDate(10, new Date(System.currentTimeMillis()));
                insertStmt.addBatch();

                insertStmt.executeBatch();
            }
        }
    }

    public void storeMetricSheet(String executionId, AdaptedImplementation adaptedImplementation, String metricReportId, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object> sheet) throws SQLException {
        try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO CELLVALUE (EXECUTIONID, SYSTEMID, SHEETID, X, Y, TYPE, VALUE, VARIANTID, ADAPTERID, LASTMODIFIED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            for (Integer row : sheet.getRows()) {
                insertStmt.setString(1, executionId);
                insertStmt.setString(2, adaptedImplementation.getAdaptee().getId());
                insertStmt.setString(3, metricReportId);
                insertStmt.setInt(4, -1);
                insertStmt.setInt(5, -1);

                // FIXME column
                String col = (String) sheet.get(row, 1);

                insertStmt.setString(6, col);
                insertStmt.setString(7, Objects.toString(sheet.get(row, 0)));
                insertStmt.setString(8, adaptedImplementation.getAdaptee().getVariantId());
                insertStmt.setString(9, adaptedImplementation.getAdapterId());
                insertStmt.setDate(10, new Date(System.currentTimeMillis()));
                insertStmt.addBatch();

                insertStmt.executeBatch();
            }
        }
    }

    public void storeOracleSheet(String executionId, String testSig, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> sheet) throws SQLException {
        try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO CELLVALUE (EXECUTIONID, SYSTEMID, SHEETID, X, Y, TYPE, VALUE, VARIANTID, ADAPTERID, LASTMODIFIED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            for (Table.Cell<Integer, Integer, String> cell : sheet.getCells()) {
                insertStmt.setString(1, executionId);
                insertStmt.setString(2, "oracle");
                insertStmt.setString(3, testSig);
                insertStmt.setInt(4, cell.getColumnKey());
                insertStmt.setInt(5, cell.getRowKey());

                // column
                String col = "input_value";
                if(cell.getColumnKey() == 0) {
                    col = "value";
                } else if(cell.getColumnKey() == 1) {
                    col = "op";
                } else if(cell.getColumnKey() == 2) {
                    col = "service";
                }

                insertStmt.setString(6, col);
                insertStmt.setString(7, cell.getValue());
                insertStmt.setString(8, "oracle");
                insertStmt.setString(9, "0");
                insertStmt.setDate(10, new Date(System.currentTimeMillis()));
                insertStmt.addBatch();

                insertStmt.executeBatch();
            }
        }
    }

    public void storeStimulusSheet(String executionId, String testSig, Test test, TestInvocation testInvocation) throws SQLException {
        try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO CELLVALUE (EXECUTIONID, SYSTEMID, SHEETID, X, Y, TYPE, VALUE, VARIANTID, ADAPTERID, LASTMODIFIED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            // sheetId
            String sheetId = test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";
            // sheet body
            String systemId = "abstraction";
            insertStmt.setString(1, executionId);
            insertStmt.setString(2, systemId);
            insertStmt.setString(3, sheetId);
            insertStmt.setInt(4, -1);
            insertStmt.setInt(5, -1);
            insertStmt.setString(6, "stimulussheet");
            insertStmt.setString(7, test.getParsedSheet().getSheet().getBody());
            insertStmt.setString(8, systemId);
            insertStmt.setString(9, systemId);
            insertStmt.setDate(10, new Date(System.currentTimeMillis()));
            insertStmt.addBatch();

            // sheet body
            insertStmt.setString(1, executionId);
            insertStmt.setString(2, systemId);
            insertStmt.setString(3, sheetId);
            insertStmt.setInt(4, -1);
            insertStmt.setInt(5, -1);
            insertStmt.setString(6, "interface");
            insertStmt.setString(7, test.getParsedSheet().getSheet().getInterfaceSpecification());
            insertStmt.setString(8, systemId);
            insertStmt.setString(9, systemId);
            insertStmt.setDate(10, new Date(System.currentTimeMillis()));
            insertStmt.addBatch();

            insertStmt.executeBatch();
        }
    }

    public Path toParquet(String executionId) throws IOException, SQLException {
        Path tmpFile = Files.createTempFile(executionId, ".parquet");

        // based on https://duckdb.org/docs/sql/statements/pivot
        String selectSql = "SELECT * FROM CELLVALUE where EXECUTIONID ='" + executionId + "'";
        String copySql = "COPY ("+selectSql+") TO '"+tmpFile.toFile().getAbsolutePath()+"' (FORMAT PARQUET);";

        // run a query
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(copySql);
        }

        return tmpFile;
    }
}
