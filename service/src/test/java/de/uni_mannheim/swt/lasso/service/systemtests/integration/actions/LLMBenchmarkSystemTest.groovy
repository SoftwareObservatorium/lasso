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
 * Demonstrates MultiPL-E benchmark pipeline for code LLMs
 *
 * @author mkessel
 */
class LLMBenchmarkSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    // BUG potentially in arena compile
    @Test
    void test_select_single_problem_HumanEval_159_eat() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
        
            action(name:'select', type:'Select') {
                abstraction('HumanEval_159_eat') {
                    queryForClasses "*:*" // always 'Problem'
                    rows = Integer.MAX_VALUE

                    filter 'id:"756b220d-d766-459a-9614-47e7d6266d6c"'
                }
            }
            
            profile('myProfile') {
                scope('class') { // measurement scope
                    type = 'class'
                    methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
                }
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            action(name:'execute',type:'ArenaExecute') {
                // use 'sequences' + 'specification' provided by benchmark
                benchmark = humanEvalBenchmark.name
                
                //features = ['mutation', 'cc'] // measure MS and BC
                //features = ['cc']
        
                dependsOn 'select'
                includeAbstractions '*'
                profile('myProfile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();
    }

    @Test
    void test_select_single_problem() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
        
            action(name:'select', type:'Select') {
                // we could also provide a DSL command fromProblem(..)
                abstraction(problemId) {
                    queryForClasses "*:*" // always 'Problem'
                    rows = Integer.MAX_VALUE

                    filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                    filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                    filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                    //filter 'k:[0 TO 9]' // top-k, here 10
                    filter 'k:0' // select first code generated
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();
    }


    @Test
    void test_select_single_problem_allgenerators() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        //String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
        
            action(name:'select', type:'Select') {
                // we could also provide a DSL command fromProblem(..)
                abstraction(problemId) {
                    queryForClasses "*:*" // always 'Problem'
                    rows = Integer.MAX_VALUE

                    filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                    filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                    //filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                    //filter 'k:[0 TO 9]' // top-k, here 10
                    filter 'k:0' // select first code generated
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();
    }

    @Test
    void test_select_all_problems() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String generatorId = "humaneval-java-davinci-0.2-reworded"
        //String kRange = "[0 TO 0]"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId) // Benchmark.problems
        
            action(name:'select', type:'Select') {
                execute() {
                    // iterate over each problem (it's a map)
                    log(humanEvalBenchmark.abstractions)
                    humanEvalBenchmark.abstractions.each { problemId, problem ->
                        log("Problem ${problemId}")
                        abstraction(problemId) {
                            queryForClasses "*:*" // always 'Problem'
                            rows = Integer.MAX_VALUE
        
                            filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                            filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                            filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                            //filter 'k:[0 TO 9]' // top-k, here 10
                            filter 'k:0' // select first code generated
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
    }

    @Test
    void test_select_IDCS() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023' // different data source (can also be set directly in Select)
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
        
            action(name:'select', type:'Select') {
                def myAb = humanEvalBenchmark.abstractions[problemId]
                
                abstraction(problemId) {
                    queryForClasses "${myAb.lql}" // IDCS
                    rows = 10
                }
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();
    }

    @Test
    void test_select_TDS() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
            def myAb = humanEvalBenchmark.abstractions[problemId]
        
            action(name:'select', type:'Select') {
                // we could also provide a DSL command fromProblem(..)
                abstraction(problemId) {
                    queryForClasses "*:*" // IDCS
                    rows = Integer.MAX_VALUE

                    filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                    filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                    filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                    filter 'k:[0 TO 2]' // top-k, here 10
                    //filter 'k:0' // select first code generated
                }
            }
            
            profile('myProfile') {
                scope('class') { // measurement scope
                    type = 'class'
                    methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
                }
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            action(name:'execute',type:'ArenaExecute') {
                // use 'sequences' + 'specification' provided by benchmark
                benchmark = humanEvalBenchmark.name
                
                //features = ['mutation', 'cc'] // measure MS and BC
                features = ['cc']
        
                dependsOn 'select'
                includeAbstractions '*'
                profile('myProfile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    // FIXME does not work as intended, see version two
    @Test
    @Deprecated
    void test_select_generate_deprecated() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
            def myAb = humanEvalBenchmark.abstractions[problemId]
        
            action(name:'select', type:'Select') {
                // we could also provide a DSL command fromProblem(..)
                abstraction(problemId) {
                    queryForClasses "*:*" // IDCS
                    rows = Integer.MAX_VALUE

                    filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                    filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                    filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                    filter 'k:[0 TO 2]' // top-k, here 10
                    //filter 'k:0' // select first code generated
                }
            }
            
            profile('myProfile') {
                scope('class') { // measurement scope
                    type = 'class'
                    methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
                }
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            action(name:'generate',type:'EvoSuite') { // generate tests
                searchBudget = 30
        
                dependsOn 'select'
                includeAbstractions '*'
                profile('myProfile')
            }
            
            action(name:'execute',type:'ArenaExecute') {
                // use configuration {} block to set up each FA?
                
                // use 'sequences' + 'specification' provided by benchmark
                benchmark = humanEvalBenchmark.name
        
                dependsOn 'generate'
                includeAbstractions '*'
                profile('myProfile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_select_generate() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
            def myAb = humanEvalBenchmark.abstractions[problemId]
        
            // FIXME use dependsOn in next action to just grab the executable ones from TDS
            action(name:'select', type:'Select') {
                // we could also provide a DSL command fromProblem(..)
                abstraction(problemId) {
                    queryForClasses "*:*" // IDCS
                    rows = Integer.MAX_VALUE

                    filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                    filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                    filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                    filter 'k:[0 TO 2]' // top-k, here 10
                    //filter 'k:0' // select first code generated
                }
            }
            
            profile('myProfile') {
                scope('class') { // measurement scope
                    type = 'class'
                    methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
                }
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            action(name:'generate',type:'EvoSuite') { // generate tests
                searchBudget = 30
                dropFailed = false // do not drop
        
                dependsOn 'select'
                includeAbstractions '*'
                profile('myProfile')
            }
            
            // uses amplification (FIXME we should restrict to lower no. of tests)
            action(name:'execute',type:'ArenaAmplify') {
                // uses 
                benchmark = humanEvalBenchmark.name
                noTestsFromBenchmark = true // FIXME better include them? so we can see increase in test set quality
                features = ['cc'] // enable CC
        
                dependsOn 'generate'
                includeAbstractions '*'
                profile('myProfile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_select_CLONES() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        //String generatorId = "humaneval-java-davinci-0.2-reworded"
        
        study(name: 'MultiPLE-tdcs-humaneval-allproblems-allgenerators-allk-CLONES') {
        
            def humanEvalBenchmark = loadBenchmark(benchmarkId) // Benchmark.problems
        
            action(name:'select', type:'Select') {
                execute() {
                    // iterate over each problem (it's a map)
                    humanEvalBenchmark.abstractions.each { problemId, problem ->
                        log("Problem ${problemId}")
                        abstraction(problemId) {
                            queryForClasses "*:*" // always 'Problem'
                            rows = Integer.MAX_VALUE
        
                            filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                            filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                            //filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                            //filter 'k:[0 TO 9]' // top-k, here 10
                            //filter 'k:0' // select first code generated
                        }
                    }
                }
            }
            
            action(name: "clones", type: 'Nicad6') {
                cloneType = "type2" // clone type to reject
                collapseClones = false // do not remove clone implementations
        
                dependsOn "select"
                includeAbstractions '*'
                
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
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

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    @Test
    void test_select_SAMPLE_N_GENERATE() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        String benchmarkId = "humaneval-java-reworded"
        String problemId = "HumanEval_23_strlen"
        String generatorId = "humaneval-java-davinci-0.2-reworded"

        study(name: 'MultiPLE') {
            def humanEvalBenchmark = loadBenchmark(benchmarkId)
            def myAb = humanEvalBenchmark.abstractions[problemId]
        
            // FIXME use dependsOn in next action to just grab the executable ones from TDS
            action(name:'select', type:'Select') {
                // we could also provide a DSL command fromProblem(..)
                abstraction(problemId) {
                    queryForClasses "*:*" // IDCS
                    rows = Integer.MAX_VALUE

                    filter 'benchmark:"'+ benchmarkId +'"' // specify benchmark
                    filter 'problem:"'+ problemId +'"' // specify problem (becomes functional abstraction)
                    filter 'generator:"'+ generatorId +'"' // specific generators (code LLMs)
                    filter 'k:[0 TO 2]' // top-k, here 10
                    //filter 'k:0' // select first code generated
                }
            }
            
            profile('myProfile') {
                scope('class') { // measurement scope
                    type = 'class'
                    methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
                }
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            action(name:'generate',type:'EvoSuite') { // generate tests
                searchBudget = 30
                dropFailed = false // do not drop
        
                dependsOn 'select'
                includeAbstractions '*'
                includeSystems {abName -> // sample systems
                    // samples
                    int N = 2 // FIXME sample X from each LLM
                    List systems = abstractions[abName].systems
                    List samples = []
                    def random = new Random()
                    // take N samples
                    (1..N).each {
                        // pick from the list at random
                        samples << systems[random.nextInt(systems.size())]
                    }
                    
                    abstractions[abName].systems = samples
                }
                profile('myProfile')
            }

            action(name:'execute',type:'ArenaExecute') {
                configure {
                    // uses 
                    benchmark = humanEvalBenchmark.name
                    noTestsFromBenchmark = true // FIXME better include them? so we can see increase in test set quality
                    features = ['cc'] // enable CC
                    
                    populateTestsFromAction = 'generate'
                }
        
                dependsOn 'select'
                includeAbstractions '*'
                profile('myProfile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    // FE from past script executions
    @Test
    void test_select_FUNCTIONAL_EQUIVALENCE() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'

        study(name: 'MultiPLE') {
            
            action(name:'equivalence') {
            
                dependsOn '28294367-2a11-4744-9061-cd749a0e070e:execute' // depends on other study execution
                includeAbstractions '*'
                
                execute {
                    def ab = abstractions['HumanEval_23_strlen']
                    def mySrm = srm(abstraction: ab, path: '28294367-2a11-4744-9061-cd749a0e070e:execute')
                    // define oracle based on expected responses in sequences
                    def expectedBehaviour = toOracle(mySrm.sequences)
                    // returns a filtered SRM
                    def matchesSrm = mySrm
                            .systems // select all systems
                            .equalTo(expectedBehaviour) // functionally equivalent

                    // iterate over sub-SRM
                    matchesSrm.systems.each { s ->
                        log("Matched class ${s.id}, ${s.packageName}.${s.name}")
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

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }

    // this is the full template to access the original SRM, conduct behavioral analysis and then to generate additional tests
    @Test
    void test_select_SAMPLE_N_GENERATE_FROM_FEs() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'multipleBenchmark23'
        
        def benchmarkId = "humaneval-java-reworded"
        def studyPath = '28294367-2a11-4744-9061-cd749a0e070e:execute' // rename to studyResource?
        def sampleSize = 2 // FIXME sample X from each LLM
        def evoSuiteSearchBudget = 30

        study(name: 'MultiPLE-TestGen') {        
            // identify FEs from past execution
            action(name:'equivalent') {
                dependsOn studyPath // depends on other study execution
                includeAbstractions '*'
                
                execute {
                    // for each abstraction, filter by functional equivalence wrt. oracle
                    abstractions.each {abName, ab -> 
                        def mySrm = srm(abstraction: ab, path: studyPath)
                        // define oracle based on expected responses in sequences
                        def expectedBehaviour = toOracle(mySrm.sequences)
                        // returns a filtered SRM
                        def matchesSrm = mySrm
                                .systems // select all systems
                                .equalTo(expectedBehaviour) // functionally equivalent
    
                        // iterate over sub-SRM
                        matchesSrm.systems.each { s ->
                            log("Matched class ${s.id}, ${s.packageName}.${s.name}")
                        }
                        
                        ab.systems = matchesSrm.systems
                    }
                }
            }
            
            profile('myProfile') {
                scope('class') { // measurement scope
                    type = 'class'
                    methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
                }
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            action(name:'generate',type:'EvoSuite') { // generate tests
                searchBudget = evoSuiteSearchBudget
                //dropFailed = false // do not drop
        
                dependsOn 'equivalent'
                includeAbstractions '*'
                includeSystems {abName -> // sample systems
                    // samples
                    List systems = abstractions[abName].systems
                    def random = new Random()
                    Set indices = new HashSet()
                    // take N unique! samples
                    while(indices.size() < sampleSize) {
                        indices.add(random.nextInt(systems.size()))
                    }
                    
                    List samples = []
                    indices.each { i -> 
                        samples << systems[i]
                    }
                    
                    abstractions[abName].systems = samples
                }
                profile('myProfile')
            }

            action(name:'execute',type:'ArenaExecute') {
                configure {
                    // uses 
                    benchmark = benchmarkId
                    noTestsFromBenchmark = true // FIXME better include them? so we can see increase in test set quality
                    features = ['cc'] // enable CC
                    
                    populateTestsFromAction = 'generate'
                }
        
                dependsOn 'equivalent'
                includeAbstractions '*'
                profile('myProfile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());
    }
}
