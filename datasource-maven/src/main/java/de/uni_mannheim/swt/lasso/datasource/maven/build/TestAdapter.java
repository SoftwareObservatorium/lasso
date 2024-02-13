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
package de.uni_mannheim.swt.lasso.datasource.maven.build;

import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;

import java.io.Serializable;
import java.util.List;

/**
 * Test adapter
 * 
 * @author Marcus Kessel
 *
 */
public class TestAdapter implements Serializable {
    
    private List<CompilationUnit> tests;
    
    private CompilationUnit adapter;
    private Candidate adaptee;
    
    /**
     * @return the tests
     */
    public List<CompilationUnit> getTests() {
        return tests;
    }
    /**
     * @param tests the test to set
     */
    public void setTests(List<CompilationUnit> tests) {
        this.tests = tests;
    }
    /**
     * @return the adapter
     */
    public CompilationUnit getAdapter() {
        return adapter;
    }
    /**
     * @param adapter the adapter to set
     */
    public void setAdapter(CompilationUnit adapter) {
        this.adapter = adapter;
    }
    /**
     * @return the adaptee
     */
    public Candidate getAdaptee() {
        return adaptee;
    }
    /**
     * @param adaptee the adaptee to set
     */
    public void setAdaptee(Candidate adaptee) {
        this.adaptee = adaptee;
    }
}
