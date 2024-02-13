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
package de.uni_mannheim.swt.lasso.benchmark;

import java.util.List;

/**
 * A simple statement in the format of
 *
 * <pre>
 *     inputs,
 *     operation,
 *     expectedOutputs
 * </pre>
 *
 * @author Marcus Kessel
 */
public class Statement {

    private String operation;
    private List<Value> inputs;
    private List<Value> expectedOutputs;

    public List<Value> getInputs() {
        return inputs;
    }

    public void setInputs(List<Value> inputs) {
        this.inputs = inputs;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<Value> getExpectedOutputs() {
        return expectedOutputs;
    }

    public void setExpectedOutputs(List<Value> expectedOutputs) {
        this.expectedOutputs = expectedOutputs;
    }
}
