package de.uni_mannheim.swt.lasso.srm.olap;

import joinery.DataFrame;
import org.apache.arrow.adapter.jdbc.*;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.ipc.ArrowReader;

import org.duckdb.DuckDBConnection;
import org.duckdb.DuckDBResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Arrow/DuckDB-based querying of SRM-related data.
 *
 * @author Marcus Kessel
 *
 */
public class ArrowOlap {

    private static final Logger LOG = LoggerFactory
            .getLogger(ArrowOlap.class);

//    public VectorSchemaRoot queryArrow(JdbcTemplate jdbcTemplate, String sql, Object ... args) {
//        return jdbcTemplate.query(sql, resultSet -> {
//            try (BufferAllocator allocator = new RootAllocator()) {
//                try (ArrowVectorIterator iterator = JdbcToArrow.sqlToArrowVectorIterator(
//                             resultSet, allocator)) {
//                    while (iterator.hasNext()) {
//                        try (VectorSchemaRoot root = iterator.next()) {
//                            LOG.debug(root.contentToTSVString());
//
//                            return root;
//                        }
//                    }
//                }
//            } catch (SQLException | IOException e) {
//                LOG.warn("queryArrow failed", e);
//            }
//
//            return null;
//        }, args);
//    }

    /**
     * JDBC Ignite to Apache Arrow to DuckDB.
     * <p>
     * Demonstrates PIVOTING.
     * <p>
     * Note that the impl. pipeline is odd, since for some reason we cannot pivot directly on the Arrow stream (table copy is current workaround)
     *
     * @param preparedStatement
     * @return
     */
    public DataFrame queryDuckDB(PreparedStatement preparedStatement, boolean pivotByStatement) {
        BufferAllocator allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator,
                JdbcToArrowUtils.getUtcCalendar()).build();

        // Arrow -> DuckDB
        try (ResultSet resultSet = preparedStatement.executeQuery();
             ArrowArrayStream arrow_array_stream = ArrowArrayStream.allocateNew(allocator);) {
            // JDBC -> Arrow
            ArrowReader reader;
            try {
                ArrowVectorIterator it = JdbcToArrow.sqlToArrowVectorIterator(
                        resultSet, allocator);
                reader = new JdbcReader(allocator, it, config);
                reader.getVectorSchemaRoot();
            } catch (Throwable e) {
                LOG.warn("ArrowReader failed", e);

                throw  e;
            }

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
                }

                String pivSql = "PIVOT "+toTable+" ON SYSTEMID USING first(VALUE) ORDER BY STATEMENT";
                if(pivotByStatement) {
                    pivSql = "PIVOT "+toTable+" ON STATEMENT USING first(VALUE) ORDER BY SYSTEMID";
                }

                // run a query
                try (Statement stmt = conn.createStatement();
                     DuckDBResultSet rs = (DuckDBResultSet) stmt.executeQuery(pivSql)) {
                    DataFrame dataFrame = DataFrame.readSql(rs);
                    return dataFrame;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JDBC Ignite to Apache Arrow to DuckDB in order to write parquet files.
     *
     * @param path
     */
    public void writeParquet(PreparedStatement preparedStatement, String path) {
        BufferAllocator allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator,
                JdbcToArrowUtils.getUtcCalendar()).build();

        // Arrow -> DuckDB
        try (ResultSet resultSet = preparedStatement.executeQuery();
             ArrowArrayStream arrow_array_stream = ArrowArrayStream.allocateNew(allocator);) {
            // JDBC -> Arrow
            ArrowReader reader;
            try {
                ArrowVectorIterator it = JdbcToArrow.sqlToArrowVectorIterator(
                        resultSet, allocator);
                reader = new JdbcReader(allocator, it, config);
                reader.getVectorSchemaRoot();
            } catch (Throwable e) {
                LOG.warn("ArrowReader failed", e);

                throw  e;
            }

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
                }

                // based on https://duckdb.org/docs/sql/statements/pivot
                String pivSql = "WITH pivot_alias AS (PIVOT "+toTable+" ON SYSTEMID USING first(VALUE) ORDER BY STATEMENT) SELECT * FROM pivot_alias";
                String copySql = "COPY ("+pivSql+") TO '"+path+"' (FORMAT PARQUET);";

                // run a query
                try (Statement stmt = conn.createStatement()) {
                    boolean rs = stmt.execute(copySql);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JDBC Ignite to Apache Arrow to DuckDB in order to write parquet files.
     *
     * @param path
     */
    public void sqlToParquet(PreparedStatement preparedStatement, String path) {
        BufferAllocator allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator,
                JdbcToArrowUtils.getUtcCalendar()).build();

        // Arrow -> DuckDB
        try (ResultSet resultSet = preparedStatement.executeQuery();
             ArrowArrayStream arrow_array_stream = ArrowArrayStream.allocateNew(allocator);) {
            // JDBC -> Arrow
            ArrowReader reader;
            try {
                ArrowVectorIterator it = JdbcToArrow.sqlToArrowVectorIterator(
                        resultSet, allocator);
                reader = new JdbcReader(allocator, it, config);
                reader.getVectorSchemaRoot();
            } catch (Throwable e) {
                LOG.warn("ArrowReader failed", e);

                throw  e;
            }

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
                }

                String pivSql = "SELECT * FROM " + toTable;
                String copySql = "COPY ("+pivSql+") TO '"+path+"' (FORMAT PARQUET);";

                // run a query
                try (Statement stmt = conn.createStatement()) {
                    boolean rs = stmt.execute(copySql);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public DataFrame readParquetAsDataFrame(String path) {
        // DuckDB stuff
        try (DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
            String pivSql = "SELECT * FROM read_parquet('"+path+"')";

            // run a query
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(pivSql);
                DataFrame dataFrame = DataFrame.readSql(rs);

                return dataFrame;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
