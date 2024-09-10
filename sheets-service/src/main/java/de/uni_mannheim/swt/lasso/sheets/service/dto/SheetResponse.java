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
package de.uni_mannheim.swt.lasso.sheets.service.dto;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 *
 */
public class SheetResponse {

    private String executionId;
    private String status;

    private List<SheetSpec> actuationSheets;
    private List<SheetSpec> adaptedActuationSheets;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SheetSpec> getActuationSheets() {
        return actuationSheets;
    }

    public void setActuationSheets(List<SheetSpec> actuationSheets) {
        this.actuationSheets = actuationSheets;
    }

    public List<SheetSpec> getAdaptedActuationSheets() {
        return adaptedActuationSheets;
    }

    public void setAdaptedActuationSheets(List<SheetSpec> adaptedActuationSheets) {
        this.adaptedActuationSheets = adaptedActuationSheets;
    }
}
