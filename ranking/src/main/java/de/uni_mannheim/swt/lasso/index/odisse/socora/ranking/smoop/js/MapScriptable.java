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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.js;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;

import org.mozilla.javascript.Scriptable;

/**
 * A workaround to work with Java {@link Map}s directly as usually array maps in
 * JavaScript.
 * 
 * @author <a href="http://stackoverflow.com/a/7605787">Stackoverflow
 *         Snippet</a>
 * 
 * @see Scriptable
 * @see ScriptEngine
 *
 */
public class MapScriptable implements Scriptable, Map {

    public final Map map;

    public MapScriptable(Map map) {
        this.map = map;
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Set entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set keySet() {
        return map.keySet();
    }

    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    public void putAll(Map m) {
        map.putAll(m);
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public Collection values() {
        return map.values();
    }

    @Override
    public void delete(String name) {
        map.remove(name);
    }

    @Override
    public void delete(int index) {
        map.remove(index);
    }

    @Override
    public Object get(String name, Scriptable start) {
        return map.get(name);
    }

    @Override
    public Object get(int index, Scriptable start) {
        return map.get(index);
    }

    @Override
    public String getClassName() {
        return map.getClass().getName();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return toString();
    }

    @Override
    public Object[] getIds() {
        Object[] res = new Object[map.size()];
        int i = 0;
        for (Object k : map.keySet()) {
            res[i] = k;
            i++;
        }
        return res;
    }

    @Override
    public Scriptable getParentScope() {
        return null;
    }

    @Override
    public Scriptable getPrototype() {
        return null;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return map.containsKey(name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return map.containsKey(index);
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return false;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        map.put(name, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        map.put(index, value);
    }

    @Override
    public void setParentScope(Scriptable parent) {
    }

    @Override
    public void setPrototype(Scriptable prototype) {
    }
}
