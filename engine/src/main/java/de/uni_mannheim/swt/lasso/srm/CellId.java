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
package de.uni_mannheim.swt.lasso.srm;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.util.Objects;

/**
 * Cell ID model
 *
 * {@link #systemId} + {@link #adapterId} + {@link #variantId} = implementationId()
 *
 * @author Marcus Kessel
 */
public class CellId implements Serializable {

    @QuerySqlField(index = true, notNull = true)
    private String executionId;

    @QuerySqlField(index = true, notNull = true)
    private String abstractionId;

    @QuerySqlField(index = true, notNull = true)
    private String actionId;

    @QuerySqlField(index = true, notNull = true)
    private String arenaId;

    @QuerySqlField(index = true, notNull = true)
    private String sheetId;

    @QuerySqlField(index = true, notNull = true)
    private String systemId;

    @QuerySqlField(index = true, notNull = true)
    private String variantId;

    @QuerySqlField(index = true, notNull = true)
    private String adapterId;

    // FIXME add datasource as well
//    @QuerySqlField(index = true, notNull = true)
//    private String dataSource;

    @QuerySqlField(index = true, notNull = true)
    private int x;
    @QuerySqlField(index = true, notNull = true)
    private int y;

    @QuerySqlField(index = true)
    private String type;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(String adapterId) {
        this.adapterId = adapterId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getAbstractionId() {
        return abstractionId;
    }

    public void setAbstractionId(String abstractionId) {
        this.abstractionId = abstractionId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getArenaId() {
        return arenaId;
    }

    public void setArenaId(String arenaId) {
        this.arenaId = arenaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CellId cellId = (CellId) o;
        return x == cellId.x && y == cellId.y && Objects.equals(executionId, cellId.executionId) && Objects.equals(abstractionId, cellId.abstractionId) && Objects.equals(actionId, cellId.actionId) && Objects.equals(arenaId, cellId.arenaId) && Objects.equals(sheetId, cellId.sheetId) && Objects.equals(systemId, cellId.systemId) && Objects.equals(variantId, cellId.variantId) && Objects.equals(adapterId, cellId.adapterId) && Objects.equals(type, cellId.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, abstractionId, actionId, arenaId, sheetId, systemId, variantId, adapterId, x, y, type);
    }
}
