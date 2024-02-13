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
package de.uni_mannheim.swt.lasso.arena.task.load;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;

import java.util.Map;

/**
 * Model that holds resolved sheets.
 *
 * @author Marcus Kessel
 */
public class ResolvedSheets {

    private ClassUnderTest classUnderTest;
    private InterfaceSpecification specification;
    private Map<String, SequenceSpecification> sheets;

    public ClassUnderTest getClassUnderTest() {
        return classUnderTest;
    }

    public void setClassUnderTest(ClassUnderTest classUnderTest) {
        this.classUnderTest = classUnderTest;
    }

    public InterfaceSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(InterfaceSpecification specification) {
        this.specification = specification;
    }

    public Map<String, SequenceSpecification> getSheets() {
        return sheets;
    }

    public void setSheets(Map<String, SequenceSpecification> sheets) {
        this.sheets = sheets;
    }

    @Override
    public String toString() {
        return "ResolvedSheets{" +
                "classUnderTest=" + classUnderTest +
                ", specification=" + specification +
                ", sheets=" + sheets +
                '}';
    }
}
