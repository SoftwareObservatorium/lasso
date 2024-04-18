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

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class RandomObjectGeneratorTest {

    @Test
    public void testRandom() {
        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();
        System.out.println(randomObjectGenerator.random(byte.class));
        System.out.println(randomObjectGenerator.random(char.class));
        System.out.println(randomObjectGenerator.random(short.class));
        System.out.println(randomObjectGenerator.random(int.class));
        System.out.println(randomObjectGenerator.random(long.class));
        System.out.println(randomObjectGenerator.random(float.class));
        System.out.println(randomObjectGenerator.random(double.class));

        System.out.println(randomObjectGenerator.random(Byte.class));
        System.out.println(randomObjectGenerator.random(Character.class));
        System.out.println(randomObjectGenerator.random(Short.class));
        System.out.println(randomObjectGenerator.random(Integer.class));
        System.out.println(randomObjectGenerator.random(Long.class));
        System.out.println(randomObjectGenerator.random(Float.class));
        System.out.println(randomObjectGenerator.random(Double.class));

        System.out.println(randomObjectGenerator.random(Object.class));
        System.out.println(randomObjectGenerator.random(String.class));
        System.out.println(randomObjectGenerator.random(Date.class));

        System.out.println(Arrays.toString(randomObjectGenerator.random(byte[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(char[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(short[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(int[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(long[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(float[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(double[].class)));

        System.out.println(Arrays.toString(randomObjectGenerator.random(Byte[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(Character[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(Short[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(Integer[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(Long[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(Float[].class)));
        System.out.println(Arrays.toString(randomObjectGenerator.random(Double[].class)));
    }

    @Test
    public void testObject() {
        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();
        System.out.println(randomObjectGenerator.random(Object.class).toString());
    }

    @Test
    public void testRandom_collections_generics() {
        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();
        System.out.println(randomObjectGenerator.random(List.class, String.class));
        System.out.println(randomObjectGenerator.random(Map.class, String.class, Integer.class));
        System.out.println(randomObjectGenerator.random(Set.class, String.class));
    }

    @Test
    public void testRandomSeed() {
        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();

        long seed = 42;

        assertEquals(1131, randomObjectGenerator.random(int.class, seed));
    }
}
