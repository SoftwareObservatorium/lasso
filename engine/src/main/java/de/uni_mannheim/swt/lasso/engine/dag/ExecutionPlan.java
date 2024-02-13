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
package de.uni_mannheim.swt.lasso.engine.dag;

import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serializable Execution Plan of LSL scripts (i.e DAG)
 *
 * @author Marcus Kessel
 */
public class ExecutionPlan extends DirectedAcyclicGraph<ActionNode, ActionEdge> {

    private String study;

    public ExecutionPlan() {
        super(ActionEdge.class);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(this);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
    }

    public void debugDAG() {
        System.out.println(">>> DAG <<< " + this.toString());
        this.vertexSet().stream().forEach(v -> {
            System.out.println("  " + ToStringBuilder.reflectionToString(v));
            System.out.println("  " + v.toString());
        });
        this.edgeSet().stream().forEach(v -> {
            System.out.println("  " + ToStringBuilder.reflectionToString(v));
            System.out.println("  " + v.toString());
        });
        System.out.println(">>> DAG <<<");
    }

    /**
     * FIXME THIS METHOD IS BUGGY
     *
     * @param actionName
     * @return
     */
    public Set<ActionNode> getAncestors(String actionName) {
        // WORKAROUND FOR FUNNY PROBLEM: ancestor instances seem not to be the actual instances
        return getAncestors(getAction(actionName)).stream().map(v -> getAction(v.getName())).collect(Collectors.toSet());
    }

    public List<ActionNode> getAncestorsSorted(String actionName) {
        // WORKAROUND FOR FUNNY PROBLEM: ancestor instances seem not to be the actual instances
        return getAncestorsSorted(getAction(actionName)).stream().map(v -> getAction(v.getName())).collect(Collectors.toList());
    }

    public List<ActionNode> getAncestorsSorted(ActionNode vertex) {
        EdgeReversedGraph<ActionNode, ActionEdge> reversedGraph = new EdgeReversedGraph<ActionNode, ActionEdge>(this);
        Iterator<ActionNode> iterator = new DepthFirstIterator<ActionNode, ActionEdge>(reversedGraph, vertex);
        List<ActionNode> ancestors = new LinkedList<>();
        if (iterator.hasNext()) {
            iterator.next();
        }

        iterator.forEachRemaining(ancestors::add);
        return ancestors;
    }

    public ActionNode getAncestor(DefaultAction action, Class<? extends DefaultAction> type) {
        List<ActionNode> ancestors = getAncestorsSorted(action.getName());

        if (CollectionUtils.isEmpty(ancestors)) {
            return null;
        }

        return ancestors.stream()
                .filter(a -> StringUtils.equals(a.getType(), type.getSimpleName())).findFirst().orElse(null);
    }

    public boolean hasAncestor(DefaultAction action, Class<? extends DefaultAction> type) {
        return hasAncestor(action.getName(), type.getSimpleName());
    }

    public boolean hasAncestor(String actionName, String ancestorType) {
        Set<ActionNode> ancestors = getAncestors(actionName);

        if (CollectionUtils.isEmpty(ancestors)) {
            return false;
        }

        return ancestors.stream()
                .anyMatch(a -> StringUtils.equals(a.getType(), ancestorType));
    }

    public boolean containsAction(String actionName) {
        Optional<ActionNode> op = this.vertexSet().stream()
                .filter(a -> StringUtils.equals(a.getName(), actionName))
                .findFirst();

        return op.isPresent();
    }

    public ActionNode getAction(String actionName) {
        Optional<ActionNode> op = this.vertexSet().stream()
                .filter(a -> StringUtils.equals(a.getName(), actionName))
                .findFirst();

        return op.orElse(null);
    }

    public List<ActionNode> queryActionsByType(Class<? extends DefaultAction> type) {
        return vertexSet().stream().filter(v -> StringUtils.equals(v.getType(), type.getSimpleName())).collect(Collectors.toList());
    }

    public ActionNode findFirstAction(Function<ActionNode, Boolean> filter) {
        TopologicalOrderIterator<ActionNode, ActionEdge> it = new TopologicalOrderIterator<>(this);
        while(it.hasNext()) {
            ActionNode node = it.next();

            if(filter.apply(node)) {
                return node;
            }
        }

        return null;
    }

    /**
     * Simple type filter.
     *
     * @param type
     * @return
     */
    public static Function<ActionNode, Boolean> typeFilter(Class<? extends DefaultAction> type) {
        return (node) -> StringUtils.equals(node.getType(), type.getSimpleName());
    }

    public Set<String> getAbstractions(ActionNode node) {
        Set<ActionEdge> incomingEdges = incomingEdgesOf(node);

        if(CollectionUtils.isEmpty(incomingEdges)) {
            return Collections.emptySet();
        }

        ActionEdge edge = incomingEdges.iterator().next();

        return edge.getAbstractions();
    }

    public String getStudy() {
        return study;
    }

    public void setStudy(String study) {
        this.study = study;
    }
}
