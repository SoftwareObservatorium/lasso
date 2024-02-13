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

import de.uni_mannheim.swt.lasso.runner.permutator.TypeUtils;

/**
 *
 * @author Marcus Kessel
 */
public class AssignableArray<T, S> implements Converter<T[], S[]> {

    @Override
    public S[] convert(T[] arr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S[] convert(T[] arr, Class<S[]> type) {
        //
        if(arr == null) {
            return null;
        }

        Class<?> requiredType = type.getComponentType();
        S[] toR = (S[]) java.lang.reflect.Array.newInstance(requiredType, arr.length);

        boolean number = TypeUtils.isAssignable(requiredType, Number.class, false);
        for (int i = 0; i < arr.length; i++) {
            T val = arr[i];

            if(number) {
                if(requiredType.equals(Byte.class)) {
                    toR[i] = (S) Byte.valueOf(((Number)val).byteValue());
                } else if(requiredType.equals(Character.class)) {
                    toR[i] = (S) Character.valueOf((char) ((Number)val).intValue());
                } else if(requiredType.equals(Short.class)) {
                    toR[i] = (S) Short.valueOf(((Number)val).shortValue());
                } else if(requiredType.equals(Integer.class)) {
                    toR[i] = (S) Integer.valueOf(((Number)val).intValue());
                } else if(requiredType.equals(Long.class)) {
                    toR[i] = (S) Long.valueOf(((Number)val).longValue());
                } else if(requiredType.equals(Double.class)) {
                    toR[i] = (S) Double.valueOf(((Number)val).doubleValue());
                } else if(requiredType.equals(Float.class)) {
                    toR[i] = (S) Float.valueOf(((Number)val).floatValue());
                }
            } else {
                // simple cast
                toR[i] = (S) arr[i];
            }
        }
        return toR;
    }

    @Override
    public boolean canConvert(Class<?> from, Class<?> to) {
        // both arrays?
        if(!from.isArray() || !to.isArray()) {
            return false;
        }

        // no primitives supported
        if(from.getComponentType().isPrimitive() || to.getComponentType().isPrimitive()) {
            return false;
        }

        // assignable?
        return TypeUtils.isAssignable(from.getComponentType(), to.getComponentType());
    }
}
