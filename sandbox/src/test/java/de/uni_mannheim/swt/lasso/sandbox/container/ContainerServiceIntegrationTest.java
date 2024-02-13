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
package de.uni_mannheim.swt.lasso.sandbox.container;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ContainerServiceIntegrationTest {

    String proxyRegistry = "swt100.informatik.uni-mannheim.de:8443";
    int pullTimeout = 10 * 60;

    @Test
    public void test_pull() throws InterruptedException {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        containerService.pullImage("3.6.3-jdk-11");
    }

    @Test
    public void test_info() {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        System.out.println(ToStringBuilder.reflectionToString(containerService.getInfo()));
    }

    @Test
    public void test_images() {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        List<Image> images = containerService.listImages();

        images.forEach(i -> {
            System.out.println(Arrays.toString(i.getRepoTags()));
        });
    }

    @Test
    public void test_containers() {
        ContainerService containerService = new ContainerService(proxyRegistry, pullTimeout);

        List<Container> containers = containerService.listContainers();

        containers.forEach(c -> {
            System.out.println(c.getId());
        });
    }
}
