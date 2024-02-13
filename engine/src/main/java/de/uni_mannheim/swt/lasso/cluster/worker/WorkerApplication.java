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

import de.uni_mannheim.swt.lasso.cluster.config.SpringLassoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Main Application for Worker Node using Spring Boot
 *
 * @author Marcus Kessel
 *
 */
@Configuration
@Import({ WorkerConfiguration.class })
public class WorkerApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerApplication.class);

    /**
     * Bootstrap application.
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) {
        // init Spring boot
        SpringApplication.run(WorkerApplication.class, args);
    }

    @Value("${cluster.nodeId}")
    private String nodeId;

    @Autowired
    SpringLassoConfiguration lassoConfiguration;

    public static SpringLassoConfiguration config;

    public static SpringLassoConfiguration getConfig() {
        return config;
    }

    @Override
    public void run(String... args) throws Exception {
        if(LOG.isInfoEnabled()) {
            LOG.info("================================");
            LOG.info("Hi from '{}'", nodeId);
            LOG.info("================================");
        }

        config = lassoConfiguration;
    }

}
