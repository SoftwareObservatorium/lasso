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
package de.uni_mannheim.swt.lasso.runner.permutator.strategy.method;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.TypeUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An adaptation strategy based on mutable objects.
 *
 * @author Marcus Kessel
 */
public class MutabilityStrategyByValue implements AdaptationStrategy {

    /**
     * Required
     *
     * <pre>
     *     char[] doSomething(char[] arr)
     * </pre>
     *
     * Given CUT (which actually is assumed to return a modified copy of the given array).
     *
     * <pre>
     *     void doSomething(char[] arr)
     * </pre>
     */
    @Override
    public List<Candidate> match(Class<?> cutClass, Class<?> returnType, Class<?>[] paramTypes, Method method, int[] positions) throws Throwable {
        // FIXME currently only single mutable input supported
        // void and mutable input
        if(!(method.getReturnType() == void.class
                && method.getParameterCount() > 0
                && MutabilityStrategyByReference.hasMutableTypes(paramTypes))) {
            return null;
        }

        // has return type in inputs?
        if(!MutabilityStrategyByReference.hasEqualInputOutput(returnType, paramTypes)) {
            return null;
        }

        // is assignable?
        // TODO convert
        if(!TypeUtils.isAssignable(paramTypes, method.getParameterTypes())) {
            return null;
        }

        Candidate candidate = new Candidate(method, positions);

        // set required types
        candidate.setParamClasses(paramTypes);
        candidate.setReturnType(returnType);

        // set adaptation strategy
        candidate.setAdaptationStrategy(this);

        return new ArrayList<>(Arrays.asList(candidate));
    }

    /**
     * Post-process invocation result.
     *
     * @param candidate
     * @param inputs
     * @param returned
     * @param throwable
     * @return
     */
    @Override
    public Object postProcessInvocationResult(Candidate candidate, Object[] inputs, Object returned, Throwable throwable) throws Throwable {
        // simply-rethrow
        if(throwable != null) {
            throw throwable;
        }

        // here is the trick
        Class<?> returnType = candidate.getReturnType();

        // lookup index of input value
        int index = 0;
        Class<?>[] paramTypes = candidate.getParamClasses();
        for(int i = 0; i < paramTypes.length; i++) {
            if(paramTypes[i] == returnType) {
                index = i;

                break;
            }
        }

        // return mutated input
        return inputs[index];
    }
}
