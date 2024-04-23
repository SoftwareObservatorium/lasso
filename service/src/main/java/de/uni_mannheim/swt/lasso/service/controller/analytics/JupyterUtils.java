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
package de.uni_mannheim.swt.lasso.service.controller.analytics;

/**
 *
 *
 * @author Marcus Kessel
 */
public class JupyterUtils {

    /**
     * Generate a Jupyter notebook
     *
     * @param fileDownloadUri
     * @return
     */
    public static String createSrmNotebook(String fileDownloadUri) {
        String notebook = "{\n" +
                "  \"metadata\": {\n" +
                "    \"kernelspec\": {\n" +
                "      \"name\": \"python\",\n" +
                "      \"display_name\": \"Python (Pyodide)\",\n" +
                "      \"language\": \"python\"\n" +
                "    },\n" +
                "    \"language_info\": {\n" +
                "      \"codemirror_mode\": {\n" +
                "        \"name\": \"python\",\n" +
                "        \"version\": 3\n" +
                "      },\n" +
                "      \"file_extension\": \".py\",\n" +
                "      \"mimetype\": \"text/x-python\",\n" +
                "      \"name\": \"python\",\n" +
                "      \"nbconvert_exporter\": \"python\",\n" +
                "      \"pygments_lexer\": \"ipython3\",\n" +
                "      \"version\": \"3.8\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"nbformat_minor\": 4,\n" +
                "  \"nbformat\": 4,\n" +
                "  \"cells\": [\n" +
                "    {\n" +
                "      \"cell_type\": \"code\",\n" +
                "      \"source\": \"# install fastparquet to read parquet files\\n%pip install fastparquet\\n\\nimport pyodide_http\\nimport pandas as pd\\n\\npyodide_http.patch_all()\\n\\ndf2 = pd.read_parquet('"+fileDownloadUri+"')\\n\\ndf2\",\n" +
                "      \"metadata\": {\n" +
                "        \"trusted\": true\n" +
                "      },\n" +
                "      \"outputs\": [],\n" +
                "      \"execution_count\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"cell_type\": \"code\",\n" +
                "      \"source\": \"\",\n" +
                "      \"metadata\": {\n" +
                "        \"trusted\": true\n" +
                "      },\n" +
                "      \"outputs\": [],\n" +
                "      \"execution_count\": null\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        return notebook;
    }
}
