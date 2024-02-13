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
package ro.pippo.core.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * A stack class implemented as a wrapper around a {@link java.util.LinkedList}
 * that silently discards {@code null} items.
 *
 * @author Decebal Suiu
 */
public class Stack<E> implements Iterable<E> {

    private LinkedList<E> list = new LinkedList<>();

    /**
     * Adds the given item to the top of the stack.
     */
    public void push(E item) {
        if (item != null) {
            list.addFirst(item);
        }
    }

    /**
     * Adds the given item to the top of the stack if the stack is not empty.
     */
    public void pushIfNotEmpty(E item) {
        if (!isEmpty()) {
            push(item);
        }
    }

    /**
     * Removes the top item from the stack and returns it.
     * Returns {@code null} if the stack is empty.
     */
    public E pop() {
        try {
            return list.removeFirst();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Returns the top item from the stack without popping it.
     * Returns {@code null} if the stack is empty.
     */
    public E peek() {
        try {
            return list.getFirst();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Returns the number of items currently in the stack.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns whether the stack is empty or not.
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

}
