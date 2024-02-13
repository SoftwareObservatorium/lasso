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
package de.uni_mannheim.swt.lasso.arena.adaptation.conversion;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

/**
 *
 * @author Marcus Kessel
 */
public class PrimitiveArrayToWrapperArray<S> implements Converter<Object, S[]> {

    /**
     * ({@link Boolean}, {@link Byte}, {@link Character},
     *      * {@link Short}, {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
     *
     * @param arr
     * @return
     */
    @Override
    public S[] convert(Object arr) {
        //
        if(arr == null) {
            return null;
        }

        Object val = arr;
        Class<?> componentType = arr.getClass().getComponentType();
        if(componentType.equals(boolean.class)) {
            return (S[]) ArrayUtils.toObject((boolean[]) val);
        } else if(componentType.equals(byte.class)) {
            return (S[]) ArrayUtils.toObject((byte[]) val);
        } else if(componentType.equals(char.class)) {
            return (S[]) ArrayUtils.toObject((char[]) val);
        } else if(componentType.equals(short.class)) {
            return (S[]) ArrayUtils.toObject((short[]) val);
        } else if(componentType.equals(int.class)) {
            return (S[]) ArrayUtils.toObject((int[]) val);
        } else if(componentType.equals(long.class)) {
            return (S[]) ArrayUtils.toObject((long[]) val);
        } else if(componentType.equals(double.class)) {
            return (S[]) ArrayUtils.toObject((double[]) val);
        } else if(componentType.equals(float.class)) {
            return (S[]) ArrayUtils.toObject((float[]) val);
        }

        throw new IllegalArgumentException("Unsupported Primitive type " + componentType.getName());
    }

    @Override
    public boolean canConvert(Class<?> from, Class<?> to) {
        // both arrays?
        if(!from.isArray() || !to.isArray()) {
            return false;
        }

        // wrapper + primitive present?
        if(!ClassUtils.isPrimitiveWrapper(to.getComponentType()) || !from.getComponentType().isPrimitive()) {
            return false;
        }

        // compare primitives
        return from.getComponentType().equals(ClassUtils.wrapperToPrimitive(to.getComponentType()));
    }
}
