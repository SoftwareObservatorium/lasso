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
package de.uni_mannheim.swt.lasso.engine.workspace;

import de.uni_mannheim.swt.lasso.engine.dag.ActionRequest;
import de.uni_mannheim.swt.lasso.engine.LSLScript;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Manages workspaces.
 *
 * @author Marcus Kessel
 *
 */
public class WorkspaceManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(WorkspaceManager.class);

    private final File root;

    public WorkspaceManager(File root) {
        Validate.isTrue(root.getParentFile().isDirectory() && root.getParentFile().canWrite(),
                "Cannot open root directory '%s'", root);

        this.root = root;
    }

    public Workspace create(LSLScript script) throws IOException {
        Workspace workspace = new Workspace();
        workspace.setLassoRoot(root);
        workspace.setScript(script);

        // root
        File workspaceRoot = getWorkspaceRoot(script.getExecutionId());
        workspaceRoot.mkdirs();
        workspace.setRoot(workspaceRoot);

        // write out script for later access
        workspace.writeObject(Workspace.SCRIPT_JSON, script);

        return workspace;
    }

    public File getWorkspaceRoot(String executionId) {
        return new File(root, executionId);
    }

    public File[] listWorkspaces() {
        // all UUIDs
        return root.listFiles((f) -> f.isDirectory() && StringUtils.contains(f.getName(), "-") && f.getName().length() == 36);
    }

    public Workspace create(ActionRequest actionRequest) throws IOException {
        Workspace workspace = new Workspace();
        workspace.setLassoRoot(root);

        // root
        File workspaceRoot = getWorkspaceRoot(actionRequest.getExecutionId());
        workspaceRoot.mkdirs();
        workspace.setRoot(workspaceRoot);

        //
        LSLScript script = new LSLScript();
        script.setExecutionId(actionRequest.getExecutionId());
        workspace.setScript(script);

        return workspace;
    }

    public Workspace load(String executionId) throws IOException, WorkspaceNotFoundException {
        // root
        File workspaceRoot = getWorkspaceRoot(executionId);

        if(!workspaceRoot.exists()) {
            throw new WorkspaceNotFoundException(
                    String.format("Could not find workspace %s", executionId));
        }

        Workspace workspace = new Workspace();
        workspace.setLassoRoot(root);
        workspace.setRoot(workspaceRoot);

        // write out script for later access
        try {
            LSLScript script = workspace.readObject(Workspace.SCRIPT_JSON, LSLScript.class);
            workspace.setScript(script);
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Cannot load LSL script for workspace '{}'", workspaceRoot);
            }
        }

        return workspace;
    }
}
