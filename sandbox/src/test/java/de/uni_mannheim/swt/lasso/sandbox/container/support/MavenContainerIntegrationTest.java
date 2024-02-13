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

import de.uni_mannheim.swt.lasso.sandbox.container.ContainerService;
import de.uni_mannheim.swt.lasso.sandbox.container.ContainerState;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Marcus Kessel
 */
public class MavenContainerIntegrationTest {

    String proxyRegistry = "swt100.informatik.uni-mannheim.de:8443";
    int pullTimeout = 10 * 60;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("thirdparty.docker.uid", "1000");
        System.setProperty("thirdparty.docker.gid", "1000");
    }

    /**
     * Create sample project with following snippet
     *
     * <pre>
     *     cd /tmp/
     *     mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false
     * </pre>
     *
     */
    @Test
    public void test_sample() throws InterruptedException {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        MavenContainer mavenContainer = new MavenContainer(containerService);
        mavenContainer.setImage("maven:3.5.4-jdk-8-alpine");

        String containerName = "test-" + UUID.randomUUID().toString();

        //
        File projectRoot = new File("/tmp/my-app");
        File mavenRoot = new File(SystemUtils.getUserHome(), ".m2/");

        List<String> args = new ArrayList<>(Arrays.asList("mvn", "clean", "package"));

        ContainerState containerState = mavenContainer.run(containerName,
                projectRoot.getAbsolutePath(), mavenRoot.getAbsolutePath(), args);

        while(true) {
            if(containerState.isRunning()) {
                Thread.sleep(2 * 1000L);
            } else {
                System.out.println(ToStringBuilder.reflectionToString(containerState.getInspectContainerResponse()));

                break;
            }

        }
    }

    /**
     * Create sample project with following snippet
     *
     * <pre>
     *     cd /tmp/
     *     mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false
     * </pre>
     *
     */
    @Test
    public void test_sample_11() throws InterruptedException {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        MavenContainer mavenContainer = new MavenContainer(containerService);
        mavenContainer.setImage("maven:3.6.3-jdk-11");

        String containerName = "test-" + UUID.randomUUID().toString();

        //
        File projectRoot = new File("/tmp/my-app");
        File mavenRoot = new File(SystemUtils.getUserHome(), ".m2/");

        List<String> args = new ArrayList<>(Arrays.asList("mvn", "clean", "package"));

        ContainerState containerState = mavenContainer.run(containerName,
                projectRoot.getAbsolutePath(), mavenRoot.getAbsolutePath(), args);

        while(true) {
            if(containerState.isRunning()) {
                Thread.sleep(2 * 1000L);
            } else {
                System.out.println(ToStringBuilder.reflectionToString(containerState.getInspectContainerResponse()));

                break;
            }

        }
    }

    @Test
    public void test_sample_multiple_commands() throws InterruptedException {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        MavenContainer mavenContainer = new MavenContainer(containerService);
        mavenContainer.setImage("maven:3.5.4-jdk-8-alpine");

        String containerName = "test-" + UUID.randomUUID().toString();

        //
        File projectRoot = new File("/tmp/my-app");
        File mavenRoot = new File(SystemUtils.getUserHome(), ".m2/");

        List<String> commands = new ArrayList<>(Arrays.asList(
                "mvn -l maven_log.txt dependency:tree -DoutputFile=deps.txt",
                "sed -n '/Total time/,/Finished at/p' maven_log.txt > results.txt"
        ));

        ContainerState containerState = mavenContainer.runMultipleCommands(containerName,
                projectRoot.getAbsolutePath(), mavenRoot.getAbsolutePath(), commands);

        while(true) {
            if(containerState.isRunning()) {
                Thread.sleep(2 * 1000L);
            } else {
                System.out.println(ToStringBuilder.reflectionToString(containerState.getInspectContainerResponse()));

                break;
            }

        }
    }
}
