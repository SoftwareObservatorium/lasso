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
import de.uni_mannheim.swt.lasso.core.model.Behaviour
import de.uni_mannheim.swt.lasso.core.model.CodeUnit
import de.uni_mannheim.swt.lasso.core.model.System
import de.uni_mannheim.swt.lasso.core.model.Sequence
import de.uni_mannheim.swt.lasso.lsl.LassoContext
import org.junit.Test

/**
 * @author Marcus Kessel
 */
// FIXME update
class SRMPathTest {

    @Test
    void test_systems_path() {
        List<Abstraction> abs = [new Abstraction(name:"ab1")]
        abs[0].systems = [new System(new CodeUnit(id:"1", name:"s1")), new System(new CodeUnit(id:"2", name:"s2"))]
        abs[0].systems[0].code.methods = ["m1", "m2"]
        abs[0].systems[0].code.measures = [loc:10, cc:5]
        abs[0].systems[1].code.methods = ["m3", "m4"]
        abs[0].systems[1].code.measures = [loc:20, cc:10]

        SRMPathSpec srmPathSpec = new SRMPathSpec(abs[0], "")
        srmPathSpec.lasso = new LassoContext()

        // SRMPath notation
        println(srmPathSpec.systems)
        println(srmPathSpec.systems[0]) // getAt int
        println(srmPathSpec.systems['1']) // getAt String
        println(srmPathSpec.systems.code.id)
        println(srmPathSpec.systems.code.name)
        println(srmPathSpec.systems.code.methods)
        println(srmPathSpec.systems.code.measures)

        println(srmPathSpec.systems.observations)
        println(srmPathSpec.systems.observations['o1'])

        // functions
        println(srmPathSpec.systems.observations['o1'].sum())
        println(srmPathSpec.systems.observations['o1'].average())
        // custom
        println(srmPathSpec.systems.observations['o1'].mean())
    }

    @Test
    void test_systems_advanced() {
        List<Abstraction> abs = [new Abstraction(name:"ab1")]
        abs[0].systems = [new System(new CodeUnit(id:"1", name:"s1")), new System(new CodeUnit(id:"2", name:"s2"))]
        abs[0].systems[0].code.methods = ["m1", "m2"]
        abs[0].systems[0].code.measures = [loc:10, cc:5]
        abs[0].systems[1].code.methods = ["m3", "m4"]
        abs[0].systems[1].code.measures = [loc:20, cc:10]

        SRMPathSpec srmPathSpec = new SRMPathSpec(abs[0], "")
        srmPathSpec.lasso = new LassoContext()

        // SRMPath notation
        Behaviour behaviour = new Behaviour()

        println(srmPathSpec.systems.equalTo(behaviour))
    }

    @Test
    void test_sequences_path() {
        List<Abstraction> abs = [new Abstraction(name:"ab1")]
        abs[0].sequences = [new Sequence(id:"1", name:"s1"), new Sequence(id:"2", name:"s2")]

        SRMPathSpec srmPathSpec = new SRMPathSpec(abs[0], "")
        srmPathSpec.lasso = new LassoContext()

        // SRMPath notation
        println(srmPathSpec.sequences)
        println(srmPathSpec.sequences[0]) // getAt int
        println(srmPathSpec.sequences['1']) // getAt String
        println(srmPathSpec.sequences.id)
        println(srmPathSpec.sequences.name)

        // TODO is this more like "sheets" from SRMPath notation?
        // those are different from "sequences" - a sequence is "instantiated" on a system?

        // alias
        println(srmPathSpec.sheets)
        println(srmPathSpec.sheets[0]) // getAt int
        println(srmPathSpec.sheets['1']) // getAt String
        println(srmPathSpec.sheets.id)
        println(srmPathSpec.sheets.name)
    }

    @Test
    void test_complex_filter_path() {
        List<Abstraction> abs = [new Abstraction(name:"ab1")]
        abs[0].sequences = [new Sequence(id:"1", name:"s1"), new Sequence(id:"2", name:"s2")]

        SRMPathSpec srmPathSpec = new SRMPathSpec(abs[0], "")
        srmPathSpec.lasso = new LassoContext()

        // SRMPath notation
        println(srmPathSpec.sequences[0]) // getAt int
        println(srmPathSpec.sequences["1:1"]) // getAt String
        println(srmPathSpec.sequences["sequenceId == 'bla' & systemId == 'blub'"]) // getAt String
    }
}
