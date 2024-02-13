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
// Copyright (c) 2003-2012, Jodd Team (jodd.org). All Rights Reserved.

package jodd.util.collection;

import java.util.LinkedList;

/**
 * Simple Stack (LIFO) class.
 */
public class SimpleStack<E> {

	private LinkedList<E> list = new LinkedList<E>();

	/**
	 * Stack push.
	 */
	public void push(E o) {
		list.addLast(o);
	}

	/**
	 * Stack pop.
	 *
	 * @return poped object from stack
	 */
	public E pop() {
		if (list.isEmpty()) {
			return null;
		}
		return list.removeLast();
	}


	public Object[] popAll() {
		Object[] res = new Object[list.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = list.get(i);
		}
		list.clear();
		return res;
	}

	/**
	 * Peek element from stack.
	 *
	 * @return peeked object
	 */
	public E peek() {
		return list.getLast();
	}


	/**
	 * Is stack empty?
	 *
	 * @return true if stack is empty
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Returns stack size.
	 *
	 * @return	stack size
	 */
	public int size() {
		return list.size();
	}

}
