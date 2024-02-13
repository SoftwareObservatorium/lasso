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
 * Java11 support tests.
 *
 * @author mkessel
 */
class Java11CompatibilitySystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    @Test
    void test_ARENA_PITEST_JACOCO() throws IOException, DataSourceNotFoundException {
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
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
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
    void test_EVOSUITE_JACOCO_PITEST() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'EvoSuite-JaCoCo-Pitest') {
            action(name:'select', type:'Select') {
                abstraction('Stack') { // assume known CUT
                    queryForClasses '*:*'
                    filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"' // known class
                }
            }
            
            profile('myProfile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            /** EvoSuite */
            action(name:'evosuite',type:'EvoSuite') {
                // configuration
                searchBudget = 30
        
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
    
                dependsOn "evosuite"
                includeAbstractions 'Stack'
                includeTests '*' // include any tests
                profile('myProfile')
                
                whenAbstractionsReady {
                    //
                }
            }
            /** Pitest */
            action(name:"pitest",type:'Pitest') {
                dependsOn "evosuite"
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
        verifyAbstraction(lslExecutionContext, 'evosuite', 'Stack', 1)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from EvosuiteGenerateClass where action = 'evosuite' and abstraction = 'Stack'")
        println(table.printAll())
    }

    @Test
    void test_RANDOOP_JACOCO_PITEST() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
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
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
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
