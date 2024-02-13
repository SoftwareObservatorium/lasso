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
package de.uni_mannheim.swt.lasso.engine.environment;

import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.sandbox.container.ContainerState;
import de.uni_mannheim.swt.lasso.sandbox.container.support.ArenaContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

/**
 * Arena Execution environment for "host" docker containers (i.e containers using host network so that we can access "localhost").
 *
 * @author Marcus Kessel
 *
 */
public class ArenaExecutionEnvironment extends DockerExecutionEnvironment {

    private static final Logger LOG = LoggerFactory
            .getLogger(ArenaExecutionEnvironment.class);

    private File repository;

    public ArenaExecutionEnvironment(String id, String proxyRegistry, int pullTimeout) {
        super(id, proxyRegistry, pullTimeout);
    }

    @Override
    public ContainerState runContainer() {
        ArenaContainer basicContainer = new ArenaContainer(getContainerService());
        basicContainer.setImage(getImage());

        setStartDate(new Date());

        ContainerState containerState = basicContainer.run(getId(), repository.getAbsolutePath(),
                getProjectRoot().getAbsolutePath(), getCommands());

        return containerState;
    }

    public File getRepository() {
        return repository;
    }

    public void setRepository(Workspace workspace, File repository) {
        // docker in docker?
        if (isRunningDockerInDocker()) {
            // rewrite arenaHome (where arena lib lives)
            this.repository = DockerExecutionEnvironment.rewritePathDind(workspace, repository);
        } else {
            this.repository = repository;
        }
    }
}
