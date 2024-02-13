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
package org.jbasics.types.container;

import org.jbasics.checker.ContractCheck;
import org.jbasics.pattern.container.Stack;
import org.jbasics.pattern.delegation.Delegate;
import org.jbasics.pattern.factory.Factory;
import org.jbasics.types.delegates.LazyDelegate;
import org.jbasics.types.factories.ListFactory;

import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class ListStack<E> implements Stack<E> {
	private final Delegate<List<E>> stackList;

	public ListStack() {
		final Factory<List<E>> temp = ListFactory.sequentialAccessListFactory();
		this.stackList = new LazyDelegate<List<E>>(temp);
	}

	public ListStack(final Factory<List<E>> factory) {
		this.stackList = new LazyDelegate<List<E>>(ContractCheck.mustNotBeNull(factory, "factory"));
	}

	public ListStack(final Delegate<List<E>> delegate) {
		this.stackList = ContractCheck.mustNotBeNull(delegate, "delegate");
	}

	@Override
	public E push(final E element) {
		this.stackList.delegate().add(element);
		return element;
	}

	@Override
	public E pop() {
		final List<E> temp = this.stackList.delegate();
		if (temp.isEmpty()) {
			throw new NoSuchElementException("Stack empty");
		}
		if (temp instanceof LinkedList<?>) {
			return ((LinkedList<E>) temp).removeLast();
		}
		return temp.remove(temp.size() - 1);
	}

	@Override
	public E[] pop(final int count) {
		throw new UnsupportedOperationException("Optional contract []pop() is unsupported by ListStack");
	}

	@Override
	public E peek() {
		final List<E> temp = this.stackList.delegate();
		if (temp.isEmpty()) {
			throw new NoSuchElementException("Stack empty");
		}
		if (temp instanceof LinkedList<?>) {
			return ((LinkedList<E>) temp).getLast();
		}
		return temp.get(temp.size() - 1);
	}

	@Override
	public E peek(final int depth) {
		throw new UnsupportedOperationException("Optional contract peek(depth) is unsupported by ListStack");
	}

	@Override
	public int depth() {
		return this.stackList.delegate().size();
	}

	@Override
	public int size() {
		return this.stackList.delegate().size();
	}

	@Override
	public boolean isEmpty() {
		return this.stackList.delegate().isEmpty();
	}

	@Override
	public E replace(final E value) {
		final int size = this.stackList.delegate().size();
		if (size == 0) {
			throw new EmptyStackException();
		}
		return this.stackList.delegate().set(size - 1, value);
	}

	@Override
	public E replace(final int depth, final E value) {
		if (depth < 1) {
			throw new IllegalArgumentException("Parameter for peek cannot be lower than 1! The peek is 1 based not 0 based!"); //$NON-NLS-1$
		}
		final int size = this.stackList.delegate().size();
		if (size == 0) {
			throw new EmptyStackException();
		}
		if (depth > size) {
			throw new IndexOutOfBoundsException();
		}
		return this.stackList.delegate().set(size - depth, value);
	}

	@Override
	public Iterator<E> iterator() {
		// TODO: Make this iterator unmodifiable
		return this.stackList.delegate().iterator();
	}
}
