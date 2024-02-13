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
package org.jbasics.parser;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

/**
 * Listenbeschreibung <p> Detailierte Beschreibung </p>
 *
 * @author stephan
 */
public class StateStack<E> {
	private final List<E> stackImpl;

	public StateStack() {
		this.stackImpl = new ArrayList<E>();
	}

	public E push(final E element) {
		this.stackImpl.add(element);
		return element;
	}

	public E pop() {
		if (this.stackImpl.isEmpty()) {
			throw new EmptyStackException();
		}
		return this.stackImpl.remove(this.stackImpl.size() - 1);
	}

	public E peek() {
		if (this.stackImpl.isEmpty()) {
			throw new EmptyStackException();
		}
		return this.stackImpl.get(this.stackImpl.size() - 1);
	}

	public E peek(int distance) {
		if (distance < 1 || this.stackImpl.size() < distance) {
			throw new IndexOutOfBoundsException("Distance out of range [1," + this.stackImpl.size() + "]");
		}
		return this.stackImpl.get(this.stackImpl.size() - distance);
	}

	public boolean isEmpty() {
		return this.stackImpl.isEmpty();
	}

	public int size() {
		return this.stackImpl.size();
	}
}
