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
package de.uni_mannheim.swt.lasso.analyzer.batch.writer;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import de.uni_mannheim.swt.lasso.analyzer.index.CompilationUnitRepository;

/**
 * {@link AnalysisResult} writer.
 *
 * @author Marcus Kessel
 */
public class MavenArtifactWriter implements ItemWriter<AnalysisResult> {

    private static final Logger LOG = LoggerFactory.getLogger(MavenArtifactWriter.class);

    private final CompilationUnitRepository repository;

    private int maxThreads = 4;

    /**
     * Constructor
     *
     * @param repository {@link CompilationUnitRepository} instance
     */
    public MavenArtifactWriter(CompilationUnitRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(List<? extends AnalysisResult> analysisResults)
            throws Exception {
        //
        if (CollectionUtils.isEmpty(analysisResults)) {
            return;
        }

        // in parallel
        if (maxThreads > 0) {
            ForkJoinPool customThreadPool = new ForkJoinPool(getMaxThreads());
            customThreadPool.submit(
                    () -> analysisResults.parallelStream().forEach(analysisResult -> {
                        LOG.info(String.format("Persisting %s", analysisResult.getMavenArtifact().toUri()));

                        try {
                            repository.save(analysisResult);

                            LOG.info(String.format("Persisting successful %s", analysisResult.getMavenArtifact().toUri()));
                        } catch (Throwable e) {
                            LOG.warn(String.format("Persisting failed for %s", analysisResult.getMavenArtifact().toUri()), e);
                        }
                    })).get();
        } else {
            for (AnalysisResult analysisResult : analysisResults) {
                LOG.info(String.format("Persisting %s", analysisResult.getMavenArtifact().toUri()));

                try {
                    repository.save(analysisResult);

                    LOG.info(String.format("Persisting successful %s", analysisResult.getMavenArtifact().toUri()));
                } catch (Throwable e) {
                    LOG.warn(String.format("Persisting failed for %s", analysisResult.getMavenArtifact().toUri()), e);
                }
            }
        }

        // free memory instantly
        analysisResults.stream().forEach(ar -> ar.getCompilationUnits().clear());
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

}
