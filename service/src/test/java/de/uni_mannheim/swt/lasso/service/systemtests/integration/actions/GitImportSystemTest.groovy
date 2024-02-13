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
 * Demonstrates GitImport action
 *
 * @author mkessel
 */
class GitImportSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * Just package system artifact.
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_import() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'gitimport' // the data source for Git imports
        /** Define a new study */
        study(name: 'GitImporterDemo') {
        
            /** defines profile (compiler etc.) */
            profile('java17Profile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java17') { // execution environment
                    image = 'maven:3.6.3-openjdk-17' // (docker) image template
                }
            }
            
            /** Import project(s) from Git repositories */
            action(name: 'import', type: 'GitImport') {
                repositories = [
                        'student1': 'https://github.com/apache/commons-lang.git',
                ]
                deploy = true // should be set to true
                
                profile('java17Profile') // Maven image has git onboard
            }
            
            /** Let's do a keyword search */
            action(name: 'select', type: 'Select') {
                abstraction('student1') {
                    queryForClasses 'StringUtils'
                    rows = 10

                    // make sure to select only from the imported repos above
                    filter 'executionId:"' + lasso.executionId + '"' // current execution id
                    filter 'action:"import"' // unique action (see above)
                    //filter 'owner:"student1"' // optional
                    
                    // others ..
                    //filter 'interface:my.pkg.StackInterface'
                }
            }
            
            // filter action
            
            action(name: 'debug', type: 'Debug') {
                dependsOn 'select'
                includeAbstractions 'student1'
                
                execute {
                    log('hello world')
                    int size = abstractions['student1'].systems.size()
                    log("no of systems. ${size}")
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
        //verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
    }

    /**
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_import_stack() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'gitimport' // the data source for Git imports
        
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int}"""
        /** Define a new study */
        study(name: 'GitImporterDemo-Stack') {
        
            /** defines profile (compiler etc.) */
            profile('java17Profile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java17') { // execution environment
                    image = 'maven:3.6.3-openjdk-17' // (docker) image template
                }
            }
            
            /** Import project(s) from Git repositories */
            action(name: 'import', type: 'GitImport') {
                repositories = [
                        'student1': 'https://github.com/alto1012/stack.git',
                ]
                deploy = true // should be set to true
                
                profile('java17Profile') // Maven image has git onboard
            }
            
            /** Let's do a keyword search */
            action(name: 'select', type: 'Select') {
                abstraction('student1') {
                    queryForClasses interfaceSpec
                    rows = 1

                    // make sure to select only from the imported repos above
                    filter 'executionId:"' + lasso.executionId + '"' // current execution id
                    filter 'action:"import"' // unique action (see above)
                    //filter 'owner:"student1"' // optional
                    
                    // others ..
                    //filter 'interface:my.pkg.StackInterface'
                }
            }
            
            action(name: 'debug', type: 'Debug') {
                dependsOn 'select'
                includeAbstractions 'student1'
                
                execute {
                    log('hello world')
                    int size = abstractions['student1'].systems.size()
                    log("no of systems. ${size}")
                }
            }
            
            // filter action
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                specification = interfaceSpec // FIXME was missing
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
                features = ["cc"] // enable code coverage
        
                dependsOn 'select'
                includeAbstractions '*'
                profile('java17Profile')
        
                whenAbstractionsReady() {
                    def stack = abstractions['student1']
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
        //verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }
}
