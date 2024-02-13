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
package de.uni_mannheim.swt.lasso.cluster.compute.job;

import de.uni_mannheim.swt.lasso.cluster.worker.LassoActionRequestEngine;
import de.uni_mannheim.swt.lasso.cluster.worker.WorkerApplication;
import de.uni_mannheim.swt.lasso.engine.dag.ActionRequest;
import de.uni_mannheim.swt.lasso.engine.dag.ActionResponse;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import org.apache.ignite.IgniteException;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobContext;
import org.apache.ignite.compute.ComputeTaskSession;
import org.apache.ignite.resources.JobContextResource;
import org.apache.ignite.resources.TaskSessionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute LASSO job on a worker node.
 *
 * @author Marcus Kessel
 */
public class ActionJob extends ComputeJobAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ActionJob.class);

    // Auto-injected task session.
    @TaskSessionResource
    private ComputeTaskSession session;

    // Auto-injected job context.
    @JobContextResource
    private ComputeJobContext jobCtx;

    @Override
    public Object execute() throws IgniteException {
        ActionRequest request = argument(0);

        // get local instance
        LassoActionRequestEngine requestEngine = WorkerApplication.getConfig().getService(LassoActionRequestEngine.class);

        LOG.info(String.format("processing item %s", request.getWorkerNodeId()));

        try {
//            ProgressEvent event = new ProgressEvent();
//            event.setType(String.format("Running Action %s", actionRequest.getName()));
//            eventHub.send(event);

            // execute action
            LSLExecutionContext lslExecutionContext = requestEngine.execute(request);

            LOG.info(String.format("-> writing item %s", request.getWorkerNodeId()));

            ActionResponse actionResponse = new ActionResponse();
            actionResponse.setWorkerNodeId(request.getWorkerNodeId());
            actionResponse.setExecutionId(request.getExecutionId());
            actionResponse.setName(request.getName());
            actionResponse.setType(request.getType());

            return actionResponse;
        } catch (Throwable e) {
            LOG.warn("failed", e);

            throw new IgniteException(e);
        }
    }
}
