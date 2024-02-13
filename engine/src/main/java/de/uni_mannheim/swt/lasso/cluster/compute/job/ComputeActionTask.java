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

import de.uni_mannheim.swt.lasso.engine.dag.ActionRequest;
import de.uni_mannheim.swt.lasso.engine.dag.ActionResponse;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.*;
import org.apache.ignite.resources.LoadBalancerResource;

import java.util.*;

/**
 * Distributes jobs to worker nodes.
 *
 * @author Marcus Kessel
 */
public class ComputeActionTask extends ComputeTaskAdapter<List<ActionRequest>, List<ActionResponse>> {

    // Inject load balancer.
    @LoadBalancerResource
    ComputeLoadBalancer balancer;

    /**
     * Map jobs to grid nodes.
     *
     * @param grid
     * @param requests
     * @return
     */
    public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> grid, List<ActionRequest> requests) {
        Map<ActionJob, ClusterNode> jobs = new HashMap<>(grid.size());

        //
        for(ActionRequest request : requests) {
//            Optional<ClusterNode> nodeOp = grid.stream().filter(n -> n.id().toString().equals(request.getWorkerNodeId())).findFirst();
//
//            if(!nodeOp.isPresent()) {
//                throw new IllegalStateException(String.format("Did not find cluster node '%s'", request.getWorkerNodeId()));
//            }

            ActionJob actionJob = new ActionJob();
            actionJob.setArguments(request);

            // get next node
            // grid is guaranteed to only contain worker nodes (see compute API)
            ClusterNode clusterNode = balancer.getBalancedNode(actionJob, null);
            request.setWorkerNodeId(clusterNode.id().toString());

            jobs.put(actionJob, clusterNode);
        }

        return jobs;
    }

//    /**
//     * Map jobs to grid nodes.
//     *
//     * @param grid
//     * @param requests
//     * @return
//     */
//    public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> grid, List<ActionRequest> requests) {
//        Map<ActionJob, ClusterNode> jobs = new HashMap<>(grid.size());
//
//        //
//        for(ActionRequest request : requests) {
//            Optional<ClusterNode> nodeOp = grid.stream().filter(n -> n.id().toString().equals(request.getWorkerNodeId())).findFirst();
//
//            if(!nodeOp.isPresent()) {
//                throw new IllegalStateException(String.format("Did not find cluster node '%s'", request.getWorkerNodeId()));
//            }
//
//            ActionJob actionJob = new ActionJob();
//            actionJob.setArguments(request);
//
//            jobs.put(actionJob, nodeOp.get());
//        }
//
//        return jobs;
//    }

    // Aggregate results into one compound result.
    public List<ActionResponse> reduce(List<ComputeJobResult> results) {
        List<ActionResponse> responses = new ArrayList<>(results.size());

        for(ComputeJobResult result : results) {
            responses.add(result.getData());
        }

        return responses;
    }

    @Override
    public ComputeJobResultPolicy result(ComputeJobResult res, List<ComputeJobResult> rcvd) throws IgniteException {
        // XXX handle exception

        return super.result(res, rcvd);
    }
}
