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

import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import java.io.File;
import java.util.List;

/**
 * {@link AnalysisResult} writer.
 * 
 * @author Marcus Kessel
 *
 */
public class MavenArtifactWriterDownloadOnly implements ItemWriter<AnalysisResult> {

    private static final Logger LOG = LoggerFactory.getLogger(MavenArtifactWriterDownloadOnly.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(List<? extends AnalysisResult> analysisResults)
            throws Exception {
        //
        for (AnalysisResult analysisResult : analysisResults) {
            LOG.info(String.format("Successfully downloaded artifact '%s'", analysisResult.getMavenArtifact().toUri()));

            try {
                // write out version head
                File folder = analysisResult.getMavenArtifact().getSourceJar().getParentFile();

                FileUtils.write(new File(folder, "lasso_info"),
                        String.format("version_head:%s", analysisResult.getMavenArtifact().getVersionHead()), "UTF-8");
            } catch (Throwable e) {
                LOG.warn(String.format("Failed to write down lasso info for '%s'", analysisResult.getMavenArtifact().toUri()), e);
            }
        }
    }

}
