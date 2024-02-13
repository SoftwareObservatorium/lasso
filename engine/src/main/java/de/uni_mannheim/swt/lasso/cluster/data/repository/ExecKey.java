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
package de.uni_mannheim.swt.lasso.cluster.data.repository;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 */
public class ExecKey implements Serializable {

    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "exec_idx", order = 0, descending = true)})
    private String executionId;
    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "exec_idx", order = 0, descending = true)})
    private String abstractionName;
    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "exec_idx", order = 0, descending = true)})
    private String actionName;
    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "exec_idx", order = 0, descending = true)})
    private String id;
    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "exec_idx", order = 0, descending = true)})
    @Deprecated
    private String workerNodeId;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getAbstractionName() {
        return abstractionName;
    }

    public void setAbstractionName(String abstractionName) {
        this.abstractionName = abstractionName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecKey execKey = (ExecKey) o;
        return Objects.equals(executionId, execKey.executionId) &&
                Objects.equals(abstractionName, execKey.abstractionName) &&
                Objects.equals(actionName, execKey.actionName) &&
                Objects.equals(workerNodeId, execKey.workerNodeId) &&
                Objects.equals(id, execKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, abstractionName, actionName, workerNodeId, id);
    }

    @Deprecated
    public String getWorkerNodeId() {
        return workerNodeId;
    }

    @Deprecated
    public void setWorkerNodeId(String workerNodeId) {
        this.workerNodeId = workerNodeId;
    }
}
