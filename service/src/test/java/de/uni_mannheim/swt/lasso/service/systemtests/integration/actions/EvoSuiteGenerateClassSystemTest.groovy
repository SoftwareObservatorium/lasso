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
import de.uni_mannheim.swt.lasso.engine.data.ReportKey
import de.uni_mannheim.swt.lasso.service.systemtests.integration.AbstractGroovySystemTest
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Demonstrates EvosuiteGenerateClass action
 *
 * @author mkessel
 */
class EvoSuiteGenerateClassSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_generate_class_stack() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
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
            
            /** EvoSuite */
            action(name:'evosuite',type:'EvosuiteGenerateClass') {
                // configuration
                //ignoreMissingReport = true
                searchBudget = 30 // we need this as upper bound for timeouts
                stoppingCondition = "MaxTime"
                criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"
        
                dependsOn 'select' // mandatory
                includeAbstractions 'Stack'
                includeImplementations {abName -> // under test
                    // custom pick of single impl.
                    abstractions[abName].implementations?.findAll { impl -> impl.id == '53ed531e-58bc-4520-8580-1e5c600cb28a'}
                }
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
                
                whenAbstractionsReady {
                    def stack = abstractions['Stack']
                    // get table and do something
                    // either use SQL for aggregation or use the Table structure
                    Table table = queryReport("select * from EvosuiteGenerateClass where action = '${getName()}' and abstraction = '${stack.name}'")
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)
        verifyAbstraction(lslExecutionContext, 'evosuite', 'Stack', 1)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from EvosuiteGenerateClass where action = 'evosuite' and abstraction = 'Stack'")
        println(table.printAll())

        // FIXME test EvosuiteExecutionReport

        assert table.size() == 1
    }

    /**
     * Tests "kill" of evosuite client processes
     *
     * <pre>
     *     timeoutClientProcess
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_generate_class_stack_TIMEOUT() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
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
            
            /** EvoSuite */
            action(name:'evosuite',type:'EvosuiteGenerateClass') {
                // configuration
                //ignoreMissingReport = true
                searchBudget = 30 // we need this as upper bound for timeouts
                stoppingCondition = "MaxTime"
                criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"
                
                timeoutClientProcess = 15 // UNDER TEST!
        
                dependsOn 'select' // mandatory
                includeAbstractions 'Stack'
                includeImplementations {abName -> // under test
                    // custom pick of single impl.
                    abstractions[abName].implementations?.findAll { impl -> impl.id == '53ed531e-58bc-4520-8580-1e5c600cb28a'}
                }
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
                
                whenAbstractionsReady {
                    def stack = abstractions['Stack']
                    // get table and do something
                    // either use SQL for aggregation or use the Table structure
                    Table table = queryReport("select * from EvosuiteGenerateClass where action = '${getName()}' and abstraction = '${stack.name}'")
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)
        verifyAbstraction(lslExecutionContext, 'evosuite', 'Stack', 0)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from EvosuiteGenerateClass where action = 'evosuite' and abstraction = 'Stack'")
        println(table.printAll())

        // FIXME test EvosuiteExecutionReport

        assert table.size() == 0
    }
}
