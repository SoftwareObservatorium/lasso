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
package de.uni_mannheim.swt.lasso.engine.action.arena;

import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.environment.ArenaExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;

import de.uni_mannheim.swt.lasso.sandbox.container.support.ArenaContainer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities to manage Java arena projects.
 *
 * @author Marcus Kessel
 */
public class JavaArenaProjectManager implements ArenaProjectManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(JavaArenaProjectManager.class);

    protected static final String ARENA_LOG_TXT = "arena_log.txt";

    private final LSLExecutionContext lslExecutionContext;
    private final Workspace workspace;

    private final File arenaHome;

    private final MavenProjectManager mavenProjectManager;

    public JavaArenaProjectManager(LSLExecutionContext lslExecutionContext) throws IOException {
        Validate.notNull(lslExecutionContext, "%s cannot be null", LSLExecutionContext.class);

        this.lslExecutionContext = lslExecutionContext;
        this.workspace = lslExecutionContext.getWorkspace();

//        // .m2/
//        if (isUseGlobalRepository()) {
        // global
        this.arenaHome = workspace.createGlobalLassoDirectory("repository");
//        } else {
//            // inside workspace
//            this.arenaHome = workspace.createDirectory("repository");
//        }

        this.mavenProjectManager = new MavenProjectManager(lslExecutionContext);
    }

    @Override
    public Systems initNew(DefaultAction action, String actionInstanceId, Abstraction abstraction, String pomTemplate, ProjectManager.ProjectSettingsHandler mavenProjectPomHandler, ProjectManager.ExecutableFilter executableFilter) throws IOException {
        return mavenProjectManager.initNew(action, actionInstanceId, abstraction, pomTemplate, mavenProjectPomHandler, executableFilter);
    }

    @Override
    public ExecutionEnvironment createExecutionEnvironment(DefaultAction action, ActionConfiguration actionConfiguration, ArenaJob job, ExecutableCorpus corpus, String task, List<String> features, long containerTimeout, long implementationTimeout, int threads) {
        // args passed to arena
        List<String> args = new ArrayList<>(Arrays.asList(
                "java",
                "-Xmx4096m", // memory
                "-XX:+IgnoreUnrecognizedVMOptions", // issue #417
                "-Dsun.misc.URLClassPath.disableJarChecking=true", // prevents classloading problems with java11
                // required for Java >= 17
                // see https://ignite.apache.org/docs/latest/quick-start/java
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
                "--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED",
                "--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED",
                "--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED",
                "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens=java.base/java.math=ALL-UNNAMED",
                "--add-opens=java.sql/java.sql=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.time=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.management/sun.management=ALL-UNNAMED",
                "--add-opens java.desktop/java.awt.font=ALL-UNNAMED",
                // -- end Java >= 17
                "-jar",
                "/var/arena/support/arena-1.0.0-SNAPSHOT.jar",
                "--mode", "distributed",
                "--lasso-addresses", "127.0.0.1:10800",
                "--lasso-job", job.getId(),
                "--work-dir", "/var/arena",
                //"--generate-junit", "/var/arena",
                //"--output-csv", "/var/arena/mycsv_"+System.currentTimeMillis()+".csv",
                "--input", ArenaContainer.WD_DEFAULT,
                "--output", ArenaContainer.WD_DEFAULT,
                "--repository-url", corpus.getArtifactRepository().getUrl(),
                "--timeout", String.valueOf((long) implementationTimeout / 1000L),
                "--threads", String.valueOf(threads),
                "&>" + ARENA_LOG_TXT
        ));

        if (CollectionUtils.isNotEmpty(features)) {
            args.add("--features");
            args.add(features.stream().collect(Collectors.joining(",")));
        }

        if (StringUtils.isNotBlank(task)) {
            args.add("--task");
            args.add(task);
        }

        // set default commands (use copy!)
        Environment environment = actionConfiguration.getProfile().getEnvironment().copy();
        if (CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        environment.getCommandArgsList().add(args);

        ArenaExecutionEnvironment arenaExecutionEnvironment = createExecutionEnvironment(action, actionConfiguration, environment);
        // set container timeout
        if(LOG.isInfoEnabled()) {
            LOG.info("Setting arena container timeout to '{}'", containerTimeout);
        }
        arenaExecutionEnvironment.setExecutionTimeout(containerTimeout);

        if(LOG.isInfoEnabled()) {
            LOG.info("Setting arena args to '{}'", String.join(",", args));
        }

        return arenaExecutionEnvironment;
    }

    public ArenaExecutionEnvironment createExecutionEnvironment(DefaultAction action, ActionConfiguration configuration, Environment environment) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = lslExecutionContext.getExecutionEnvironmentManager();

        ArenaExecutionEnvironment executionEnvironment =
                (ArenaExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment(ExecutionEnvironmentManager.ARENA);

        //
        File projectRoot = workspace.getRoot(action.getInstanceId(), configuration.getAbstraction());

        // configure environment
        executionEnvironment.setImage(environment.getImage());
        // set repository path: locates fat jar + m2 repository
        executionEnvironment.setRepository(workspace, arenaHome);
        executionEnvironment.setProjectRoot(workspace, projectRoot);
        executionEnvironment.setCommands(environment.getCommandArgsList().get(0));

        return executionEnvironment;
    }

    @Override
    public LSLExecutionContext getLslExecutionContext() {
        return lslExecutionContext;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public File getRepository() {
        return this.arenaHome;
    }
}
