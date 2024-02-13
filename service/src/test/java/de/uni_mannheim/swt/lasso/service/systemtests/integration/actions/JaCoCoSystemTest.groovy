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

import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.service.systemtests.integration.AbstractGroovySystemTest
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Demonstrates JaCoCo action
 *
 * @author mkessel
 */
class JaCoCoSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * JaCoCo using classic JUnit class
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_junit_class_stack() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
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
            
            action(name:'test',type:'Test') { // we need tests for jacoco 
                testClasses = stackMap
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
                    }
                }
            }
            
            action(name:"jacoco",type:'JaCoCo') {
                minimumTestCoverage = 1d
                generateReport = true
    
                dependsOn "test"
                includeAbstractions 'Stack'
                includeTests '*' // include any tests
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
                    }
                }
                
                whenAbstractionsReady {
                    def stack = abstractions['Stack']
                    // get table and do something
                    // either use SQL for aggregation or use the Table structure
                    Table table = queryReport("select * from ClassLevelJaCoCoReport where action = '${getName()}' and abstraction = '${stack.name}'")
                    log(table?.printAll())
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
        verifyAbstraction(lslExecutionContext, 'jacoco', 'Stack', 1)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from ClassLevelJaCoCoReport where action = 'jacoco' and abstraction = 'Stack'")
        println(table.printAll())

        assert table.size() == 1
    }

    /**
     * JaCoCo using classic JUnit class
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_junit_class_stack_TIMEOUT() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
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
                
                Thread.sleep(10 * 1000L);
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
            
            action(name:'test',type:'Test') { // we need tests for jacoco 
                testClasses = stackMap
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
                    }
                }
            }
            
            action(name:"jacoco",type:'JaCoCo') {
                minimumTestCoverage = 1d
                generateReport = true
                
                timeoutClientProcess = 3 // TIMEOUT
    
                dependsOn "test"
                includeAbstractions 'Stack'
                includeTests '*' // include any tests
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
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
        verifyAbstraction(lslExecutionContext, 'jacoco', 'Stack', 0)
    }

    /**
     * Run JaCoCo in parallel
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_arena_class_stack_PARALLEL() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses '*:*', 'concept'
                    rows = 3
                    
                    directly = true // without cursor functionality

                    // pick two known stacks
                    filter 'id:("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025" OR "4e73bb0d-f01f-43e5-bf46-7ab7870a289f" OR "91a8e57d-cffe-4bd6-b1f0-1859366e8f5e")'
                }
            }
            
            action(name:'execute',type:'Arena') { // execute
                specification = """Stack {
                    push(java.lang.Object)->java.lang.Object
                    pop()->java.lang.Object
                    peek()->java.lang.Object
                    size()->int}"""
                sheets = [
                        sheet1: sheet() {
                            row  '',  'CREATE', 'Stack'
                            row 'hi!',  'push',   'A1',     'hi!'
                            row 'hi!',  'peek',   'A1'
                            row     1,  'size',   'A1'
                            row 'hi!',  'pop',    'A1'
                            row     0,  'size',   'A1'
                        }
                ]
        
                maxPermutations = 1
                task = 'Execute'
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                includeTests '*'
                profile {
                    environment('java8') {
                        image = 'openjdk:8-jdk-alpine'
                    }
                }
            }
            
            action(name:"jacoco",type:'JaCoCo') {
                minimumTestCoverage = 1d
                generateReport = true
    
                dependsOn "execute"
                includeAbstractions 'Stack'
                includeTests '*' // include any tests
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
                    }
                }
                
                whenAbstractionsReady {
                    def stack = abstractions['Stack']
                    // get table and do something
                    // either use SQL for aggregation or use the Table structure
                    Table table = queryReport("select * from ClassLevelJaCoCoReport where action = '${getName()}' and abstraction = '${stack.name}'")
                    log(table?.printAll())
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 3)
        verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 3)
        verifyAbstraction(lslExecutionContext, 'jacoco', 'Stack', 3)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from ClassLevelJaCoCoReport where action = 'jacoco' and abstraction = 'Stack'")
        println(table.printAll())

        assert table.size() == 3
    }
}
