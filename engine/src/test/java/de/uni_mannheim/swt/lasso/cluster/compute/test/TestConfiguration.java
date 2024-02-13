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
package de.uni_mannheim.swt.lasso.cluster.compute.test;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@PropertySource("classpath:application.properties")
public class TestConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(de.uni_mannheim.swt.lasso.cluster.worker.WorkerConfiguration.class);

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

    @Bean(destroyMethod = "close")
    public ClusterEngine clusterEngine() {
        ClusterEngine clusterEngine = new ClusterEngine(nodeId, role, localAddress, multicastIp);

        clusterEngine.setParallelJobsNumber(env.getProperty("master.jobs.parallel", Integer.class));

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
}

