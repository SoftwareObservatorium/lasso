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
package de.uni_mannheim.swt.lasso.runner.permutator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

/**
 *
 * @author Marcus Kessel
 */
public class TypeUtils {

    /**
     * Convert all primitive wrappers to primitives in order to allow casting
     *
     * @param params
     * @return
     */
    public static Class<?>[] wrappersToPrimitives(Class<?>[] params) {
        if (ArrayUtils.isEmpty(params)) {
            return new Class<?>[] {};
        }
        Class<?>[] nParams = new Class<?>[params.length];
        int i = 0;
        for (Class<?> clazz : params) {
            if (ClassUtils.isPrimitiveWrapper(clazz)) {
                nParams[i] = ClassUtils.wrapperToPrimitive(clazz);
            } else {
                nParams[i] = clazz;
            }

            // inc
            i++;
        }

        return nParams;
    }

    /**
     * Check if the params arrays are assignable to each other (including
     * casting of primitives)
     *
     * @param classArray
     * @param toClassArray
     * @return
     */
    public static boolean isAssignable(Class<?>[] classArray,
                                       Class<?>[] toClassArray) {
        Class<?>[] nClassArray = wrappersToPrimitives(classArray);
        Class<?>[] nToClassArray = wrappersToPrimitives(toClassArray);

        return ClassUtils.isAssignable(nClassArray, nToClassArray, true);
    }

    /**
     * Check if first class is assignable to the second (including casting of
     * primitives)
     *
     * @param clazz
     * @param toClazz
     * @param isReturn
     * @return
     */
    public static boolean isAssignable(Class<?> clazz, Class<?> toClazz, boolean isReturn) {
        // FIXME decide if void is useful or not
        if(isReturn) {
            // expected void: allow any
            if(void.class.equals(clazz) || Void.class.equals(clazz)) {
                return true;
            }

            // actual void
            if(void.class.equals(toClazz) || Void.class.equals(toClazz)) {
                return true;
            }
        }

        return isAssignable(new Class<?>[] { clazz },
                new Class<?>[] { toClazz });
    }

    /**
     * Check if first class is assignable to the second (including casting of
     * primitives)
     *
     * @param clazz
     * @param toClazz
     * @return
     */
    public static boolean isAssignable(Class<?> clazz, Class<?> toClazz) {
        return isAssignable(clazz, toClazz, false);
    }
}
