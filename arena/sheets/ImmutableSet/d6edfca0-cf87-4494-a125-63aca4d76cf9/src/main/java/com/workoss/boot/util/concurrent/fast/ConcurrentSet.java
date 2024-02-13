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
package com.workoss.boot.util.concurrent.fast;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于ConcurrentHashMap实现的支持并发的Set（实际上就是使用map的set，原理类比：HashSet和HashMap），所以在mina中该类被称为ConcurrentHashSet
 *
 * @author workoss
 */
public class ConcurrentSet<E> extends AbstractSet<E> implements Serializable {

	private static final long serialVersionUID = -1244664838594578508L;

	/**
	 * 基本数据结构
	 */
	private ConcurrentMap<E, Boolean> map;

	/**
	 * 创建ConcurrentSet实例 并初始化内部的ConcurrentHashMap
	 */
	public ConcurrentSet() {
		map = new ConcurrentHashMap<>();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean add(E e) {
		return map.putIfAbsent(e, Boolean.TRUE) == null;
	}

	@Override
	public boolean remove(Object o) {
		return map.remove(o);
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public int size() {
		return map.size();
	}

}
