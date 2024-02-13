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
class ReuseSISystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

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
                maxAdaptations = 5 // how many adaptations to try

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

        SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository())
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId)
        System.out.println(df)
    }

    @Test
    void test_StringComparator() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        def interfaceSpec = """StringComparator {
            compare(java.lang.String,java.lang.String)->int}"""
        study(name:'JSS-SI-Reuse-Comparator') {
            action(name:'select', type:'Select') {
                abstraction('ab') {
                    queryForClasses interfaceSpec
                    rows = 10
                    
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    
                    // FIXME with or without
                    filter 'interface_exact:"java/util/Comparator"'
                }
            }
            
            // FIXME cloning are comparators all clones?
            
            profile('java11Profile') {
                scope('class') { type = 'class' }
                environment('java11') {
                    image = 'maven:3.6.3-openjdk-11'
                }
            }
            
            action(name: 'filter', type: 'ArenaExecute') {
                specification = interfaceSpec
                sequences = [
                        'testCompare': sheet(p1: 'StringComparator') {
                            row '', 'create', '?p1', ''
                            row '', 'compare', 'A1', 'hello', 'world'
                        }
                ]
                maxAdaptations = 10 // how many adaptations to try

                dependsOn 'select'
                includeAbstractions 'ab'
                profile('java11Profile')

                whenAbstractionsReady() {
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

//        // assertions
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
        DataFrame df = srmManager.getActuationSheets(lslExecutionResult.executionId)
        System.out.println(df)
    }

    @Test
    void test_fromJSON() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        def interfaceSpec = """Json {
            fromJson(java.lang.String)->java.util.Map}"""
        study(name:'JSS-SI-Reuse-JSONfrom') {
            action(name:'select', type:'Select') {
                abstraction('ab') {
                    queryForClasses interfaceSpec // IDCS
                    rows = 10
                    
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                }
            }
            
            // FIXME cloning
            
            profile('java11Profile') {
                scope('class') { type = 'class' }
                environment('java11') {
                    image = 'maven:3.6.3-openjdk-11'
                }
            }
            
            action(name: 'filter', type: 'ArenaExecute') {
                specification = interfaceSpec
                sequences = [
                        'testFromJson': sheet(p1: 'Json') {
                            row '', 'create', '?p1'
                            row '', 'fromJson', 'A1', '{"name" : "Jane Doe"}'
                            row '', 'get', 'A2', 'name'
                        }
                ]
                maxAdaptations = 1 // how many adaptations to try

                dependsOn 'select'
                includeAbstractions 'ab'
                profile('java11Profile')

                whenAbstractionsReady() {
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

//        // assertions
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
    void test_toJSON() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023'
        def interfaceSpec = """Json {
            toJson(java.util.Map)->java.lang.String}"""
        study(name:'JSS-SI-Reuse-JSONto') {
            action(name:'select', type:'Select') {
                abstraction('ab') {
                    queryForClasses interfaceSpec // IDCS
                    rows = 10
                    
                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                }
            }
            
            // FIXME cloning
            
            profile('java11Profile') {
                scope('class') { type = 'class' }
                environment('java11') {
                    image = 'maven:3.6.3-openjdk-11'
                }
            }
            
            action(name: 'filter', type: 'ArenaExecute') {
                specification = interfaceSpec
                sequences = [
                        'testFromJson': sheet(p1: 'Json') {
                            row '', 'create', '?p1'
                            row '', 'create', 'java.util.HashMap'
                            row '', 'put', 'A2', 'name', 'Jane Doe'
                            row '', 'toJson', 'A1', 'A2'
                            row '', 'trim', 'A4' // use replaceAll to remove space, tabs, newlines etc.
                            row '', 'length', 'A5'
                        }
                ]
                maxAdaptations = 1 // how many adaptations to try

                dependsOn 'select'
                includeAbstractions 'ab'
                profile('java11Profile')

                whenAbstractionsReady() {
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

//        // assertions
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
}
