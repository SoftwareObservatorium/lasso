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
package de.uni_mannheim.swt.lasso.service.systemtests.integration.exp

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine
import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.service.systemtests.integration.AbstractGroovySystemTest
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import de.uni_mannheim.swt.lasso.srm.ClusterSRMRepository
import de.uni_mannheim.swt.lasso.srm.SRMManager
import joinery.DataFrame
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
class StudyReuseSISystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    /**
     * Add second version with superclass java.util.Collection?
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_Stack_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int}"""
        study(name: 'JSSReuse-Stack-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Stack') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Stack'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'pushPop': sheet(p1: 'Stack', p2: "hello", p3: "world") {
                            row '', 'create', '?p1'
                            row '?p2', 'push', 'A1', '?p2'
                            row '?p3', 'push', 'A1', '?p3'
                            row '?p3', 'peek', 'A1'
                            row 2, 'size', 'A1'
                            row '?p3', 'pop', 'A1'
                            row 1, 'size', 'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Stack'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Stack']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                    
                    log("hello")
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Stack'
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

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_Bag_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Bag {
            add(java.lang.Object)->void
            count(java.lang.Object)->int
            remove(java.lang.Object)->void
            contains(java.lang.Object)->boolean
            size()->int
        }"""
        study(name: 'JSSReuse-Bag-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Bag') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Bag'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'pushPop': sheet(p1: 'Bag', p2: "hello", p3: "world") {
                            row '', 'create', '?p1'
                            row '', 'add', 'A1', '?p2'
                            row '', 'add', 'A1', '?p3'
                            row '', 'add', 'A1', '?p2'
                            row 2, 'size', 'A1'
                            row 2, 'count', 'A1', '?p2'
                            row 1, 'count', 'A1', '?p3'
                            row false, 'contains', 'A1', "engineering"
                            row '', 'remove', 'A1', '?p2'
                            row 1, 'count', 'A1', '?p2'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Bag'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Bag']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Bag'
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

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    // TODO add removeValue?
    @Test
    void test_MultiMap_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """MultiMap{
            put(java.lang.Object,java.lang.Object)->java.lang.Object
            get(java.lang.Object)->java.util.Collection
            size()->int
        }"""
        study(name: 'JSSReuse-MultiMap-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('MultiMap') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'MultiMap'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'pushPop': sheet(p1: 'MultiMap', p2: "hello", p3: "world") {
                            row '', 'create', '?p1'
                            row '', 'put', 'A1', '?p2', '?p3'
                            row '', 'put', 'A1', '?p2', '?p3'
                            row '', 'get', 'A1', '?p2'
                            row 2, 'size', 'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'MultiMap'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['MultiMap']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'MultiMap'
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

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_ImmutableSet_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """ImmutableSet {
            ImmutableSet(java.util.Collection)
            add(java.lang.Object)->void
            remove(java.lang.Object)->void
            iterator()->java.util.Iterator
            size()->int
        }"""
        study(name: 'JSSReuse-ImmutableSet-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('ImmutableSet') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'ImmutableSet'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'add': sheet(p1: 'ImmutableSet') {
                        // TODO we have to create your own set in SSN (NonReceiver issue)
                            row '', 'create', 'java.util.ArrayList'
                            row '', 'add', 'A1', "hello"
                            row '', 'add', 'A1', "world"
                            row '', 'create', '?p1', 'A1'
                            row 2, 'size', 'A4'
                            row '', 'add', 'A4', 'se'
                        },
                        'remove': sheet(p1: 'ImmutableSet') {
                            row '', 'create', 'java.util.ArrayList'
                            row '', 'add', 'A1', "hello"
                            row '', 'add', 'A1', "world"
                            row '', 'create', '?p1', 'A1'
                            row 2, 'size', 'A4'
                            row '', 'remove', 'A4', 'hello'
                        },
                        'iterate': sheet(p1: 'ImmutableSet') {
                            row '', 'create', 'java.util.ArrayList'
                            row '', 'add', 'A1', "hello"
                            row '', 'add', 'A1', "world"
                            row '', 'create', '?p1', 'A1'
                            row 2, 'size', 'A4'
                            row '', 'iterator', 'A4'
                            row '', 'next', 'A6'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'ImmutableSet'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['ImmutableSet']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'ImmutableSet'
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

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_BoundedStack_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """BoundedStack {
            BoundedStack(int)
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int
            isFull()->boolean
            isEmpty()->boolean
        }"""
        study(name: 'JSSReuse-BoundedStack-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Stack') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Stack'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'pushPop': sheet(p1: 'BoundedStack', p2: "hello", p3: "world") {
                            row '', 'create', '?p1', 2
                            row true, 'isEmpty', 'A1'
                            row false, 'isFull', 'A1'
                            row '?p2', 'push', 'A1', '?p2'
                            row '?p3', 'push', 'A1', '?p3'
                            row true, 'isFull', 'A1'
                            row false, 'isEmpty', 'A1'
                            row '?p3', 'peek', 'A1'
                            row 2, 'size', 'A1'
                            row '?p3', 'pop', 'A1'
                            row 1, 'size', 'A1'
                            row false, 'isFull', 'A1'
                            row '?p2', 'pop', 'A1'
                            row true, 'isEmpty', 'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Stack'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Stack']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Stack'
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

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    /**
     * Add second version with superclass java.util.Collection?
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_PriorityQueue_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """PriorityQueue {
            insert(java.lang.Object)->void
            peek()->java.lang.Object
            pull()->java.lang.Object
            size()->int
            isEmpty()->boolean
        }"""
        study(name: 'JSSReuse-Queue-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Queue') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Queue'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'enqueueDequeue': sheet(p1: 'PriorityQueue', p2: 5, p3: 10, p4: 1) {
                            row '', 'create', '?p1'
                            row '', 'insert', 'A1', '?p2'
                            row '', 'insert', 'A1', '?p3'
                            row '', 'insert', 'A1', '?p4'
                            row 3, 'size', 'A1'
                            row '?p4', 'peek', 'A1'
                            row 3, 'size', 'A1'
                            row '?p4', 'pull', 'A1'
                            row 2, 'size', 'A1'
                            row '?p2', 'pull', 'A1'
                            row 1, 'size', 'A1'
                            row '?p3', 'pull', 'A1'
                            row true, 'isEmpty', 'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Queue'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Queue']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Queue'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    /**
     * Add second version with superclass java.util.Collection?
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_Queue_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Queue {
            enqueue(java.lang.Object)->void
            dequeue()->java.lang.Object
            size()->int
        }"""
        study(name: 'JSSReuse-Queue-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Queue') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Queue'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'enqueueDequeue': sheet(p1: 'Queue', p2: "hello", p3: "world") {
                            row '', 'create', '?p1'
                            row '', 'enqueue', 'A1', '?p2'
                            row '', 'enqueue', 'A1', '?p3'
                            row 2, 'size', 'A1'
                            row '?p2', 'dequeue', 'A1'
                            row 1, 'size', 'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Queue'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Queue']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Queue'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    /**
     * Add second version with superclass java.util.Collection?
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    // FIXME better call Deque
    @Test
    void test_DoubleEndedQueue_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """DoubleEndedQueue {
            push(java.lang.Object)->void
            enqueue(java.lang.Object)->void
            pop()->java.lang.Object
            dequeue()->java.lang.Object
            last()->java.lang.Object
            first()->java.lang.Object
            size()->int
        }"""
        study(name: 'JSSReuse-DoubleEndedQueue-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('DoubleEndedQueue') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'DoubleEndedQueue'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'enqueueDequeue': sheet(p1: 'DoubleEndedQueue', p2: "hello", p3: "world", p4: "se") {
                            row '', 'create', '?p1'
                            row '', 'enqueue', 'A1', '?p2'
                            row '', 'enqueue', 'A1', '?p3'
                            row 2, 'size', 'A1'
                            row '?p2', 'first', 'A1'
                            row 2, 'size', 'A1'
                            row '?p2', 'dequeue', 'A1'
                            row 1, 'size', 'A1'
                            row '', 'push', 'A1', '?p4'
                            row 2, 'size', 'A1'
                            row '?p4', 'last', 'A1'
                            row 2, 'size', 'A1'
                            row '?p4', 'pop', 'A1'
                            row 1, 'size', 'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'DoubleEndedQueue'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['DoubleEndedQueue']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'DoubleEndedQueue'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_StringComparator_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """StringComparator {
            compare(java.lang.String,java.lang.String)->int}"""
        study(name: 'JSSReuse-StringComparator-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('StringComparator') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'StringComparator'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testCompare1': sheet(p1: 'StringComparator') {
                            row '', 'create', '?p1', ''
                            row '', 'compare', 'A1', 'hello', 'world'
                        },
                        'testCompare2': sheet(p1: 'StringComparator') {
                            row '', 'create', '?p1', ''
                            row '', 'compare', 'A1', 'engineering', 'software'
                        },
                        'testCompare3': sheet(p1: 'StringComparator') {
                            row '', 'create', '?p1', ''
                            row '', 'compare', 'A1', 'jane', 'jane'
                        },
                        'testCompare4': sheet(p1: 'StringComparator') {
                            row '', 'create', '?p1', ''
                            row '', 'compare', 'A1', 'b', 'a'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'StringComparator'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['StringComparator']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'StringComparator'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_fromJSON_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Json {
            fromJson(java.lang.String)->java.util.Map}"""
        study(name: 'JSSReuse-FromJson-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Json') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Json'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testFromJson': sheet(p1: 'Json') {
                            row '', 'create', '?p1'
                            row '', 'fromJson', 'A1', '{"name" : "Jane Doe"}'
                            row '', 'get', 'A2', 'name'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Json'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Json']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Json'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_toJSON_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Json {
            toJson(java.util.Map)->java.lang.String}"""
        study(name: 'JSSReuse-ToJson-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Json') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Json'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testToJson': sheet(p1: 'Json') {
                            row '', 'create', '?p1'
                            row '', 'create', 'java.util.HashMap'
                            row '', 'put', 'A2', 'name', 'Jane Doe'
                            row '', 'toJson', 'A1', 'A2'
                            //row '', 'trim', 'A4' // use replaceAll to remove space, tabs, newlines etc.
                            //row '', 'length', 'A5'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Json'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Json']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Json'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_Base64encode_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Base64{encode(byte[])->byte[]}"""
        study(name: 'JSSReuse-Base64encode-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Base64') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Base64'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testEncode': sheet(base64:'Base64', p2:"user:pass".getBytes()) {
                            row  '',    'create', '?base64'
                            row 'dXNlcjpwYXNz'.getBytes(),  'encode',   'A1',     '?p2'
                        },
                        'testEncode_padding': sheet(base64:'Base64', p2:"Hello World".getBytes()) {
                            row  '',    'create', '?base64'
                            row 'SGVsbG8gV29ybGQ='.getBytes(),  'encode',   'A1',     '?p2'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Base64'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Base64']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Base64'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_Base64decode_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Base64{decode(byte[])->byte[]}"""
        study(name: 'JSSReuse-Base64decode-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Base64') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Base64'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testDecode': sheet(base64:'Base64', p2:'dXNlcjpwYXNz'.getBytes()) {
                            row  '',    'create', '?base64'
                            row "user:pass".getBytes(),  'decode',   'A1',     '?p2'
                        },
                        'testDecode_padding': sheet(base64:'Base64', p2:'SGVsbG8gV29ybGQ='.getBytes()) {
                            row  '',    'create', '?base64'
                            row "Hello World".getBytes(),  'decode',   'A1',     '?p2'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Base64'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Base64']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Base64'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    // TODO add,divide,sub,multiply,reciprocal?
    @Test
    void test_Fraction_simplify_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Fraction {
            Fraction(int,int)
            asDouble()->double
            getDenominator()->int
            getNumerator()->int
            simplify()->Fraction
        }"""
        study(name: 'JSSReuse-Fraction-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Fraction') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Fraction'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testFraction': sheet(base64:'Fraction', num:2, denom:4) {
                            row  '',    'create', '?base64' , '?num', '?denom'
                            row '?num',  'getNumerator',   'A1'
                            row '?denom',  'getDenominator',   'A1'
                            row '',  'simplify',   'A1'
                            row 1,  'getNumerator',   'A4'
                            row 2,  'getDenominator',   'A4'
                            row 0.5d,  'asDouble',   'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Fraction'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Fraction']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Fraction'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    // FIXME add simplify?
    @Test
    void test_Fraction_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Fraction {
            Fraction(int,int)
            asDouble()->double
            getDenominator()->int
            getNumerator()->int
        }"""
        study(name: 'JSSReuse-Fraction-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Fraction') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Fraction'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testFraction': sheet(base64:'Fraction', num:2, denom:4) {
                            row  '',    'create', '?base64' , '?num', '?denom'
                            row '?num',  'getNumerator',   'A1'
                            row '?denom',  'getDenominator',   'A1'
                            row 0.5d,  'asDouble',   'A1'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Fraction'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Fraction']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Fraction'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_Hash_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Sha256 {
            hash(byte[])->java.lang.String
        }"""
        study(name: 'JSSReuse-Sha256-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Sha256') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Sha256'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testHash': sheet(base64:'Sha256', p2:'hello world'.getBytes()) {
                            row  '',    'create', '?base64'
                            row "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",  'hash',   'A1',     '?p2'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Sha256'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Sha256']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Sha256'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    // change constructor?
    @Test
    void test_Matrix_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Matrix {
            Matrix(double[][],int,int)
            set(int,int,double)->void
            get(int, int)->double
            add(Matrix)->Matrix
            transpose()->Matrix
        }"""
        study(name: 'JSSReuse-Matrix-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Matrix') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Matrix'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testMatrix': sheet(base64:'Matrix') {
                            row  '',    'create', '?base64' , new double[][]{new double[]{1d,1d}, new double[]{1d,1d}}, 2, 2
                            row 1,  'get', 'A1',  0, 0
                            row '',  'set', 'A1',  0, 0, 5d
                            row '', 'create', '?base64' , new double[][]{new double[]{2d,2d}, new double[]{2d,2d}}, 2,2
                            row '',  'add',   'A1', 'A4'
                            row '', 'transpose', 'A5'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Matrix'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Matrix']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Matrix'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_Statistics_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Statistics {
            min(double[])->double
            max(double[])->double
            median(double[])->double
            mean(double[])->double
            harmonicMean(double[])->double
            geometricMean(double[])->double
        }"""
        study(name: 'JSSReuse-Statistics-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Statistics') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Statistics'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testStats': sheet(stats:'Statistics', N:new double[]{3d,1d,2d}) {
                            row  '',    'create', '?stats'
                            row 1d,  'min', 'A1',  '?N'
                            row 3d,  'max', 'A1',  '?N'
                            row 2d,  'median', 'A1', '?N'
                            row 2d,  'mean', 'A1',  '?N'
                            row 1.6363636363636d,  'harmonicMean', 'A1', '?N'
                            row 1.8171205928321d,  'geometricMean', 'A1', '?N'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Statistics'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Statistics']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Statistics'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }

    @Test
    void test_HTMLSanitizer_plain() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        
        def totalRows = 10
        def minimumFunctionalSimilarity = 0.0
        def noOfAdapters = 10
        // interface in LQL notation
        def interfaceSpec = """Html {
            sanitize(java.lang.String)->java.lang.String
        }"""
        study(name: 'JSSReuse-Html-Plain') {
            action(name: 'select', type: 'Select') {
                abstraction('Html') { // interface-driven code search
                    queryForClasses interfaceSpec
                    rows = totalRows
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we do not want it to be a collection
                    //excludeSuperClass("java.util.Collection")
                    // non empty classes, i.e having complexity > 1
                    //filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
                }
            }
        
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = true // remove clone implementations
        
                dependsOn "select"
                includeAbstractions 'Html'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                containerTimeout = 10 * 60 * 1000L // 10 minutes
                specification = interfaceSpec
                sequences = [
                        // parameterised sheet (SSN) with default input parameter values
                        // expected values are given in first row (oracle)
                        'testJs': sheet(html:'Html', h:"abc <script>alert(1)</script> def") {
                            row  '',    'create', '?html'
                            row '',  'sanitize', 'A1',  '?h'
                        }
                ]
                features = ['cc'] // enable code coverage measurement (class scope)
                maxAdaptations = noOfAdapters // how many adaptations to try
        
                dependsOn 'clones'
                includeAbstractions 'Html'
                profile('myTdsProfile') {
                    scope('class') { type = 'class' }
                    environment('java11') {
                        image = 'maven:3.6.3-openjdk-11' // change
                    }
                }
        
                whenAbstractionsReady() {
                    def stack = abstractions['Html']
                    def stackSrm = srm(abstraction: stack)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: stack)
                            .systems // select all systems
                            .similarTo(expectedBehaviour, minimumFunctionalSimilarity) // functionally similar

                    // continue pipeline with matched systems only
                    stack.systems = matchesSrm.systems
                }
            }
        
            action(name:'rank', type:'Rank') {
                bestMatch = true
                strategy = 'HDS_SMOOP' // SOCORA
                criteria = ['FunctionalSimilarityReport.score:MAX:1']
        
                dependsOn 'filter'
                includeAbstractions 'Html'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId, "execute")
        System.out.println(df)
    }
}
