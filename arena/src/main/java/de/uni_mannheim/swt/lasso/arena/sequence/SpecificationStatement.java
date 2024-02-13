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
package de.uni_mannheim.swt.lasso.arena.sequence;

import java.util.LinkedList;
import java.util.List;

/**
 * An abstract statement.
 *
 * @author Marcus Kessel
 */
public abstract class SpecificationStatement {

    protected List<SpecificationStatement> inputs;
    protected int position = - 1;

    public SpecificationStatement() {
        this.inputs = new LinkedList<>();
    }

    public void addInput(SpecificationStatement statement) {
        inputs.add(statement);
    }

    public List<SpecificationStatement> getInputs() {
        return inputs;
    }

    public void setInputs(List<SpecificationStatement> inputs) {
        this.inputs = inputs;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public abstract boolean isClassUnderTest();
}
