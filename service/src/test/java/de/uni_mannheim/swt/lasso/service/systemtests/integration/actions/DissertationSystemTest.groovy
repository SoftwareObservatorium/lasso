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
package de.uni_mannheim.swt.lasso.service.systemtests.integration.actions

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine
import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.service.systemtests.integration.AbstractGroovySystemTest
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import de.uni_mannheim.swt.lasso.srm.ClusterSRMRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Demonstrates dissertation examples.
 *
 * @author mkessel
 */
class DissertationSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    @Test
    void test_MOTIVATIONAL_EXAMPLE_1() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // select from given data source
        /* define new analysis pipeline */
        study(name:'Stack-Test-Quality') {
            /* selects a given stack implementation s */
            action(name:'select', type:'Select') {
                abstraction('Stack') {
                    queryForClasses "*:*"
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"' // assumes some existing Java class (dummy)
                }
            }
            /* defines an execution profile for the arena */
            profile('myProfile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java8') { // execution environment
                    image = 'maven:3.5.4-jdk-8-alpine' // (docker) image template
                }
            }
            /* populate and execute the arena */
            action(name:'execute',type:'ArenaExecute') {
                specification = """Stack {
                            push(java.lang.Object)->java.lang.Object
                            size()->int
                        }
                        """
                sequences = [ // defines sequence sheets using LSL keywords
                        'ti': sheet(stack:'Stack', p:'hello world') {
                            row '',  'create', '?stack'
                            row '',  'push',   'A1',     '?p'
                            row '',  'size',   'A1'
                        },
                        // ... // other tests
                ]
        
                dependsOn 'select'
                includeAbstractions 'Stack' // select implementation from former action
                profile('myProfile')
            }
            /* measure MS */
            action(name:"mutationScore",type:'Pitest') {
                dependsOn "execute"
                includeAbstractions 'Stack'
                profile('myProfile')
            }
            /* measure BC */
            action(name:"branchCoverage",type:'JaCoCo') {
                dependsOn "execute"
                includeAbstractions 'Stack'
                profile('myProfile')
            }
            /* analyse obtained measures within LSL (optionally, export) */
            action(name:'analyse') {
                dependsOn 'branchCoverage'
                includeAbstractions 'Stack'
                // custom analysis based on SRM structure
                execute() {
                    def stack = abstractions['Stack']
                    def branchTotal = srm(abstraction: stack)
                            .systems['4e73bb0d-f01f-43e5-bf46-7ab7870a289f'].observations['JaCoCoReport.allComplexityTotal'] // not always clear which report without explicit action name (default: last)
                    def mutationScore = srm(abstraction: stack)
                            .systems['4e73bb0d-f01f-43e5-bf46-7ab7870a289f'].observations['PitestReport.coverage']
                    //... // do something
                    log("$branchTotal and $mutationScore")
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_MOTIVATIONAL_EXAMPLE_2() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // select from given data source
        /* define new analysis pipeline */
        study(name:'Base64-Heteromorphic-Redundancy') {
            /* query Base64 implementations by interface signatures */
            action(name:'select', type:'Select') {
                abstraction('Base64') { // interface-driven search
                    queryForClasses 'Base64{encode(byte[])->byte[]}'
                    rows = 10 // no. of Java classes to return
                }
            }
            /* reject code clones */
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions '*'
            }
            /* defines an execution profile for the arena */
            profile('myTdsProfile') {
                scope('class') { type = 'class' }
                environment('java8') {
                    image = 'maven:3.5.4-jdk-8-alpine' // (docker) image template
                }
            }
            /* populate and execute the arena */
            action(name:'filter',type:'ArenaExecute') {
                sequences = [
                        'testEncode': sheet(base64:'Base64', p2:"user:pass".getBytes()) {
                            row  '',    'create', '?base64'
                            row 'dXNlcjpwYXNz'.getBytes(),  'encode',   'A1',     '?p2'
                        },
                        'testEncode_padding': sheet(base64:'Base64', p2:"Hello World".getBytes()) {
                            row  '',    'create', '?base64'
                            row 'SGVsbG8gV29ybGQ='.getBytes(),  'encode',   'A1',     '?p2'
                        }
                ]
                maxAdaptations = 5 // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Base64' // select implementations from former action
                profile('myTdsProfile')
                // match implementations and compute simple statistics
                whenAbstractionsReady() {
                    def base64 = abstractions['Base64']
                    def base64Srm = srm(abstraction: base64)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: base64).sequences)
                    // alternatively, use any system as a (pseudo) oracle
                    def referenceImpl = toOracle(srm(abstraction: base64).systems.first())
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: base64)
                            .systems // select all systems
                            .equalTo(expectedBehaviour) // functionally equivalent
        
                    // get LOC measures for advanced heteromorphic redundancy assessment
                    def loc = matchesSrm
                            .systems.observations['cc.loc']
                    // average
                    double locAvg = loc.mean()
                    log("Average number of lines of code is ${locAvg}")
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

//        while(true) {
//            Thread.sleep(100);
//        }
    }

    /**
     * From Chapter 9
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_LSL_TDS() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        // interface of a Stack in LQL notation
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int}"""
        study(name: 'Stack-TestDrivenSelection') {
            action(name: 'select', type: 'Select') {
                abstraction('Stack') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = 10
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    filter 'complexity:[2 TO *]'
                }
            }

            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'pushPop': sheet(p1: 'Stack', p2: "hello world") {
                            row '', 'create', '?p1'
                            row '?p2', 'push', 'A1', '?p2'
                            row '?p2', 'peek', 'A1'
                            row 1, 'size', 'A1'
                            row '?p2', 'pop', 'A1'
                            row 0, 'size', 'A1'
                        }
                ]
                maxAdaptations = 1 // how many adaptations to try

                dependsOn 'select'
                includeAbstractions 'Stack'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
                    }
                }

                whenAbstractionsReady() {
                    def stack = abstractions['Stack']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // alternatively, use any system as a (pseudo) oracle
                    def referenceImpl = toOracle(srm(abstraction: stack).systems.first())
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .equalTo(expectedBehaviour) // functionally equivalent

                    // iterate over sub-SRM
                    matchesSrm.systems.each { s ->
                        log("Matched class ${s.id}, ${s.packageName}.${s.name}")
                    }
                    // export to individual CSV file (if desired)
                    export(matchesSrm, 'stacks.csv')
                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    // TODO from Chapter 13: Search and Curate: TDS and CDS, semantics-agnostic and semantics-aware

    /**
     * From Chapter 13: TDS with rank
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_CH13_SEARCH_TDS() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()-> int
        }
        """
        study(name: 'Stack-TDS') {
            action(name: 'select', type: 'Select') {
                abstraction('Stack') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = 10
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    filter 'complexity:[1 TO *]'
                }
            }

            action(name: 'clonesAlt', type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true

                dependsOn 'select'
                includeAbstractions 'Stack'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }

            action(name: 'filter', type: 'ArenaExecute') { // test filter
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'pushPop': sheet(p1: 'Stack', p2: 5) {
                            row '', 'create', '?p1'
                            row '?p2', 'push', 'A1', '?p2'
                            row '?p2', 'peek', 'A1'
                            row 1, 'size', 'A1'
                            row '?p2', 'pop', 'A1'
                            row 0, 'size', 'A1'
                        }
                ]
                maxAdaptations = 1 // how many adaptations to try

                dependsOn 'select'
                includeAbstractions 'Stack'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
                    }
                }

                whenAbstractionsReady() {
                    def stack = abstractions['Stack']
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .equalTo(expectedBehaviour) // functionally equivalent

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }

            action(name: 'rank', type: 'Rank') { // rank based on two criteria
                strategy = 'HDS_SMOOP' // SOCORA ranking strategy
                criteria = ['IndexMeasurements.m_static_loc_td:MIN:1',
                            'cc.branch.total:MIN:2']

                dependsOn 'filter'
                includeAbstractions '*'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * From Chapter 13: CDS
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_CH13_SEARCH_CDS() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        study(name:'Stack-CDS') {
        
            action(name:'selectStack', type:'Select') {
                abstraction('Stack') { // select single, known class
                    queryForClasses '*:*'
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"'
                }
            }
        
            action(name:'executeRef',type:'EvoSuite') { // generate tests
                searchBudget = 30
        
                dependsOn 'selectStack'
                includeAbstractions 'Stack'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
            }
        
            action(name: 'selectAlt', type: 'Select') { // select alternative impls.
                dependsOn 'executeRef'
                includeAbstractions 'Stack'
        
                execute() {
                    List refImpls = abstractions['Stack'].implementations
                    refImpls.each { impl ->
                        abstraction(impl) {
                            queryByExample impl, 'class'
                            rows = 10
        
                            excludeClassesByKeywords(["private", "abstract"])
                            excludeTestClasses()
                            excludeInternalPkgs()
        
                            excludeImplementation(impl.id)
                        }
                    }
                }
            }
        
            action(name: 'clonesAlt', type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true
                refActionRef = "executeRef"
        
                dependsOn 'selectAlt'
                includeAbstractions '*-*'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name:'arena',type:'ArenaExecute') { // execute in the arena
                disablePartitioning = true
                maxAdaptations = 1
                task = 'Amplify'
                features = ['mutation', 'cc'] // measure MS and BC
        
                dependsOn 'clonesAlt'
                includeAbstractions '*-*'
                includeSequences '*' // take any
                profile {
                    environment('java8') {
                        image = 'openjdk:8-jdk-alpine'
                    }
                }
        
                whenAbstractionsReady() { 
                    def stack = abstractions[abstractions.keySet().first()]
                    def expectedBehaviour = toOracle(srm(abstraction: stack).systems['4e73bb0d-f01f-43e5-bf46-7ab7870a289f'])
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, 0.8d) // functionally similar (>= 80% passing of tests)

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * From Chapter 13: Curate
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    // TODO NOT INCLUDE! better use CDS for this
    void test_CH13_CURATE_BEHAVIOUR_AWARE() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        
        study(name:'Behaviour-Aware-Curation-Criteria') {
        
            action(name:'selectRandom', type:'Select') { // random sampling
                abstraction('Random') {
                    queryForClasses '*:*'
                    random = true // random selection
                    rows = 10 // number of classes to select
                }
            }
        
            action(name:'executeRef',type:'EvoSuite') { // generate tests
                searchBudget = 120
        
                dependsOn 'selectRandom'
                includeAbstractions 'Random'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
            }
        
            action(name: 'selectAlt', type: 'Select') { // select alternative impls.
                dependsOn 'executeRef'
                includeAbstractions 'Random'
        
                execute() {
                    List refImpls = abstractions['Random'].implementations
                    refImpls.each { impl ->
                        abstraction(impl) {
                            queryByExample impl, 'class'
                            rows = 10 // change
        
                            excludeClassesByKeywords(["private", "abstract"])
                            excludeTestClasses()
                            excludeInternalPkgs()
        
                            excludeImplementation(impl.id)
                        }
                    }
                }
            }
        
            action(name: 'clonesAlt', type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true
                refActionRef = "executeRef"
        
                dependsOn 'selectAlt'
                includeAbstractions '*-*'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
            
            // ...
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * From Chapter 13: Curate
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    // TODO NOT INCLUDE! better use TDS for this
    void test_CH13_CURATE_BEHAVIOUR_AGNOSTIC() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        
        study(name:'Behaviour-Agnostic-Curation-Criteria') {
        
            action(name:'selectRandom', type:'Select') { // random sampling
                abstraction('Random') {
                    queryForClasses '*:*'
                    random = true // random selection
                    rows = 10 // number of classes to select
                }
            }
        
            action(name:'executeRef',type:'EvoSuite') { // generate tests
                searchBudget = 120
        
                dependsOn 'selectRandom'
                includeAbstractions 'Random'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
            }
        
            action(name: 'clones', type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true
        
                dependsOn 'executeRef'
                includeAbstractions '*-*'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            // ...
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * From Chapter 14: TestGen
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_CH14_TestGen() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        
        def altImpls = 10
        def adapterImplementations = 1
        def refEvoSuiteTimeBudget = 30
        def altEvoSuiteTimeBudget = 30
        
        study(name:'LASSO-TestGen') {
            action(name:'select', type:'Select') {
                abstraction('Stack') { // assume known CUT
                    queryForClasses '*:*'
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"' // known class
                }
            }
        
            action(name: 'selectAlt', type: 'Select') { // select alternative impls.
                dependsOn 'select'
                includeAbstractions 'Stack'
        
                execute() {
                    List refImpls = abstractions['Stack'].implementations
                    refImpls.each { impl ->
                        abstraction(impl) {
                            queryByExample impl, 'class'
                            rows = altImpls
                            excludeClassesByKeywords(["private", "abstract"])
                            excludeTestClasses()
                            excludeInternalPkgs()
                            excludeImplementation(impl.id)
                        }
                    }
                }
            }
        
            action(name: 'clonesAlt', type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true
                refActionRef = "select"
        
                dependsOn 'selectAlt'
                includeAbstractions '*-*'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            profile('evosuite') { // execution profile
                scope('class') { type = 'class' }
                environment('java8') {
                    image = 'maven:3.5.4-jdk-8'
                }
            }
        
            action(name:"evosuiteRef",type:'EvoSuite') { // generate tests for reference impl.
                ignoreMissingReport = true
                searchBudget = refEvoSuiteTimeBudget
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                profile('evosuite')
            }
        
            action(name:"evosuiteAlt",type:'EvoSuite') { // generate tests for alternative impls.
                ignoreMissingReport = true
                searchBudget = altEvoSuiteTimeBudget
        
                dependsOn "clonesAlt"
                includeAbstractions '*-*'
                profile('evosuite')
            }
        
            action(name:"arena",type:'Arena') { // obtain test sequences
                maxAdaptations = adapterImplementations
                task = 'Amplify'
                referenceImplementationOnly = true
        
                dependsOn "evosuiteAlt"
                includeAbstractions '*-*'
                includeSequences '*'
                profile('evosuite')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * From Chapter 14: TestAmp
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_CH14_TestAmp() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int
        }
        """
        study(name: 'LASSO-TestAmp') {
            action(name: 'select', type: 'Select') {
                abstraction('Stack') { // select 10 stack classes based on IDCS
                    queryForClasses interfaceSpec
                    rows = 10
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    filter 'complexity:[1 TO *]'
                }
            }

            action(name: 'clonesAlt', type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true

                dependsOn 'select'
                includeAbstractions 'Stack'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }

            profile('arena') { // execution profile
                scope('class') { type = 'class' }
                environment('java8') {
                    image = 'maven:3.5.4-jdk-8'
                }
            }

            action(name: 'filter', type: 'ArenaExecute') { // determine functionally equivalent classes
                sequences = [
                        'pushPop': sheet(p1: 'Stack', p2: 5) {
                            row '', 'create', '?p1'
                            row '?p2', 'push', 'A1', '?p2'
                            row '?p2', 'peek', 'A1'
                            row 1, 'size', 'A1'
                            row '?p2', 'pop', 'A1'
                            row 0, 'size', 'A1'
                        }
                ]
                maxAdaptations = 1

                dependsOn 'select'
                includeAbstractions 'Stack'
                profile('arena')

                whenAbstractionsReady() {
                    def stack = abstractions['Stack']
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .equalTo(expectedBehaviour) // functionally equivalent
                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }

            action(name: 'evosuite', type: 'EvoSuite') { // generate tests
                ignoreMissingReport = true
                searchBudget = 120

                dependsOn 'filter' // mandatory
                includeAbstractions 'Stack'
                profile('arena')
            }

            action(name: "arena", type: 'Arena') { // amplify test sequences
                disablePartitioning = true
                maxAdaptations = 1
                task = 'Amplify'
                exportCsv = true

                dependsOn "evosuite"
                includeAbstractions 'Stack'
                includeSequences '*'
                profile('arena')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_LSL_CH15_EvoSuite_tool_study() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        
        def randomClassesTotal = 20 // number of classes to randomly sample
        def repetitions = 10 // number of repetitions
        def timeBudgets = [30, 60] // time budgets in seconds
        
        study(name:'EvoSuite_TimeBudgets') {
            action(name: 'selectRandom', type: 'Select') { // random sampling
                abstraction('Random') {
                    queryForClasses '*:*', 'concept'
                    rows = randomClassesTotal
                    random = true
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    excludeExceptions()
                    // rule out trivial classes
                    filter 'methods:[1 TO *]'
                    filter 'branches:[10 TO *]'
                }
            }
        
            action(name: "clones", type: 'Nicad6') { // reject code clones
                cloneType = "type2"
                collapseClones = true
        
                dependsOn "selectRandom"
                includeAbstractions 'Random'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            profile('evosuite') { // execution profile
                scope('class') { type = 'class' }
                environment('java8') {
                    image = 'maven:3.5.4-jdk-8'
                }
            }
        
            for(int repetition = 0; repetition < repetitions; repetition++) { // repeat
                for(int timeBudget : timeBudgets) {
                    action(name:"evosuite_${timeBudget}_${repetition}",type:'EvoSuite') { // run EvoSuite
                        version = '1.1.0'
                        searchBudget = timeBudget
                        // other parameters: criteria, algorithm etc.
                        dependsOn 'clones'
                        includeAbstractions 'Random'
                        profile('evosuite')
                    }
                    action(name:"pitest_${timeBudget}_${repetition}",type:'Pitest') { // measure MS
                        dependsOn "evosuite_${timeBudget}_${repetition}"
                        includeAbstractions 'Random'
                        profile('evosuite')
                    }
                    action(name:"jacoco_${timeBudget}_${repetition}",type:'JaCoCo') { // measure BC
                        minimumTestCoverage = 0d
        
                        dependsOn "evosuite_${timeBudget}_${repetition}"
                        includeAbstractions 'Random'
                        profile('evosuite')
                    }
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
//        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'mutationScore', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'branchCoverage', 'Stack', 1)
//        verifyAbstraction(lslExecutionContext, 'analyse', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * BONUS
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_generate_class_stack_jacoco() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Randoop-JaCoCo-Pitest') {
            action(name:'select', type:'Select') {
                abstraction('Stack') { // assume known CUT
                    queryForClasses '*:*'
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"' // known class
                }
            }
            
            profile('myProfile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java8') { // execution environment
                    image = 'maven:3.5.4-jdk-8' // (docker) image template
                }
            }
            
            /** Randoop */
            action(name:'randoop',type:'Randoop') {
                // configuration
        
                dependsOn 'select' // mandatory
                includeAbstractions 'Stack'
                profile('myProfile')
                
                whenAbstractionsReady {
                    //
                }
            }
            /** JaCoCo */
            action(name:"jacoco",type:'JaCoCo') {
                minimumTestCoverage = 1d
                generateReport = true
    
                dependsOn "randoop"
                includeAbstractions 'Stack'
                includeTests '*' // include any tests
                profile('myProfile')
                
                whenAbstractionsReady {
                    //
                }
            }
            /** Pitest */
            action(name:"pitest",type:'Pitest') {
                dependsOn "randoop"
                includeAbstractions 'Stack'
                includeTests '*' // include any tests
                profile('myProfile')
                
                whenAbstractionsReady {
                    //
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'randoop', 'Stack', 1)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from RandoopReport where action = 'randoop' and abstraction = 'Stack'")
        println(table.printAll())
    }
}
