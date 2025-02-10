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
package de.uni_mannheim.swt.lasso.core.model;

import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Specification of an abstract system (functional abstraction or system).
 *
 * @author Marcus Kessel
 */
public class Specification implements Serializable {

    private Interface interfaceSpecification;

    private List<Sheet> tests = new LinkedList<>();

    // FIXME remove
    private List<Sequence> sequences = new LinkedList<>();

    private List<String> dependencies;

    public Interface getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(Interface interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public List<Sequence> getSequences() {
        return sequences;
    }

    public void setSequences(List<Sequence> sequences) {
        this.sequences = sequences;
    }

    public List<Sheet> getTests() {
        return tests;
    }

    public void setTests(List<Sheet> tests) {
        this.tests = tests;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
}
