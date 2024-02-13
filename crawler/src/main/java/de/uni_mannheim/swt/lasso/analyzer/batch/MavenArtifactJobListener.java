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
package de.uni_mannheim.swt.lasso.analyzer.batch;

import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;

/**
 * Job listener for {@link MavenArtifact}s.
 * 
 * @author Marcus Kessel
 *
 */
public class MavenArtifactJobListener extends JobExecutionListenerSupport {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenArtifactJobListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        //
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Job finished " + jobExecution.getJobConfigurationName());
            }
        }
    }

}
