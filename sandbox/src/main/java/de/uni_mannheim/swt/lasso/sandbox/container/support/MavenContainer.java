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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A maven/java docker container.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://github.com/carlossg/docker-maven">Docker Maven</a>
 * @see <a href="https://hub.docker.com/_/maven">Maven/Java Images</a>
 */
public class MavenContainer extends Container {

    public static final String IMAGE_DEFAULT = "maven:3.5.4-jdk-8-alpine";
    public static final String WD_DEFAULT = "/usr/src/mymaven";

    private String mavenHome = "/var/maven";
    private String mavenConfigurationDirectory = mavenHome + "/.m2";

    public MavenContainer(ContainerService containerHandler) {
        super(containerHandler);

        // defaults to image
        this.id = IMAGE_DEFAULT;
        this.workingDirectory = WD_DEFAULT;
        this.image = IMAGE_DEFAULT;
    }

    /**
     * Run multiple commands.
     *
     *
     * <pre>
     *     docker run -it --rm -u 1000:1000 --name lasso -v "$(pwd)":/usr/src/mymaven -v ~/.m2:/var/maven/.m2 -w /usr/src/mymaven -e MAVEN_CONFIG=/var/maven/.m2 maven:3.5.4-jdk-8-alpine sh -c "mvn -Duser.home=/var/maven -l maven_log.txt dependency:tree -DoutputFile=deps.txt && sed -n '/Reactor Summary/,/Finished at/p' maven_log.txt | grep 'LASSO Candidate' > lol.csv"
     * </pre>
     *
     * @param containerName
     * @param projectRoot
     * @param mavenRoot
     * @param commands
     * @return
     */
    public ContainerState runMultipleCommands(String containerName, String projectRoot, String mavenRoot, List<String> commands) {
        // TODO allocate container resources like CPU, memory etc.
        // https://docs.docker.com/config/containers/resource_constraints/
        // "By default, a container has no resource constraints and can use as much of a given resource as the host’s kernel scheduler allows. "

        Bind[] binds = new Bind[]{new Bind(mavenRoot, new Volume(mavenConfigurationDirectory)),
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
                .withEnv("MAVEN_CONFIG=" + mavenConfigurationDirectory)
                .withNetworkMode("host") // require to access local services on the host
                .withBinds(binds);

        // use sh to achieve multiple commands connected via &&
        List<String> args = new ArrayList<>();
        args.add("sh");
        args.add("-c");

        StringBuilder shellCommand = new StringBuilder();
        int cCount = 0;
        for(String commandStr : commands) {
            if(cCount > 0) {
                shellCommand.append(" && ");
            }

            if(StringUtils.contains(commandStr, "mvn ")) {
                shellCommand.append(
                        StringUtils.replaceOnce(commandStr, "mvn ",
                                String.format("mvn -Duser.home=%s ", mavenHome)));
            } else {
                shellCommand.append(commandStr);
            }

            cCount++;
        }

        args.add(shellCommand.toString());

        CreateContainerResponse createContainerResponse = command.withCmd(args).exec();

        this.containerService.startContainer(createContainerResponse.getId());

        return new ContainerState(this, createContainerResponse.getId());
    }

    /**
     * Create and start container.
     *
     * Similar to the following
     *
     * <pre>
     *     docker run -it --rm -u 1000:1000 --name lasso -v "$(pwd)":/usr/src/mymaven -v ~/.m2:/var/maven/.m2 -w /usr/src/mymaven -e MAVEN_CONFIG=/var/maven/.m2 maven:3.5.4-jdk-8-alpine mvn -Duser.home=/var/maven clean package
     * </pre>
     *
     * @param containerName
     * @param projectRoot
     * @param mavenRoot
     * @param args
     * @return
     */
    public ContainerState run(String containerName, String projectRoot, String mavenRoot, List<String> args) {
        // TODO allocate container resources like CPU, memory etc.
        // https://docs.docker.com/config/containers/resource_constraints/
        // "By default, a container has no resource constraints and can use as much of a given resource as the host’s kernel scheduler allows. "

        Bind[] binds = new Bind[]{new Bind(mavenRoot, new Volume(mavenConfigurationDirectory)),
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
                .withEnv("MAVEN_CONFIG=" + mavenConfigurationDirectory)
                .withBinds(binds);

        // create copy
        List<String> commandLineArgs = new ArrayList<>(args);

        // find mvn command and append required 'user.home' system property
        if(commandLineArgs.contains("mvn")) {
            int index = commandLineArgs.indexOf("mvn");
            commandLineArgs.add(index + 1, "-Duser.home=" + mavenHome);
        }

        CreateContainerResponse createContainerResponse = command.withCmd(commandLineArgs).exec();

        this.containerService.startContainer(createContainerResponse.getId());

        return new ContainerState(this, createContainerResponse.getId());
    }

    public String getMavenConfigurationDirectory() {
        return mavenConfigurationDirectory;
    }

    public void setMavenConfigurationDirectory(String mavenConfigurationDirectory) {
        this.mavenConfigurationDirectory = mavenConfigurationDirectory;
    }
}
