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
package de.uni_mannheim.swt.lasso.arena.sequence.miner.guess;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/*
Copyright (c) 2022, Chair of Software Technology
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of the University Mannheim nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Malte Brockmeier
 */
public class TestClassAnalysisContext {
    private HashMap<String, String> variableTypes;

    private HashMap<String, TestMethodAnalysisContext> testMethodAnalysisContextCache;

    private List<String> dependencies;

    private List<String> beforeCalls;

    private List<String> afterCalls;

    private List<TestMethod> testMethods;

    public TestClassAnalysisContext() {
        this.variableTypes = new HashMap<>();
        this.beforeCalls = new LinkedList<>();
        this.afterCalls = new LinkedList<>();
        this.testMethods = new LinkedList<>();
        this.dependencies = new LinkedList<>();
        this.testMethodAnalysisContextCache = new HashMap<>();
    }

    public HashMap<String, String> getVariableTypes() {
        return variableTypes;
    }

    public void setVariableTypes(HashMap<String, String> variableTypes) {
        this.variableTypes = variableTypes;
    }

    public List<String> getBeforeCalls() {
        return beforeCalls;
    }

    public void setBeforeCalls(List<String> beforeCalls) {
        this.beforeCalls = beforeCalls;
    }

    public List<String> getAfterCalls() {
        return afterCalls;
    }

    public void setAfterCalls(List<String> afterCalls) {
        this.afterCalls = afterCalls;
    }

    public List<TestMethod> getTestMethods() {
        return testMethods;
    }

    public void setTestMethods(List<TestMethod> testMethods) {
        this.testMethods = testMethods;
    }

    public HashMap<String, TestMethodAnalysisContext> getTestMethodAnalysisContextCache() {
        return testMethodAnalysisContextCache;
    }

    public void setTestMethodAnalysisContextCache(HashMap<String, TestMethodAnalysisContext> testMethodAnalysisContextCache) {
        this.testMethodAnalysisContextCache = testMethodAnalysisContextCache;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
}
