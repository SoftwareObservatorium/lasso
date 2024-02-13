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
package de.uni_mannheim.swt.lasso.engine.data;

import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.util.Objects;

/**
 * Models report keys.
 *
 * @author Marcus Kessel
 */
public class ReportKey implements Serializable {

    @QuerySqlField(index = true)
    @AffinityKeyMapped
    private String system;

    @QuerySqlField(index = true)
    private String action;

    @QuerySqlField(index = true)
    private String dataSource;

    @QuerySqlField(index = true)
    private String abstraction;

    @QuerySqlField(index = true)
    private int permId;

    public static ReportKey of(String actionName, String abstractionName, System system, Adapter executionSignature) {
        return new ReportKey(actionName, abstractionName, system.getId(), system.getCode().getDataSource(), executionSignature == null ? -1 : executionSignature.getId());
    }

    public static ReportKey of(Action action, Abstraction abstraction, System system, Adapter executionSignature) {
        return new ReportKey(action.getName(), abstraction.getName(), system.getId(), system.getCode().getDataSource(), executionSignature == null ? -1 : executionSignature.getId());
    }

    public static ReportKey of(Action action, String abstractionName, System system, int permId) {
        return new ReportKey(action.getName(), abstractionName, system.getId(), system.getCode().getDataSource(), permId);
    }

    public static ReportKey of(Action action, Abstraction abstraction, System system, int permId) {
        return new ReportKey(action.getName(), abstraction.getName(), system.getId(), system.getCode().getDataSource(), permId);
    }

    public static ReportKey of(Action action, Abstraction abstraction, System system) {
        return new ReportKey(action.getName(), abstraction.getName(), system.getId(), system.getCode().getDataSource(), -1);
    }

    public static ReportKey of(String actionName, String abstractionName, System system) {
        return new ReportKey(actionName, abstractionName, system.getId(), system.getCode().getDataSource(), -1);
    }

    public static ReportKey of(String actionName, String abstractionName, System system, int permId) {
        return new ReportKey(actionName, abstractionName, system.getId(), system.getCode().getDataSource(), permId);
    }

    public static ReportKey of(String actionName, String abstraction, String system, String dataSource, int permId) {
        return new ReportKey(actionName, abstraction, system, dataSource, permId);
    }

    public static ReportKey of(String actionName, String abstraction, String system, String dataSource) {
        return new ReportKey(actionName, abstraction, system, dataSource,-1);
    }

    public static ReportKey of(DefaultAction action, String abstractionName, System system) {
        return new ReportKey(action.getName(), abstractionName, system.getId(), system.getCode().getDataSource(),-1);
    }

    public ReportKey(String actionName, String abstraction, String system, String dataSource, int permId) {
        this.action = actionName;
        this.system = system;
        this.dataSource = dataSource;
        this.abstraction = abstraction;
        this.permId = permId;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getAbstraction() {
        return abstraction;
    }

    public void setAbstraction(String abstraction) {
        this.abstraction = abstraction;
    }

    public int getPermId() {
        return permId;
    }

    public void setPermId(int permId) {
        this.permId = permId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "ReportKey{" +
                "action=" + action + '\'' +
                "system='" + system + '\'' +
                "dataSource='" + dataSource + '\'' +
                ", abstraction='" + abstraction + '\'' +
                ", permId=" + permId +
                '}';
    }

    public String toActionLessString() {
        return "ReportKey{" +
                "system='" + system + '\'' +
                "dataSource='" + dataSource + '\'' +
                ", abstraction='" + abstraction + '\'' +
                ", permId=" + permId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportKey reportKey = (ReportKey) o;
        return permId == reportKey.permId && Objects.equals(system, reportKey.system) && Objects.equals(action, reportKey.action) && Objects.equals(dataSource, reportKey.dataSource) && Objects.equals(abstraction, reportKey.abstraction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, action, dataSource, abstraction, permId);
    }
}
