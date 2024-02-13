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

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;

import java.io.Serializable;

/**
 *
 * @author Marcus Kessel
 */
public class ActionRequest implements Serializable {

    private String sessionId;

    private String workerNodeId;

    private ExecutionPlan executionPlan;

    private String executionId;

    private String name;
    private String type;

    private ActionConfiguration configuration;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkerNodeId() {
        return workerNodeId;
    }

    public void setWorkerNodeId(String workerNodeId) {
        this.workerNodeId = workerNodeId;
    }

    public ActionConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ActionConfiguration configuration) {
        this.configuration = configuration;
    }

    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
