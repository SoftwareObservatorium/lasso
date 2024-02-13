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
package de.uni_mannheim.swt.lasso.engine;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.engine.data.LassoOperations;
import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.AbstractionsContainer;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.dag.ExecutionPlan;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.engine.data.fs.LassoFileSystem;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.event.EventManager;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;

import java.util.Map;

/**
 *
 * @author Marcus Kessel
 *
 */
public class LSLExecutionContext {

    private ExecutionPlan executionPlan;

    private Map<String, DataSource> dataSourceMap;
    private AbstractionsContainer abstractionsContainer;
    private Workspace workspace;

    private ExecutionEnvironmentManager executionEnvironmentManager;
    private ActionManager actionManager;

    private LassoContext lassoContext;

    private LassoConfiguration configuration;

    private ReportOperations reportOperations;
    private LassoFileSystem lassoFileSystem;

    private LassoOperations lassoOperations;

    private String workerNodeId;

    private EventManager eventManager;

    private BenchmarkManager benchmarkManager;

    public String getExecutionId() {
        return workspace.getExecutionId();
    }

    public Map<String, DataSource> getDataSourceMap() {
        return dataSourceMap;
    }

    public void setDataSourceMap(Map<String, DataSource> dataSourceMap) {
        this.dataSourceMap = dataSourceMap;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public AbstractionsContainer getAbstractionsContainer() {
        return abstractionsContainer;
    }

    public void setAbstractionsContainer(AbstractionsContainer abstractionsContainer) {
        this.abstractionsContainer = abstractionsContainer;
    }

    public ExecutionEnvironmentManager getExecutionEnvironmentManager() {
        return executionEnvironmentManager;
    }

    public void setExecutionEnvironmentManager(ExecutionEnvironmentManager executionEnvironmentManager) {
        this.executionEnvironmentManager = executionEnvironmentManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    public LassoContext getLassoContext() {
        return lassoContext;
    }

    public void setLassoContext(LassoContext lassoContext) {
        this.lassoContext = lassoContext;
    }

    public LassoConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LassoConfiguration configuration) {
        this.configuration = configuration;
    }

    public ReportOperations getReportOperations() {
        return reportOperations;
    }

    public void setReportOperations(ReportOperations reportOperations) {
        this.reportOperations = reportOperations;
    }

    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }

    public LassoFileSystem getLassoFileSystem() {
        return lassoFileSystem;
    }

    public void setLassoFileSystem(LassoFileSystem lassoFileSystem) {
        this.lassoFileSystem = lassoFileSystem;
    }

    public LassoOperations getLassoOperations() {
        return lassoOperations;
    }

    public void setLassoOperations(LassoOperations lassoOperations) {
        this.lassoOperations = lassoOperations;
    }

    public String getWorkerNodeId() {
        return workerNodeId;
    }

    public void setWorkerNodeId(String workerNodeId) {
        this.workerNodeId = workerNodeId;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public BenchmarkManager getBenchmarkManager() {
        return benchmarkManager;
    }

    public void setBenchmarkManager(BenchmarkManager benchmarkManager) {
        this.benchmarkManager = benchmarkManager;
    }
}
