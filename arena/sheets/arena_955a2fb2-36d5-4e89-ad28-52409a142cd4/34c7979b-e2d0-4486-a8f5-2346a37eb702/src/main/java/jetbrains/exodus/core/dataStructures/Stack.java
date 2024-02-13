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
package jetbrains.exodus.core.dataStructures;

import java.util.ArrayList;

@SuppressWarnings({"CloneableClassInSecureContext", "CloneableClassWithoutClone", "ClassExtendsConcreteCollection"})
public class Stack<T> extends ArrayList<T> {

    private T last;

    public void push(T t) {
        if (last != null) {
            add(last);
        }
        last = t;
    }

    public T peek() {
        return last;
    }

    public T pop() {
        final T result = last;
        if (result != null) {
            last = super.isEmpty() ? null : remove(super.size() - 1);
        }
        return result;
    }

    @Override
    public int size() {
        return last == null ? 0 : super.size() + 1;
    }

    @Override
    public boolean isEmpty() {
        return last == null;
    }
}
