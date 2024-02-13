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
package net.enilink.komma.common.adapter;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AdapterSet extends AbstractSet<IAdapter>implements IAdapterSet {
	protected final Set<IAdapter> adapters = new HashSet<>();
	protected final Object target;

	public AdapterSet(Object target) {
		this.target = target;
	}

	public IAdapter getAdapter(Object type) {
		for (IAdapter adapter : this) {
			if (adapter.isAdapterForType(type)) {
				return adapter;
			}
		}
		return null;
	}

	@Override
	public boolean add(IAdapter e) {
		if (adapters.add(e)) {
			e.addTarget(target);
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(Object o) {
		if (adapters.remove(o)) {
			if (o instanceof IAdapter) {
				((IAdapter) o).removeTarget(target);
			}
			return true;
		}
		return false;
	}

	@Override
	public Iterator<IAdapter> iterator() {
		return new Iterator<IAdapter>() {
			IAdapter current;
			final Iterator<IAdapter> base = adapters.iterator();

			@Override
			public boolean hasNext() {
				return base.hasNext();
			}

			@Override
			public IAdapter next() {
				return current = base.next();
			}

			@Override
			public void remove() {
				if (current != null) {
					base.remove();
					current.removeTarget(target);
				}
			}
		};
	}

	@Override
	public int size() {
		return adapters.size();
	}
}
