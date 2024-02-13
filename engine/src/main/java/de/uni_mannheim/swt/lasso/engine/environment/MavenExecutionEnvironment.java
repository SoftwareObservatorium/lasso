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
import de.uni_mannheim.swt.lasso.sandbox.container.support.MavenContainer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 *
 * @author Marcus Kessel
 *
 */
public class MavenExecutionEnvironment extends DockerExecutionEnvironment {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenExecutionEnvironment.class);

    private File m2Home;

    private final String mavenDefaultImage;

    public MavenExecutionEnvironment(String id, String proxyRegistry, int pullTimeout, String mavenDefaultImage) {
        super(id, proxyRegistry, pullTimeout);
        this.mavenDefaultImage = mavenDefaultImage;
    }

    @Override
    public ContainerState runContainer() {
        MavenContainer mavenContainer = createContainer();

        return mavenContainer.runMultipleCommands(getId(),
                getProjectRoot().getAbsolutePath(), m2Home.getAbsolutePath(), getCommands());
    }

    protected MavenContainer createContainer() {
        MavenContainer mavenContainer = new MavenContainer(getContainerService());
        if(StringUtils.isNotEmpty(getImage())) {
            mavenContainer.setImage(getImage());
        } else {
            mavenContainer.setImage(StringUtils.isNotBlank(mavenDefaultImage) ? mavenDefaultImage : "maven:3.5.4-jdk-8-alpine");
        }

        if(LOG.isInfoEnabled()) {
            LOG.info(String.format("Running container with image '%s'", mavenContainer.getImage()));
        }

        return mavenContainer;
    }

    public File getM2Home() {
        return m2Home;
    }

    public void setM2Home(Workspace workspace, File m2Home) {
        // docker in docker?
        if (DockerExecutionEnvironment.isRunningDockerInDocker()) {
            // rewrite m2home
            this.m2Home = DockerExecutionEnvironment.rewritePathDind(workspace, m2Home);
        } else {
            this.m2Home = m2Home;
        }
    }
}
