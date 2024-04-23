package de.uni_mannheim.swt.lasso.srm.olap;

import de.uni_mannheim.swt.lasso.srm.JDBC;
import de.uni_mannheim.swt.lasso.srm.SRHRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 *
 * @author Marcus Kessel
 */
public class Warehouse {

    public static void writeSrm(String executionId, String type, File path) throws SQLException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ? order by sheetid";
        Object[] args = {executionId, SRHRepository.ARENA_DEFAULT, type};

        olap.writeParquet(jdbc.getJdbcTemplate(), sql, path.getAbsolutePath(), args);
    }

    public static Resource writeSrmResource(String executionId, String type) throws SQLException, IOException {
        JDBC jdbc = new JDBC();
        ArrowOlap olap = new ArrowOlap();

        String sql = "SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, CONCAT(SYSTEMID,'_',ADAPTERID) as SYSTEMID, VALUE FROM srm.cellvalue where executionid = ? and arenaid = ? and type = ? order by sheetid";
        Object[] args = {executionId, SRHRepository.ARENA_DEFAULT, type};

        Path tmpFile = Files.createTempFile(executionId, ".parquet");

        olap.writeParquet(jdbc.getJdbcTemplate(), sql, tmpFile.toFile().getAbsolutePath(), args);

        Resource resource = new UrlResource(tmpFile.toUri());

        return resource;
    }
}
