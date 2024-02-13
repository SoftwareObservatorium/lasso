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
package de.uni_mannheim.swt.lasso.core.model.query;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.Interface;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class QueryResult {

    private List<CodeUnit> implementations;

    private long numFound;

    private Interface interfaceSpecification;

    public List<CodeUnit> getImplementations() {
        return implementations;
    }

    public void setImplementations(List<CodeUnit> implementations) {
        this.implementations = implementations;
    }

    public long getNumFound() {
        return numFound;
    }

    public void setNumFound(long numFound) {
        this.numFound = numFound;
    }

    public Interface getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(Interface interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }
}
