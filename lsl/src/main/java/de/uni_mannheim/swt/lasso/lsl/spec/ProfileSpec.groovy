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
 * A profile block
 *
 * <code>
 *     profile(map) {closure}
 * </code>
 *
 * @author Marcus Kessel
 */
class ProfileSpec extends LassoSpec {
    /**
     * Properties of the profile (e.g., name)
     */
    Map<String, ?> map
    /**
     * Internal variable for name
     */
    String name
    /**
     * Environment block
     */
    EnvironmentSpec environmentSpec
    /**
     * Analysis class
     */
    ScopeSpec scopeSpec

    /**
     * Set environment
     *
     * <code>
     *     environment(name) {closure}
     * </code>
     *
     * @param name
     * @param closure
     * @return
     */
    def environment(String name, Closure<EnvironmentSpec> closure) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        EnvironmentSpec environmentSpec = new EnvironmentSpec(map: aMap, closure: closure)
        //lasso.registerEnvironment(environmentSpec)

        this.environmentSpec = environmentSpec

        // apply
        environmentSpec.apply()
    }

    /**
     * Set analysis scope
     *
     * <code>
     *     scope(name) {closure}
     * </code>
     *
     * @param name
     * @param closure
     * @return
     */
    def scope(String name, Closure<ScopeSpec> closure) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        ScopeSpec scopeSpec = new ScopeSpec(map: aMap, closure: closure)
        this.scopeSpec = scopeSpec

        // apply
        scopeSpec.apply()
    }
}
