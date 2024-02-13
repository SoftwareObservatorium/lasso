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
package de.uni_mannheim.swt.lasso.cluster.worker;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;

/**
 * Models an active LSL script execution session.
 *
 * @author Marcus Kessel
 *
 */
public class LSLSession {
    private String executionId;
    private String sessionId;
    private LSLExecutionContext lslExecutionContext;

    private DefaultAction currentAction;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public LSLExecutionContext getLslExecutionContext() {
        return lslExecutionContext;
    }

    public void setLslExecutionContext(LSLExecutionContext lslExecutionContext) {
        this.lslExecutionContext = lslExecutionContext;
    }

    public DefaultAction getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(DefaultAction currentAction) {
        this.currentAction = currentAction;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
