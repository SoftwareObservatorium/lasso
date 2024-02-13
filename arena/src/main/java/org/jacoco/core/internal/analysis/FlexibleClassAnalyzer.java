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
package org.jacoco.core.internal.analysis;

/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
//package org.jacoco.core.internal.analysis;

import java.util.HashSet;
import java.util.Set;

import org.jacoco.core.internal.analysis.filter.Filters;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.flow.ClassProbesVisitor;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * Copied to hook into JaCoCo's internals.
 *
 * Analyzes the structure of a class.
 */
public class FlexibleClassAnalyzer extends ClassProbesVisitor
        implements IFilterContext {

    private final ClassCoverageImpl coverage;
    private final boolean[] probes;
    private final StringPool stringPool;

    private final Set<String> classAnnotations = new HashSet<String>();

    private final Set<String> classAttributes = new HashSet<String>();

    private String sourceDebugExtension;

    private final IFilter filter;

    /**
     * Creates a new analyzer that builds coverage data for a class.
     *
     * @param coverage
     *            coverage node for the analyzed class data
     * @param probes
     *            execution data for this class or <code>null</code>
     * @param stringPool
     *            shared pool to minimize the number of {@link String} instances
     */
    public FlexibleClassAnalyzer(final ClassCoverageImpl coverage,
                         final boolean[] probes, final StringPool stringPool, IFilter filter) {
        this.coverage = coverage;
        this.probes = probes;
        this.stringPool = stringPool;
        this.filter = filter;
    }

    @Override
    public void visit(final int version, final int access, final String name,
                      final String signature, final String superName,
                      final String[] interfaces) {
        coverage.setSignature(stringPool.get(signature));
        coverage.setSuperName(stringPool.get(superName));
        coverage.setInterfaces(stringPool.get(interfaces));
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
                                             final boolean visible) {
        classAnnotations.add(desc);
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        classAttributes.add(attribute.type);
    }

    @Override
    public void visitSource(final String source, final String debug) {
        coverage.setSourceFileName(stringPool.get(source));
        sourceDebugExtension = debug;
    }

    @Override
    public MethodProbesVisitor visitMethod(final int access, final String name,
                                           final String desc, final String signature,
                                           final String[] exceptions) {

        InstrSupport.assertNotInstrumented(name, coverage.getName());

        final InstructionsBuilder builder = new InstructionsBuilder(probes);

        return new MethodAnalyzer(builder) {

            @Override
            public void accept(final MethodNode methodNode,
                               final MethodVisitor methodVisitor) {
                super.accept(methodNode, methodVisitor);
                addMethodCoverage(stringPool.get(name), stringPool.get(desc),
                        stringPool.get(signature), builder, methodNode);
            }
        };
    }

    private void addMethodCoverage(final String name, final String desc,
                                   final String signature, final InstructionsBuilder icc,
                                   final MethodNode methodNode) {
        final MethodCoverageCalculator mcc = new MethodCoverageCalculator(
                icc.getInstructions());
        filter.filter(methodNode, this, mcc);

        final MethodCoverageImpl mc = new MethodCoverageImpl(name, desc,
                signature);
        mcc.calculate(mc);

        if (mc.containsCode()) {
            // Only consider methods that actually contain code
            coverage.addMethod(mc);
        }

    }

    @Override
    public FieldVisitor visitField(final int access, final String name,
                                   final String desc, final String signature, final Object value) {
        InstrSupport.assertNotInstrumented(name, coverage.getName());
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitTotalProbeCount(final int count) {
        // nothing to do
    }

    // IFilterContext implementation

    public String getClassName() {
        return coverage.getName();
    }

    public String getSuperClassName() {
        return coverage.getSuperName();
    }

    public Set<String> getClassAnnotations() {
        return classAnnotations;
    }

    public Set<String> getClassAttributes() {
        return classAttributes;
    }

    public String getSourceFileName() {
        return coverage.getSourceFileName();
    }

    public String getSourceDebugExtension() {
        return sourceDebugExtension;
    }

}