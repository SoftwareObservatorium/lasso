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
/** 
 * @Project Name : LuckyExp
*
* @File name : LinkedStack.java
*
* @Author : FayeWong
*
* @Date : 2019年9月3日
*
----------------------------------------------------------------------------------
*     Date       Who       Version     Comments
* 1. 2019年9月3日    FayeWong    1.0   链表栈
*
*
*
*
----------------------------------------------------------------------------------
*/
package org.lucky.exp.util;

import java.util.LinkedList;
/**
 * 链表栈
*
* @author FayeWong
* @since 2019年9月16日
* @param <E> 泛型
 */
public class LinkedStack<E> {
	private final LinkedList<E> list ;
	public LinkedStack(){
		list = new LinkedList<E>();
	}
	public void push(E e ) {
		list.add(e);
	}
	public E peek() {
		return list.getLast();
	}
	public E pop() {
		return list.removeLast();
	}
	public boolean isEmpty() {
		return list.size() == 0;		
	}
	public E search(int index) {
		return list.get(index);
	}
	public int size() {
		return list.size();
	}
}
