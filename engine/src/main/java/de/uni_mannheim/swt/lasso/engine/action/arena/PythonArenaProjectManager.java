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
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.datasource.maven.build.Candidate;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.build.PythonProjectBuildManager;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.PythonBasicExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities to manage python projects.
 *
 * @author Marcus Kessel
 */
public class PythonArenaProjectManager implements ArenaProjectManager {

    protected static final String ARENA_LOG_TXT = "arena_log.txt";

    private static final Logger LOG = LoggerFactory
            .getLogger(PythonArenaProjectManager.class);

    private final LSLExecutionContext lslExecutionContext;
    private final Workspace workspace;

    public PythonArenaProjectManager(LSLExecutionContext lslExecutionContext) throws IOException {
        Validate.notNull(lslExecutionContext, "%s cannot be null", LSLExecutionContext.class);

        this.lslExecutionContext = lslExecutionContext;
        this.workspace = lslExecutionContext.getWorkspace();
    }

    public Systems initNew(DefaultAction action, String actionInstanceId, Abstraction abstraction, String projectTemplate, ProjectManager.ProjectSettingsHandler pythonProjectSettingsHandler, ProjectManager.ExecutableFilter executableFilter) throws IOException {
        //
        File abstractionRoot = workspace.createDirectory(abstraction);

        // init other stuff
        Map<String, String> projectOptions = new HashMap<>();

        //Mavenizer mavenizer = new Mavenizer(abstractionRoot, mvnOptions);

        List<CodeUnit> impls = abstraction.getImplementations().stream().map(System::getCode).toList();

        List<System> execs = new ArrayList<>();

        // XXX remove duplicates
        LassoUtils.findDuplicates(abstraction, false);

        for (CodeUnit implementation : impls) {
            Candidate candidate = new Candidate();
            // set id
            candidate.setId(implementation.getId());
//            // set candidate class
//            CompilationUnit cunit = new CompilationUnit();
//            cunit.setName(implementation.getName());
//            cunit.setPkg(implementation.getPackagename());
//            candidate.setCompilationUnit(cunit);
//
//            // artifact
//            MavenArtifact artifact = new MavenArtifact();
//            artifact.setGroupId(implementation.getGroupId());
//            artifact.setArtifactId(implementation.getArtifactId());
//            artifact.setVersion(implementation.getVersion());
//            candidate.setArtifact(artifact);

            // add bytecodename
            Map<String, Object> valueMap = new HashMap<>();

            // call before creation
            try {
                pythonProjectSettingsHandler.onFillTemplate(implementation, candidate, valueMap);
            } catch (Throwable e) {
                LOG.warn("onFillTemplate failed for {}", implementation.getId());
                LOG.warn("Exception", e);

                continue;
            }

            System executable = new System(implementation);

            //
            try {
                if (executableFilter.accept(executable)) {
                    execs.add(executable);
                } else {
                    // FIXME remove project
                }
            } catch (Throwable e) {
                LOG.warn("executableFilter failed for {}", implementation.getId());
                LOG.warn("Exception", e);

                continue;
            }
        }

        Systems executables = new Systems();
        executables.setAbstractionName(abstraction.getName());
        executables.setExecutables(execs);
        executables.setActionInstanceId(actionInstanceId);
        executables.setSpecification(abstraction.getSpecification());

        return executables;
    }

    @Override
    public ExecutionEnvironment createExecutionEnvironment(DefaultAction action, ActionConfiguration actionConfiguration, ArenaJob job, ExecutableCorpus corpus, String task, List<String> features, long containerTimeout) {
        // args passed to arena
        List<String> args = new ArrayList<>(Arrays.asList(
                "--mode", "distributed",
                "--lassoaddresses", "127.0.0.1:10800",
                "--lassojob", job.getId(),
                "--workdir", "/project"//,
                //"&>" + ARENA_LOG_TXT
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

        PythonBasicExecutionEnvironment arenaExecutionEnvironment = PythonProjectBuildManager.createExecutionEnvironment(action, lslExecutionContext, actionConfiguration, environment);
        // set container timeout
        if(LOG.isInfoEnabled()) {
            LOG.info("Setting python arena container timeout to '{}'", containerTimeout);
        }
        arenaExecutionEnvironment.setExecutionTimeout(containerTimeout);

        if(LOG.isInfoEnabled()) {
            LOG.info("Setting python arena args to '{}'", String.join(",", args));
        }

        return arenaExecutionEnvironment;
    }

    public LSLExecutionContext getLslExecutionContext() {
        return lslExecutionContext;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public File getRepository() {
        return null;
    }
}
