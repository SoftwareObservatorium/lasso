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
package de.uni_mannheim.swt.lasso.service.systemtests.util;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.compute.LassoComputeEngine;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.dag.ExecutionPlan;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;

import java.io.IOException;

/**
 * Special version of engine for testing purposes.
 *
 * @author Marcus Kessel
 */
public class LassoTestEngine extends LassoComputeEngine {

    private LSLExecutionContext lastContext;

    public LassoTestEngine(LassoConfiguration configuration, ActionManager actionManager, WorkspaceManager workspaceManager, ExecutionEnvironmentManager executionEnvironmentManager,
                           BenchmarkManager benchmarkManager,
                           ClusterEngine clusterEngine) {
        super(configuration, actionManager, workspaceManager, executionEnvironmentManager, benchmarkManager, clusterEngine);
    }

    @Override
    protected void runExecutionPlan(ExecutionPlan actionsDag, LSLExecutionContext lslExecutionContext) throws IOException {
        this.lastContext = lslExecutionContext;

        super.runExecutionPlan(actionsDag, lslExecutionContext);
    }

    public LSLExecutionContext getLastContext() {
        return lastContext;
    }
}
