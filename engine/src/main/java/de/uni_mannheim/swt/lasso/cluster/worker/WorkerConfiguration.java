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
package de.uni_mannheim.swt.lasso.cluster.worker;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.config.SpringLassoConfiguration;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Configuration
@PropertySource("classpath:application.properties")
public class WorkerConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(WorkerConfiguration.class);

	@Autowired
	ApplicationContext applicationContext;

	@Value("${cluster.nodeId}")
	private String nodeId;

	@Value("${cluster.role}")
	private String role;

	@Value("${cluster.multicast.ip}")
	private String multicastIp;

	@Value("${cluster.ip}")
	private String localAddress;

	@Value("${cluster.addresses}")
	private String addressList;

	@Value("${cluster.failureDetectionTimeoutInSecs}")
	private int failureDetectionTimeoutInSecs;

	@Autowired
	private Environment env;

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

	@Bean
	public SpringLassoConfiguration lassoConfiguration() {
		return new SpringLassoConfiguration(applicationContext);
	}

	@Bean
	public LassoActionRequestEngine lassoActionRequestEngine(LassoConfiguration lassoConfiguration,
															 WorkspaceManager workspaceManager,
															 ExecutionEnvironmentManager executionEnvironmentManager,
															 BenchmarkManager benchmarkManager,
															 ClusterEngine clusterEngine) {
		return new LassoActionRequestEngine(lassoConfiguration, workspaceManager, executionEnvironmentManager, benchmarkManager, clusterEngine);
	}

	@Bean(destroyMethod = "close")
	public ClusterEngine clusterEngine() {
		ClusterEngine clusterEngine = new ClusterEngine(nodeId, role, localAddress, multicastIp);

		clusterEngine.setFailureDetectionTimeout(failureDetectionTimeoutInSecs * 1000L);
		String[] addrs = StringUtils.split(addressList, ",");
		List<String> remoteAddresses = Arrays.asList(addrs);
		if(CollectionUtils.isNotEmpty(remoteAddresses)) {
			clusterEngine.setRemoteAddresses(remoteAddresses);
		}

		// start
		clusterEngine.start();

		return clusterEngine;
	}

	@Bean
	public BenchmarkManager benchmarkManager() {
		BenchmarkManager benchmarkManager = new BenchmarkManager();
		return benchmarkManager;
	}
}
