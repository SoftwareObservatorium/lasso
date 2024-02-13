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
 *
 * @author Marcus Kessel
 */
class AbstractionSpec extends LassoSpec {

    Map<String, ?> map
    Closure<AbstractionSpec> closure

    Abstraction abstraction

    String getName() {
        map.name
    }

    void apply(def queryModel) {
        callRehydrate(closure, queryModel, this, null)
    }

    def methodMissing(String name, args) {
        //abstraction.invokeMethod(name, args)

        MetaMethod method = abstraction.metaClass.getMetaMethod(name, args)
        if (method != null) {
            return method.invoke(abstraction, args)
        }

        throw new MissingMethodException(name, AbstractionSpec, args)
    }

    def propertyMissing(String name) {
        if(abstraction.hasProperty(name)) {
            return abstraction.metaClass.getProperty(abstraction, name)
        }

        throw new MissingPropertyException(name, AbstractionSpec)
    }

    List<System> getImplementations() {
        //String defaultDataSource = lasso.dataSources.get(0)

        return abstraction.implementations
    }

    void setImplementations(List<System> implementations) {
        abstraction.implementations = implementations
    }

    List<System> getSystems() {
        //String defaultDataSource = lasso.dataSources.get(0)

        return abstraction.implementations
    }

    void setSystems(List<System> systems) {
        abstraction.implementations = systems
    }
}
