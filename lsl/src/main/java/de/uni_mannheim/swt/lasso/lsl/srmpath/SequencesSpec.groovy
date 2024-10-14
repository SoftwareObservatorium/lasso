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
import de.uni_mannheim.swt.lasso.core.model.Sequence
import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec

/**
 * Experimental DSL support for SRMs
 * 
 * @author Marcus Kessel
 */
class SequencesSpec extends LassoSpec {

    Abstraction abstraction
    Sequence sequence

//    def getAt(String property) {
//        println("getting property $property for object $delegate.class")
//
//        return null;
//    }

    def propertyMissing(String name) {
        println("$sequence.id ab $abstraction.name getting property $name")

        return 5

        //throw new MissingPropertyException(name, SRMPathSpec)
    }
}
