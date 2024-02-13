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

/**
 * Demonstrates Select action
 *
 * @author mkessel
 */
class SelectSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_select_stack_interface_driven_code_search() throws IOException, DataSourceNotFoundException {
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
                    queryForClasses interfaceSpec // query for classes by interface specification
                    rows = 10
                    
                    directly = true // without cursor functionality

                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we don't want it to be a collection
                    //excludeSuperClass("java.util.Collection", true)
                    // non empty classes, i.e having complexity > 1
                    filter 'm_static_complexity_td:[1 TO *]'
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)
    }

    @Test
    void test_select_stack_keyword() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)
    }

    @Test
    void test_select_stack_keyword_execute() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                execute {
                    abstraction('Stack') {
                        queryForClasses 'Stack', 'concept'
                        rows = 10
                        directly = true // without cursor functionality
                    }
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest)
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext()

        // assertions
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)
    }

    @Test
    void test_select_stack_keyword_by_example() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses '*:*', 'concept'
                    rows = 1
                    
                    directly = true // without cursor functionality
                    
                    // pick a known stack
                    filter 'id:"6ce338e3-3c3c-4f52-b595-9b3ed5bb4025"'
                }
            }
            
            action(name: "selectAlt", type: 'Select') {
                dependsOn "select"
                includeAbstractions 'Stack'
        
                execute() {
                    List refImpls = abstractions['Stack'].implementations
                    refImpls.each { impl ->
                        abstraction(impl) { // by example
                            queryByExample impl, 'class', false
                            rows = 1
                            
                            directly = true
        
                            dropPojo = true // drop Pojos
        
                            collapseBy("hash")
                            useAlternatives = false
        
                            // EvoSuite is compatible up to Java 8 (i.e bytecode version 52)
                            filter 'bytecodeversion_i:[* TO 52]' // FIXME
                            // visible classes only
                            // CLASS must be visible
                            filter '-keyword_ss:("private" OR "abstract")' // non abstract
        
                            // not from tests classifier
                            filter '-classifier_s:"tests"'
                            // no JUnit3 tests
                            filter '-dep_exact:"junit/framework/TestCase"'
                            // no JUnit4 tests
                            filter '-dep_exact:"org/junit/Test"'
                            // no JUnit5 tests
                            filter '-dep_exact:"org/junit/jupiter/api/Test"'
                            // no TestNG
                            filter '-dep_exact:"org/testng/annotations/Test"'
                            // no JEE and internal packages
                            filter '-packagename_fq:("java.*" OR "javax.*" OR "com.sun.*" OR "sun.*" OR "org.evosuite.*")'
        
                            // -- rule out trivial classes
                            // minimum number of methods
                            filter 'm_static_methods_td:[1 TO *]'
                            // minimum number of branches
                            filter 'm_static_branch_td:[1 TO *]'
        
                            // ignore impls which break EvoSuite
                            filter '!id:"5bce88a7-b927-422b-8d10-754e418309e5"'
        
                            filter '-groupId:"software.amazon.awssdk"' // large
                        }
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
        verifyAbstraction(lslExecutionContext, 'selectAlt', '6ce338e3-3c3c-4f52-b595-9b3ed5bb4025', 1)
    }
}
