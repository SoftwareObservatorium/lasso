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

import com.github.javaparser.JavaParser
import de.uni_mannheim.swt.lasso.core.dto.srm.JUnitCodeUnit
import de.uni_mannheim.swt.lasso.core.model.Abstraction
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration
import de.uni_mannheim.swt.lasso.core.model.Behaviour
import de.uni_mannheim.swt.lasso.core.model.CodeUnit
import de.uni_mannheim.swt.lasso.core.model.Specification
import de.uni_mannheim.swt.lasso.core.model.System
import de.uni_mannheim.swt.lasso.core.model.Sequence
import de.uni_mannheim.swt.lasso.core.srm.SRM
import de.uni_mannheim.swt.lasso.lsl.srmpath.SRMPathSpec
import org.apache.commons.lang3.StringUtils
import tech.tablesaw.api.Table

/**
 * Represents an action block in LSL.
 *
 * <code>
 *      action(map) {closure}
 * </code>
 *
 * @author Marcus Kessel
 */
class ActionSpec extends LassoSpec {

    /**
     * Properties passed to action block
     */
    Map<String, ?> map
    /**
     * Closure which is called to populate action block
     */
    Closure<ActionSpec> closure
    /**
     * Link to container of abstraction containers..
     */
    AbstractionContainerSpec abstractionContainerSpec = new AbstractionContainerSpec()
    /**
     * AbstractionSpecs defined by this action
     */
    List<AbstractionSpec> abstractionSpecs = []
    /**
     * Action lifecycle closure:  <code>whenAbstractionsReady() {closure}</code>
     */
    Closure whenAbstractionsReadyClosure
    /**
     * Action lifecycle closure in execute phase:  <code>execute() {closure}</code>
     */
    Closure executeClosure
    /**
     * Action lifecycle closure in configure phase:  <code>configure() {closure}</code>
     */
    Closure configureClosure
    /**
     * Profile block inside an action:  <code>profile(name) {closure}</code>
     */
    ProfileSpec profileSpec
    /**
     * List of sequence sheets (SheetSpec)
     */
    List<SheetSpec> sheetSpecs = []
    /**
     * List of actions that this action depends.
     *
     * Either local actions <code>myActionName</code>, or actions from past script executions: <code>scriptExecutionId:myActionName</code>
     */
    List<String> dependsOn = []
    /**
     * Comma-separated list of abstraction names (ANT matcher syntax).
     *
     * Examples in "de.uni_mannheim.swt.lasso.engine.matcher.AbstractionMatcherTest" (engine module)
     */
    String includeAbstractions = '*'
    /**
     * Filter a list of systems.
     *
     * old syntax:
     * <code>includeImplementations(abName -> abstractions[abName].implementations?.findAll { impl -> impl.id == 'XXX'}</code>
     *
     * new syntax:
     * <code>includeSystems(abName -> abstractions[abName].systems?.findAll { impl -> impl.id == 'XXX'}</code>
     */
    Closure<List<System>> includeImplementationsClosure
    /**
     * Filter tests by name (ANT matcher syntax)
     *
     * Examples in "de.uni_mannheim.swt.lasso.engine.matcher.TestMatcherTest" (engine module)
     */
    String includeTests = '*'
    /**
     * Map that holds all unknown settings in the configuration block.
     */
    Map<String, Serializable> unknownSettings = [:]
    /**
     * Internal reference to ActionConfiguration used by the LASSO engine.
     */
    ActionConfiguration actionConfiguration
    /**
     * @return Name of action
     */
    String getName() {
        map.name
    }
    /**
     * @return Type of action
     */
    String getType() {
        map.type
    }
    /**
     * @return Abstractions registered for this action
     */
    def getAbstractions() {
        abstractionContainerSpec.abstractions
    }
    /**
     * @return Abstractions registered for this action
     */
    def getStimulusMatrices() {
        abstractionContainerSpec.abstractions
    }
    /**
     * @return Actions that this action depends on
     */
    def getActions() {
        lasso.actionContainerSpec.actions
    }

//    void apply(Action action) {
//        callRehydrate(closure, action, this, null)
//    }

    /**
     * Internal method to register all unknown properties
     *
     * @param name
     * @param value
     * @return
     */
    def propertyMissing(String name, value) {
        unknownSettings.put(name, value)
    }

    def methodMissing(String name, args) {
        println("Missing method " + name + " / " + args)

        unknownSettings.put(name, args)
    }

    /**
     * Internal method to call the action closure
     */
    void apply() {
        callRehydrate(closure, this, this, null)

        actionConfiguration = new ActionConfiguration()
        actionConfiguration.addConfiguration(unknownSettings)
        actionConfiguration.setDependsOnActions(dependsOn) // not to be confused with dependsOn list
        actionConfiguration.setIncludeTestsPattern(includeTests)
    }

    /**
     * Retrieve query report
     *
     * @param sql
     * @return
     */
    Table queryReport(String sql) {
        return lasso.executionContext.reportOperations.select(lasso.executionId, sql)
    }

    /**
     * Save report for a System
     *
     * @param reportName
     * @param abstractionSpec
     * @param implementation
     * @param values
     */
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

    /**
     * DSL command
     *
     * <code>dependsOn 'action1,action2,...'</code>
     *
     * @param dependsOn
     */
    void dependsOn(String ... dependsOn) {
        this.dependsOn = dependsOn
    }

    /**
     * DSL command
     *
     * <code>includeAbstractions 'abstraction1,abstraction2,...'</code>
     *
     * @param includeAbstractions ANT matcher syntax
     *
     * Examples in "de.uni_mannheim.swt.lasso.engine.matcher.AbstractionMatcherTest" (engine module)
     */
    void includeAbstractions(String includeAbstractions) {
        this.includeAbstractions = includeAbstractions
    }

    /**
     * Synonym to includeAbstractions(..).
     *
     * @param abstractions
     */
    void include(String abstractions) {
        includeAbstractions(abstractions)
    }

    /**
     * DSL command
     *
     * <code>includeTests 'XXX'</code>
     *
     * @param includeTests  ANT matcher syntax
     *
     * Examples in "de.uni_mannheim.swt.lasso.engine.matcher.TestMatcherTest" (engine module)
     */
    void includeTests(String includeTests) {
        this.includeTests = includeTests
    }

    /**
     * DSL command alias for ActionSpec#includeTests.
     *
     * @param includeSequences
     */
    void includeSequences(String includeSequences) {
        includeTests(includeSequences)
    }

    /**
     * DSL command: Create new abstraction container
     *
     * <code>abstraction(name) {closure}</code>
     *
     * @param name
     * @param closure
     */
    AbstractionSpec abstraction(String name, Closure<AbstractionSpec> closure) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: closure)

        lasso.registerAbstraction(abstractionSpec)
        abstractionSpecs << abstractionSpec
        // set to scope
        //abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)

        return abstractionSpec
    }

    System implementation(String id, String className) {
        // FIXME use some placeholder artifact
        String EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT = "commons-codec:commons-codec:1.15";

        return implementation(id, className, EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT)
    }

    String generateUUID() {
        return UUID.randomUUID().toString();
    }

    System implementation(String id, String className, String mavenCoordinate) {
        String[] uriParts = StringUtils.split(mavenCoordinate, ":");

        CodeUnit codeUnit = new CodeUnit()
        codeUnit.setUnitType(CodeUnit.CodeUnitType.CLASS)
        codeUnit.setName(StringUtils.substringAfterLast(className, "."))
        codeUnit.setPackagename(StringUtils.substringBeforeLast(className, "."))
        codeUnit.setId(id)
        codeUnit.setGroupId(uriParts[0])
        codeUnit.setArtifactId(uriParts[1])
        codeUnit.setVersion(uriParts[2])
        codeUnit.setMetaData(new HashMap<String, Object>())
        codeUnit.setMeasures(new HashMap<String, Double>())
        System system = new System(codeUnit)

        return system
    }

    /**
     * Implementation from source (i.e., manually defined)
     *
     * @param id
     * @param className
     * @param content
     * @return
     */
    // FIXME realize: mark model with "unresolved" and then compile ad hoc in engine
    System implementationFromSource(String id, String className, String content) {
        // FIXME use some placeholder artifact
        String EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT = "commons-codec:commons-codec:1.15";

        System system = implementation(id, className, EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT)
        system.code.content = content

        return system
    }

    System pyImplementationFromSource(String id, String className, String content) {
        // FIXME use some placeholder artifact
        String EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT = "commons-codec:commons-codec:1.15";

        System system = implementation(id, className, EXAMPLES_LASSO_EXAMPLES_1_0_0_SNAPSHOT)
        system.code.content = content
        system.code.lang = CodeUnit.PYTHON

        return system
    }

    /**
     * Manually specify JUnit tests
     *
     * @param content
     * @param interfaceSpecification
     * @return
     */
    JUnitCodeUnit testFromJUnit(String content, String interfaceSpecification) {
        try {
            JavaParser javaParser = new JavaParser();
            com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(content).getResult().get();

            // parse name
            CodeUnit unit = new CodeUnit();
            unit.setId(generateUUID());
            unit.setName(cu.getType(0).getNameAsString());

            // add package name
            unit.setPackagename(cu.getPackageDeclaration().isPresent() ? cu.getPackageDeclaration().get().toString() : "");
            unit.setContent(cu.toString());
            unit.setUnitType(CodeUnit.CodeUnitType.CLASS);

            JUnitCodeUnit jUnitCodeUnit = new JUnitCodeUnit(unit, interfaceSpecification);
            jUnitCodeUnit.setTestPrefix("junit");
            return jUnitCodeUnit;
        } catch (Throwable e) {
            //LOG.warn("failed to parse code", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Manually specify JUnit tests
     *
     * @param content
     * @return
     */
    JUnitCodeUnit testFromJUnit(String content) {
        return testFromJUnit(content, null)
    }

     /**
     * DSL command: Create new abstraction container and use the interface specified in LQL.
     *
     * <code>abstraction(name,lql) {closure}</code>
     *
     *
     * @param name
     * @param lql
     * @param closure
     */
    AbstractionSpec abstraction(String name, String lql, Closure<AbstractionSpec> closure) {
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

        return abstractionSpec
    }

    /**
     * DSL command: Create new abstraction container and use a list of Systems and the interface specified in LQL.
     *
     * <code>abstraction(name,[Systems],lql) {closure}</code>
     *
     * @param name
     * @param implementationIds
     * @param lql
     * @param closure
     */
    AbstractionSpec abstraction(String name, List<String> implementationIds, String lql, Closure<AbstractionSpec> closure) {
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

        return abstractionSpec
    }

    /**
     * DSL command: Create new abstraction container from a given System.
     *
     * <code>abstraction(System) {closure}</code>
     *
     * @param system
     * @param closure
     */
    AbstractionSpec abstraction(System system, Closure<AbstractionSpec> closure) {
        return abstraction(system.id, closure)
    }

    /**
     * DSL command: Merge abstraction container
     *
     * <code>abstraction(name,[AbstractionSpec]) {closure}</code>
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

    /**
     * DSL command: Create a stimulus matrix
     *
     * <code>stimulusMatrix([System], [SheetSpec], name, interfaceSpecification) {closure}</code>
     *
     * @param name
     * @param lql
     * @param implementations
     * @param tests
     * @return
     */
    AbstractionSpec stimulusMatrix(String name, String lql, List<System> implementations, List tests) {
        AbstractionSpec abSpec = abstraction(implementations, name, lql)
        abSpec.tests = tests

        return abSpec
    }

    /**
     * DSL command: Create a stimulus matrix
     *
     * <code>stimulusMatrix([System], [SheetSpec], name, interfaceSpecification) {closure}</code>
     *
     * @param name
     * @param lql
     * @param implementations
     * @param tests
     * @param dependencies
     * @return
     */
    AbstractionSpec stimulusMatrix(String name, String lql, List<System> implementations, List tests, List dependencies) {
        AbstractionSpec abSpec = stimulusMatrix(name, lql, implementations, tests)
        abSpec.dependencies = dependencies

        return abSpec
    }

    AbstractionSpec stimulusMatrix(String name, String lql, Closure<AbstractionSpec> closure) {
        return abstraction(name, lql, closure)
    }

    AbstractionSpec stimulusMatrix(String name, String lql, Closure<AbstractionSpec> closure, List tests) {
        AbstractionSpec abSpec = abstraction(name, lql, closure)
        abSpec.tests = tests

        return abSpec
    }

                                   /**
     * DSL command: Create an abstraction container from a list of System.
     *
     * <code>abstraction([System],name, interfaceSpecification) {closure}</code>
     *
     * @param implementations
     * @param name
     * @param lql
     * @return
     */
    AbstractionSpec abstraction(List<System> implementations, String name, String lql) {
        // apply closure to action
        Map<String, ?> aMap = [:]
        aMap.put("name", name)

        AbstractionSpec abstractionSpec = new AbstractionSpec(map: aMap, closure: null)

        Abstraction merged = new Abstraction()
        merged.setName(name)

        if(lql) {
            abstractionSpec.lql = lql
        }

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

    /**
     * DSL command: Create an abstraction container from a list of Systems, also assign a Specification (LQL interface as well as a list of sequence sheets).
     *
     * <code>abstraction([System],name,Specification) {closure}</code>
     *
     * @param implementations
     * @param name
     * @param specification
     * @return
     */
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

    /**
     * Unnamed profile block for setting target execution profile.
     *
     * <code>profile(map) {closure}</code>
     *
     * @param closure
     */
    void profile(Closure closure) {
        profile(this.name + "_" + java.lang.System.currentTimeMillis(), closure)
    }

     /**
     * Named profile block for setting target execution profile.
     *
     * <code>profile(name,map) {closure}</code>
     *
     * @param name
     * @param closure
     */
    void profile(String name, Closure<ProfileSpec> closure) {
        ProfileSpec profileSpec = new ProfileSpec(name:name)
        callRehydrate(closure, profileSpec, this, null)

        lasso.register(profileSpec)

        this.profileSpec = profileSpec
    }

    /**
     * Reference to existing profile block (by name)
     *
     * <code>profile('myProfileName')
     *
     * @param name
     */
    void profile(String name) {
        this.profileSpec = lasso.profileContainerSpec.profiles[name]
    }

    @Deprecated
    void filter(Closure<System> closure) {
        // TODO
    }

    /**
     * Action lifecycle method: whenAbstractionsReady
     *
     * <code>whenAbstractionsReady() {closure}</code>
     *
     * @param closure
     */
    void whenAbstractionsReady(Closure closure) {
        whenAbstractionsReadyClosure = closure
    }

    /**
     * Action lifecycle method: analyze (same as whenAbstractionsReady)
     *
     * <code>analyze() {closure}</code>
     *
     * @param closure
     */
    void analyze(Closure closure) {
        whenAbstractionsReadyClosure = closure
    }

    /**
     * Filter a list of systems.
     *
     * old syntax:
     * <code>includeImplementations(abName -> abstractions[abName].implementations?.findAll { impl -> impl.id == 'XXX'}</code>
     *
     * new syntax:
     * <code>includeSystems(abName -> abstractions[abName].systems?.findAll { impl -> impl.id == 'XXX'}</code>
     *
     * @param closure
     */
    void includeImplementations(Closure<List<System>> closure) {
        // clone action
        //Closure cloneCl = closure.rehydrate(this, this, this)
        //cloneCl.resolveStrategy = Closure.DELEGATE_FIRST

        includeImplementationsClosure = closure
    }

    /**
     * Alias for ActionSpec#includeImplementations.
     *
     * @param closure
     */
    void includeSystems(Closure<List<System>> closure) {
        includeImplementations(closure)
    }

    /**
     * Action lifecycle method: execute
     *
     * <code>execute() {closure}</code>
     *
     * @param closure
     */
    void execute(Closure closure) {
        executeClosure = closure
    }

    /**
     * Action lifecycle method: configure
     *
     * <code>configure() {closure}</code>
     *
     * @param closure
     */
    void configure(Closure closure) {
        configureClosure = closure
    }

    /**
     * Internal method to call closure for configure
     */
    void applyConfigure() {
        if(configureClosure)  {
            callRehydrate(configureClosure, this, this, null)

            actionConfiguration = new ActionConfiguration()
            actionConfiguration.addConfiguration(unknownSettings)
            actionConfiguration.setDependsOnActions(dependsOn)
            actionConfiguration.setIncludeTestsPattern(includeTests)
        }
    }

    Object applyCustomCommand(Closure customClosure, Object model, args) {
        callRehydrate(customClosure, model, this, args)
    }

    /**
     * Internal method to call closure for execute
     */
    void applyExecute() {
        if(executeClosure)  {
            callRehydrate(executeClosure, this, this, null)
        }
    }

    /**
     * Internal method to call closure for whenAbstractionsReady
     */
    void applyWhenAbstractionsReady() {
        if(whenAbstractionsReadyClosure)  {
            callRehydrate(whenAbstractionsReadyClosure, this, this, null)
        }
    }

    /**
     * Internal method to call closure for includeImplementations
     */
    List<System> applyIncludeImplementations(String abName) {
        if(includeImplementationsClosure) {
            return callRehydrate(includeImplementationsClosure, this, this, abName)
        } else {
            return []
        }
    }

    /**
     * Retrieve SRM data by AbstractionSpec
     *
     * <code>
     *     def base64 = abstractions['Base64Encode']
     *     srm(abstraction: base64)
     * </code>
     *
     *
     *
     * <code>
     *     srm(abstraction: stack).systems['ArrayStack'].observations['cc.branch.total']
     * </code>
     *
     * @param map
     * @return
     */
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
     * Export SRM data to given file name into script's workspace.
     *
     * <code>
     *     export(srm, 'filename')
     * </code>
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
     * Set a given System as the pseudo oracle for a set of sequences.
     *
     * <code>
     *     toOracle(System, Sequences)
     * </code>
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

    /**
     * Set a given System as the pseudo oracle.
     *
     * <code>
     *     toOracle(System)
     * </code>
     *
     * @param system
     * @return
     */
    def toOracle(System system) {
        return toOracle(system, [])
    }

    /**
     * Set the oracle values given in the sequences as the pseudo oracle.
     *
     * <code>
     *     toOracle(srm(abstraction: base64).sequences)
     * </code>
     *
     * @param sequences
     * @return
     */
    def toOracle(List<Sequence> sequences) {
        Behaviour behaviour = new Behaviour();
        behaviour.sequences = sequences

        return behaviour
    }

    /**
     * Create new sequence sheet
     *
     * @param closure
     * @return
     */
    def sheet(Closure closure) {
        return sheet(null, closure)
    }

    /**
     * Create new sequence sheet
     *
     * @param map
     * @param closure
     * @return
     */
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
     * <code>
     *  'sheet2': sheet(name: 'pushPop', p1:'Stack', p2:5)
     * </code>
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

    /**
     * Create new sequence sheet
     *
     * @param map
     * @param closure
     * @return
     */
    def test(Map<String, ?> map, Closure closure) {
        String sheetName = map.name

        SheetSpec sheetSpec = new SheetSpec(inputParameters: map, closure: closure)
        lasso.register(sheetSpec)

        // apply
        sheetSpec.apply()

        sheetSpecs << sheetSpec

        return sheetSpec
    }

    /**
     * reuse existing sheet with different parameters
     *
     * <code>
     *  'sheet2': sheet(name: 'pushPop', p1:'Stack', p2:5)
     * </code>
     *
     * @param map
     * @return
     */
    def test(Map<String, ?> map) {
        String sheetName = map.name

        SheetSpec sheetSpec = sheetSpecs.find { it.inputParameters.name == sheetName}

        SheetSpec copy = new SheetSpec(inputParameters: map)
        copy.rows = new ArrayList<>(sheetSpec.rows)

        sheetSpecs << copy

        return copy
    }
}
