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

import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.File;

/**
 * StarCoder benchmarks (Java only).
 *
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://huggingface.co/datasets/bigcode/MultiPL-E-completions/">MultiPL-E-completions</a>
 */
public class StarCoder {

    //private final Configuration testConf = new Configuration(false);

    public void read() {
        File dir = new File("/home/marcus/development/repositories/huggingface/MultiPL-E-completions/data");

        File parquet = new File(dir, "humaneval.java.davinci.0.2.reworded-00000-of-00001-2f05972f87ef7add.parquet");

        Path path = new Path("file://" + parquet.getAbsolutePath());

        try(ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(path).build()) {
            GenericRecord nextRecord = reader.read();

            //nextRecord.

            System.out.println(nextRecord);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

//    public void readArrow() {
//        File dir = new File("/home/marcus/development/repositories/huggingface/MultiPL-E-completions/data");
//
//        File parquet = new File(dir, "humaneval.java.davinci.0.2.reworded-00000-of-00001-2f05972f87ef7add.parquet");
//
//        ScanOptions options = new ScanOptions(/*batchSize*/ 32768);
//        try (
//                BufferAllocator allocator = new RootAllocator();
//                DatasetFactory datasetFactory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, parquet.getAbsolutePath());
//                Dataset dataset = datasetFactory.finish();
//                Scanner scanner = dataset.newScan(options);
//                ArrowReader reader = scanner.scanBatches()
//        ) {
//            while (reader.loadNextBatch()) {
//                try (VectorSchemaRoot root = reader.getVectorSchemaRoot()) {
//                    System.out.print(root.contentToTSVString());
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
