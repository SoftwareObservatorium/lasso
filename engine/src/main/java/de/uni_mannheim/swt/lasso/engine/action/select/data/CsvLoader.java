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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class CsvLoader {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvLoader.class);

    public static List<String> load(File csvFile) {
        Table csv = Table.read().csv(csvFile);
        try {
            return (List<String>) csv.column("SYSTEMID").asList();
        } catch (Throwable e) {
            LOG.warn("Could not load {}", csvFile);
            LOG.warn("Stack:", e);

            return new LinkedList<>();
        }
    }

    public static List<String> loadFile(String csvFile) {
        Table csv = Table.read().csv(csvFile);
        try {
            return (List<String>) csv.column("SYSTEMID").asList();
        } catch (Throwable e) {
            LOG.warn("Could not load {}", csvFile);
            LOG.warn("Stack:", e);

            return new LinkedList<>();
        }
    }

    public static List<String> load(String content) {
        Table csv = Table.read().csv(content, "CSV");
        try {
            return (List<String>) csv.column("SYSTEMID").asList();
        } catch (Throwable e) {
            LOG.warn("Could not load {}", content);
            LOG.warn("Stack:", e);

            return new LinkedList<>();
        }
    }
}
