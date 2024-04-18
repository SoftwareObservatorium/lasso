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
package de.uni_mannheim.swt.lasso.testing.generate.random;

import org.instancio.Instancio;
import org.instancio.settings.Settings;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Random object value generator based on Instanceio.
 *
 * The generation can be controlled via {@link #settings}.
 *
 * @author Marcus Kessel
 */
public class RandomObjectGenerator {

    private final List<Class<?>> TYPES = Arrays.asList(
            boolean.class,
            byte.class,
            char.class,
            int.class,
            short.class,
            long.class,
            float.class,
            double.class,
            String.class
    );

    private final Settings settings;

    public RandomObjectGenerator() {
        this(Settings.defaults());
    }

    public RandomObjectGenerator(Settings settings) {
        this.settings = settings;
    }

    /**
     * Generate random value.
     *
     * @param type
     * @return
     * @param <T>
     */
    public <T> T random(Class<T> type) {
        if(type == Object.class) {
            // make random choice of object
            type = (Class<T>) randomType();
        }

        return Instancio.of(type).withSettings(settings).asResult().get();
    }

    public Class<?> randomType() {
        return TYPES.get(new Random().nextInt(TYPES.size()));
    }

    /**
     * Generate random value based on given random seed value (makes it deterministic).
     *
     * @param type
     * @param seed
     * @return
     * @param <T>
     */
    public <T> T random(Class<T> type, long seed) {
        if(seed > -1) {
            return Instancio.of(type).withSettings(settings).withSeed(seed).asResult().get();
        } else {
            return random(type);
        }
    }

    /**
     * Generate random value.
     *
     * @param type
     * @param typeParameters Type parameters (generics)
     * @return
     * @param <T>
     */
    public <T> T random(Class<T> type, Class<?> ... typeParameters) {
        return Instancio.of(type).withTypeParameters(typeParameters).withSettings(settings)
                .asResult().get();
    }

    /**
     * Generate random value.
     *
     * @param type
     * @param seed
     * @param typeParameters Type parameters (generics)
     * @return
     * @param <T>
     */
    public <T> T random(Class<T> type, long seed, Class<?> ... typeParameters) {
        return Instancio.of(type).withTypeParameters(typeParameters).withSettings(settings)
                .asResult().get();
    }
}

