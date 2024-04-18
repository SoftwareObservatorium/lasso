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
package de.uni_mannheim.swt.lasso.llm.problem;

import de.uni_mannheim.swt.lasso.llm.eval.ExecutedSolution;
import de.uni_mannheim.swt.lasso.llm.eval.Results;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * StarCoder benchmarks (Java only).
 *
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://huggingface.co/datasets/bigcode/MultiPL-E-completions/">MultiPL-E-completions</a>
 */
public class MultiPLECompletions {

    public List<Results> read(File parquet) {
        Path path = new Path("file://" + parquet.getAbsolutePath());

        try(ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(path).build()) {
            List<Results> resultsList = new LinkedList<>();
            GenericRecord nextRecord;
            while((nextRecord = reader.read()) != null) {
                System.out.println(nextRecord.getSchema());

                Results results = new Results();
                results.setLanguage(nextRecord.get("language").toString());
                results.setName(parquet.getName());
                results.setProblem(nextRecord.get("problem").toString());
                results.setTests(nextRecord.get("tests").toString());
                results.setTop_p((Double) nextRecord.get("top_p"));
                results.setMax_tokens((Long) nextRecord.get("max_tokens"));
                results.setExperiment(nextRecord.get("experiment").toString());
                results.setPrompt(nextRecord.get("prompt").toString());

                List<GenericData.Record> completions = (List<GenericData.Record>) nextRecord.get("completions");
                List<GenericData.Record> programs = (List<GenericData.Record>) nextRecord.get("programs");
                List<GenericData.Record> stdouts = (List<GenericData.Record>) nextRecord.get("stdouts");
                List<GenericData.Record> stderrs = (List<GenericData.Record>) nextRecord.get("stderrs");
                List<GenericData.Record> exit_codes = (List<GenericData.Record>) nextRecord.get("exit_codes");
                List<GenericData.Record> statuses = (List<GenericData.Record>) nextRecord.get("statuses");
                List<GenericData.Record> timestamps = (List<GenericData.Record>) nextRecord.get("timestamps");

                results.setResults(new ArrayList<>(completions.size()));

                int field = 0;

                // iterate (assumes consistency across sequences)
                for(int i = 0; i < completions.size(); i++) {
                    ExecutedSolution executedSolution = new ExecutedSolution();
                    executedSolution.setProgram(programs.get(i).get(field).toString());
                    executedSolution.setStatus(statuses.get(i).get(field).toString());
                    executedSolution.setExit_code(((Long)exit_codes.get(i).get(field)).intValue());
                    executedSolution.setStdout(stdouts.get(i).get(field).toString());
                    executedSolution.setStderr(stderrs.get(i).get(field).toString());
                    executedSolution.setTimestamp((Long) timestamps.get(i).get(field));

                    results.getResults().add(executedSolution);
                }

                resultsList.add(results);
            }

            return resultsList;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
