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

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.cluster.ClusterNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 *
 * @author Marcus Kessel
 */
@ExtendWith(MockitoExtension.class)
public class EquivalentBlocksTest {

    @Mock
    LassoConfiguration configuration;

    @Test
    public void test_6_3_100() {
        when(configuration.getProperty("cluster.implementations.shuffle", Boolean.class)).thenReturn(true);
        when(configuration.getProperty("cluster.workernodes.shuffle", Boolean.class)).thenReturn(true);

        EquivalentBlocks partitioning = new EquivalentBlocks(configuration);

        int total = 100;
        List<System> implementationList = createImpls(total);

        List<ClusterNode> nodes = new ArrayList<>();
        IntStream.range(0, 6).forEach(i -> nodes.add(node(20)));
        IntStream.range(0, 3).forEach(i -> nodes.add(node(8)));

        Map<String, List<System>> partition = partitioning.partition(implementationList, nodes);

        int totalImpls = 0;
        for(String nodeId : partition.keySet()) {
            ClusterNode node = nodes.stream().filter(n -> StringUtils.equals(n.id().toString(), nodeId)).findFirst().get();
            java.lang.System.out.println(String.format("%s => %s", nodeId, partition.get(nodeId).size()));

            totalImpls += partition.get(nodeId).size();
        }

        assertEquals(total, totalImpls);
    }

    @Test
    public void test_6_3_1() {
        when(configuration.getProperty("cluster.implementations.shuffle", Boolean.class)).thenReturn(true);
        when(configuration.getProperty("cluster.workernodes.shuffle", Boolean.class)).thenReturn(true);

        EquivalentBlocks partitioning = new EquivalentBlocks(configuration);

        int total = 1;
        List<System> implementationList = createImpls(total);

        List<ClusterNode> nodes = new ArrayList<>();
        IntStream.range(0, 6).forEach(i -> nodes.add(node(20)));
        IntStream.range(0, 3).forEach(i -> nodes.add(node(8)));

        Map<String, List<System>> partition = partitioning.partition(implementationList, nodes);

        int totalImpls = 0;
        for(String nodeId : partition.keySet()) {
            ClusterNode node = nodes.stream().filter(n -> StringUtils.equals(n.id().toString(), nodeId)).findFirst().get();
            java.lang.System.out.println(String.format("%s => %s", nodeId, partition.get(nodeId).size()));

            totalImpls += partition.get(nodeId).size();
        }

        assertEquals(total, totalImpls);
    }

    @Test
    public void test_6_3_0() {
        when(configuration.getProperty("cluster.implementations.shuffle", Boolean.class)).thenReturn(true);
        when(configuration.getProperty("cluster.workernodes.shuffle", Boolean.class)).thenReturn(true);

        EquivalentBlocks partitioning = new EquivalentBlocks(configuration);

        int total = 0;
        List<System> implementationList = createImpls(total);

        List<ClusterNode> nodes = new ArrayList<>();
        IntStream.range(0, 6).forEach(i -> nodes.add(node(20)));
        IntStream.range(0, 3).forEach(i -> nodes.add(node(8)));

        Map<String, List<System>> partition = partitioning.partition(implementationList, nodes);

        int totalImpls = 0;
        for(String nodeId : partition.keySet()) {
            ClusterNode node = nodes.stream().filter(n -> StringUtils.equals(n.id().toString(), nodeId)).findFirst().get();
            java.lang.System.out.println(String.format("%s => %s", nodeId, partition.get(nodeId).size()));

            totalImpls += partition.get(nodeId).size();
        }

        assertEquals(total, totalImpls);
    }

    private List<System> createImpls(int total) {
        return IntStream.range(0, total).mapToObj(i -> new System(implementation())).collect(Collectors.toList());
    }

    private CodeUnit implementation() {
        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());

        return implementation;
    }

    private ClusterNode node(int cpus) {
        ClusterNode node = Mockito.mock(ClusterNode.class);
        lenient().when(node.id()).thenReturn(UUID.randomUUID());

        return node;
    }
}
