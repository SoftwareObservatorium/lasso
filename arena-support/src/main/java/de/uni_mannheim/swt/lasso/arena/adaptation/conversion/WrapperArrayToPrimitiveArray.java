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
public class WrapperArrayToPrimitiveArray<T> implements Converter<T[], Object> {

    @Override
    public Object convert(T[] arr) {
        //
        if(arr == null) {
            return null;
        }

        return ArrayUtils.toPrimitive(arr);
    }

    @Override
    public boolean canConvert(Class<?> from, Class<?> to) {
        // both arrays?
        if(!from.isArray() || !to.isArray()) {
            return false;
        }

        // wrapper + primitive present?
        if(!ClassUtils.isPrimitiveWrapper(from.getComponentType()) || !to.getComponentType().isPrimitive()) {
            return false;
        }

        // compare wrappers
        return from.getComponentType().equals(ClassUtils.primitiveToWrapper(to.getComponentType()));
    }
}
