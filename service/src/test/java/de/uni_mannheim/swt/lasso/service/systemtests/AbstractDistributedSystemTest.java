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
package de.uni_mannheim.swt.lasso.service.systemtests;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.config.SpringLassoConfiguration;
import de.uni_mannheim.swt.lasso.cluster.worker.LassoActionRequestEngine;
import de.uni_mannheim.swt.lasso.cluster.worker.WorkerApplication;
import de.uni_mannheim.swt.lasso.engine.*;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.service.app.LassoApplication;
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine;
import org.apache.ignite.cluster.ClusterGroup;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Allows to run integration tests with Spring boot.
 *
 * @author Marcus Kessel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {LassoApplication.class}, properties = {"spring.main.allow-bean-definition-overriding=true"})
public abstract class AbstractDistributedSystemTest {

    /**
     * Override default LassoEngine
     */
    @Configuration
    static class ContextConfiguration {

        @Autowired
        private Environment env;

        @Bean("testLassoEngine")
        @Primary
        public LassoTestEngine testLassoEngine(LassoConfiguration lassoConfiguration,
                                               ActionManager actionManager,
                                               WorkspaceManager workspaceManager,
                                               ExecutionEnvironmentManager executionEnvironmentManager,
                                               BenchmarkManager benchmarkManager,
                                               ClusterEngine clusterEngine,
                                               @Qualifier("testClusterEngine") ClusterEngine testClusterEngine,
                                               ApplicationContext applicationContext) {
            // disable default ignite to work with only one node
            //clusterEngine.getIgnite().close();

            // setup worker application
            WorkerApplication.config = new SpringLassoConfiguration(applicationContext);

            return new LassoTestEngine(lassoConfiguration, actionManager, workspaceManager, executionEnvironmentManager, benchmarkManager, testClusterEngine);
        }

        @Bean
        @Primary
        public LassoActionRequestEngine lassoActionRequestEngine(LassoConfiguration lassoConfiguration,
                                                                 WorkspaceManager workspaceManager,
                                                                 ExecutionEnvironmentManager executionEnvironmentManager,
                                                                 BenchmarkManager benchmarkManager,
                                                                 @Qualifier("testClusterEngine") ClusterEngine clusterEngine) {
            return new LassoActionRequestEngine(lassoConfiguration, workspaceManager, executionEnvironmentManager, benchmarkManager, clusterEngine);
        }

        @Bean("testClusterEngine")
        @Primary
        public ClusterEngine clusterEngine() {
            // my cluster engine
            ClusterEngine testClusterEngine = new ClusterEngine("testNode", "master", "127.0.0.1", "228.1.2.55" /*change to different port than default*/) {

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

            testClusterEngine.setParallelJobsNumber(env.getProperty("master.jobs.parallel", Integer.class));

            // start
            testClusterEngine.start();

            return testClusterEngine;
        }
    }

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;
}

