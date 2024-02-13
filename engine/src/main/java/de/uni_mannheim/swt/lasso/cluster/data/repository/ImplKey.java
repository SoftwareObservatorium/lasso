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
public class ImplKey implements Serializable {

    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "impl_idx", order = 0, descending = true)})
    private String executionId;
    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "impl_idx", order = 0, descending = true)})
    private String abstractionName;
    @QuerySqlField(orderedGroups={@QuerySqlField.Group(
            name = "impl_idx", order = 0, descending = true)})
    private String id;

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
        ImplKey implKey = (ImplKey) o;
        return Objects.equals(executionId, implKey.executionId) &&
                Objects.equals(abstractionName, implKey.abstractionName) &&
                Objects.equals(id, implKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, abstractionName, id);
    }
}
