package de.uni_mannheim.swt.lasso.srm.olap;

import de.uni_mannheim.swt.lasso.srm.JDBC;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author Marcus Kessel
 */
public class Warehouse {

    public static String sqlAllTypes = "SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID, '_', VARIANTID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ?";

    public static void writeSrm(String executionId, String type, File path) throws SQLException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = sqlAllTypes + " and type = ? order by sheetid";

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);
            preparedStatement.setString(2, type);

            olap.writeParquet(preparedStatement, path.getAbsolutePath());
        }
    }

    public static Resource writeSrmResource(String executionId, String type) throws SQLException, IOException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        Path tmpFile = Files.createTempFile(executionId, ".parquet");

        String sql = sqlAllTypes + " and type = ? order by sheetid";

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);
            preparedStatement.setString(2, type);

            olap.writeParquet(preparedStatement, tmpFile.toFile().getAbsolutePath());
        }

        Resource resource = new UrlResource(tmpFile.toUri());

        return resource;
    }

    public static Resource writeSrmResource(String executionId) throws SQLException, IOException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = sqlAllTypes + " order by sheetid";

        Path tmpFile = Files.createTempFile(executionId, ".parquet");

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);

            olap.writeParquet(preparedStatement, tmpFile.toFile().getAbsolutePath());
        }

        Resource resource = new UrlResource(tmpFile.toUri());

        return resource;
    }

    public static Resource writeRawSrmResource(String executionId) throws SQLException, IOException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT * FROM srm.cellvalue where executionid = ?";

        Path tmpFile = Files.createTempFile(executionId, ".parquet");

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);

            olap.sqlToParquet(preparedStatement, tmpFile.toFile().getAbsolutePath());
        }

        Resource resource = new UrlResource(tmpFile.toUri());

        return resource;
    }
}
