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

import java.lang.reflect.Array;

/**
 *
 * @author Marcus Kessel
 */
public class PrimitiveAssignableArray implements Converter<Object, Object> {

    @Override
    public Object convert(Object arr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object convert(Object arr, Class type) {
        //
        if(arr == null) {
            return null;
        }

        int length = Array.getLength(arr);

        Class<?> requiredType = type.getComponentType();
        Object toR = java.lang.reflect.Array.newInstance(requiredType.isArray() ? requiredType.getComponentType() : requiredType, length);

        boolean number = TypeUtils.isAssignable(requiredType, Number.class);
        for (int i = 0; i < length; i++) {
            Object val = Array.get(arr, i);

            Object valSet = null;
            if(number) {
                if(requiredType.equals(byte.class)) {
                    valSet = Byte.valueOf(((Number)val).byteValue());
                } else if(requiredType.equals(char.class)) {
                    valSet = Character.valueOf((char) ((Number)val).intValue());
                } else if(requiredType.equals(short.class)) {
                    valSet= Short.valueOf(((Number)val).shortValue());
                } else if(requiredType.equals(int.class)) {
                    valSet = Integer.valueOf(((Number)val).intValue());
                } else if(requiredType.equals(long.class)) {
                    valSet = Long.valueOf(((Number)val).longValue());
                } else if(requiredType.equals(double.class)) {
                    valSet = Double.valueOf(((Number)val).doubleValue());
                } else if(requiredType.equals(float.class)) {
                    valSet = Float.valueOf(((Number)val).floatValue());
                }
            } else {
                // simple cast
                valSet = val;
            }

            Array.set(toR, i, valSet);
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
        if(!from.getComponentType().isPrimitive() || !to.getComponentType().isPrimitive()) {
            return false;
        }

        // assignable?
        return TypeUtils.isAssignable(from.getComponentType(), to.getComponentType());
    }
}
