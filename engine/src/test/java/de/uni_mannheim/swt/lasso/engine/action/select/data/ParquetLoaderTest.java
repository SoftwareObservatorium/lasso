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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 *
 * @author Marcus Kessel
 */
public class ParquetLoaderTest {

    @Test
    public void test() throws SQLException, IOException {
        File dir = new File("/home/marcus/development/playground/data/problems_fe_only");
        String problemId = "HumanEval_13_greatest_common_divisor";
        File file = new File(dir, problemId + ".parquet");

        List<String> systemIds = ParquetLoader.load(file);
        System.out.println(systemIds);

        assertFalse(systemIds.isEmpty());
    }

    @Test
    public void testListAll() {
        File dir = new File("/home/marcus/development/playground/data/problems_fe_only");
        File[] files = dir.listFiles((d, name) -> name.contains(".parquet"));

        Arrays.stream(files).forEach(file -> {
            try {
                ParquetLoader.load(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
