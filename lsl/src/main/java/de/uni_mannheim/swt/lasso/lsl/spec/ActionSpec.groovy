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
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration
import de.uni_mannheim.swt.lasso.core.model.Behaviour
import de.uni_mannheim.swt.lasso.core.model.Specification
import de.uni_mannheim.swt.lasso.core.model.System
import de.uni_mannheim.swt.lasso.core.model.Sequence
import de.uni_mannheim.swt.lasso.core.srm.SRM
import de.uni_mannheim.swt.lasso.lsl.srmpath.SRMPathSpec
import tech.tablesaw.api.Table

/**
 *
 * @author Marcus Kessel
 */
class ActionSpec extends LassoSpec {

    Map<String, ?> map
    Closure<ActionSpec> closure
    AbstractionContainerSpec abstractionContainerSpec = new AbstractionContainerSpec()
    List<AbstractionSpec> abstractionSpecs = []

    Closure whenAbstractionsReadyClosure

    Closure executeClosure

    Closure configureClosure

    ProfileSpec profileSpec

    List<SheetSpec> sheetSpecs = []

    List<String> dependsOn = []
    String includeAbstractions = '*'

    Closure<List<System>> includeImplementationsClosure

    String includeTests = '*'

    Map<String, Serializable> unknownSettings = [:]

    ActionConfiguration actionConfiguration

    String getName() {
        map.name
    }

    String getType() {
        map.type
    }

    def getAbstractions() {
        abstractionContainerSpec.abstractions
    }

    def getActions() {
        lasso.actionContainerSpec.actions
    }

//    void apply(Action action) {
//        callRehydrate(closure, action, this, null)
//    }

    def propertyMissing(String name, value) {
        unknownSettings.put(name, value)
    }

    void apply() {
        callRehydrate(closure, this, this, null)

        actionConfiguration = new ActionConfiguration()
        actionConfiguration.addConfiguration(unknownSettings)
        actionConfiguration.setDependsOnActions(dependsOn) // not to be confused with dependsOn list
        actionConfiguration.setIncludeTestsPattern(includeTests)
    }

    Table queryReport(String sql) {
        return lasso.executionContext.reportOperations.select(lasso.executionId, sql)
    }

    void saveReport(String reportName, AbstractionSpec abstractionSpec, System implementation, Map values) {
        Map newReport = values.collectEntries {it -> [it.key, it.value.class.getCanonicalName()]}

        // publish report metadata
        try {
            lasso.executionContext.reportOperations.newValuesReport(lasso.executionId, reportName, newReport)
        } catch(Throwable t) {
            t.printStackTrace()
        }

        // store report
        try {
            lasso.executionContext.reportOperations.putValues(lasso.executionId, getName(),
                    abstractionSpec.abstraction, implementation, reportName, values)
        } catch(Throwable t) {
            t.printStackTrace()
        }
    }

    void dependsOn(String ... dependsOn) {
        this.dependsOn = dependsOn
    }

    void includeAbstractions(String includeAbstractions) {
        this.includeAbstractions = includeAbstractions
    }

    void includeTests(String includeTests) {
        this.includeTests = includeTests
    }

    /**
     * Alias for includeTests.
     *
     * @param includeSequences
     */
    void includeSequences(String includeSequences) {
        includeTests(includeSequences)
    }

    void abstraction(String name, Closure<AbstractionSpec> closure) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: closure)

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)
    }

    void abstraction(String name, String lql, Closure<AbstractionSpec> closure) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: closure)
        if(lql) {
            abstractionSpec.lql = lql
        }

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)
    }

    void abstraction(String name, List<String> implementationIds, String lql, Closure<AbstractionSpec> closure) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: closure)
        if(lql) {
            abstractionSpec.lql = lql
        }
        if(implementationIds) {
            abstractionSpec.implementationIds = implementationIds
        }

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)
    }

    void abstraction(System implementation, Closure<AbstractionSpec> closure) {
        abstraction(implementation.id, closure)
    }

    /**
     * Merge abstractions
     *
     * @param name
     * @param specs
     */
    AbstractionSpec abstraction(String name, List<AbstractionSpec> specs) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: null)

        Abstraction merged = new Abstraction()
        merged.setName(name)
        List all = []
        specs.each {
            if(it.implementations) {
                all.addAll(it.implementations)
            }
        }
        merged.implementations = all

        abstractionSpec.abstraction = merged

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)

        return abstractionSpec
    }

    AbstractionSpec abstraction(List<System> implementations, String name) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: null)

        Abstraction merged = new Abstraction()
        merged.setName(name)
        List all = []
        if(implementations) {
            all.addAll(implementations)
        }
        merged.implementations = all

        abstractionSpec.abstraction = merged

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)

        return abstractionSpec
    }

    AbstractionSpec abstraction(List<System> implementations, String name, Specification specification) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: null)

        Abstraction merged = new Abstraction()
        merged.setName(name)
        List all = []
        if(implementations) {
            all.addAll(implementations)
        }
        merged.implementations = all

        //
        merged.specification = specification

        abstractionSpec.abstraction = merged

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)

        return abstractionSpec
    }

    void profile(Closure closure) {
        profile(this.name + "_" + java.lang.System.currentTimeMillis(), closure)
    }

    void profile(String name, Closure<ProfileSpec> closure) {
        ProfileSpec profileSpec = new ProfileSpec(name:name)
        callRehydrate(closure, profileSpec, this, null)

        lasso.register(profileSpec)

        this.profileSpec = profileSpec
    }

    void profile(String name) {
        this.profileSpec = lasso.profileContainerSpec.profiles[name]
    }

    void filter(Closure<System> closure) {
        // TODO
    }

    void whenAbstractionsReady(Closure closure) {
        whenAbstractionsReadyClosure = closure
    }

    void includeImplementations(Closure<List<System>> closure) {
        // clone action
        //Closure cloneCl = closure.rehydrate(this, this, this)
        //cloneCl.resolveStrategy = Closure.DELEGATE_FIRST

        includeImplementationsClosure = closure
    }

    /**
     * Alias for includeImplementations.
     *
     * @param closure
     */
    void includeSystems(Closure<List<System>> closure) {
        includeImplementations(closure)
    }

    void execute(Closure closure) {
        executeClosure = closure
    }

    void configure(Closure closure) {
        configureClosure = closure
    }

    void applyConfigure() {
        if(configureClosure)  {
            callRehydrate(configureClosure, this, this, null)

            actionConfiguration = new ActionConfiguration()
            actionConfiguration.addConfiguration(unknownSettings)
            actionConfiguration.setDependsOnActions(dependsOn)
            actionConfiguration.setIncludeTestsPattern(includeTests)
        }
    }

    void applyExecute() {
        if(executeClosure)  {
            callRehydrate(executeClosure, this, this, null)
        }
    }

    void applyWhenAbstractionsReady() {
        if(whenAbstractionsReadyClosure)  {
            callRehydrate(whenAbstractionsReadyClosure, this, this, null)
        }
    }

    List<System> applyIncludeImplementations(String abName) {
        if(includeImplementationsClosure) {
            return callRehydrate(includeImplementationsClosure, this, this, abName)
        } else {
            return []
        }
    }

    // srm(abstraction: stack).systems['ArrayStack'].observations['cc.branch.total']
    def srm(Map<String, ?> map) {

        if(map.containsKey("abstraction")) {
            AbstractionSpec abstractionSpec = map.get("abstraction") as AbstractionSpec
            SRMPathSpec srmPathSpec = new SRMPathSpec(abstractionSpec.abstraction, map.get("path"))
            srmPathSpec.actionSpec = this
            lasso.register(srmPathSpec)

            return srmPathSpec
        }
    }

    /**
     * Export SRM
     *
     * @param srmPathSpec
     * @param filename
     * @return
     */
    def export(SRMPathSpec srmPathSpec, String filename) {
        File file = new File(lasso.workspaceRoot, filename)
        SRM srm = lasso.executionContext.configuration.getService(SRM.class)
        srm.export(lasso.executionId, this.name, srmPathSpec.abstraction, file)
    }

    /**
     * Given System acts as pseudo oracle.
     *
     * @param system
     * @param sequences
     */
    def toOracle(System system, List<Sequence> sequences) {
        Behaviour behaviour = new Behaviour();
        behaviour.sequences = sequences
        behaviour.pseudoOracle = system

        return behaviour
    }
    def toOracle(System system) {
        return toOracle(system, [])
    }

    /**
     * Manual oracle (provided by user).
     *
     * @param sequences
     * @return
     */
    def toOracle(List<Sequence> sequences) {
        Behaviour behaviour = new Behaviour();
        behaviour.sequences = sequences

        return behaviour
    }

    def sheet(Closure closure) {
        return sheet(null, closure)
    }

    def sheet(Map<String, ?> map, Closure closure) {
        SheetSpec sheetSpec = new SheetSpec(inputParameters: map, closure: closure)
        lasso.register(sheetSpec)

        // apply
        sheetSpec.apply()

        sheetSpecs << sheetSpec

        return sheetSpec
    }

    /**
     * reuse existing sheets with different parameters
     *
     * <pre>
     *  'sheet2': sheet(name: 'pushPop', p1:'Stack', p2:5)
     * </pre>
     *
     * @param map
     * @return
     */
    def sheet(Map<String, ?> map) {
        String sheetName = map.name

        SheetSpec sheetSpec = sheetSpecs.find { it.inputParameters.name == sheetName}

        SheetSpec copy = new SheetSpec(inputParameters: map)
        copy.rows = new ArrayList<>(sheetSpec.rows)

        sheetSpecs << copy

        return copy
    }
}
