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
package de.uni_mannheim.swt.lasso.service.systemtests.integration

import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Demonstrates LSL language features
 *
 * @author mkessel
 */
class LSLLanguageSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * <pre>
     *     dependsOn 'one', 'two', ...
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_dependsOn_many_actions() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select1', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
            
            /** Retrieve class implementations */
            action(name: 'select2', type: 'Select') {
                abstraction('List') {
                    queryForClasses 'List', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
            
            action(name: 'doSomething') { // plain action
                dependsOn 'select1', 'select2' // depends on two former actions
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
        verifyAbstraction(lslExecutionContext, 'doSomething', 'Stack', 10)
        verifyAbstraction(lslExecutionContext, 'doSomething', 'List', 10)
    }

    /**
     * <pre>
     *     dependsOn 'executionId:one'
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_dependsOn_external_action() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content1 = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select1', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest1 = createScript(content1)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult1 = lassoEngine.execute(scriptUnderTest1);
        LSLExecutionContext lslExecutionContext1 = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext1, 'select1', 'Stack', 10)

        // second study which uses FA of first study

        @Language("Groovy")
        String content2 = '''
        dataSource 'mavenCentral2020' // define data source to use
        study(name: 'Something') {
            action(name: 'doSomething') { // plain action
                dependsOn 'executionId.select1'
                includeAbstractions '*'
            }
        }
        '''
        // executionId:select1
        content2 = content2.replace("executionId.select1", scriptUnderTest1.getExecutionId() + ":select1");

        //
        LSLScript scriptUnderTest2 = createScript(content2)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult2 = lassoEngine.execute(scriptUnderTest2);
        LSLExecutionContext lslExecutionContext2 = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext2, 'doSomething', 'Stack', 10)
    }

    /**
     * <pre>
     *     includeAbstractions '*'
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_includeAbstractions() throws IOException, DataSourceNotFoundException {
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
                abstraction('Stack1') {
                    queryForClasses interfaceSpec // query for classes by interface specification
                    rows = 1
                    
                    directly = true // without cursor functionality

                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we don't want it to be a collection
                    //excludeSuperClass("java.util.Collection", true)
                    // non empty classes, i.e having complexity > 1
                    filter 'm_static_complexity_td:[1 TO *]'
                }
                
                abstraction('Stack2') {
                    queryForClasses interfaceSpec // query for classes by interface specification
                    rows = 1
                    
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
            
            action(name: 'debug', type: 'Debug') {
                dependsOn 'select'
                includeAbstractions 'Stack1'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext, 'debug', 'Stack1', 1)

        // does not contain the other FA
        assert !lslExecutionContext.lassoContext.
                actionContainerSpec.actions['debug'].abstractions.containsKey('Stack2')
    }

    /**
     * <pre>
     *     includeImplementations {abName -> ...}
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_includeImplementations() throws IOException, DataSourceNotFoundException {
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
            
            action(name: 'debug', type: 'Debug') {
                dependsOn 'select'
                includeAbstractions 'Stack'
                includeImplementations {abName -> // under test
                    // custom pick of single impl.
                    log("abstraction ${abName}")
                    log("actions ${actions.keySet()}")
                    abstractions[abName].implementations?.findAll { impl -> impl.id == '53ed531e-58bc-4520-8580-1e5c600cb28a'}
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
        verifyAbstraction(lslExecutionContext, 'debug', 'Stack', 1)

        // verify in-memory model of select action
        AbstractionSpec abstractionSpec = lslExecutionContext.lassoContext.
                actionContainerSpec.actions['debug'].abstractions['Stack']
        assert abstractionSpec.implementations[0].id == '53ed531e-58bc-4520-8580-1e5c600cb28a'
    }

    /**
     * Alternative, dynamic version to "static" configuration in the header of an action
     *
     * <pre>
     *     configure {...}
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_configure() throws IOException, DataSourceNotFoundException {
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
                configure {
                    log('we do some dynamic execution block as part of configuration')
                    dataSource = 'mavenCentral2020'
                }
            
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

    /**
     * <pre>
     *     execute {...}
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_execute() throws IOException, DataSourceNotFoundException {
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
            
            action(name: 'debug', type: 'Debug') {
                dependsOn 'select'
                includeAbstractions 'Stack'
                
                execute {
                    log('hello world')
                    int implsSize = abstractions['Stack'].implementations.size()
                    log("no of impls. ${implsSize}")
                    
                    int actionSize = actions.size()
                    log("no of actions ${actionSize}")
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
        verifyAbstraction(lslExecutionContext, 'debug', 'Stack', 10)
    }

    /**
     * <pre>
     *     whenAbstractionsReady {...}
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_whenAbstractionsReady() throws IOException, DataSourceNotFoundException {
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
                
                whenAbstractionsReady {
                    // remove 5 implementations from the tail of the list
                    abstractions['Stack'].implementations = abstractions['Stack'].implementations.dropRight(5)
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 5)
    }

    /**
     * When no action type is specified, delegate to NoOp
     *
     * <pre>
     *     action(name:'myAction') {...}
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_plain_action_noop() throws IOException, DataSourceNotFoundException {
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
            
            action(name: 'myAction') { // plain action
                dependsOn 'select'
                includeAbstractions 'Stack'
                
                execute {
                    log('hello world')
                    int implsSize = abstractions['Stack'].implementations.size()
                    log("no of impls. ${implsSize}")
                    
                    int actionSize = actions.size()
                    log("no of actions ${actionSize}")
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
        verifyAbstraction(lslExecutionContext, 'myAction', 'Stack', 10)
    }

    /**
     * When no action type is specified, delegate to NoOp.
     *
     * Use case of merging abstractions into a new one.
     *
     * <pre>
     *     action(name:'myAction') {...}
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_plain_action_merge_new_abstraction() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select1', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
            
            /** Retrieve class implementations */
            action(name: 'select2', type: 'Select') {
                abstraction('List') {
                    queryForClasses 'List', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
            
            action(name: 'merge') { // plain action
                dependsOn 'select2'
                includeAbstractions '*'
                
                execute {
                    // merge two abstractions into new as DSL command
                    def merged = abstraction('Collection',
                        [
                            actions['select1'].abstractions['Stack'], 
                            actions['select2'].abstractions['List']
                        ]
                    )
                    
                    log("abstraction ${merged.name} has ${merged.implementations.size()} implementations")
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
        verifyAbstraction(lslExecutionContext, 'merge', 'Collection', 20)
        // others untouched
        //verifyAbstraction(lslExecutionContext, 'merge', 'Stack', 10)
        verifyAbstraction(lslExecutionContext, 'merge', 'List', 10)
    }
}
