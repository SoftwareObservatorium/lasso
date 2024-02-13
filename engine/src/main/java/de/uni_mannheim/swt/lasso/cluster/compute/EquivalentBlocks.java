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
package de.uni_mannheim.swt.lasso.cluster.compute;

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.ignite.cluster.ClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Produces equivalent blocks (each worker node is assigned a similar subset of implementations).
 *
 * @author Marcus Kessel
 */
public class EquivalentBlocks implements PartitioningStrategy {

    private static final Logger LOG = LoggerFactory
            .getLogger(EquivalentBlocks.class);

    protected final boolean shuffleImplementations;
    protected final boolean shuffleWorkerNodes;

    public EquivalentBlocks(LassoConfiguration configuration) {
        //
        this.shuffleImplementations = configuration.getProperty("cluster.implementations.shuffle", Boolean.class);
        this.shuffleWorkerNodes = configuration.getProperty("cluster.workernodes.shuffle", Boolean.class);
    }

    @Override
    public Map<String, List<System>> partition(List<System> implementationList, Collection<ClusterNode> nodes) {
        Map<String, List<System>> partition = new HashMap<>(nodes.size());

        // shuffle?
        if(shuffleImplementations && CollectionUtils.isNotEmpty(implementationList)) {
            Collections.shuffle(implementationList);
        }

        // split into "equal" chunks
        int groupSize = nodes.size();
        int chunkingSize = (implementationList.size() + groupSize - 1) / groupSize;

        if(chunkingSize > 0) {
            List<List<System>> partitionList = ListUtils.partition(implementationList, chunkingSize);
            List<ClusterNode> workerNodes = new ArrayList<>(nodes);
            if(shuffleWorkerNodes) { // shuffle nodes as well?
                Collections.shuffle(workerNodes);
            }

            int c = 0;
            for(ClusterNode node : workerNodes) {
                if(c >= partitionList.size()) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("No more implementations left for worker node '{}'", node);
                    }

                    continue;
                }

                List<System> implementations = new ArrayList<>(partitionList.get(c++));
                //implementations.forEach(impl -> impl.setWorkerNodeId(node.id().toString()));

                // note: node id just used as a distinct ID for a block of the partition
                partition.put(node.id().toString(), implementations);
            }
        }

        return partition;
    }
}
