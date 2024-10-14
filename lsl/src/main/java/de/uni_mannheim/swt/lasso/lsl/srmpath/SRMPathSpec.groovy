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
import de.uni_mannheim.swt.lasso.core.model.System
import de.uni_mannheim.swt.lasso.core.model.Sequence
import de.uni_mannheim.swt.lasso.core.srm.SRM
import de.uni_mannheim.swt.lasso.lsl.spec.ActionSpec
import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec
import org.apache.commons.lang3.StringUtils

/**
 * SRMPath DSL commands to evaluate SRMs (e.g., doing equivalence checks etc.).
 *
 * @author Marcus Kessel
 */
class SRMPathSpec extends LassoSpec {

    Abstraction abstraction

    ActionSpec actionSpec

    SRMPathSpec(Abstraction abstraction, String path) {
        initialize(abstraction, path)
        this.abstraction = abstraction
    }

    /**
     * Register expandos for Implementation as well as for custom functions for ArrayLists etc.
     *
     * @param abstraction
     * @param executionId
     * @return
     */
    def initialize(Abstraction abstraction, String path) {
        SRMPathSpec thiss = this

        // add missing property for observations
        System.metaClass.propertyMissing = { String name ->
            //println(((Implementation)delegate).code.name)
            String systemId = ((System)delegate).id
            String abName = abstraction.name

//            if(abstraction.systems.stream().filter(s -> s.id.equals(systemId))) {
//                println("found ${systemId}");
//            }

            if(name == "observations") {
                //println("fetching observations for $systemId from $abName")

                ObservationsSpec observationsSpec = new ObservationsSpec(abstraction:abstraction,
                        system:(System)delegate, actionSpec: actionSpec)
                lasso.register(observationsSpec)

                // fetch all observations
                return observationsSpec
            }

            // delegate to CodeUnit
            if(((System)delegate).code.properties.containsKey(name)) {
                return ((System)delegate).code.properties.get(name)
            }

            if(name == "packageName") {
                return ((System)delegate).code.properties.get("packagename")
            }

            throw new MissingPropertyException(name, System)
        }

        // add missing property for observations
        Sequence.metaClass.propertyMissing = { String name ->
            //println(((Implementation)delegate).code.name)
            String sequenceId = ((Sequence)delegate).id
            String abName = abstraction.name

            if(name == "observations") {
                //println("fetching observations for $sequenceId from $abName")

                SequencesSpec sequencesSpec = new SequencesSpec(abstraction:abstraction,
                        sequence: (Sequence)delegate)
                lasso.register(sequencesSpec)

                // fetch all
                return sequencesSpec
            }

            throw new MissingPropertyException(name, System)
        }

        // add custom aggregation functions (like UDFs)
        ArrayList.metaClass.mean << { ->
            return delegate.average()
        }

        ArrayList.metaClass.equalTo << { Behaviour behaviour ->
            // call SRM
            SRM srm = lasso.executionContext.configuration.getService(SRM.class)

            String executionId = actionSpec.lasso.executionId
            String actionName = actionSpec.name
            if(path != null) {
                String[] parts = StringUtils.split(path, ":")
                executionId = parts[0]
                actionName = parts[1]
            }

            Map<String, ?> queryMap = [:]
            queryMap.put("executionId", executionId)
            queryMap.put("actionName", actionName)
            queryMap.put("abstractionId", abstraction.name)

            List<String> matchesByAdapters = srm.equalTo(behaviour, queryMap)

            List<String> matchesBySystems = new ArrayList<>(matchesByAdapters.size())
            for(String a : matchesByAdapters) {
                matchesBySystems.add(StringUtils.substringBeforeLast(a, "_"))
            }

            List<System> filtered = []
            for(System system : delegate) {
                //
                //println("equalTo ${system.id}")

                if(matchesBySystems.contains(system.id)) {
                    filtered << system
                    //println("matched ${system.id}")
                }
            }

            // FIXME return SRMPath model with filtered systems
            Abstraction ab = new Abstraction()
            ab.systems = filtered
            ab.name = abstraction.name
            ab.specification = abstraction.specification
            // TODO set sequences as well

            SRMPathSpec srmPathSpec = new SRMPathSpec(ab, path)
            srmPathSpec.actionSpec = thiss.actionSpec
            lasso.register(srmPathSpec)

            return srmPathSpec
        }

        ArrayList.metaClass.similarTo << { Behaviour behaviour, double minimum ->
            // call SRM
            SRM srm = lasso.executionContext.configuration.getService(SRM.class)

            String executionId = actionSpec.lasso.executionId
            String actionName = actionSpec.name
            if(path != null) {
                String[] parts = StringUtils.split(path, ":")
                executionId = parts[0]
                actionName = parts[1]
            }

            Map<String, ?> queryMap = [:]
            queryMap.put("executionId", executionId)
            queryMap.put("actionName", actionName)
            queryMap.put("abstractionId", abstraction.name)

            List<String> matchesByAdapters = srm.similarTo(behaviour, minimum, queryMap)

            List<String> matchesBySystems = new ArrayList<>(matchesByAdapters.size())
            for(String a : matchesByAdapters) {
                matchesBySystems.add(StringUtils.substringBeforeLast(a, "_"))
            }

            List<System> filtered = []
            for(System system : delegate) {
                //
                //println("similarTo ${system.id}")
                if(matchesBySystems.contains(system.id)) {
                    filtered << system
                    //println("matched ${system.id}")
                }
            }

            // FIXME return SRMPath model with filtered systems
            Abstraction ab = new Abstraction()
            ab.systems = filtered
            ab.name = abstraction.name
            ab.specification = abstraction.specification
            // TODO set sequences as well

            SRMPathSpec srmPathSpec = new SRMPathSpec(ab, path)
            srmPathSpec.actionSpec = thiss.actionSpec
            lasso.register(srmPathSpec)

            return srmPathSpec
        }
    }

    def propertyMissing(String name) {
        //println(((Implementation)delegate).code.name)

        // to map
        if(name == "systems") { // by id
            List<System> systems = new ArrayList<>(abstraction.systems)
            // also allow access by "ID"
            systems.metaClass.getAt = {String query ->
                println("system query '$query'")

                // TODO evaluate filters (e.g., id == "XXX")
                //if(query.contains(''))
                // e.g. using Eval.

                return delegate[delegate.findIndexOf { it.id == query}]
            }

            return systems
        }

        if(name == "sequences" || name == "sheets") { // by id FIXME agree if second property is alias or not
            List<Sequence> sequences = new ArrayList<>(abstraction.sequences)
            // also allow access by "ID"
            sequences.metaClass.getAt = {String query ->
                println("sequence query '$query'")

                // TODO evaluate filters (e.g., sequenceId:systemId)
                if(query.contains(':')) {
                    List<String> parts = query.split(":")
                    println(parts)
                }
                // e.g. using Eval.

                return delegate[delegate.findIndexOf { it.id == query}]
            }

            return sequences
        }

        throw new MissingPropertyException(name, SRMPathSpec)
    }
}

