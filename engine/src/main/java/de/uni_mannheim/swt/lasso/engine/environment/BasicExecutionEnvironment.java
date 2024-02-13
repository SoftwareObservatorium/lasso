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

import de.uni_mannheim.swt.lasso.sandbox.container.ContainerState;
import de.uni_mannheim.swt.lasso.sandbox.container.support.BasicContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

/**
 *
 * @author Marcus Kessel
 *
 */
public class BasicExecutionEnvironment extends DockerExecutionEnvironment {

    private static final Logger LOG = LoggerFactory
            .getLogger(BasicExecutionEnvironment.class);

    // docker working directory
    private File workingDirectory;

    public BasicExecutionEnvironment(String id, String proxyRegistry, int pullTimeout) {
        super(id, proxyRegistry, pullTimeout);
    }

    @Override
    public ContainerState runContainer() {
        BasicContainer basicContainer = new BasicContainer(getContainerService());
        basicContainer.setImage(getImage());
        basicContainer.setWorkingDirectory(workingDirectory.getAbsolutePath());

        setStartDate(new Date());

        ContainerState containerState = basicContainer.run(getId(),
                getProjectRoot().getAbsolutePath(), getCommands());

        return containerState;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
