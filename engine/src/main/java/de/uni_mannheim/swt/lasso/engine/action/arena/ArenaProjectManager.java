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
package de.uni_mannheim.swt.lasso.engine.action.arena;

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.Environment;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.environment.ArenaExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.DockerExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Utilities to manage arena projects.
 *
 * @author Marcus Kessel
 */
public class ArenaProjectManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(ArenaProjectManager.class);

    private final LSLExecutionContext lslExecutionContext;
    private final Workspace workspace;

    private File arenaHome;

    private boolean useGlobalRepository = true;

    public ArenaProjectManager(LSLExecutionContext lslExecutionContext) throws IOException {
        Validate.notNull(lslExecutionContext, "%s cannot be null", LSLExecutionContext.class);

        this.lslExecutionContext = lslExecutionContext;
        this.workspace = lslExecutionContext.getWorkspace();

//        // .m2/
//        if (isUseGlobalRepository()) {
        // global
        this.arenaHome = workspace.createGlobalLassoDirectory("repository");
//        } else {
//            // inside workspace
//            this.arenaHome = workspace.createDirectory("repository");
//        }
    }

    public ArenaExecutionEnvironment createExecutionEnvironment(DefaultAction action, ActionConfiguration configuration, Environment environment) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = lslExecutionContext.getExecutionEnvironmentManager();

        ArenaExecutionEnvironment executionEnvironment =
                (ArenaExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment(ExecutionEnvironmentManager.ARENA);

        //
        File projectRoot = workspace.getRoot(action.getInstanceId(), configuration.getAbstraction());

        // configure environment
        executionEnvironment.setImage(environment.getImage());
        // set repository path: locates fat jar + m2 repository
        executionEnvironment.setRepository(workspace, arenaHome);
        executionEnvironment.setProjectRoot(workspace, projectRoot);
        executionEnvironment.setCommands(environment.getCommandArgsList().get(0));

        return executionEnvironment;
    }

    public LSLExecutionContext getLslExecutionContext() {
        return lslExecutionContext;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public File getRepository() {
        return this.arenaHome;
    }
}
