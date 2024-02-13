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
package de.uni_mannheim.swt.lasso.cluster.worker.standalone.config;

import de.uni_mannheim.swt.lasso.cluster.worker.standalone.LassoManager;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;

import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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
    public LassoManager lassoManager(LassoConfiguration lassoConfiguration, WorkspaceManager workspaceManager) {
        LassoManager lassoManager = new LassoManager(lassoConfiguration, workspaceManager);

        return lassoManager;
    }
}
