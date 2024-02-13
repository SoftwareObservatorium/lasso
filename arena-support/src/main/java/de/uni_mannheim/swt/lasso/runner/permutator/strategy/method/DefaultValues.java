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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class DefaultValues {

    static int[] intValues = new int[]{-1, 0, 1};

    static Map<Class<?>, Object[]> defaults = new HashMap<>();
    static {
        defaults.put(boolean.class, new Object[]{true, false});
        defaults.put(Boolean.class, new Object[]{true, false});

        defaults.put(char.class, Arrays.stream(intValues).mapToObj(v -> (char) v).toArray());
        defaults.put(Character.class, defaults.get(char.class));

        defaults.put(short.class, Arrays.stream(intValues).mapToObj(v -> (short) v).toArray());
        defaults.put(Short.class, defaults.get(short.class));

        defaults.put(int.class, Arrays.stream(intValues).mapToObj(v -> (int) v).toArray());
        defaults.put(Integer.class, defaults.get(int.class));

        defaults.put(float.class, Arrays.stream(intValues).mapToObj(v -> (float) v).toArray());
        defaults.put(Float.class, defaults.get(float.class));

        defaults.put(double.class, Arrays.stream(intValues).mapToObj(v -> (double) v).toArray());
        defaults.put(Double.class, defaults.get(double.class));

        defaults.put(String.class, new String[]{null, ""});
    }
}
