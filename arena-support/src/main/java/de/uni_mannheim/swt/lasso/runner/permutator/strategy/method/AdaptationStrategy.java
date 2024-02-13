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
import de.uni_mannheim.swt.lasso.runner.permutator.MethodSignature;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interface for specific adaptation strategies.
 *
 * @author Marcus Kessel
 */
public interface AdaptationStrategy {

    /**
     * Attention: Implementing class is supposed to match methods on its own.
     *
     * Match a method which may return one or more candidates (i.e permutations).
     *
     * Attention: Duplicate matches (i.e candidates) are removed after the adaptation process
     *
     * @param cutClass
     * @param returnType
     * @param paramTypes
     * @param method
     * @return
     */
    default List<Candidate> matchMethod(Class<?> cutClass, Class<?> returnType, Class<?>[] paramTypes, Method method) throws Throwable {
        return null;
    }

    /**
     * Match a method which may return one or more candidates (i.e permutations).
     *
     * Attention: Duplicate matches (i.e candidates) are removed after the adaptation process
     *
     * @param cutClass
     * @param returnType
     * @param paramTypes
     * @param method
     * @param positions
     * @return
     */
    default List<Candidate> match(Class<?> cutClass, Class<?> returnType, Class<?>[] paramTypes, Method method, int[] positions) throws Throwable {
        return null;
    }

    /**
     * Default behavior: simply return passed inputs (unchanged).
     *
     * Subtypes may override this behavior by returning modified inputs (e.g., like for conversion).
     *
     * @param candidate
     * @param inputs
     * @return
     * @throws Throwable
     */
    default Object[] preProcessInputs(Candidate candidate, Object[] inputs) throws Throwable {
        return inputs;
    }

    /**
     * Default behavior: simply return passed 'returned' value (unchanged) or re-throw thrown exception.
     *
     * Post-process the invocation result (i.e returned value/exception by invoking underlying candidate).
     *
     * Subtypes may decide to return modified returned value or exceptions (e.g., like for conversion).
     *
     * @param candidate
     * @param inputs
     * @param returned
     * @param throwable
     * @return
     */
    default Object postProcessInvocationResult(Candidate candidate, Object[] inputs, Object returned, Throwable throwable) throws Throwable {
        if(throwable != null) {
            return throwable;
        }

        return returned;
    }

    /**
     * Serialize from {@link Candidate} to {@link MethodSignature}.
     *
     * @param candidate
     * @param methodSignature
     */
    default void serialize(Candidate candidate, MethodSignature methodSignature) {
        //
    }

    /**
     * Deserialize from {@link MethodSignature} to {@link Candidate}.
     *
     * @param candidate
     * @param methodSignature
     */
    default void deserialize(Candidate candidate, MethodSignature methodSignature) {
        //
    }
}
