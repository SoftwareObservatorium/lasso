package de.uni_mannheim.swt.lasso.srm.olap;

import joinery.DataFrame;
import org.apache.arrow.adapter.jdbc.*;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;

import org.duckdb.DuckDBConnection;
import org.duckdb.DuckDBResultSet;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.*;

/**
 * Arrow/DuckDB-based querying of SRM-related data.
 *
 * @author Marcus Kessel
 *
 */
public class ArrowOlap {

    public VectorSchemaRoot queryArrow(JdbcTemplate jdbcTemplate, String sql, Object ... args) {
        return jdbcTemplate.query(sql, resultSet -> {
            try (BufferAllocator allocator = new RootAllocator()) {
                try (ArrowVectorIterator iterator = JdbcToArrow.sqlToArrowVectorIterator(
                             resultSet, allocator)) {
                    while (iterator.hasNext()) {
                        try (VectorSchemaRoot root = iterator.next()) {
                            System.out.print(root.contentToTSVString());

                            return root;
                        }
                    }
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }, args);
    }

    /**
     * JDBC Ignite to Apache Arrow to DuckDB.
     *
     * Demonstrates PIVOTING
     *
     * Note that the impl. pipeline is odd, since for some reason we cannot pivot directly on the Arrow stream (table copy is current workaround)
     *
     * @param jdbcTemplate
     * @param sql
     * @param args
     */
    public void queryDuckDB(JdbcTemplate jdbcTemplate, String sql, Object ... args) {
        BufferAllocator allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator,
                JdbcToArrowUtils.getUtcCalendar()).build();

        // JDBC -> Arrow
        ArrowReader reader = jdbcTemplate.query(sql, resultSet -> {
                try {
                    ArrowVectorIterator it = JdbcToArrow.sqlToArrowVectorIterator(
                            resultSet, allocator);
                    ArrowReader r = new JdbcReader(allocator, it, config);
                    r.getVectorSchemaRoot();

                    return r;
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }, args);

        // Arrow -> DuckDB
        try (ArrowArrayStream arrow_array_stream = ArrowArrayStream.allocateNew(allocator)) {
            Data.exportArrayStream(allocator, reader, arrow_array_stream);

            String fromTable = "asdf";
            String toTable = "bbb";

            // DuckDB stuff
            try (DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
                conn.registerArrowStream(fromTable, arrow_array_stream);

                // FIXME for some reason, PIVOT does not work on Arrow Stream
                // workaround is to copy table (view doesn't work either)
                String copyTableSql = "CREATE TABLE "+toTable+" AS select * from " + fromTable;

                // run a query
                try (Statement stmt = conn.createStatement()) {
                    boolean rs = stmt.execute(copyTableSql);
                    System.out.println("successful? " + rs);
                }

                String pivSql = "PIVOT "+toTable+" ON SYSTEMID USING first(VALUE) ORDER BY STATEMENT";

                // run a query
                try (Statement stmt = conn.createStatement();
                     DuckDBResultSet rs = (DuckDBResultSet) stmt.executeQuery(pivSql)) {
                    DataFrame dataFrame = DataFrame.readSql(rs);
                    System.out.println(dataFrame.toString());
                }

                pivSql = "PIVOT "+toTable+" ON STATEMENT USING first(VALUE) ORDER BY SYSTEMID";

                // run a query
                try (Statement stmt = conn.createStatement();
                     DuckDBResultSet rs = (DuckDBResultSet) stmt.executeQuery(pivSql)) {
                    DataFrame dataFrame = DataFrame.readSql(rs);
                    System.out.println(dataFrame.toString());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void queryDuckDBAllTypes(JdbcTemplate jdbcTemplate, String sql, Object ... args) {
        BufferAllocator allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator,
                JdbcToArrowUtils.getUtcCalendar()).build();

        // JDBC -> Arrow
        ArrowReader reader = jdbcTemplate.query(sql, resultSet -> {
            try {
                ArrowVectorIterator it = JdbcToArrow.sqlToArrowVectorIterator(
                        resultSet, allocator);
                ArrowReader r = new JdbcReader(allocator, it, config);
                r.getVectorSchemaRoot();

                return r;
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }, args);

        // Arrow -> DuckDB
        try (ArrowArrayStream arrow_array_stream = ArrowArrayStream.allocateNew(allocator)) {
            Data.exportArrayStream(allocator, reader, arrow_array_stream);

            String fromTable = "asdf";
            String toTable = "bbb";

            // DuckDB stuff
            try (DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
                conn.registerArrowStream(fromTable, arrow_array_stream);

                // FIXME for some reason, PIVOT does not work on Arrow Stream
                // workaround is to copy table (view doesn't work either)
                String copyTableSql = "CREATE TABLE "+toTable+" AS select * from " + fromTable;

                // run a query
                try (Statement stmt = conn.createStatement()) {
                    boolean rs = stmt.execute(copyTableSql);
                    System.out.println("successful? " + rs);
                }

                String pivSql = "PIVOT "+toTable+" ON SYSTEMID,TYPE USING first(VALUE) ORDER BY STATEMENT";

                // run a query
                try (Statement stmt = conn.createStatement();
                     DuckDBResultSet rs = (DuckDBResultSet) stmt.executeQuery(pivSql)) {
                    DataFrame dataFrame = DataFrame.readSql(rs);
                    System.out.println(dataFrame.toString());
                }

                pivSql = "PIVOT "+toTable+" ON STATEMENT,TYPE USING first(VALUE) ORDER BY SYSTEMID";

                // run a query
                try (Statement stmt = conn.createStatement();
                     DuckDBResultSet rs = (DuckDBResultSet) stmt.executeQuery(pivSql)) {
                    DataFrame dataFrame = DataFrame.readSql(rs);
                    System.out.println(dataFrame.toString());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JDBC Ignite to Apache Arrow to DuckDB
     *
     * @param jdbcTemplate
     * @param sql
     * @param args
     */
    public void writeParquet(JdbcTemplate jdbcTemplate, String sql, Object ... args) {
        // pass to duckdb?
        // FIXME see https://duckdb.org/docs/api/java
        BufferAllocator allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator,
                JdbcToArrowUtils.getUtcCalendar()).build();

        ArrowReader reader = jdbcTemplate.query(sql, resultSet -> {
            try {
                ArrowVectorIterator it = JdbcToArrow.sqlToArrowVectorIterator(
                        resultSet, allocator);
                ArrowReader r = new JdbcReader(allocator, it, config);
                r.getVectorSchemaRoot();

                return r;
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }, args);

        try (ArrowArrayStream arrow_array_stream = ArrowArrayStream.allocateNew(allocator)) {
            Data.exportArrayStream(allocator, reader, arrow_array_stream);

            // DuckDB stuff
            try (DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
                String table = "asdf";
                conn.registerArrowStream(table, arrow_array_stream);

                //String pivSql = "select count(*) from asdf";
                String pivSql = "COPY "+table+" TO '/tmp/output.parquet' (FORMAT PARQUET);";
                //String pivSql = "PIVOT asdf ON SYSTEMID USING first(VALUE)";

                // run a query
                try (Statement stmt = conn.createStatement()) {
                    boolean rs = stmt.execute(pivSql);
                    System.out.println("successful? " + rs);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void read() {
        // DuckDB stuff
        try (DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
            String table = "asdf";

            // run a query
            try (Statement stmt = conn.createStatement()) {
                boolean rs = stmt.execute("CREATE TABLE "+table+" AS SELECT * FROM read_parquet('/tmp/output.parquet')");
                System.out.println("successful? " + rs);
            }

            //String pivSql = "select count(*) from asdf";
            String pivSql = "PIVOT "+table+" ON SYSTEMID USING first(VALUE) ORDER BY STATEMENT";

            // run a query
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(pivSql);
                DataFrame dataFrame = DataFrame.readSql(rs);
                System.out.println(dataFrame.toString());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
