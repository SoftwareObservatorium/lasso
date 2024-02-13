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
package de.uni_mannheim.swt.lasso.llm.eval;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * @author Marcus Kessel
 */
public class EvalReader {

    public Results getResults(File path, String generator, String problem) throws IOException {
        File subPath = new File(new File(path, generator), problem + ".results.json.gz");

        return getResults(subPath);
    }

    public Results getResults(File path) throws IOException {
        String json = IOUtils.toString(new GZIPInputStream(FileUtils.openInputStream(path)), StandardCharsets.UTF_8.name());

        return getResultsFromJson(json);
    }

    public Results getResultsFromJson(String json) {
        Gson gson = new Gson();
        Results results = gson.fromJson(json, Results.class);

        return results;
    }
}
