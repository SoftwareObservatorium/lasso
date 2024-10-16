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
package de.uni_mannheim.swt.lasso.sandbox.container.support;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import de.uni_mannheim.swt.lasso.sandbox.container.Container;
import de.uni_mannheim.swt.lasso.sandbox.container.ContainerService;
import de.uni_mannheim.swt.lasso.sandbox.container.ContainerState;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * Container impl. for "host" docker containers (i.e containers using host network so that we can access "localhost").
 *
 *
 * @author Marcus Kessel
 */
public class ArenaContainer extends Container {

    public static final String IMAGE_DEFAULT = "openjdk:8-jdk-alpine";
    public static final String WD_DEFAULT = "/usr/src/myarena";

    private String repositoryDirectory = "/var/arena";

    public ArenaContainer(ContainerService containerHandler) {
        super(containerHandler);

        // defaults to image
        this.id = IMAGE_DEFAULT;
        this.workingDirectory = WD_DEFAULT;
        this.image = IMAGE_DEFAULT;
    }

    /**
     * Create and start container.
     *
     * Similar to the following
     *
     * <pre>
     *     docker run --rm -v "$PWD":/usr/src/myapp -w /usr/src/myapp openjdk:8-jdk-alpine javac Main.java
     * </pre>
     *
     * @param containerName
     * @param repositoryRoot
     * @param projectRoot
     * @param args
     * @return
     */
    public ContainerState run(String containerName, String repositoryRoot, String projectRoot, List<String> args) {
        Validate.notBlank(workingDirectory);
        //Validate.notBlank(id);
        Validate.notBlank(image);

        // TODO allocate container resources like CPU, memory etc.
        // https://docs.docker.com/config/containers/resource_constraints/
        // "By default, a container has no resource constraints and can use as much of a given resource as the host’s kernel scheduler allows. "

        Bind[] binds = new Bind[]{new Bind(repositoryRoot, new Volume(repositoryDirectory)),
                new Bind(projectRoot, new Volume(workingDirectory))};

        //  get user and group id, read from System property
        String userId = "1000";
        try {
            userId = System.getProperty("thirdparty.docker.uid");
        } catch (Throwable e) {
            //
        }
        String groupId = "1000";
        try {
            groupId = System.getProperty("thirdparty.docker.gid");
        } catch (Throwable e) {
            //
        }

        String user = String.format("%s:%s", userId, groupId);

        CreateContainerCmd command = containerService.createContainer(containerName, image, null)
                .withUser(user) // default user and group id (e.g., someuser:someuser)
                .withWorkingDir(workingDirectory)
                .withNetworkMode("host") // require to access local services on the host
                .withBinds(binds);

        // create copy
        List<String> commandLineArgs = new ArrayList<>(args);

        CreateContainerResponse createContainerResponse = command.withCmd(commandLineArgs).exec();

        this.containerService.startContainer(createContainerResponse.getId());

        return new ContainerState(this, createContainerResponse.getId());
    }
}
