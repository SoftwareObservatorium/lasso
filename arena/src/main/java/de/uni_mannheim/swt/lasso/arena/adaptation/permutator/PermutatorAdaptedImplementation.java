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
package de.uni_mannheim.swt.lasso.arena.adaptation.permutator;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.*;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.JavaConverterStrategy;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.core.adapter.AdapterDesc;
import de.uni_mannheim.swt.lasso.core.adapter.MethodDesc;
import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * An adapted implementation.
 *
 * @author Marcus Kessel
 */
public class PermutatorAdaptedImplementation extends AdaptedImplementation {

    private static JavaConverterStrategy javaConverterStrategy = new JavaConverterStrategy();

    private ClassPermutation adapter;

    public PermutatorAdaptedImplementation(ClassUnderTest adaptee, ClassPermutation adapter) {
        super(adaptee);

        this.adapter = adapter;
    }

    @Override
    public AdaptedMethod getMethod(InterfaceSpecification specification, int m) {
        MethodSignature signature = specification.getMethods().get(m);

        Candidate candidate = adapter.getMethods().get(m);

        if(candidate == null) {
            throw new MissingCandidateMethodException(String.format("Missing method for signature '%s'", signature.toLQL()));
        }

        PermutatorAdaptedMethod adaptedMethod = new PermutatorAdaptedMethod(
                signature,
                getAdaptee(),
                (Method) candidate.getMethod(),
                javaConverterStrategy);

        adaptedMethod.setPositions(candidate.getPositions());

        return adaptedMethod;
    }

    @Override
    public int getNoOfMethods() {
        return adapter.getMethods().size();
    }

    @Override
    public AdaptedInitializer getInitializer(InterfaceSpecification specification, int c) {
        MethodSignature signature = specification.getConstructors().get(c);

        List<Candidate> candidates = adapter.getConstructors().get(c);
        Candidate candidate = candidates.get(0); // best match

        if(candidate == null) {
            throw new MissingCandidateMethodException(String.format("Missing constructor for signature '%s'", signature.toLQL()));
        }

        // FIXME can be any "Member"
        PermutatorAdaptedInitializer adaptedInitializer = new PermutatorAdaptedInitializer(
                signature,
                getAdaptee(),
                candidate.getMethod(),
                javaConverterStrategy,
                candidate);
        adaptedInitializer.setPositions(candidate.getPositions());

        return adaptedInitializer;
    }

    @Override
    public AdaptedInitializer getDefaultInitializer() {
        // FIXME which is the default?
        List<Candidate> candidates = adapter.getConstructors().get(0);

        Candidate candidate = candidates.get(0); // best match

        PermutatorAdaptedInitializer adaptedInitializer = new PermutatorAdaptedInitializer(
                new MethodSignature(null),
                getAdaptee(),
                candidate.getMethod(),
                javaConverterStrategy,
                candidate);
        adaptedInitializer.setPositions(candidate.getPositions());

        return adaptedInitializer;
    }

    @Override
    public String getAdapterId() {
        return String.valueOf(adapter.getId());
    }

    public ClassPermutation getAdapter() {
        return adapter;
    }

    public AdapterDesc toDescription(InterfaceSpecification interfaceSpecification) {
        AdapterDesc adapterDesc = new AdapterDesc();
        adapterDesc.setAdapterId(getAdapterId());
        adapterDesc.setSystemId(getAdaptee().getId());
        adapterDesc.setClassName(getAdaptee().getClassName());

        List<MethodDesc> initializers = new ArrayList<>(interfaceSpecification.getConstructors().size());
        adapterDesc.setInitializers(initializers);
        for(int m = 0; m <interfaceSpecification.getConstructors().size(); m++) {
            try {
                AdaptedInitializer init = getInitializer(interfaceSpecification, m);

                MethodDesc methodDesc = new MethodDesc();
                initializers.add(methodDesc);
                methodDesc.setConstructor(init.isConstructor());
                if(init.hasMember()) {
                    methodDesc.setMethod(init.getMember().toString());
                } else {
                    methodDesc.setMethod("_UNKNOWN_");
                }

                List<Candidate> candidates = adapter.getConstructors().get(m);
                Candidate candidate = candidates.get(0); // best match

                if(candidate != null) {
                    methodDesc.setDeclaringClass(candidate.getMethod().getDeclaringClass().getName());
                    methodDesc.setPositions(candidate.getPositions());
                    methodDesc.setAdaptationStrategy(
                            candidate.getAdaptationStrategy() != null ? candidate.getAdaptationStrategy().getClass().getName() : null);
                    methodDesc.setProducerStrategy(
                            candidate.getProducerStrategy() != null ? candidate.getProducerStrategy().getClass().getName() : null);
                    methodDesc.setConverterStrategy(javaConverterStrategy.getClass().getName());
                }
            } catch (Throwable e) {
                MethodDesc methodDesc = new MethodDesc();
                initializers.add(methodDesc);
                //methodDesc.setConstructor(init.isConstructor());
                methodDesc.setMethod("_NA_");
            }
        }

        List<MethodDesc> methods = new ArrayList<>(interfaceSpecification.getConstructors().size());
        adapterDesc.setMethods(methods);
        for(int m = 0; m < interfaceSpecification.getMethods().size(); m++) {
            AdaptedMethod am = getMethod(interfaceSpecification, m);

            MethodDesc methodDesc = new MethodDesc();
            methods.add(methodDesc);
            methodDesc.setConstructor(false);
            methodDesc.setMethod(am.getMember().toString());

            Candidate candidate = adapter.getMethods().get(m);

            if(candidate != null) {
                methodDesc.setDeclaringClass(candidate.getMethod().getDeclaringClass().getName());
                methodDesc.setPositions(candidate.getPositions());
                methodDesc.setAdaptationStrategy(
                        candidate.getAdaptationStrategy() != null ? candidate.getAdaptationStrategy().getClass().getName() : null);
                methodDesc.setProducerStrategy(
                        candidate.getProducerStrategy() != null ? candidate.getProducerStrategy().getClass().getName() : null);
                methodDesc.setConverterStrategy(javaConverterStrategy.getClass().getName());
            }
        }

        return adapterDesc;
    }

    @Override
    public String toString() {
        return "PermutatorAdaptedImplementation{" +
                "adaptee=" + getAdaptee() +
                "adapter=" + adapter +
                '}';
    }
}
