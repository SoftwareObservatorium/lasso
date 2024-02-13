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
package de.uni_mannheim.swt.lasso.core.srm;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.Behaviour;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SRM interface.
 *
 * @author Marcus Kessel
 */
public interface SRM {

    /**
     * Get an observation (analysis attribute).
     *
     * @param map
     * @return
     * @throws IOException
     */
    Object getObservation(Map<String, ?> map) throws IOException;

    /**
     * Requires functional equivalence.
     *
     * @param behaviour
     * @param map
     * @return
     * @throws IOException
     */
    List<String> equalTo(Behaviour behaviour, Map<String, ?> map) throws IOException;

    /**
     * Requires functional similarity.
     *
     * @param behaviour
     * @param map
     * @return
     * @throws IOException
     */
    List<String> similarTo(Behaviour behaviour, double minimum, Map<String, ?> map) throws IOException;

    /**
     * Export to CSV
     *
     * @param executionId
     * @param actionName
     * @param abstraction
     * @throws IOException
     */
    void export(String executionId, String actionName, Abstraction abstraction, File csvFile) throws IOException;
}
