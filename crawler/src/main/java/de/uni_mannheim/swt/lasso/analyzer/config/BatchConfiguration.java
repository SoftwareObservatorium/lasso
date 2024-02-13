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
package de.uni_mannheim.swt.lasso.analyzer.config;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import de.uni_mannheim.swt.lasso.analyzer.batch.processor.MavenArtifactProcessorDownloadOnly;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.SingleMavenArtifactReader;
import de.uni_mannheim.swt.lasso.analyzer.batch.writer.MavenArtifactWriterDownloadOnly;
import de.uni_mannheim.swt.lasso.analyzer.batch.MavenArtifactJobListener;
import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import de.uni_mannheim.swt.lasso.analyzer.batch.processor.MavenAetherResolver;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.LatestVersionArtifactReader;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.task.support.TaskExecutorAdapter;

/**
 * Spring Boot Batch Configuration
 * 
 * @author Marcus Kessel
 *
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration extends DefaultBatchConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(BatchConfiguration.class);

    @Value("${lasso.indexer.downloadonly}")
    private boolean downloadOnly;

    @Value("${artifacts}")
    private String artifacts;

    @Value("${lasso.indexer.worker.threads}")
    private int workerThreads;

    @Value("${batch.job.commit.interval}")
    private int commitInterval;

    @Value("${batch.maven.repo.url}")
    private String mavenRepoUrl;

    @Value("${batch.maven.repo.path}")
    private String mavenRepoPath;

    @Value("${batch.maven.index.url}")
    private String mavenIndexRepoUrl;

    @Value("${batch.maven.index.path}")
    private String mavenIndexPath;

    @Value("${batch.maven.index.update}")
    private boolean allowIndexUpdate;

    @Value("${batch.maven.resume.at.artifact}")
    private String resumeAtArtifact;

    @Value("${batch.maven.latest.head}")
    private int latestVersionHeadSize;

    @Value("${batch.maven.resume.enabled}")
    private boolean resumeEnabled;

    @Value("${batch.maven.mode.testartifact}")
    private boolean testArtifactMode;

    @Value("${batch.maven.mode.extension}")
    private String archiveExtension;

    @Resource
    private JobBuilderFactory jobBuilder;

    @Resource
    private StepBuilderFactory stepBuilder;

    @Bean
    public ItemReader<MavenArtifact> itemReader()
            throws IOException, PlexusContainerException, ComponentLookupException {
        ItemReader<MavenArtifact> reader;

        if(StringUtils.isNotBlank(artifacts)) {
            LOG.info("Direct download mode enabled. Identified the following artifacts\n{}", artifacts);

            reader = new SingleMavenArtifactReader(artifacts);
        } else {
            LOG.info("Index mode enabled");

            LatestVersionArtifactReader lReader = new LatestVersionArtifactReader(new File(mavenIndexPath),
                    mavenIndexRepoUrl, latestVersionHeadSize, allowIndexUpdate,
                    testArtifactMode);
            reader = lReader;

            lReader.setArchiveExtension(archiveExtension);

            if(downloadOnly) {
                // in download only no resume support (disables Solr lookups!)
                lReader.setResumeEnabled(false);
            } else {
                // resume support enabled?
                lReader.setResumeEnabled(resumeEnabled);
            }
        }

        return reader;
    }

    @Bean
    public ItemProcessor<MavenArtifact, AnalysisResult> itemProcessor() throws IOException {
        // resolver
        MavenAetherResolver resolver = new MavenAetherResolver(mavenRepoPath, mavenRepoUrl);

        return new MavenArtifactProcessorDownloadOnly(resolver);
    }

    @Bean
    public ItemWriter<AnalysisResult> itemWriter() {
        return new MavenArtifactWriterDownloadOnly();
    }

    @Bean
    public Job mavenArtifactJob(JobBuilderFactory jobs, Step s1, JobExecutionListener listener) {
        return jobs.get("mavenArtifactJob").incrementer(new RunIdIncrementer()).listener(listener).flow(s1).end()
                .build();
    }

    @Bean
    public Step step1(StepBuilderFactory stepBuilderFactory, ItemReader<MavenArtifact> reader,
            ItemProcessor<MavenArtifact, AnalysisResult> processor, ItemWriter<AnalysisResult> writer) {
        ExecutorService workerExecutor = Executors.newFixedThreadPool(workerThreads,
                new ThreadFactory() {

                    private AtomicInteger inc = new AtomicInteger();

                    /**
                     * Assign a specific name to the worker threads
                     */
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                "analysis-worker-" + inc.getAndIncrement());
                    }

                });

        return stepBuilderFactory.get("mavenArtifactJob_step1").<MavenArtifact, AnalysisResult>chunk(commitInterval)
                .reader(reader).processor(processor).writer(writer)
                // task executor
                .taskExecutor(new TaskExecutorAdapter(
                        workerExecutor)).throttleLimit(workerThreads)
                .build();
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new MavenArtifactJobListener();
    }
}
