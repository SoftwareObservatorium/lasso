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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * LASSO Query Language Parser Model
 *
 * @author Marcus Kessel
 */
public class MethodSignature {

    private String name;
    private boolean constructor;

    private List<String> inputs = new LinkedList<>();

    private List<String> outputs = new LinkedList<>();

    public boolean isConstructor() {
        return constructor;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignature method = (MethodSignature) o;
        return constructor == method.constructor && Objects.equals(name, method.name) && Objects.equals(inputs, method.inputs) && Objects.equals(outputs, method.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, constructor, inputs, outputs);
    }

    @Override
    public String toString() {
        return "Method{" +
                "name='" + name + '\'' +
                ", constructor=" + constructor +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }
}
