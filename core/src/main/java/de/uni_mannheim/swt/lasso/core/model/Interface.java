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

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * An interface of a system.
 *
 * @author Marcus Kessel
 */
public class Interface {

    private String name;

    private List<MethodSignature> methods = new LinkedList<>();

    private String lqlQuery;

    public String getName() {
        return name;
    }

    public String getSimpleName() {
        if(StringUtils.contains(getName(), ".")) {
            return StringUtils.substringAfterLast(getName(), ".");
        }

        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MethodSignature> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodSignature> methods) {
        this.methods = methods;
    }

    public String getLqlQuery() {
        return lqlQuery;
    }

    public void setLqlQuery(String lqlQuery) {
        this.lqlQuery = lqlQuery;
    }

    @Override
    public String toString() {
        return "Interface{" +
                "name='" + name + '\'' +
                ", methods=" + methods +
                ", lql=" + lqlQuery +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interface system = (Interface) o;
        return Objects.equals(name, system.name) && Objects.equals(methods, system.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, methods);
    }

}
