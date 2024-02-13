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

import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.sandbox.container.ContainerService;
import de.uni_mannheim.swt.lasso.sandbox.container.ContainerState;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Controls lifecycle of Docker containers.
 *
 * @author Marcus Kessel
 *
 */
public abstract class DockerExecutionEnvironment extends ExecutionEnvironment {

    private static final Logger LOG = LoggerFactory
            .getLogger(DockerExecutionEnvironment.class);

    /**
     * Assumed to be passed as part of running LASSO in docker
     * with "docker in docker" support.
     */
    public static final String DIND_SUPPORT_LIBS = "DIND_SUPPORT_LIBS";

    private final ContainerService containerService;

    private final String proxyRegistry;
    private final int pullTimeout;

    /**
     * 1 day timeout (graceful)
     */
    private long executionTimeout = 24 * 60 * 60 * 1000L;

    private File projectRoot;
    private List<String> commands;

    protected ContainerState containerState;

    private boolean killed = false;

    public DockerExecutionEnvironment(String id, String proxyRegistry, int pullTimeout) {
        super(id);

        this.proxyRegistry = proxyRegistry;
        this.pullTimeout = pullTimeout;

        this.containerService = new ContainerService(proxyRegistry, pullTimeout);
    }

    public void run() throws InterruptedException {
        setStartDate(new Date());

        containerState = runContainer();

        long sleepTime = 2 * 1000L;
        long execTime = 0L;

        while(true) {
            if(containerState.isRunning()) {
                Thread.sleep(sleepTime);
                execTime += sleepTime;

                //
                if(execTime > getExecutionTimeout()) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Timeout exceeded '{}'. Stopping container", getExecutionTimeout());
                    }

                    // stop now
                    stop();

                    // wait 10secs
                    Thread.sleep(10* 1000L);

                    if(containerState.isRunning()) {
                        if(LOG.isWarnEnabled()) {
                            LOG.warn("Timeout exceeded '{}'. Killing container", getExecutionTimeout());
                        }

                        // kill
                        kill();
                    }
                }
            } else {
                if(LOG.isInfoEnabled()) {
                    LOG.info(ToStringBuilder.reflectionToString(containerState.getInspectContainerResponse()));
                }

                break;
            }
        }

        setEndDate(new Date());
    }

    /**
     *
     * @return
     */
    public abstract ContainerState runContainer();

    /**
     * Is container still running?
     *
     * @return
     */
    public boolean isRunning() {
        return containerState != null && containerState.isRunning();
    }

    @Override
    public void kill() {
        //
        if(!killed && containerState != null) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Killing container '{}'", containerState.getContainerId());
            }

            // kill container
            KillContainerCmd killContainerCmd = containerState.getContainer().getContainerService().getDockerClient().killContainerCmd(containerState.getContainerId());
            // execute now
            killContainerCmd/*.withSignal("9")*/.exec();

            killed = true;

            if(LOG.isWarnEnabled()) {
                LOG.warn("Successfully killed container '{}'", containerState.getContainerId());
            }
        }
    }

    @Override
    public void stop() {
        //
        if(!killed && containerState != null) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Stopping container '{}'", containerState.getContainerId());
            }

            // kill container
            StopContainerCmd stopContainerCmd = containerState.getContainer().getContainerService().getDockerClient().stopContainerCmd(containerState.getContainerId());
            // execute now
            stopContainerCmd/*.withSignal("9")*/.exec();

            killed = true;

            if(LOG.isWarnEnabled()) {
                LOG.warn("Successfully stopped container '{}'", containerState.getContainerId());
            }
        }
    }

    @Override
    public void delete() {
        //
        if(containerState != null) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Removing container '{}'", containerState.getContainerId());
            }

            // remove container
            containerState.getContainer().getContainerService().getDockerClient().removeContainerCmd(containerState.getContainerId());
        }
    }

    public List<String> getLogs() {
        if(containerState == null) {
            return null;
        }
        final List<String> logs = new ArrayList<>();

        LogContainerCmd logContainerCmd = containerState.getContainer().getContainerService().getDockerClient().logContainerCmd(getId());
        logContainerCmd.withStdOut(true).withStdErr(true);

        logContainerCmd.withTimestamps(false);

        try {
            logContainerCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame item) {
                    logs.add(item.toString());
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            LOG.warn("logging interrupted", e);
        }

        return logs;
    }

    /**
     * Checks if the LASSO instance is running in docker
     *
     * @return
     */
    public static boolean isRunningDockerInDocker() {
        return StringUtils.isNotBlank(getDindPath());
    }

    /**
     * Rewrites absolute path with DinD host path.
     *
     * @param original
     * @param workspace
     * @return
     */
    public static File rewritePathDind(Workspace workspace, File original) {
        String hostRootPath = DockerExecutionEnvironment.getDindPath();
        String originalRootPath = original.getAbsolutePath();

        // rewrites the global LASSO work dir path
        String rewrittenPath = StringUtils.replace(originalRootPath,
                workspace.getLassoRoot().getAbsolutePath(), hostRootPath);

        LOG.info("DinD detected. Rewritten path '{}' to '{}'", originalRootPath, rewrittenPath);

        return new File(rewrittenPath);
    }

    /**
     * Checks the system environmental variable
     *
     * @return
     *
     * @see #DIND_SUPPORT_LIBS
     */
    public static String getDindPath() {
        return System.getenv(DIND_SUPPORT_LIBS);
    }

    public ContainerService getContainerService() {
        return containerService;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(Workspace workspace, File projectRoot) {
        // docker in docker?
        if (isRunningDockerInDocker()) {
            // where project lives
            this.projectRoot = DockerExecutionEnvironment.rewritePathDind(workspace, projectRoot);
        } else {
            this.projectRoot = projectRoot;
        }
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public long getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(long executionTimeout) {
        this.executionTimeout = executionTimeout;
    }
}
