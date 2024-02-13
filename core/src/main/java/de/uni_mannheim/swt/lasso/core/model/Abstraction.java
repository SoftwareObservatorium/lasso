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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A functional abstraction.
 *
 * Note: Not anymore. More like a "container" structure of systems
 * (that may not be related to a single functional abstraction).
 *
 * @author Marcus Kessel
 */
public class Abstraction implements Serializable {

    private String name;

    private List<System> implementations;

    private Specification specification = new Specification();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<System> getImplementations() {
        return implementations;
    }

    // alias
    public List<System> getSystems() {
        return getImplementations();
    }

    public void setImplementations(List<System> implementations) {
        this.implementations = implementations;
    }

    public void setSystems(List<System> implementations) {
        this.setImplementations(implementations);
    }

    public List<Sequence> getSequences() {
        return specification.getSequences();
    }

    public void setSequences(List<Sequence> sequences) {
        specification.setSequences(sequences);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Abstraction that = (Abstraction) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Specification getSpecification() {
        return specification;
    }

    public void setSpecification(Specification specification) {
        this.specification = specification;
    }
}
