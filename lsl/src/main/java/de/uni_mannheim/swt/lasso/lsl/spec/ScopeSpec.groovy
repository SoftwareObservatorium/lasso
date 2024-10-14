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
package de.uni_mannheim.swt.lasso.lsl.spec

/**
 * Scope block inside EnvironmentSpec.
 *
 * <code>
 *     scope(map) {closure}
 * </code>
 *
 * @author Marcus Kessel
 */
class ScopeSpec extends LassoSpec {
    /**
     * Properties of the scope
     */
    Map<String, ?> map
    /**
     * Closure of the scope
     */
    Closure closure
    /**
     * analysis type (e.g., 'class')
     */
    String type
    /**
     * whitelist by package name
     */
    List<String> pkgWhitelist
    /**
     * blacklist by package name
     */
    List<String> pkgBlacklist
    /**
     * method blacklist
     */
    List<String> methodBlacklist
    /**
     * method whitelist
     */
    List<String> methodWhitelist

    /**
     *
     * @return name of scope
     */
    String getName() {
        map.name
    }

    /**
     * Internal method to call closure
     */
    void apply() {
        callRehydrate(closure, this, null, null)
    }
}
