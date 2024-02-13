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
package de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.MethodSignature;

import java.util.List;

/**
 * Locate/match producers for some object T.
 *
 * @author Marcus Kessel
 */
public interface ProducerStrategy {

    /**
     * Match producer.
     *
     * @param t
     * @param paramTypes
     * @return
     * @throws Throwable
     */
    List<Candidate> match(Class<?> t, Class<?>[] paramTypes) throws Throwable;

    /**
     * Create instance
     *
     * @param candidate
     * @param inputs
     * @return
     * @throws Throwable
     */
    Object createInstance(Candidate candidate, Object[] inputs) throws Throwable;

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
