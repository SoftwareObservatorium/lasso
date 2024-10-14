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

import de.uni_mannheim.swt.lasso.core.model.Abstraction
import de.uni_mannheim.swt.lasso.core.model.System

/**
 * An abstraction container that describes an abstraction (i.e., high-level description of functionality).
 *
 * @author Marcus Kessel
 */
class AbstractionSpec extends LassoSpec {

    /**
     * Map of properties
     */
    Map<String, ?> map
    /**
     * Closure to call
     */
    Closure<AbstractionSpec> closure
    /**
     * Reference to actual Abstraction domain model.
     */
    Abstraction abstraction
    /**
     * LQL interface
     */
    String lql
    /**
     * If non-empty, instructs the engine to retrieve Systems by their IDs.
     */
    List<String> implementationIds

    /**
     *
     * @return name of the abstraction
     */
    String getName() {
        map.name
    }

    /**
     * Internal closure to call
     *
     * @param queryModel
     */
    void apply(def queryModel) {
        callRehydrate(closure, queryModel, this, null)
    }

    /**
     * Internal method to delegate missing methods.
     *
     * @param name
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        //abstraction.invokeMethod(name, args)

        MetaMethod method = abstraction.metaClass.getMetaMethod(name, args)
        if (method != null) {
            return method.invoke(abstraction, args)
        }

        throw new MissingMethodException(name, AbstractionSpec, args)
    }

    /**
     * Internal method to delegate missing properties
     *
     * @param name
     * @return
     */
    def propertyMissing(String name) {
        if(abstraction.hasProperty(name)) {
            return abstraction.metaClass.getProperty(abstraction, name)
        }

        throw new MissingPropertyException(name, AbstractionSpec)
    }

    /**
     * Get list of Systems for this abstraction container.
     *
     * @return
     */
    List<System> getImplementations() {
        //String defaultDataSource = lasso.dataSources.get(0)

        return abstraction.implementations
    }

    /**
     * Set list of Systems for this abstraction container.
     *
     * @param implementations
     */
    void setImplementations(List<System> implementations) {
        abstraction.implementations = implementations
    }

    /**
     * Alias for #getImplementations
     *
     * @return
     */
    List<System> getSystems() {
        //String defaultDataSource = lasso.dataSources.get(0)

        return abstraction.implementations
    }

    /**
     * Alias for setImplementations
     *
     * @param systems
     */
    void setSystems(List<System> systems) {
        abstraction.implementations = systems
    }
}
