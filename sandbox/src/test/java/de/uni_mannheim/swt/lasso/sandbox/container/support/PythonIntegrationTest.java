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
public class PythonIntegrationTest {

    String proxyRegistry = null;//;"docker.io";
    int pullTimeout = 10 * 60;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("thirdparty.docker.uid", "1000");
        System.setProperty("thirdparty.docker.gid", "1000");
    }

    @Test
    public void test_analyzer() throws InterruptedException {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        PythonContainer basicContainer = new PythonContainer(containerService);
        basicContainer.setImage("swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/analyzer-python:latest");
        basicContainer.setWorkingDirectory("/project");

        String containerName = "analyzer-python-" + UUID.randomUUID().toString();

        //
        File projectRoot = new File("samples/nicad/"); // FIXME

        List<String> args = new ArrayList<>(Arrays.asList("--solrurl=http://localhost:8983/solr/lasso_quickstart/update/json/docs", "--projectroot=/project", "--batchsize=50", "--meta=meta_project_category=python", "--meta=meta_customfield=foo"));

        ContainerState containerState = basicContainer.run(containerName,
                projectRoot.getAbsolutePath(), args);

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
    public void test_arena() throws InterruptedException {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        PythonContainer basicContainer = new PythonContainer(containerService);
        basicContainer.setImage("swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/arena-python:latest");
        basicContainer.setWorkingDirectory("/project");

        String containerName = "arena-python-" + UUID.randomUUID().toString();

        //
        File projectRoot = new File("samples/nicad/"); // FIXME

        List<String> args = new ArrayList<>(Arrays.asList("--someparam=XXX"));

        ContainerState containerState = basicContainer.run(containerName,
                projectRoot.getAbsolutePath(), args);

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
