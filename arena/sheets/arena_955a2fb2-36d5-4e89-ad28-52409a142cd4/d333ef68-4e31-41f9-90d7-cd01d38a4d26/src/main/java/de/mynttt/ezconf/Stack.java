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
package de.mynttt.ezconf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class Stack<T> implements Iterable<T> {
    private final ArrayList<T> backing = new ArrayList<>();
    private int idx = -1;

    public T pop() {
        if(idx < 0)
            throw new NoSuchElementException("stack is empty");
        return backing.remove(idx--);
    }

    public T peek() {
        if(idx < 0)
            throw new NoSuchElementException("stack is empty");
        return backing.get(idx);
    }

    public boolean isEmpty() {
        return idx < 0;
    }

    public void push(T element) {
        backing.add(element);
        ++idx;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final Iterator<T> it = backing.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next();
            }
        };
    }

    public int size() {
        return idx + 1;
    }

}
