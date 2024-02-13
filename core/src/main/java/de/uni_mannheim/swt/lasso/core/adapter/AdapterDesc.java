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
package de.uni_mannheim.swt.lasso.core.adapter;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class AdapterDesc {

    private String systemId;
    private String className;
    private String adapterId;
    private List<MethodDesc> initializers;
    private List<MethodDesc> methods;

    public List<MethodDesc> getInitializers() {
        return initializers;
    }

    public void setInitializers(List<MethodDesc> initializers) {
        this.initializers = initializers;
    }

    public List<MethodDesc> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodDesc> methods) {
        this.methods = methods;
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(String adapterId) {
        this.adapterId = adapterId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
