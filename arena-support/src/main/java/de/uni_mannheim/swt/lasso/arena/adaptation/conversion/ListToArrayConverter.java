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

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ListToArrayConverter<T> implements Converter<List<T>, T[]> {

    @Override
    public T[] convert(List<T> list) {
        // conversion does not work for empty lists
        if(list == null || list.size() < 1) {
            return null;
        }

        return toArray(list);
    }

    @Override
    public boolean canConvert(Class<?> from, Class<?> to) {
        return TypeUtils.isAssignable(from, List.class) && to.isArray();
    }

    // copied from https://stackoverflow.com/a/6522958
    public static <T> T[] toArray(List<T> list) {
        T[] toR = (T[]) java.lang.reflect.Array.newInstance(list.get(0)
                .getClass(), list.size());
        for (int i = 0; i < list.size(); i++) {
            toR[i] = list.get(i);
        }
        return toR;
    }
}
