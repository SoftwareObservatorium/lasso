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
package com.buschmais.cdo.impl.proxy.collection;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class SetProxy<T> extends AbstractSet<T> implements Set<T> {

    private final AbstractCollectionProxy<T, ?, ?, ?> collectionProxy;

    public SetProxy(AbstractCollectionProxy<T, ?, ?, ?> collectionProxy) {
        this.collectionProxy = collectionProxy;
    }

    @Override
    public Iterator<T> iterator() {
        return collectionProxy.iterator();
    }

    @Override
    public int size() {
        return collectionProxy.size();
    }

    @Override
    public boolean add(T t) {
        if (contains(t)) {
            return false;
        }
        return collectionProxy.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return collectionProxy.remove(o);
    }
}
