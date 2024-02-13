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
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An adaptation strategy based on mutable objects.
 *
 * @author Marcus Kessel
 */
public class MutabilityStrategyByReference implements AdaptationStrategy {

    /**
     * Required
     *
     * <pre>
     *     void doSomething(char[] arr)
     * </pre>
     *
     * Given CUT (which actually is assumed to return a modified copy of the given array).
     *
     * <pre>
     *     char[] doSomething(char[] arr)
     * </pre>
     */
    @Override
    public List<Candidate> match(Class<?> cutClass, Class<?> returnType, Class<?>[] paramTypes, Method method, int[] positions) throws Throwable {
        // FIXME currently only single mutable input supported
        // void and mutable input
        if(!(returnType == void.class
                && method.getParameterCount() > 0
                && hasMutableTypes(paramTypes))) {
            return null;
        }

        // has return type in inputs?
        if(!hasEqualInputOutput(method.getReturnType(), method.getParameterTypes())) {
            return null;
        }

        // is assignable?
        // TODO convert
        if(!TypeUtils.isAssignable(paramTypes, method.getParameterTypes())) {
            return null;
        }

        Candidate candidate = new Candidate(method, positions);

        // set adaptation strategy
        candidate.setAdaptationStrategy(this);

        return new ArrayList<>(Arrays.asList(candidate));
    }

    public static boolean hasEqualInputOutput(Class<?> returnType, Class<?>[] paramTypes) {
        return Arrays.stream(paramTypes).filter(c -> c == returnType).findFirst().isPresent();
    }

    public static boolean hasMutableTypes(Class<?>[] paramTypes) {
        return Arrays.stream(paramTypes).filter(MutabilityStrategyByReference::isMutable).findFirst().isPresent();
    }

    /**
     * Is given {@link Class} mutable? I.e complex types that is changeable.
     *
     * Attention: Java has no first-class immutability support,
     * so this method is more like a best-effort approach.
     *
     * @param clazz
     * @return
     */
    public static boolean isMutable(Class clazz) {
        // strings are immutable
        if(clazz == String.class) {
            return false;
        }

        // primitives and their wrappers are immutable
        return !ClassUtils.isPrimitiveOrWrapper(clazz);
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
        Class<?> returnType = ((Method)candidate.getMethod()).getReturnType();

        // lookup index of input value
        int index = 0;
        Class<?>[] paramTypes = ((Method)candidate.getMethod()).getParameterTypes();
        for(int i = 0; i < paramTypes.length; i++) {
            if(paramTypes[i] == returnType) {
                index = i;

                break;
            }
        }

        // set mutated inputs
        inputs[index] = returned;

        return null;
    }
}
