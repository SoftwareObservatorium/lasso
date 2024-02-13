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
package com.jn.langx.util.concurrent;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentHashSet<E> extends AbstractSet<E> {
    private ConcurrentMap<E, Integer> map;

    public ConcurrentHashSet() {
        this.map = new ConcurrentHashMap<E, Integer>();
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean add(E e) {
        if (e == null) {
            return false;
        }
        map.putIfAbsent(e, 1);
        return true;
    }

    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        Integer value = map.remove(o);
        return value == null || value == 1;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        Integer v = map.get(o);
        return v != null && v == 1;
    }

    @Override
    public int size() {
        return map.size();
    }
}
