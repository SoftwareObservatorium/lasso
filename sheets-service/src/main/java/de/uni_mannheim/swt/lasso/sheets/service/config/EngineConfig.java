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
package de.uni_mannheim.swt.lasso.sheets.service.config;

import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.SSNTestDriver;
import de.uni_mannheim.swt.lasso.sheets.service.SheetsManager;
import de.uni_mannheim.swt.lasso.sheets.service.driver.LocalSimpleTestDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;

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

    @Bean
    public SheetsManager sheetsManager(MavenRepository mavenRepository) {
        SheetsManager sheetsManager = new SheetsManager(new LocalSimpleTestDriver(mavenRepository));

        return sheetsManager;
    }

    @Bean
    public MavenRepository mavenRepository() {
        // FIXME change maven repo
        String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
        File localRepo = new File("/tmp/my_repo/local-repo");
        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        return new MavenRepository(resolver);
    }
}
