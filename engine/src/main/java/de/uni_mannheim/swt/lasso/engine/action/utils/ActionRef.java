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
package de.uni_mannheim.swt.lasso.engine.action.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Study utilities
 *
 * @author Marcus Kessel
 */
public class ActionRef {

    private String executionId;
    private String actionId;

    /**
     *
     * @param actionId
     * @return
     */
    public static ActionRef from(String actionId) {
        ActionRef actionRef = new ActionRef();
        // action defined in other study
        boolean externalStudyRef = StringUtils.contains(actionId, ":");
        if(externalStudyRef) {
            String[] parts = StringUtils.split(actionId, ":");

            actionRef.setExecutionId(parts[0]);
            actionRef.setActionId(parts[1]);
        } else {
            actionRef.setActionId(actionId);
        }

        return actionRef;
    }

    public boolean hasExecutionId() {
        return StringUtils.isNotBlank(executionId);
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
}
