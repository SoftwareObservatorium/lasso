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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;

import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Container service based on docker(-ce).
 *
 * Note: assumes local docker installation (tested with docker-ce 18.09.1 on ubuntu 18.04.1LTS).
 *
 * @author Marcus Kessel
 *
 */
public class ContainerService {

    private static final Logger LOG = LoggerFactory
            .getLogger(ContainerService.class);

    private DockerClient dockerClient;

    private int pullTimeoutInSeconds = 10 * 60;

    private final String proxyRegistry;

    public ContainerService(String proxyRegistry, int pullTimeoutInSeconds) {
        this.proxyRegistry = proxyRegistry;
        this.pullTimeoutInSeconds = pullTimeoutInSeconds;
        this.dockerClient = createClient();
    }

    protected DockerClient createClient() {
        DefaultDockerClientConfig.Builder configBuilder
                = DefaultDockerClientConfig.createDefaultConfigBuilder();
        configBuilder.withRegistryUrl(proxyRegistry);
                //.withRegistryUsername("marcus")
                //.withRegistryEmail("marcus@swt100.informatik.uni-mannheim.de");
                //.withRegistryPassword("");

        DefaultDockerClientConfig config = configBuilder.build();

        //
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        return dockerClient;
    }

    public Info getInfo() {
        Info info = dockerClient.infoCmd().exec();

        return info;
    }

    public CreateContainerCmd createContainer(String name, String image, String[] binds) {
        Optional<Image> found = listImages().stream()
                //.peek(img -> System.out.println("ARRAYS " + Arrays.toString(img.getRepoTags()) + " for " + name + " " + image))
                .filter(img -> img.getRepoTags().length > 0) // sometimes [] ...
                .filter(img -> StringUtils.endsWith(image, img.getRepoTags()[0]))
                .findFirst();

        // pull image if not present on host
        if(!found.isPresent()) {
            // retrieve image first

            try {
                pullImage(image);
            } catch (Throwable e) {
                LOG.warn("Pulling image failed for " + image, e);
            }
        }

        String repoImageName = getActualImageName(image);

        if(LOG.isInfoEnabled()) {
            LOG.info("Creating container with image '{}'", repoImageName);
        }

        CreateContainerCmd cmd = dockerClient.createContainerCmd(repoImageName)
                .withName(name);

        if(ArrayUtils.isNotEmpty(binds)) {
            cmd = cmd.withBinds(Arrays.stream(binds).map(Bind::parse).toArray(Bind[]::new));
        }

        return cmd;
    }

    public void startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
    }

    public InspectContainerResponse getContainerStatus(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).exec();
    }

    public void pullImage(String image) throws InterruptedException {
        String repoName = getActualImageName(image);

        dockerClient.pullImageCmd(repoName)
                .exec(new PullImageResultCallback())
                .awaitCompletion(pullTimeoutInSeconds, TimeUnit.SECONDS);
    }

    public List<Image> listImages() {
        List<Image> images = dockerClient.listImagesCmd()
                .withShowAll(true).exec();

        return images;
    }

    public List<Container> listContainers() {
        List<Container> containers = dockerClient.listContainersCmd().exec();

        return containers;
    }

    public void log(String containerId,
                    LogContainerResultCallback logContainerResultCallback) throws InterruptedException {
        dockerClient.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();
    }

    public CreateVolumeResponse createVolume(String volumeId) {
        CreateVolumeResponse namedVolume
                = dockerClient.createVolumeCmd().withName(volumeId).exec();

        return namedVolume;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public String getProxyRegistry() {
        return proxyRegistry;
    }

    public String getActualImageName(String image) {
        // FIXME hack to workaround publishing issue of NICAD
        if(StringUtils.startsWith(image, "nicad")) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Assuming local deploy of nicad image");
            }

            return image;
        }

        return StringUtils.isNotBlank(proxyRegistry) ? String.format("%s/%s", proxyRegistry, image) : image;
    }
}
