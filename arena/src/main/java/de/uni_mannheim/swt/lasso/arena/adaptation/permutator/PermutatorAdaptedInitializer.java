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
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.Converter;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.JavaConverterStrategy;
import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;

import java.lang.reflect.Member;
import java.util.Optional;

/**
 * An adapted initializer
 *
 * @author Marcus Kessel
 */
public class PermutatorAdaptedInitializer extends AdaptedInitializer {

    private final JavaConverterStrategy javaConverterStrategy;

    private final Candidate candidate;

    public PermutatorAdaptedInitializer(MethodSignature specification, ClassUnderTest adaptee, Member initializer, JavaConverterStrategy javaConverterStrategy, Candidate candidate) {
        super(specification, adaptee, initializer);
        this.javaConverterStrategy = javaConverterStrategy;
        this.candidate = candidate;
    }

    @Override
    public boolean canConvert(Class<?> fromClazz, Class<?> toClazz) {
        return javaConverterStrategy.canConvert(fromClazz, toClazz);
    }

    @Override
    public Class<? extends Converter> getConverterClass(Class<?> fromClazz, Class<?> toClazz) {
        Optional<Converter<?, ?>> converter = javaConverterStrategy.getConverter(fromClazz, toClazz);

        return converter.orElseThrow((() -> new IllegalArgumentException("no converter found"))).getClass();
    }

    public Candidate getCandidate() {
        return candidate;
    }
}
