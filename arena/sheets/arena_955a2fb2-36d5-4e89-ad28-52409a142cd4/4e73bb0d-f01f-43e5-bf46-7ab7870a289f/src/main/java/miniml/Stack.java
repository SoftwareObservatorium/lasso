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
package miniml;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

/**
 * A simple implementation of a stack. 
 * Used while parsing a MiniML document.
 * 
 * @see Document
 * 
 * @param <T> The type this stack will be storing.
 */
final class Stack<T> {
	/**
	 * The list storing the data.
	 */
	private final List<T> list;
	
	/**
	 * Creates the stack.
	 */
	Stack() {
		list = new ArrayList<>();
	}
	
	/**
	 * adds the new value to the stack.
	 * 
	 * @param newVal The new value.
	 */
	final void push(T newVal) {
		list.add(newVal);
	}
	
	/**
	 * Retrieves but does not remove the last element. 
	 * Returns null if the stack is empty.
	 * 
	 * @return The last element.
	 */
	final T peek() {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(list.size() - 1);
	}
	
	/**
	 * Retrieves and removes the last element. 
	 * Throws an {@code EmptyStackException} if the stack is empty.
	 * 
	 * @return The last element.
	 */
	final T pop() {
		if (list.isEmpty()) {
			throw new EmptyStackException();
		}
		return list.remove(list.size() - 1);
	}
	
	/**
	 * Checks if the stack is empty.
	 * 
	 * @return Whether or not the stack is empty.
	 */
	final boolean isEmpty() {
		return list.isEmpty();
	}
	
	/**
	 * Returns how many elements are in the stack (the stack's size).
	 * 
	 * @return The size of the stack.
	 */
	final int size() {
		return list.size();
	}
}
