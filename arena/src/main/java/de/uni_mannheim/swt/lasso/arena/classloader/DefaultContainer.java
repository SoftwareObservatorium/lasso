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
package de.uni_mannheim.swt.lasso.arena.classloader;

/**
 * A default execution container (no instrumentation supported).
 *
 * @author Marcus Kessel
 */
public class DefaultContainer extends Container {

    /**
     * Creates a new class realm.
     *
     * @param containers      The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     */
    public DefaultContainer(Containers containers, String id, ClassLoader baseClassLoader) {
        super(containers, id, baseClassLoader);
    }

    @Override
    protected boolean instrumentClass(String name) {
        return false;
    }

    @Override
    protected byte[] instrumentClassBytes(String name, byte[] bytes) {
        // should never happen
        throw new UnsupportedOperationException();
    }
}
