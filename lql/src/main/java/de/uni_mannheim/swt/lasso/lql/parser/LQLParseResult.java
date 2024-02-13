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
package de.uni_mannheim.swt.lasso.lql.parser;

import de.uni_mannheim.swt.lasso.core.model.Interface;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * LASSO Query Language Parser Model
 *
 * @author Marcus Kessel
 */
public class LQLParseResult {

    private Interface interfaceSpecification;
    private List<String> filters = new LinkedList<>();

    public boolean hasInterfaceSpecification() {
        return interfaceSpecification != null;
    }

    public Interface getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(Interface interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LQLParseResult that = (LQLParseResult) o;
        return Objects.equals(interfaceSpecification, that.interfaceSpecification) && Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceSpecification, filters);
    }

    @Override
    public String toString() {
        return "LQLParseResult{" +
                "interfaceSpecification=" + interfaceSpecification +
                ", filters=" + filters +
                '}';
    }
}
