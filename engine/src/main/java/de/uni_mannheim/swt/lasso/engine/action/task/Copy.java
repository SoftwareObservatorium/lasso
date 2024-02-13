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
package de.uni_mannheim.swt.lasso.engine.action.task;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LSLScript;
import de.uni_mannheim.swt.lasso.engine.LassoEngine;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.*;
import de.uni_mannheim.swt.lasso.engine.action.arena.Arena;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.engine.dag.ExecutionPlan;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.lsl.LSLDelegatingScript;
import de.uni_mannheim.swt.lasso.lsl.LSLRunner;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copy candidate projects
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Copy candidate projects.")
@Stable
// distributable
@DisablePartitioning // disable partitioning of implementations
@Tester // handles tests
public class Copy extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Copy.class);

    @LassoInput(desc = "Execution id", optional = false)
    public String executionId;

    @LassoInput(desc = "Action from which we make a copy", optional = false)
    public String actionName;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        //
        WorkspaceManager workspaceManager = context.getConfiguration().getService(WorkspaceManager.class);
        Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("Reading from workspace root '{}'", workspace.getRoot());
        }

        // retrieve script
        String path = String.format("/workspace/%s/script", executionId);
        String scriptContent;
        try {
            scriptContent = IOUtils.toString(context.getLassoFileSystem().read(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ExecutionPlan executionPlan = readExecutionPlan(scriptContent);

        Iterator<ActionNode> actionNodeIterator = executionPlan.iterator();

        List<ActionNode> actionNodes = new LinkedList<>();
        while(actionNodeIterator.hasNext()) {
            ActionNode actionNode = actionNodeIterator.next();
            if(StringUtils.equals(actionNode.getName(), actionName)) {
                actionNodes.add(actionNode);
            }
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("Resolved action nodes {}", actionNodes.stream().map(n -> n.getName()).collect(Collectors.joining(",")));
        }

        if(actionNodes.size() != 1) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Did not find distinct node {}. Returning.", actionName);
            }

            return;
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        // we need to set MavenProject
        File abstractionRoot = workspace.getRoot(abstraction);

        File[] dirs = abstractionRoot.listFiles((f) -> f.isDirectory() && StringUtils.startsWith(f.getName(), String.format("%s_", actionName)));

        if(ArrayUtils.isEmpty(dirs)) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Directory not found, doing nothing");
            }

            return;
        }

        // resolve working directory
        File dir = dirs[0];

        if(LOG.isInfoEnabled()) {
            LOG.info("Resolved project directory '{}'", dir);
        }

        File[] candidates = dir.listFiles((f) -> f.isDirectory() && StringUtils.contains(f.getName(), "-"));

        if(ArrayUtils.isEmpty(candidates)) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("No candidate projects found");
            }

            return;
        }

        Systems allExecutables = Systems.fromAbstraction(actionConfiguration.getAbstraction(), getInstanceId());

        MavenProjectManager mavenProjectManager;
        try {
            mavenProjectManager = new MavenProjectManager(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // identify existing impls
        // obtain successful and set maven project
        Map<String, System> successful = Arrays.stream(candidates).map(projectDir -> {
            MavenProject mavenProject = new MavenProject(projectDir, false);
            mavenProject.setArtifactRepository(mavenProjectManager.getM2Repository());

            String candidateId = projectDir.getName();

            System executable;
            try {
                executable = allExecutables.getExecutable(candidateId);
            } catch (Throwable e) {
                LOG.warn("Project found for {}, but skipping since not in list of executables", candidates);

                return null;
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("Setting maven project for '{}' to '{}'", candidateId, mavenProject.getBaseDir());
            }

            executable.setProject(mavenProject);

            return executable;
        }).filter(Objects::nonNull).collect(Collectors.toMap(System::getId, e -> e));

        Systems executables = mavenProjectManager.initNew(this,
                getInstanceId(),
                actionConfiguration.getAbstraction(),
                Arena.POM_TEMPLATE,
                (implementation, candidate, valueMap) -> {
                    //
                },
                e -> {
                    System existing = successful.get(e.getId());

                    if(existing == null) {
                        return false;
                    }

                    // copy project contents
                    MavenProject targetProject = e.getProject();
                    MavenProject sourceProject = existing.getProject();

                    LOG.info("Copying project. Source '{}' to target '{}'", sourceProject.getBaseDir(), targetProject.getBaseDir());

                    try {
                        FileUtils.copyDirectory(sourceProject.getBaseDir(), targetProject.getBaseDir());

                        // FINALLY mark implementation for future assignment to same worker node
                        e.getCode().setWorkerNodeId(context.getWorkerNodeId());

                        return true;
                    } catch (IOException ex) {
                        LOG.warn("Copying project failed. Source '{}' to target '{}'", sourceProject.getBaseDir(), targetProject.getBaseDir());
                    }

                    return false;
                });

        // set
        setExecutables(executables);
    }

    private ExecutionPlan readExecutionPlan(String scriptContent) {
        LSLScript script = new LSLScript();
        script.setContent(scriptContent);

        // setup logger
        script.setLogger(new SimpleLogger());

        // new LSL runner
        LSLRunner lslRunner = new LSLRunner();

        // delegating script
        LSLDelegatingScript delegatingScript = lslRunner.runScript(script.getContent(), script.getLogger());

        LassoContext lassoContext = delegatingScript.getLasso();
        lassoContext.setExecutionId(script.getExecutionId());
        //lassoContext.setWorkspaceRoot(workspace.getRoot());

        // process
        ExecutionPlan executionPlan = LassoEngine.createActionExecutionPlan(lassoContext);

        return executionPlan;
    }
}
