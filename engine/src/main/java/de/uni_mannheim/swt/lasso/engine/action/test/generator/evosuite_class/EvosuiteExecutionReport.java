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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.evosuite_class;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 *
 * @author Marcus Kessel
 */
public class EvosuiteExecutionReport extends LassoReport {

    @QuerySqlField
    private boolean mojoFailed; // EvoSuite failed indirectly
    @QuerySqlField
    private boolean projectFailed; // project failed
    @QuerySqlField
    private boolean evosuiteFailed; // evosuite failed
    @QuerySqlField
    private String cause;

    public boolean isMojoFailed() {
        return mojoFailed;
    }

    public void setMojoFailed(boolean mojoFailed) {
        this.mojoFailed = mojoFailed;
    }

    public boolean isProjectFailed() {
        return projectFailed;
    }

    public void setProjectFailed(boolean projectFailed) {
        this.projectFailed = projectFailed;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public boolean isEvosuiteFailed() {
        return evosuiteFailed;
    }

    public void setEvosuiteFailed(boolean evosuiteFailed) {
        this.evosuiteFailed = evosuiteFailed;
    }
}
