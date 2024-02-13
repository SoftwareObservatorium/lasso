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
import org.apache.ignite.cluster.ClusterNode;

import java.util.*;

/**
 * Assign more implementations to more powerful nodes (i.e., based on CPU).
 *
 * @author Marcus Kessel
 */
// FIXME experimental
public class PrioritizePowerfulNodes extends EquivalentBlocks {

    public PrioritizePowerfulNodes(LassoConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Map<String, List<System>> partition(List<System> implementationList, Collection<ClusterNode> nodes) {
        Map<String, List<System>> partition = new HashMap<>(nodes.size());

        // shuffle?
        if(shuffleImplementations && CollectionUtils.isNotEmpty(implementationList)) {
            Collections.shuffle(implementationList);
        }

        int totalCpus = nodes.stream().mapToInt(node -> node.metrics().getTotalCpus()).sum();
        int totalImpls = implementationList.size();

        ClusterNode remain = null;
        double maxWeight = -1d;
        for(ClusterNode node : nodes) {
            double weight = (double) node.metrics().getTotalCpus() / totalCpus;

            if(maxWeight < weight) {
                maxWeight = weight;

                remain = node;
            }

            int chunkSize = (int) Math.round(weight * totalImpls);

            // take "chunkSize" implementations from list
            List<System> subList = new ArrayList<>(implementationList.subList(0, Math.min(chunkSize, implementationList.size())));
            // remove
            implementationList.removeAll(subList);

            partition.put(node.id().toString(), subList);
        }

        if(CollectionUtils.isNotEmpty(implementationList)) {
            // add to one of the most powerful
            partition.get(remain.id().toString()).addAll(implementationList);
        }

        // drop those with 0 impls
        partition.entrySet().removeIf(entry -> entry.getValue().size() < 1);

        return partition;
    }
}
