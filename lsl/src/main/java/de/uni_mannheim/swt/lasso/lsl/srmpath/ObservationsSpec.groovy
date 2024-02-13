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
package de.uni_mannheim.swt.lasso.lsl.srmpath

import de.uni_mannheim.swt.lasso.core.model.Abstraction
import de.uni_mannheim.swt.lasso.core.model.System
import de.uni_mannheim.swt.lasso.core.srm.SRM
import de.uni_mannheim.swt.lasso.lsl.spec.ActionSpec
import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec

/**
 * 
 * @author Marcus Kessel
 */
class ObservationsSpec extends LassoSpec {

    Abstraction abstraction
    System system

    ActionSpec actionSpec

//    def getAt(String property) {
//        println("getting property $property for object $delegate.class")
//
//        return null;
//    }

    def propertyMissing(String propertyName) {
        println("$system.id ab $abstraction.name getting property $propertyName")

        // FIXME TBR (debugging)
        if(!lasso.executionContext) {
            return 5
        }

        Map<String, ?> queryMap = [:]
        queryMap.put("executionId", lasso.executionId)
        queryMap.put("actionName", actionSpec.name)

        queryMap.put("systemId", system.id)

        // FIXME sequence
        //queryMap.put("sequenceId", actionSpec.name)

        queryMap.put("abstractionId", abstraction.name)
        queryMap.put("row", -1)
        queryMap.put("column", -1)
        queryMap.put("type", propertyName)

        SRM srm = lasso.executionContext.configuration.getService(SRM.class)

        try {
            def val = srm.getObservation(queryMap)
            return val
        } catch(Throwable e) {
            e.printStackTrace()

            return null
        }

        //throw new MissingPropertyException(name, SRMPathSpec)
    }
}
