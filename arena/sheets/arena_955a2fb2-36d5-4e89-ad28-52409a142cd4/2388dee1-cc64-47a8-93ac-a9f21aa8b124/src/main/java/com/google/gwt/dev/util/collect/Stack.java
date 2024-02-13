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
package com.google.gwt.dev.util.collect;

import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Stack based on {@link ArrayList}. Unlike {@link java.util.ArrayDeque},
 * this one allows {@code null} values.
 *
 * @param <T> the value type
 */
public final class Stack<T> implements Iterable<T> {

  private ArrayList<T> elements = Lists.newArrayList();

  /**
   * Returns the number of elements in the stack (including pushed nulls).
   */
  public int size() {
    return elements.size();
  }

  /**
   * Returns true if the stack contains element, false otherwise.
   */
  public boolean contains(T element) {
    return elements.contains(element);
  }

  /**
   * Returns true if the stack is empty false otherwise.
   */
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return elements.iterator();
  }

  /**
   * Returns the top of the stack.
   */
  public T peek() {
    return elements.get(elements.size() - 1);
  }

  /**
   * Returns the element at location index (from bottom of stack).
   */
  public T peekAt(int index) {
    return elements.get(index);
  }

  /**
   * Returns the top of the stack and removes it.
   * @return
   */
  public T pop() {
    return elements.remove(elements.size() - 1);
  }

  /**
   * Pops {@code count} elements from the stack and returns them as a list with to top of the stack
   * last.
   */
  public List<T> pop(int count) {
    int size = elements.size();
    List<T> nodesToPop = elements.subList(size - count, size);
    List<T> result = Lists.newArrayList(nodesToPop);
    nodesToPop.clear();
    return result;
  }

  public void push(T value) {
    elements.add(value);
  }

  /**
   * Creates a new Stack for type {@code T}.
   */
  public static <T> Stack<T> create() {
    return new Stack<T>();
  }
}
