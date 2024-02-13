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
 * Demonstrates GenerativeAI action
 *
 * @author mkessel
 */
class GenerativeAISystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_OpenAI_Base64() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'gitimport' // the data source for Git imports

        /** Define a new study */
        study(name: 'GenerativeAIDemo-Base64WithoutPadding') {
        
            /** defines profile (compiler etc.) */
            profile('java17Profile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java17') { // execution environment
                    image = 'maven:3.6.3-openjdk-17' // (docker) image template
                }
            }
            
            /** OpenAI (alternatively Gpt4All */
            action(name: 'gai', type: 'GenerativeAI') {
                apiUrl = "https://api.openai.com/v1/chat/completions"
                apiKey = "XXXXX"
                deploy = true // should be set to true
                
                abstraction('Base64Encode') {           
                    // NLP prompt         
                    prompt 'write a java method that encodes a string to base64 without padding and returns a byte array'
                    
                    model = "gpt-3.5-turbo"
                    role = "user"
                    temperature = 0.7
                }
                
                profile('java17Profile') // for building purposes
            }
            
            // filter action
            action(name: 'filter', type: 'ArenaExecute') { // filter by tests
                specification = 'Base64{encode(java.lang.String)->byte[]}'
                sequences = [
                        'testEncode': sheet(base64:'Base64', p2:"user:pass") {
                            row  '',    'create', '?base64'
                            row 'dXNlcjpwYXNz'.getBytes(),  'encode',   'A1',     '?p2'
                        },
                        'testEncode_padding': sheet(base64:'Base64', p2:"Hello World") {
                            row  '',    'create', '?base64'
                            row 'SGVsbG8gV29ybGQ'.getBytes(),  'encode',   'A1',     '?p2'
                        }
                ]
                maxAdaptations = 1 // how many adaptations to try
                features = ["cc"] // enable code coverage
        
                dependsOn 'gai'
                includeAbstractions '*'
                profile('java17Profile')
        
                whenAbstractionsReady() {
                    def base64 = abstractions['Base64Encode']
                    def base64Srm = srm(abstraction: base64)
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(srm(abstraction: base64).sequences)
                    // returns a filtered SRM
                    def matchesSrm = srm(abstraction: base64)
                            .systems // select all systems
                            .equalTo(expectedBehaviour) // functionally equivalent
        
                    // iterate over sub-SRM
                    matchesSrm.systems.each { s ->
                        log("Matched class ${s.id}, ${s.packageName}.${s.name}")
                    }
                    // continue pipeline with matched systems only
                    base64.systems = matchesSrm.systems
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
