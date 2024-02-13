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

import de.uni_mannheim.swt.lasso.analyzer.asm.ASMAnalyzer;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.LocalArtifactReader;
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

import de.uni_mannheim.swt.lasso.analyzer.batch.MavenArtifactJobListener;
import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import de.uni_mannheim.swt.lasso.analyzer.batch.processor.MavenArtifactProcessor;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.batch.writer.MavenArtifactWriter;
import de.uni_mannheim.swt.lasso.analyzer.index.CompilationUnitRepository;
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

    @Value("${lasso.indexer.worker.threads}")
    private int workerThreads;

    @Value("${batch.job.commit.interval}")
    private int commitInterval;

    @Value("${batch.job.writer.threads}")
    private int writerThreads;

    @Value("${batch.maven.repo.path}")
    private String mavenRepoPath;

    @Resource
    private JobBuilderFactory jobBuilder;

    @Resource
    private StepBuilderFactory stepBuilder;

    @Resource
    private CompilationUnitRepository compilationUnitRepository;

    @Bean
    public ItemReader<MavenArtifact> itemReader()
            throws IOException {
        return new LocalArtifactReader(new File(mavenRepoPath));
    }

    @Bean
    public ItemProcessor<MavenArtifact, AnalysisResult> itemProcessor() throws IOException {
        // resolver

        // init ProjectAnalyzer, default settings
        boolean excludeJavaLangRootObject = true;
        boolean ignoreAnonymousClasses = true;

        ASMAnalyzer asmAnalyzer = new ASMAnalyzer(excludeJavaLangRootObject, ignoreAnonymousClasses);

        return new MavenArtifactProcessor(asmAnalyzer);
    }

    @Bean
    public ItemWriter<AnalysisResult> itemWriter(CompilationUnitRepository repository) {
        MavenArtifactWriter mavenArtifactWriter = new MavenArtifactWriter(repository);

        mavenArtifactWriter.setMaxThreads(writerThreads);

        return mavenArtifactWriter;
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
