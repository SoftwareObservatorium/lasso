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

import de.uni_mannheim.swt.lasso.cluster.data.repository.ExecKey;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;

import javax.cache.Cache;
import java.util.Map;

/**
 * Interface for LASSO model operations.
 *
 * @author Marcus Kessel
 */
public interface LassoOperations {

    Cache<ExecKey, System> getExecutableCache();

    Cache.Entry<ExecKey, System> getExecutableFromAction(String executionId, String actionName, String id);

    Systems getExecutables(String executionId, String abstractionName, String actionName);

    Map<String, Systems> getAbstractions(String executionId, String actionName);

    void putExecutables(String executionId, String actionName, Systems executables);

    void putExecutables(String executionId, String actionName, Systems executables, boolean removeExisting);
}
