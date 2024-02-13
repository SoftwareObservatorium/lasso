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
package de.uni_mannheim.swt.lasso.core.dto;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;

import java.util.Map;

/**
 *
 * @author Marcus Kessel
 *
 */
public class ImplementationResponse {

    private Map<String, CodeUnit> implementations;
    private Map<String, Map> implementationsRaw;

    public Map<String, CodeUnit> getImplementations() {
        return implementations;
    }

    public void setImplementations(Map<String, CodeUnit> implementations) {
        this.implementations = implementations;
    }

    public Map<String, Map> getImplementationsRaw() {
        return implementationsRaw;
    }

    public void setImplementationsRaw(Map<String, Map> implementationsRaw) {
        this.implementationsRaw = implementationsRaw;
    }
}
