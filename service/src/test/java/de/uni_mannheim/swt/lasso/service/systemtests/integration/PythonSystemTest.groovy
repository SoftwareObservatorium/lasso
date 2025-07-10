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
import de.uni_mannheim.swt.lasso.srm.olap.Warehouse
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

class PythonSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    @Test
    void test_base64_function_fromImplementation() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource "mavenCentral2023" // default dataSource
study(name: 'Python') {

    profile('pythonProfile') {
        scope('class') { type = 'class' }
        environment('python-arena') {
            image = 'swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/arena-python:latest' // arena-python
        }
    }
      
    action(name: 'createStimulusMatrix') {
        execute {
            stimulusMatrix('myAb', """Base64 {
    encode(str)->str
}""", [pyImplementationFromSource("1", "SomeName", """
import base64

def encode(string):
    # Convert the string to bytes
    string_bytes = string.encode('utf-8')
    
    # Encode the bytes into Base64
    base64_encoded = base64.b64encode(string_bytes)
    
    # Decode the bytes back into a string
    base64_string = base64_encoded.decode('utf-8')
    
    return base64_string

""")], [
  test(name: 'testEncode()') {
    row '', 'create', 'Base64'
    row '', 'encode', 'A1', '"Hello World!"'
  }
])
        }
    }

    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'createStimulusMatrix'
        include '*'
        profile('pythonProfile')
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
    void test_base64_function_GAI() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Python') {

    profile('arenaPythonProfile') {
        scope('class') { type = 'class' }
        environment('python-arena') {
            image = 'swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/arena-python:latest' // arena-python
        }
    }
    
    profile('analyzerPythonProfile') {
        scope('class') { type = 'class' }
        environment('python-analyzer') {
            image = 'swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/analyzer-python:latest' // arena-analyzer
        }
    }
    
    action(name: 'createStimulusMatrix') {
        execute {
            stimulusMatrix('myAb', """Base64 {
    encode(str)->str
}""", [], [
  test(name: 'testEncode()') {
    row '', 'create', 'Base64'
    row '', 'encode', 'A1', '"Hello World!"'
  }
])
        }
    }

    action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'createStimulusMatrix'
        include '*'
        profile('analyzerPythonProfile')

        // action configuration block
        servers = ["http://localhost:11434"]
        model = "llama3.1:latest"
        samples = 1
        
        // lang to python
        lang = "python"

        // custom DSL command offered by the action (for each stimulus matrix, create one prompt to obtain impls)
        prompt { stimulusMatrix ->
            // can by for any prompts: FA, impls, models etc.
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a python function with the following signature: ```${stimulusMatrix.lql}```. Only output the python code and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }
    }
    
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'generateCodeLlama'
        include '*'
        profile('arenaPythonProfile')
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

    // NOTE: works only if lasso_quickstart already contains candidates ...
    @Test
    void test_base64_function_SEARCH() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Python') {

    profile('arenaPythonProfile') {
        scope('class') { type = 'class' }
        environment('python-arena') {
            image = 'swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/arena-python:latest' // arena-python
        }
    }
    
    action(name: 'createStimulusMatrix') {
        execute {
            stimulusMatrix('myAb', """Base64 {
    encode(str)->str
}""", [], [
  test(name: 'testEncode()') {
    row '', 'create', 'Base64'
    row '', 'encode', 'A1', '"Hello World!"'
  }
])
        }
    }
    
    /* select class candidates using interface-driven code search */
    action(name: 'search', type: 'Search') {
        dependsOn 'createStimulusMatrix'
        include '*'

        query { stimulusMatrix ->
            def query = [:] // create query model
            query.queryContent = stimulusMatrix.lql
            query.rows = 10
            // lang to python
            query.lang = "python"
            return [query] // list of queries is expected
        }
    }
    
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        maxAdaptations = 1 // how many adaptations to try
        
        features = ['cc'] // enable code coverage measurement

        dependsOn 'search'
        include '*'
        profile('arenaPythonProfile')
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
