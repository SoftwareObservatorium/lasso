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

class LSL2AndNewArenaSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_EXECUTE_Base64_sheet_search() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Base64encode') {

      profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
          image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
        }
      }
      
    action(name: 'createStimulusMatrix') {
        execute {
            stimulusMatrix('Base64', """Base64{
                    encode(byte[])->byte[]
                    decode(java.lang.String)->byte[]
                }
                """, [/*impls*/], [ // tests
                    test(name: 'testEncode()') {
                        row  '',    'create', 'Base64'
                        row '"dXNlcjpwYXNz".getBytes()',  'encode',   'A1',     '"user:pass".getBytes()'
                },
                test(name: 'testEncode_padding()') {
                    row  '',    'create', 'Base64'
                    row '"SGVsbG8gV29ybGQ=".getBytes()',  'encode',   'A1',     '"Hello World".getBytes()'
                }])
        }
    }

    /* select class candidates using interface-driven code search */
    action(name: 'select', type: 'Search') {
        dependsOn 'createStimulusMatrix'
        include '*'

        query { stimulusMatrix ->
            def query = [:] // create query model
            query.queryContent = "*:*"
            query.rows = 1
            // pick known impl
            query.filters = ['id:"4b824c04-1c9f-434f-907f-194f8b79a344"']
            return [query] // list of queries is expected
        }
    }
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        features = ['cc'] // enable code coverage measurement (class scope)
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'select'
        include 'Base64'
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
    void test_EXECUTE_Base64_manualartifact() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Base64encode') {

      profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
          image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
        }
      }

    action(name: 'select') {
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
                        row  '',    'create', 'Base64'
                        row '"dXNlcjpwYXNz".getBytes()',  'encode',   'A1',     '"user:pass".getBytes()'
                },
                test(name: 'testEncode_padding()') {
                    row  '',    'create', 'Base64'
                    row '"SGVsbG8gV29ybGQ=".getBytes()',  'encode',   'A1',     '"Hello World".getBytes()'
                }])
        }
    }
    
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        features = ['cc'] // enable code coverage measurement (class scope)
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'select'
        include 'Base64'
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
    void test_EXECUTE_Stack_manualjdk() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Stack') {

      profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
          image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
        }
      }

    action(name: 'select') {
        execute {
            // from JDK classes
            stimulusMatrix('Stack', """Stack {
                    push(java.lang.String)->java.lang.String
                    size()->int
                }
                """, 
                [
                    implementation("1", "java.util.Stack"),
                    implementation("2", "java.util.ArrayDeque"),
                    implementation("3", "java.util.LinkedList")
                ], [
                test(name: 'testPush()') {
                    row '',    'create', 'Stack'
                    row '',  'push',   'A1',     '"Hello World!"'
                    row '',  'size',   'A1'
                }])
        }
    }
    
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'select'
        include 'Stack'
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
    void test_EXECUTE_BoundedQueue_manualjdk_MUTATION() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'BoundedQueue') {

      profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
          image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
        }
      }

    action(name: 'select') {
        execute {
            // from JDK classes
            stimulusMatrix('BoundedQueue', """MyBoundedQueue {
                    MyBoundedQueue(int)
                    enQueue(java.lang.Object)->void
                    deQueue()->java.lang.Object
                    isEmpty()->boolean
                    isFull()->boolean
                }
                """,
                [
                    implementation("1", "demo_examples.BoundedQueue")
                ], [
                        test(name: 'testEnqueue()') {
                            row '',    'create', 'MyBoundedQueue', '10'
                            row '',  'enQueue',   'A1',     '"Hello World!"'
                            row '',  'isEmpty',   'A1'
                            row '',  'isFull',   'A1'
                            row '',  'deQueue',   'A1'
                            row '',  'isEmpty',   'A1'
                        }
                ])
        }
    }
    
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'Arena') { // filter by tests
        adapterStrategy = 'PassThroughAdaptationStrategy'

        features = ["mutation"]
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'select'
        include 'BoundedQueue'
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
    void test_Stack_manualjdk() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
// interface in LQL notation
def interfaceSpec = """Stack {
    push(java.lang.String)->java.lang.String
    size()->int
}
"""
study(name: 'Stack') {

      profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
          image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
        }
      }

    action(name: 'select') {
        stimulusMatrix('Stack', interfaceSpec, // abstraction details
                [ // implementations
                    implementation("1", "java.util.Stack"),
                    implementation("2", "java.util.ArrayDeque"),
                    implementation("3", "java.util.LinkedList")
                ], 
                [ // tests
                    test(name: 'testPush()') {
                        row '',  'create', 'Stack'
                        row '',  'push',   'A1',     '"Hi"'
                        row '',  'size',   'A1'
                    },
                    test(name: 'testPushParameterized(p1=java.lang.String)', p1: "Hello World!") {
                        row '',  'create', 'Stack'
                        row '',  'push',   'A1',     '?p1'
                        row '',  'size',   'A1'
                    },
                    test(name: 'testPushParameterized(p1=java.lang.String)', p1: "Bla blub!") // e.g., parameterized
                ]
            )
    }

    action(name: 'typeAware', type: 'TypeAwareMutatorTestGen') { // add more tests
        noOfTests = 1 // create one mutation per test
    
        dependsOn 'select'
        include 'Stack'
    }    

    action(name: 'random', type: 'RandomTestGen') { // add more tests
        noOfTests = 5 // create 5 additional random tests
        shuffleSequence = false
    
        dependsOn 'typeAware'
        include 'Stack'
    }

    action(name: 'filter', type: 'Arena') { // run all tests
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'random'
        include 'Stack'
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

    // Evosuite needs to be deployed in nexus
    @Test
    void test_benchmark_dgai() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Evosuite-Models') {

    // profile for execution
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }
    
    // profile for execution
    profile('java11Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.6.3-openjdk-11' // EvoSuite won't run in > JDK 11
        }
    }

    // load benchmark: better benchmark.problems?
    def humanEval = loadBenchmark("humaneval-java-reworded")

    action(name: "createStimulusMatrices") {
        // FIXME problem.lql -> problem.interface
        // FIXME stimulusMatrix.lql -> stimulusMatrix.interface
        execute {
            // create stimulus matrices for given problems
            def myProblems = [humanEval.abstractions['HumanEval_13_greatest_common_divisor']]
            myProblems.each { problem ->
                stimulusMatrix(problem.id, problem.lql, [/*impls*/], problem.tests, problem.dependencies) // id, interface, impls, tests, dependencies
            }
            
            // add more
        }
    }

    // DECLARATIVE add implementations: This way, each action maps to one PROMPT template - this is what we want
      action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'createStimulusMatrices'
        include '*'
        profile('java11Profile') // evosuite 11

        // action configuration block 
        servers = ["http://bagdana.informatik.uni-mannheim.de:11434"]
        model = "llama3.1:latest"
        samples = 2 // FIXME how many to sample
        javaVersion = "11" // because of EvoSuite ..

        prompt { stimulusMatrix ->
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }
      }
      
    // DECLARATIVE add implementations: This way, each action maps to one PROMPT template - this is what we want
      action(name: 'generateCodeDeepSeek', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'generateCodeLlama'
        include '*'
        profile('java11Profile') // evosuite 11

        // action configuration block 
        servers = ["http://bagdana.informatik.uni-mannheim.de:11434"]
        model = "deepseek-r1:32b"
        samples = 2 // FIXME how many to sample
        javaVersion = "11" // because of EvoSuite ..

        prompt { stimulusMatrix ->
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }
      }
      
             // add tests (mutates existing tests)
             // FIXME make configurable for closeness to original values
    action(name: 'typeAware', type: 'TypeAwareMutatorTestGen') { // add more tests
        noOfTests = 1 // create one mutation per test
    
        dependsOn 'generateCodeDeepSeek'
        include '*'
    }    

       // add tests: randomly add new
    action(name: 'random', type: 'RandomTestGen') { // add more tests
        noOfTests = 5 // create 5 additional random tests
        shuffleSequence = false
    
        dependsOn 'typeAware'
        include '*'
    }
    
      action(name: 'generateTestsLlama', type: 'GenerateTestsOllama') {
        // pipeline specific
        dependsOn 'random'
        include '*'
        profile('java17Profile')

        // action configuration block 
        servers = ["http://bagdana.informatik.uni-mannheim.de:11434"]
        model = "llama3.1:latest"
        samples = 10 // FIXME how many to sample
          
        prompt { stimulusMatrix ->
            def prompt = [:] // create prompt model
            prompt.promptContent = """generate a junit test class to test the functionality of the following interface specification: ```${stimulusMatrix.lql}```. Assume that the specification is encapsulated in a class that uses the same naming as in the interface specification. Only output the JUnit test class and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }  
      }
      
       // add tests: SBST
    action(name: 'evoSuite', type: 'EvoSuite') {
        searchBudget = 30 // we need this as upper bound for timeouts
        stoppingCondition = "MaxTime"
        //criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"

        dependsOn 'generateTestsLlama'
        include '*'
        profile('java11Profile')
    }
    
    action(name: 'filter', type: 'Arena') { // run all collected stimulus sheets on all impls in arena
        maxAdaptations = 1 // how many adaptations to try
        //features = ["cc", "mutation"]
        
        // FIXME run how often? runs = X (then generate different arena ids?)

        dependsOn 'evoSuite'
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
    void test_codeclone() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'CodeClone') {

    // profile for execution
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }

    // load benchmark: better benchmark.problems?
    def humanEval = loadBenchmark("humaneval-java-reworded")

    action(name: "createStimulusMatrices") {
        // FIXME problem.lql -> problem.interface
        // FIXME stimulusMatrix.lql -> stimulusMatrix.interface
        execute {
            // create stimulus matrices for given problems
            def myProblems = [humanEval.abstractions['HumanEval_13_greatest_common_divisor']]
            myProblems.each { problem ->
                stimulusMatrix(problem.id, problem.lql, [/*impls*/], problem.tests, problem.dependencies) // id, interface, impls, tests, dependencies
            }
            
            // add more
        }
    }

    action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'createStimulusMatrices'
        include '*'
        profile('java17Profile') // evosuite 11

        // action configuration block 
        servers = ["http://bagdana.informatik.uni-mannheim.de:11434"]
        model = "llama3.1:latest"
        samples = 5 // FIXME how many to sample
        
        // custom DSL command offered by the action (for each stimulus matrix, create one prompt to obtain impls)
        prompt { stimulusMatrix ->
            // can by for any prompts: FA, impls, models etc.
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
            prompt.id = "lql_prompt"
            //prompt.model = "llama3.1:latest"
            return [prompt] // list of prompts is expected
        }
      }
      
    action(name: 'filter', type: 'Nicad6') {
        collapseClones = true // drop clones

        dependsOn 'generateCodeLlama'
        include '*'
        profile('nicad:6.2')
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

    // Randoop needs to be deployed in nexus
    @Test
    void test_randoop() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Randoop') {

    // profile for execution
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }

    action(name: "createStimulusMatrices") {
        execute {
            stimulusMatrix('Base64', """Base64{
                    encode(byte[])->byte[]
                    decode(java.lang.String)->byte[]
                }
                """, [
                    implementation(UUID.randomUUID().toString(), "org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15"),
                ], [/*tests*/])
        }
    }
      
    action(name: 'randoop', type: 'Randoop') {
        collapseClones = true // drop clones

        dependsOn 'createStimulusMatrices'
        include '*'
        profile('java17Profile')
    }
    
    action(name: 'filter', type: 'Arena') { // run all collected stimulus sheets on all impls in arena
        maxAdaptations = 1 // how many adaptations to try
        //features = ["cc", "mutation"]
        
        // FIXME run how often? runs = X (then generate different arena ids?)

        dependsOn 'randoop'
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
    void test_Ollama_parallel() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'Ollama-Parallel') {

    // profile for execution
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }

    // load benchmark: better benchmark.problems?
    def humanEval = loadBenchmark("humaneval-java-reworded")

    action(name: "createStimulusMatrices") {
        execute {
            // create stimulus matrices for given problems
            def myProblems = [humanEval.abstractions['HumanEval_13_greatest_common_divisor']]
            myProblems.each { problem ->
                stimulusMatrix(problem.id, problem.lql, [/*impls*/], problem.tests, problem.dependencies) // id, interface, impls, tests, dependencies
            }
            
            // add more
        }
    }

    action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'createStimulusMatrices'
        include '*'
        profile('java17Profile') // evosuite 11

        // action configuration block 
        servers = ["http://bagdana.informatik.uni-mannheim.de:11434", "http://dybbuk.informatik.uni-mannheim.de:11434"]
        model = "llama3.1:latest"
        samples = 5 // FIXME how many to sample
        promptRequestThreads = 4 // parallel threads
        
        // custom DSL command offered by the action (for each stimulus matrix, create one prompt to obtain impls)
        prompt { stimulusMatrix ->
            // can by for any prompts: FA, impls, models etc.
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
            prompt.id = "lql_prompt"
            //prompt.model = "llama3.1:latest"
            return [prompt] // list of prompts is expected
        }
      }
      
      action(name: 'generateTestsLlama', type: 'GenerateTestsOllama') {
        // pipeline specific
        dependsOn 'generateCodeLlama'
        include '*'
        profile('java17Profile')

        // action configuration block 
        servers = ["http://bagdana.informatik.uni-mannheim.de:11434", "http://dybbuk.informatik.uni-mannheim.de:11434"]
        model = "llama3.1:latest"
        samples = 10 // FIXME how many to sample
        promptRequestThreads = 4 // parallel threads
          
        prompt { stimulusMatrix ->
            def prompt = [:] // create prompt model
            prompt.promptContent = """generate a junit test class to test the functionality of the following interface specification: ```${stimulusMatrix.lql}```. Assume that the specification is encapsulated in a class that uses the same naming as in the interface specification. Only output the JUnit test class and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
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
    void test_OpenAI_chatgpt() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'ChatGPT') {

    // profile for execution
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }

    // load benchmark: better benchmark.problems?
    def humanEval = loadBenchmark("humaneval-java-reworded")

    action(name: "createStimulusMatrices") {
        // FIXME problem.lql -> problem.interface
        // FIXME stimulusMatrix.lql -> stimulusMatrix.interface
        execute {
            // create stimulus matrices for given problems
            def myProblems = [humanEval.abstractions['HumanEval_13_greatest_common_divisor']]
            myProblems.each { problem ->
                stimulusMatrix(problem.id, problem.lql, [/*impls*/], problem.tests, problem.dependencies) // id, interface, impls, tests, dependencies
            }
            
            // add more
        }
    }

    action(name: 'generateCodeGpt', type: 'GenerateCodeOpenAI') {
        // pipeline specific
        dependsOn 'createStimulusMatrices'
        include '*'
        profile('java17Profile') // evosuite 11

        // action configuration block 
        apiKey = "demo" // see https://docs.langchain4j.dev/integrations/language-models/open-ai/
        model = "gpt-4o-mini"
        samples = 1
        
        // custom DSL command offered by the action (for each stimulus matrix, create one prompt to obtain impls)
        prompt { stimulusMatrix ->
            // can by for any prompts: FA, impls, models etc.
            def prompt = [:] // create prompt model
            prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
            prompt.id = "lql_prompt"
            //prompt.model = "llama3.1:latest"
            return [prompt] // list of prompts is expected
        }
      }
      
      action(name: 'generateTestsGpt', type: 'GenerateTestsOpenAI') {
        // pipeline specific
        dependsOn 'generateCodeGpt'
        include '*'
        profile('java17Profile')

        // action configuration block 
        apiKey = "demo" // see https://docs.langchain4j.dev/integrations/language-models/open-ai/
        model = "gpt-4o-mini"
        samples = 1
          
        prompt { stimulusMatrix ->
            def prompt = [:] // create prompt model
            prompt.promptContent = """generate a junit test class to test the functionality of the following interface specification: ```${stimulusMatrix.lql}```. Assume that the specification is encapsulated in a class that uses the same naming as in the interface specification. Only output the JUnit test class and nothing else."""
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }  
      }
      
    action(name: 'execute', type: 'Arena') { // run all collected stimulus sheets on all impls in arena
        maxAdaptations = 1 // how many adaptations to try
        //features = ["cc", "mutation"]

        dependsOn 'generateTestsGpt'
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

    // take prompt from benchmark, generate tests by presenting code
    @Test
    void test_original_Benchmark() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'
study(name: 'OriginalBenchmarkPrompt') {

    // profile for execution
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }

    // load benchmark: better benchmark.problems?
    def humanEval = loadBenchmark("humaneval-java-reworded")

    action(name: "createStimulusMatrices") {
        execute {
            // create stimulus matrices for given problems
            def myProblems = [humanEval.abstractions['HumanEval_13_greatest_common_divisor']]
            myProblems.each { problem ->
                stimulusMatrix(problem.id, problem.lql, [/*impls*/], problem.tests, problem.dependencies) // id, interface, impls, tests, dependencies
            }
            
            // add more
        }
    }
    
    action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
        // pipeline specific
        dependsOn 'createStimulusMatrices'
        include '*'
        profile('java17Profile') // evosuite 11

        // action configuration block 
        servers = ["http://localhost:11434"]
        model = "llama3.1:latest"
        samples = 1 // FIXME how many to sample
        promptRequestThreads = 4 // parallel threads
        
        // custom DSL command offered by the action (for each stimulus matrix, create one prompt to obtain impls)
        prompt { stimulusMatrix ->
            def prompt = [:] // create prompt model
            // get original prompt from benchmark
            prompt.promptContent = humanEval.abstractions[stimulusMatrix.name].prompt
            prompt.id = "lql_prompt"
            return [prompt] // list of prompts is expected
        }
      }
      
      action(name: 'generateTestsLlama', type: 'GenerateTestsOllama') {
        // pipeline specific
        dependsOn 'generateCodeLlama'
        include '*'
        profile('java17Profile')

        // action configuration block 
        servers = ["http://localhost:11434"]
        model = "llama3.1:latest"
        samples = 1 // FIXME how many to sample
        promptRequestThreads = 4 // parallel threads
          
        prompt { stimulusMatrix ->
            List prompts = stimulusMatrix.implementations.collect { impl ->
                def prompt = [:] // create prompt model
                prompt.promptContent = """generate a junit test class to test the functionality of the following java class `${impl.code.name}` : ```${impl.code.content}```. Initialize the class and call its methods. Only output the JUnit test class and nothing else."""
                prompt.id = "lql_prompt"
                return prompt
            }

            return prompts
        }  
      }
      
    action(name: 'execute', type: 'Arena') { // run all collected stimulus sheets on all impls in arena
        maxAdaptations = 1 // how many adaptations to try
        //features = ["cc", "mutation"]

        dependsOn 'generateTestsLlama'
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
