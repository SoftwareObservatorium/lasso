package de.uni_mannheim.swt.lasso.srm.olap;

import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.srm.JDBC;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class Warehouse {

    public static String sqlAllTypes = "SELECT CONCAT(SHEETID,'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID, '_', VARIANTID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ?";

    public static List<Sheet> queryStimulusSheets(String executionId, String actionId, String abstractionId) throws SQLException, IOException {
        JDBC jdbc = new JDBC();
        //ArrowOlap olap = new ArrowOlap();

        String sql1 = "SELECT distinct(type) FROM srm.cellvalue where type = 'interface' and executionid = ? and actionid = ? and abstractionid = ?";

        Table table1 = jdbc.sqlToTable(sql1, new Object[]{executionId, actionId, abstractionId});
        String interfaceLql = (String) table1.get(0,0);

        // FIXME distinct? more than one arenaid possible (e.g., jacoco)
        String sql2 = "SELECT * FROM srm.cellvalue where type = 'stimulussheet' and executionid = ? and actionid = ? and abstractionid = ?";

        Table table2 = jdbc.sqlToTable(sql2, new Object[]{executionId, actionId, abstractionId});
        List<Sheet> stimulusSheets = new LinkedList<>();
        for(Row row : table2) {
            String signature = row.getString("SHEETID");
            String body = row.getString("VALUE");
            Sheet stimulusSheet = new Sheet(signature, body, interfaceLql);
            stimulusSheets.add(stimulusSheet);
        }

        return stimulusSheets;
    }

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

    public static void writeRawSrmToFile(String executionId, File file) throws SQLException, IOException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT * FROM srm.cellvalue where executionid = ?";

        try (PreparedStatement preparedStatement = jdbc.createPreparedStatement(sql)) {
            preparedStatement.setString(1, executionId);

            olap.sqlToParquet(preparedStatement, file.getAbsolutePath());
        }
    }
}
