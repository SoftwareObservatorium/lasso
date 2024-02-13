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
package de.uni_mannheim.swt.lasso.cluster.worker.standalone;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.worker.standalone.dto.UserInfo;
import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewItem;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;

import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceNotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.ignite.cluster.ClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author Marcus Kessel
 */
public class LassoManager {

    private static final Logger LOG = LoggerFactory.getLogger(LassoManager.class);

    private final LassoConfiguration lassoConfiguration;

    private WorkspaceManager workspaceManager;

    public LassoManager(LassoConfiguration lassoConfiguration, WorkspaceManager workspaceManager) {
        this.lassoConfiguration = lassoConfiguration;
        this.workspaceManager = workspaceManager;
    }

    public StreamingResponseBody streamRecords(RecordsRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException {
        //
        final Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        executionId), e);
            }

            throw new WorkspaceNotFoundException();
        }

        StreamingResponseBody stream = new StreamingResponseBody() {

            @Override
            public void writeTo(OutputStream output) throws IOException {
                workspace.createZipFile(output, request.getFilePatterns());
            }
        };

        return stream;
    }

    public StreamingResponseBody streamCSV(RecordsRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException, IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Requested files '{}'", Arrays.toString(request.getFilePatterns().toArray(new String[0])));
        }

        //
        final Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        executionId), e);
            }

            throw new WorkspaceNotFoundException();
        }

        String[] files = workspace.scanForFiles(request.getFilePatterns());
        if (files == null) {
            throw new IOException(String.format("CSV File not found: '%s'", Arrays.asList(request.getFilePatterns())));
        }

        if (files.length > 1) {
            throw new IOException(String.format("More than one CSV File matched: '%s'", Arrays.asList(request.getFilePatterns())));
        }

        final String csvPath = files[0];

        StreamingResponseBody stream = new StreamingResponseBody() {

            @Override
            public void writeTo(OutputStream output) throws IOException {
                FileUtils.copyFile(new File(workspace.getRoot(), csvPath), output);
            }
        };

        return stream;
    }

    public RecordsResponse getRecords(RecordsRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException {
        //
        final Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        executionId), e);
            }

            throw new WorkspaceNotFoundException();
        }

        RecordsResponse recordsResponse = new RecordsResponse();

        recordsResponse.setFiles(Arrays.asList(workspace.scanForFiles(request.getFilePatterns())));

        return recordsResponse;
    }

    public FileViewResponse getFiles(FileViewRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException {
        //
        final Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        executionId), e);
            }

            throw new WorkspaceNotFoundException();
        }

        FileViewResponse response = new FileViewResponse();

        ClusterEngine clusterEngine = this.lassoConfiguration.getService(ClusterEngine.class);
        ClusterNode localNode = clusterEngine.getIgnite().cluster().localNode();

        String id = localNode.id().toString();

        Map<String, FileViewItem> items = workspace.scanForFileItems(request.getFilePatterns());
        // rewrite path with id
        items.values().forEach(i -> {
            i.setValue(String.format("%s:%s", id, i.getValue()));
        });

        FileViewItem root = items.get("/");

        //debugTree(root, 0);

        response.setRoot(root);

        return response;
    }
}
