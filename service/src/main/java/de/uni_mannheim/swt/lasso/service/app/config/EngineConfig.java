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
package de.uni_mannheim.swt.lasso.service.app.config;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.compute.LassoComputeEngine;
import de.uni_mannheim.swt.lasso.cluster.worker.LassoActionRequestEngine;
import de.uni_mannheim.swt.lasso.cluster.config.SpringLassoConfiguration;
import de.uni_mannheim.swt.lasso.cluster.worker.WorkerApplication;
import de.uni_mannheim.swt.lasso.core.srm.SRM;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.engine.LassoEngine;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.service.LassoManager;
import de.uni_mannheim.swt.lasso.service.app.ShutdownManager;
import de.uni_mannheim.swt.lasso.service.controller.file.FileStorageService;
import de.uni_mannheim.swt.lasso.service.notification.NotificationService;
import de.uni_mannheim.swt.lasso.service.persistence.UserRepository;
import de.uni_mannheim.swt.lasso.service.persistence.ScriptJobRepository;

import de.uni_mannheim.swt.lasso.srm.DefaultSRM;
import de.uni_mannheim.swt.lasso.srm.SRMManager;
import de.uni_mannheim.swt.lasso.srm.operators.FunctionalCorrectness;
import de.uni_mannheim.swt.lasso.srm.operators.HeuristicsBasedCorrectness;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.cluster.ClusterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Basic engine config.
 *
 * @author Marcus Kessel
 */
@Configuration
public class EngineConfig {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private Environment env;

    @Value("${cluster.addresses}")
    private String addressList;

    @Value("${cluster.failureDetectionTimeoutInSecs}")
    private int failureDetectionTimeoutInSecs;

    @Value("${pipeline.jobs}")
    private int pipelineJobs;

    /**
     * is embedded mode?
     *
     * @return
     */
    public boolean isEmbeddedMode() {
        return env.getProperty("cluster.embedded", boolean.class, false);
    }

    /**
     * Friendly shutdown hook.
     *
     * @return
     */
    @Bean
    public ShutdownManager shutdownManager() {
        return new ShutdownManager();
    }

    @Bean
    public LassoConfiguration lassoConfiguration() {
        return new SpringLassoConfiguration(applicationContext);
    }

    @Bean
    //@Primary
    public LassoEngine lassoEngine(LassoConfiguration lassoConfiguration,
                                   ActionManager actionManager,
                                   WorkspaceManager workspaceManager,
                                   BenchmarkManager benchmarkManager,
                                   ExecutionEnvironmentManager executionEnvironmentManager,
                                   ClusterEngine clusterEngine) {
        if(isEmbeddedMode()) {
            // setup worker application
            WorkerApplication.config = new SpringLassoConfiguration(applicationContext);
            // assumes LassoActionRequestEngine bean
        }

        return new LassoComputeEngine(lassoConfiguration, actionManager, workspaceManager, executionEnvironmentManager, benchmarkManager, clusterEngine);
    }

    @Bean
    @ConditionalOnProperty(
            name = "cluster.embedded",
            havingValue = "true")
    public LassoActionRequestEngine lassoActionRequestEngine(LassoConfiguration lassoConfiguration,
                                                             WorkspaceManager workspaceManager,
                                                             ExecutionEnvironmentManager executionEnvironmentManager,
                                                             BenchmarkManager benchmarkManager,
                                                             ClusterEngine clusterEngine) {
        return new LassoActionRequestEngine(lassoConfiguration, workspaceManager, executionEnvironmentManager, benchmarkManager, clusterEngine);
    }

    @Bean
    public LassoManager lassoManager(LassoEngine lassoEngine, LassoConfiguration lassoConfiguration, WorkspaceManager workspaceManager,
                                     UserRepository userRepository,
                                     ScriptJobRepository scriptJobRepository) {
        LassoManager lassoManager = new LassoManager(lassoEngine, lassoConfiguration);
        lassoManager.setPipelineJobs(pipelineJobs);
        lassoManager.setWorkspaceManager(workspaceManager);
        lassoManager.setUserRepository(userRepository);
        lassoManager.setScriptJobRepository(scriptJobRepository);

        NotificationService notificationService = applicationContext.getBean(NotificationService.class);
        lassoManager.setNotificationService(notificationService);

        return lassoManager;
    }

    @Bean
    public ActionManager actionManager() {
        ActionManager actionManager = new ActionManager();
        return actionManager;
    }

    @Bean
    public BenchmarkManager benchmarkManager() {
        BenchmarkManager benchmarkManager = new BenchmarkManager();
        return benchmarkManager;
    }

    @Bean
    public WorkspaceManager workspaceManager() {
        File root = new File(env.getRequiredProperty("lasso.workspace.root"));

        WorkspaceManager workspaceManager = new WorkspaceManager(root);
        return workspaceManager;
    }

    @Bean
    public ExecutionEnvironmentManager executionEnvironmentManager() {
        // populate system properties for docker
        System.setProperty("thirdparty.docker.uid", env.getRequiredProperty("thirdparty.docker.uid"));
        System.setProperty("thirdparty.docker.gid", env.getRequiredProperty("thirdparty.docker.gid"));

        String proxyRegistry = env.getRequiredProperty("thirdparty.docker.proxyRegistry");
        Integer pullTimeout = env.getRequiredProperty("thirdparty.docker.pullTimeout", Integer.class);
        String mavenDefaultImage = env.getRequiredProperty("thirdparty.docker.image.default");

        ExecutionEnvironmentManager executionEnvironmentManager = new ExecutionEnvironmentManager(proxyRegistry, pullTimeout, mavenDefaultImage);

        return executionEnvironmentManager;
    }

    @Bean(destroyMethod = "close")
    public ClusterEngine clusterEngine() {
        ClusterEngine clusterEngine;
        if(isEmbeddedMode()) {
            clusterEngine = new ClusterEngine(env.getRequiredProperty("cluster.nodeId"),
                    env.getRequiredProperty("cluster.role"),
                    env.getRequiredProperty("cluster.ip"),
                    env.getRequiredProperty("cluster.multicast.ip")) {

                /**
                 * Master == Worker node
                 *
                 * @return
                 */
                @Override
                public ClusterGroup getWorkerNodes() {
                    return getMasterNode();
                }
            };
        } else {
            clusterEngine = new ClusterEngine(env.getRequiredProperty("cluster.nodeId"),
                    env.getRequiredProperty("cluster.role"),
                    env.getRequiredProperty("cluster.ip"),
                    env.getRequiredProperty("cluster.multicast.ip"));
        }

        clusterEngine.setFailureDetectionTimeout(failureDetectionTimeoutInSecs * 1000L);
        String[] addrs = StringUtils.split(addressList, ",");
        List<String> remoteAddresses = Arrays.asList(addrs);
        if(CollectionUtils.isNotEmpty(remoteAddresses)) {
            clusterEngine.setRemoteAddresses(remoteAddresses);
        }

        clusterEngine.setParallelJobsNumber(env.getProperty("master.jobs.parallel", Integer.class));

        // start
        clusterEngine.start();

        return clusterEngine;
    }

    @Bean
    public FileStorageService fileStorageService() {
        return new FileStorageService(env.getRequiredProperty("lasso.file.upload.root"));
    }

    /**
     * Default implementation for {@link FunctionalCorrectness}.
     *
     * @return
     */
    @Bean
    public FunctionalCorrectness functionalCorrectness() {
        HeuristicsBasedCorrectness correctness = new HeuristicsBasedCorrectness();
        return correctness;
    }

    /**
     * SRM service for script usage
     *
     * @param clusterEngine
     * @param correctness
     * @return
     */
    @Bean
    public SRM srm(ClusterEngine clusterEngine, FunctionalCorrectness correctness) {
        return new DefaultSRM(clusterEngine, correctness);
    }

    /**
     *
     *
     * @param clusterEngine
     * @param correctness
     * @return
     */
    @Bean
    public SRMManager srmManager(ClusterEngine clusterEngine, FunctionalCorrectness correctness) {
        return new SRMManager(clusterEngine.getClusterSRMRepository(), correctness);
    }
}
