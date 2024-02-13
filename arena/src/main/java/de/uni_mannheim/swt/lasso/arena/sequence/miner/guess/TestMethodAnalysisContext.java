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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
public class TestMethodAnalysisContext {
    private static final Logger LOG = LoggerFactory.getLogger(TestMethodAnalysisContext.class);

    private final Map<String, Double> cutWeightMapping;

    private final Map<String, String> variableTypes;

    private final Map<String, LinkedHashSet<String>> methodSignatures;

    private String name;

    private String signature;

    public TestMethodAnalysisContext() {
        this.cutWeightMapping = new HashMap<>();
        this.variableTypes = new HashMap<>();
        this.methodSignatures = new HashMap<>();
    }

    public Map<String, Double> getCutWeightMapping() {
        return this.cutWeightMapping;
    }

    public void printCutWeightMapping() {
        System.out.println("=======================================================================");
        for (Map.Entry<String, Double> cutWeight : cutWeightMapping.entrySet()) {
            System.out.println("\t" + cutWeight.getKey() + ": " + cutWeight.getValue());
        }
        System.out.println("=======================================================================");
    }

    public void increaseCutMapping(String potentialCut, double weightIncrease) {
        if (cutWeightMapping.containsKey(potentialCut)) {
            double oldWeight = cutWeightMapping.get(potentialCut);
            cutWeightMapping.put(potentialCut, (oldWeight + weightIncrease));
        } else {
            cutWeightMapping.put(potentialCut, weightIncrease);
        }
    }

    public void mergeCutMapping(Map<String, Double> cutMapping) {
        cutMapping.forEach((type, score) -> cutWeightMapping.merge(type, score, Double::sum));
    }

    public void mergeMethodSignatures(Map<String, LinkedHashSet<String>> newMethodSignatures) {
        newMethodSignatures.forEach((type, signatures) -> this.methodSignatures.merge(type, signatures, (existingSignatures, newSignatures) -> {existingSignatures.addAll(newSignatures); return existingSignatures;}));
    }

    public CUTCandidate getHighestProbableCUT() {
        String probableCUT = null;
        double currentScore = -1;
        for(Map.Entry<String, Double> cutWeight : cutWeightMapping.entrySet()) {
            if (cutWeight.getValue() > currentScore) {
                currentScore = cutWeight.getValue();
                probableCUT = cutWeight.getKey();
            }
        }

        return new CUTCandidate(probableCUT, currentScore);
    }

    public void addMethodSignature(String typeName, String signature) {
        if (!methodSignatures.containsKey(typeName)) {
            methodSignatures.put(typeName, new LinkedHashSet<>());
        }
        methodSignatures.get(typeName).add(signature);
    }

    public void addMethodSignatures(String typeName, LinkedHashSet<String> signatures) {
        if (!methodSignatures.containsKey(typeName)) {
            methodSignatures.put(typeName, new LinkedHashSet<>());
        }
        methodSignatures.get(typeName).addAll(signatures);
    }

    public LinkedHashSet<String> getMethodSignatures(String typeName) {
        return methodSignatures.get(typeName);
    }

    public Map<String, LinkedHashSet<String>> getMethodSignatures() {
        return methodSignatures;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getVariableTypes() {
        return variableTypes;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
