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
 * Demonstrates Randoop action
 *
 * @author mkessel
 */
class RandoopSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_generate_class_stack() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Randoop') {
            action(name:'select', type:'Select') {
                abstraction('Stack') { // assume known CUT
                    queryForClasses '*:*'
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"' // known class
                }
            }
            
            /** Randoop */
            action(name:'randoop',type:'Randoop') {
                // configuration
        
                dependsOn 'select' // mandatory
                includeAbstractions 'Stack'
                
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8'
                    }
                }
                
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

        assert table.size() == 1
    }

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
        Table table1 = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from JaCoCoReport where action = 'jacoco' and abstraction = 'Stack'")
        println(table1.printAll())
        Table table2 = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from PitestReport where action = 'pitest' and abstraction = 'Stack'")
        println(table2.printAll())
    }
}
