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

import de.uni_mannheim.swt.lasso.engine.dag.model.LEdge;
import de.uni_mannheim.swt.lasso.engine.dag.model.LGraph;
import de.uni_mannheim.swt.lasso.engine.dag.model.LNode;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DAG utilities.
 *
 * @author Marcus Kessel
 */
public class DAG {

    public static ExecutionPlan createExecutionPlan() {
        ExecutionPlan executionPlan = new ExecutionPlan();

        return executionPlan;
    }

    public static void writeGraph(ExecutionPlan actionsDag, Workspace workspace) {
        writeGraph(actionsDag, workspace.createFile("dag.png"));
    }

    public static void writeGraph(ExecutionPlan actionsDag, File dagPng ) {
        try(FileOutputStream out = new FileOutputStream(dagPng)) {
            StringBuilder sb = new StringBuilder();
            sb.append("@startuml\n");

            actionsDag.edgeSet().forEach(e -> {
                ActionNode source = actionsDag.getEdgeSource(e);
                ActionNode target = actionsDag.getEdgeTarget(e);

                if(actionsDag.getAncestors(source).isEmpty()) {
                    sb.append("[*] --> " + source.getName() + "\n");
                }

                if(source != null && target != null) {
                    sb.append(source.getName() + " -> " + target.getName()
                            + " : " + target.getActionSpec().getIncludeAbstractions() + " >\n");
                }

                if(actionsDag.getDescendants(target).isEmpty()) {
                    sb.append(target.getName() + " --> [*]" + "\n");
                }
            });

            actionsDag.vertexSet().forEach(v -> {
                sb.append(v.getName() + " : " + v.getType() + "\n");
            });

            sb.append("@enduml\n");

            System.out.println(sb.toString());

            SourceStringReader reader = new SourceStringReader(sb.toString());
            reader.generateImage(out, new FileFormatOption(FileFormat.PNG));
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    public static LGraph writeGraphToModel(ExecutionPlan actionsDag) {
        List<LEdge> edges = actionsDag.edgeSet().stream().map(e -> {
            ActionNode source = actionsDag.getEdgeSource(e);
            ActionNode target = actionsDag.getEdgeTarget(e);

            LEdge edge = new LEdge();
            edge.setId(source.getName() + "_" + target.getName());
            edge.setLabel(target.getActionSpec().getIncludeAbstractions());
            edge.setSource(source.getName());
            edge.setTarget(target.getName());

            return edge;
        }).collect(Collectors.toList());

        List<LNode> nodes = actionsDag.vertexSet().stream().map(v -> {
            LNode node = new LNode();
            node.setId(v.getName());
            node.setLabel(v.getType());

            return node;
        }).collect(Collectors.toList());

        LGraph graph = new LGraph();
        graph.setId(actionsDag.getStudy());

        graph.setEdges(edges);
        graph.setNodes(nodes);

        return graph;
    }
}
