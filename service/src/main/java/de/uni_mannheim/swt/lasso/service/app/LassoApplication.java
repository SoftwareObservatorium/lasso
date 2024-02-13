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
package de.uni_mannheim.swt.lasso.service.app;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.corpus.CorpusConfig;
import de.uni_mannheim.swt.lasso.service.app.config.*;
import de.uni_mannheim.swt.lasso.service.persistence.User;
import de.uni_mannheim.swt.lasso.service.persistence.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Main Application using Spring Boot
 *
 * @author Marcus Kessel
 *
 */
@Configuration
@EnableAutoConfiguration(exclude = { SolrAutoConfiguration.class })
@ComponentScan(basePackages = { "de.uni_mannheim.swt.lasso.service" })
@Import({ EngineConfig.class, SecurityConfig.class, WebAppConfig.class, SwaggerConfig.class, JpaConfig.class, CorpusConfig.class, NotificationConfig.class })
public class LassoApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LassoApplication.class);

    /**
     * Bootstrap application.
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) {
        // init Spring boot
        SpringApplication.run(LassoApplication.class, args);
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    Environment env;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    ClusterEngine clusterEngine;

    @Override
    public void run(String... args) throws Exception {
        // FIXME setup users should be moved somewhere else
        List<User> users = UserManagementUtils.read(resourceLoader.getResource(env.getProperty("users")).getInputStream(), passwordEncoder);

        // add users to database
        users.forEach(u -> {
            if(!userRepository.existsByUsername(u.getUsername())) {
                userRepository.save(u);
            }
        });

        if(LOG.isDebugEnabled()) {
            userRepository.findAll().forEach(v -> LOG.debug(" User :" + v.toString()));
        }

        if(env.getProperty("cluster.embedded", boolean.class, false)) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Embedded mode enabled");
            }

            //clusterEngine.getIgnite().active(true);
        }
    }
}
