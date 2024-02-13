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

import de.uni_mannheim.swt.lasso.arena.MethodSignature;

/**
 * Models an abstract statement call.
 *
 * @author Marcus Kessel
 */
public abstract  class CallStatement extends SpecificationStatement {

    protected final MethodSignature methodSignature;
    private boolean classUnderTest;

    public CallStatement(MethodSignature methodSignature) {
        this.methodSignature = methodSignature;
    }

    public MethodSignature getMethodSignature() {
        return methodSignature;
    }

    public abstract boolean isResolved();

    @Override
    public boolean isClassUnderTest() {
        return classUnderTest;
    }

    public void setClassUnderTest(boolean classUnderTest) {
        this.classUnderTest = classUnderTest;
    }

    @Override
    public String toString() {
        return "CUT=" + classUnderTest + " / " + methodSignature.toLQL();
    }
}
