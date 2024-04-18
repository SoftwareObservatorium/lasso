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
package de.uni_mannheim.swt.lasso.engine.action.select.data;

import de.uni_mannheim.swt.lasso.data.DuckDbLoader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Simple loader for parquet files based on DuckDb
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://duckdb.org/docs/data/parquet/overview">DuckDb</a>
 * @see DuckDbLoader
 */
public class ParquetLoader {

    /**
     *
     * @param data
     * @return
     * @throws IOException
     * @throws SQLException
     */
    public static List<String> load(File data) throws IOException, SQLException {
        DuckDbLoader loader = new DuckDbLoader();
        String parquetFile = StringUtils.wrap(data.getAbsolutePath(), "'");
        List<String> systemIds = loader.getJdbcTemplate().queryForList("select SYSTEMID from " + parquetFile, String.class);

        return systemIds;
    }
}
