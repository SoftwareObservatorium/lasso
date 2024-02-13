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
package de.uni_mannheim.swt.lasso.analyzer.asm.jacoco;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICoverageVisitor;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.objectweb.asm.ClassReader;

/**
 * Measure static code metrics using JaCoCo.
 * 
 * @author Marcus Kessel
 *
 */
public class JaCoCoSizeMetrics implements ICoverageVisitor {

    private final Map<String, Map<String, Double>> methodBodyWeights = new HashMap<>();

    /**
     * Measure metrics for given array of classes, jars etc
     * 
     * @param classReader
     *            classes, jars etc.
     * @param className
     * @throws IOException
     */
    public JaCoCoSizeMetrics(ClassReader classReader, String className) throws IOException {
        Analyzer analyzer = new Analyzer(new ExecutionDataStore(), this);

        analyzer.analyzeClass(classReader.b, className);
    }

    /**
     * {@inheritDoc}
     */
    public void visitCoverage(final IClassCoverage coverage) {
        Collection<IMethodCoverage> methods = coverage.getMethods();
        if (methods != null) {
            for (IMethodCoverage method : methods) {
                StringBuilder signature = new StringBuilder();
                // Java FQ name expected instead of Byte code FQ
                signature.append(coverage.getName());
                signature.append(".");
                signature.append(method.getName());
                signature.append(method.getDesc());

                Map<String, Double> metrics = new HashMap<String, Double>();
                methodBodyWeights.put(signature.toString(), metrics);

                metrics.put("instr", Double.valueOf(method.getInstructionCounter().getTotalCount()));
                metrics.put("branch", Double.valueOf(method.getBranchCounter().getTotalCount()));
                metrics.put("line", Double.valueOf(method.getLineCounter().getTotalCount()));
                // metrics.put("METHOD",
                // Double.valueOf(method.getMethodCounter()
                // .getTotalCount()));
                metrics.put("complexity", Double.valueOf(method.getComplexityCounter().getTotalCount()));
            }

        }
    }

    public Map<String, Map<String, Double>> getMethodBodyWeights() {
        return methodBodyWeights;
    }
}
