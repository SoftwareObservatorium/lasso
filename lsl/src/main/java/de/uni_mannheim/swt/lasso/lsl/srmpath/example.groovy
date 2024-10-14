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
import de.uni_mannheim.swt.lasso.core.model.CodeUnit
import de.uni_mannheim.swt.lasso.core.model.System

/**
 * For debugging purposes.
 *
 * @author Marcus Kessel
 */
class example {

    static void main(String[] args) {
//        Implementation.metaClass.getProperty = { Implementation impl, String name ->
//            if(name == "observations") {
//                return [o1:1, o2:2,o3:3]
//            }
//
//            throw new MissingPropertyException(name, Map.class)
//        }

//        Implementation.metaClass.getProperty = { String name ->
//            def meta = Implementation.metaClass.getMetaProperty(name)
//            println(meta)
//            if (meta) {
//                meta.getProperty(delegate)
//            } else if(name == "observations") {
//                return [o1:1, o2:2,o3:3]
//            }
//
//            println("found property $name")
//
//            throw new MissingPropertyException(name, Map.class)
//        }

        System.metaClass.propertyMissing = { String name ->
            println(((System)delegate).code.name)

            if(name == "observations") {
                return [o1:1, o2:2,o3:3]
            }

            throw new MissingPropertyException(name, System)
        }

        List<Abstraction> abs = [new Abstraction(name:"ab1"), new Abstraction(name:"ab2")]
        abs[0].systems = [new System(new CodeUnit(name:"s1"))]
        abs[0].systems[0].code.methods = ["m1", "m2"]
        abs[0].systems[0].code.measures = [loc:10, cc:5]
        abs[1].systems = [new System(new CodeUnit(name:"s2"))]
        abs[1].systems[0].code.methods = ["m3", "m4"]
        abs[1].systems[0].code.measures = [loc:20, cc:10]
        println(abs.name)
        println(abs.systems)
        println(abs.systems.code.methods)
        println(abs.systems.code.measures)

        println(abs.systems.observations)
        println(abs.systems.observations['o1'])
    }
}
