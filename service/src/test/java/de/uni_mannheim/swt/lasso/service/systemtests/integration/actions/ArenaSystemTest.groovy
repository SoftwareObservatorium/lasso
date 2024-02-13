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
 * Demonstrates Arena action
 *
 * @author mkessel
 */
class ArenaSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    static {
        System.setProperty("models.embedding.code2vec", "/home/marcus/Downloads/target_vecs.txt");
    }

    @Test
    void test_EXECUTE_bug() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'mavenCentral2023'

def totalRows = 1
def minimumFunctionalSimilarity = 0.0
def noOfAdapters = 1
// interface in LQL notation
def interfaceSpec = """TreeNode{
    setName(java.lang.String)->void
    addChild(TreeNode)->void
    getChildren()->java.util.List
}"""
study(name: 'test-JSSReuse-TreeNode') {
    action(name: 'select', type: 'Select') {
        abstraction('TreeNode') { // interface-driven code search
            queryForClasses interfaceSpec, 'class-simple'
            rows = totalRows
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }

    action(name: "clones", type: 'Nicad6') {
        cloneType = "type2" // clone type to reject
        collapseClones = true // remove clone implementations

        dependsOn "select"
        includeAbstractions 'TreeNode'
        
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
                'testChildren': sheet(cut:'TreeNode', n1: 'node1', n2: 'node2') {
                    row  '',    'create', '?cut'
                    row  '',    'setName', 'A1', '?n1'
                    row  '',    'create', '?cut'
                    row  '',    'setName', 'A3', '?n2'
                    row  '',    'addChild', 'A1', 'A3'
                    row  '',    'getChildren', 'A1'
                    row  1,    'size', 'A6'
                    row  '',    'getChildren', 'A3'
                    row  1,    'size', 'A8'
                }
        ]
        features = ['cc'] // enable code coverage measurement (class scope)
        maxAdaptations = noOfAdapters // how many adaptations to try

        dependsOn 'clones'
        includeAbstractions 'TreeNode'
        profile('myTdsProfile') {
            scope('class') { type = 'class' }
            environment('java11') {
                image = 'maven:3.6.3-openjdk-17' // change
            }
        }

        whenAbstractionsReady() {
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

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }


    /**
     * Arena EXECUTE Sequence task (resembles a test-driven search based on a given sheet)
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_EXECUTE_class_stack_by_sheet() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        // interface of a Stack in LQL notation
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int}"""
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses '*:*', 'concept'
                    rows = 1
                    
                    directly = true // without cursor functionality
                    
                    // pick a known stack
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"'
                }
            }
            
            action(name:'execute',type:'Arena') { // execute
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
        
                maxPermutations = 1
                task = 'Execute'
                features = ['mutation', 'cc'] // enable mutation and code coverage
        
                exportCsv = true
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                includeTests '*'
                profile {
                    environment('java17') {
                        image = 'maven:3.6.3-openjdk-17'
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    /**
     * Arena EXECUTE Sequence task (based on given JUnit class)
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_EXECUTE_class_stack_by_testclass() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        def stackMap = [:]
        stackMap.put('6ce338e3-3c3c-4f52-b595-9b3ed5bb4025', """
        package edu.mines.jtk.sgl;
        import org.junit.Test;
        import static org.junit.Assert.*;
        
        public class StackTest {
            @Test
            public void test() throws Throwable {
                ArrayStack<String> stack = new ArrayStack<String>();
                String input = "hi!";
                String str2 = stack.push(input);
                String str3 = stack.peek();
                int int4 = stack.size();
                String str5 = stack.pop();
                int int6 = stack.size();
                assertEquals(str2, "hi!");
                assertEquals(str3, "hi!");
                assertTrue(int4 == 1);
                assertEquals(str5, "hi!");
                assertTrue(int6 == 0);
            }
        }
        """
        )
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses '*:*', 'concept'
                    rows = 1
                    
                    directly = true // without cursor functionality
                    
                    // pick a known stack
                    filter 'id:"6ce338e3-3c3c-4f52-b595-9b3ed5bb4025"'
                }
            }
            
            action(name:'execute',type:'Arena') { // execute
                testClasses = stackMap
        
                maxPermutations = 1
                task = 'Execute'
        
                exportCsv = true
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                includeTests '*'
                profile {
                    environment('java8') {
                        image = 'openjdk:8-jdk-alpine'
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_AMPLIFY_evosuite() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses '*:*', 'concept'
                    rows = 2
                    
                    directly = true // without cursor functionality

                    // pick two known stacks
                    filter 'id:("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025" OR "53ed531e-58bc-4520-8580-1e5c600cb28a")'
                }
            }
            
            /** EvoSuite */
            action(name:'evosuite',type:'EvosuiteGenerateClass') {
                ignoreMissingReport = true
                searchBudget = 30
                stoppingCondition = "MaxTime"
                criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"
        
                dependsOn 'select' // mandatory
                includeAbstractions 'Stack'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
            }
            
            action(name:'amplify',type:'Arena') { // amplify
                disablePartitioning = true // create new action where this is enabled by default, inject into setter/getter instead of fields to make reuse possible (extension points)

                maxPermutations = 1
                task = 'Amplify'
        
                exportCsv = true // tbr
        
                dependsOn 'evosuite'
                includeAbstractions 'Stack'
                includeTests '*' // add sequence specifications from EvoSuite
                profile {
                    environment('java8') {
                        image = 'openjdk:8-jdk-alpine'
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 2)
        verifyAbstraction(lslExecutionContext, 'evosuite', 'Stack', 2)
        verifyAbstraction(lslExecutionContext, 'amplify', 'Stack', 2)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }
}
