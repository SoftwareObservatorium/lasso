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

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine
import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
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

class JavaSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    @Test
    void test_base64_method_fromImplementation() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Base64encodedecode') {

    action(name: 'create') {
        execute {
            // from known maven artifact (assuming maven repository is able to provide the artifact)
            stimulusMatrix('Base64', """Base64{
                    encode(byte[])->byte[]
                    decode(java.lang.String)->byte[]
                }
                """, [
                    implementation("1", "org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15"),
            ], [ // tests
                 test(name: 'testEncode()') {
                     row '', 'create', 'Base64'
                     row '"dXNlcjpwYXNz".getBytes()', 'encode', 'A1', '"user:pass".getBytes()'
                 },
                 test(name: 'testEncode_padding()') {
                     row '', 'create', 'Base64'
                     row '"SGVsbG8gV29ybGQ=".getBytes()', 'encode', 'A1', '"Hello World".getBytes()'
                 }])
        }
    }

    action(name: 'test', type: 'Arena') {
        features = ['cc'] // enable code coverage measurement (class scope)
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'create'
        include 'Base64'
        profile('java17Profile') {
            scope('class') { type = 'class' }
            environment('java17') {
                image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
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
        //verifyAbstraction(lslExecutionContext, 'select', 'Base64', 1)
        //verifyAbstraction(lslExecutionContext, 'execute', 'Base64', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_base64_method_GAI() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
// LSL generated
dataSource 'lasso_quickstart'
def ollamaServers = ["http://localhost:11434"]
study(name: 'GenOllama') {

    // target profile
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }

    action(name: 'createStimulusMatrix') {
        execute {
            stimulusMatrix('myAb', """Base64 {
    encode(byte[])->byte[]
    decode(java.lang.String)->byte[]
}""", [/*impls*/], [
  test(name: 'testEncode()') {
    row '', 'create', 'Base64'
    row '"SGVsbG8gV29ybGQh".getBytes()', 'encode', 'A1', '"Hello World!".getBytes()'
  }
])
        }
    }

    action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'createStimulusMatrix'
        include '*'
        profile('java17Profile')

        // action configuration block
        servers = ollamaServers
        model = "llama3.1:latest"
        samples = 1

        // custom DSL command offered by the action (for each stimulus matrix, create one prompt to obtain impls)
        prompt { stimulusMatrix ->
            // can by for any prompts: FA, impls, models etc.
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }
    }

    action(name: 'execute', type: 'Arena') {
        maxAdaptations = 1 // how many adaptations to try
        features = ['cc']

        dependsOn 'generateCodeLlama'
        include '*'
        profile('java17Profile')
    }
}
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        //verifyAbstraction(lslExecutionContext, 'select', 'Base64', 1)
        //verifyAbstraction(lslExecutionContext, 'execute', 'Base64', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_base64_method_SEARCH() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource "mavenCentral2023" // default dataSource
study(name: 'TDSGenerated') {

    profile('java17Profile') {
    scope('class') { type = 'class' }
    environment('java17') {
        image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
    }
    }
      
    action(name: 'createStimulusMatrix') {
        execute {
            stimulusMatrix('myAb', """Base64 {
    encode(byte[])->byte[]
    decode(java.lang.String)->byte[]
}""", [/*impls*/], [
  test(name: 'testEncode(p1=byte[], p2=byte[])', p1:'"Hello World!".getBytes()', p2:'"SGVsbG8gV29ybGQh".getBytes()') {
    row '', 'create', 'Base64'
    row '?p2', 'encode', 'A1', '?p1'
  },
  test(name: 'testEncode(p1=byte[], p2=byte[])', p1:'"Hello World".getBytes()', p2:'"SGVsbG8gV29ybGQ=".getBytes()')
])
        }
    }

    /* select class candidates using interface-driven code search */
    action(name: 'select', type: 'Search') {
        dependsOn 'createStimulusMatrix'
        include '*'

        query { stimulusMatrix ->
            def query = [:] // create query model
            query.queryContent = stimulusMatrix.lql
            query.rows = 5

            return [query] // list of queries is expected
        }
    }
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        maxAdaptations = 1 // how many adaptations to try
        features = ['cc']

        dependsOn 'select'
        include '*'
        profile('java17Profile')
    }
}
        '''

        //
        LSLScript scriptUnderTest = createScript(content)


        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        //verifyAbstraction(lslExecutionContext, 'select', 'Base64', 1)
        //verifyAbstraction(lslExecutionContext, 'execute', 'Base64', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }
}
