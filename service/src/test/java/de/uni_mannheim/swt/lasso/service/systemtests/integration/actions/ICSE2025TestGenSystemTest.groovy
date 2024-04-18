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
 * Demonstrates GAITestGen action
 *
 * @author mkessel
 */
class ICSE2025TestGenSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void testSum() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
dataSource 'lasso_quickstart'

String benchmarkId = "humaneval-java-reworded"
int noOfCandidates = Integer.MAX_VALUE

study(name: 'ICSE2025-HumanEval-ALL-CC') {
    def humanEvalBenchmark = loadBenchmark(benchmarkId)

    action(name:'select', type:'Select') {
        execute() {
            // iterate over each problem (it's a map)
            humanEvalBenchmark.abstractions.each { problemId, problem ->
                log("Problem ${problemId}")
                abstraction(problemId) {
                    queryForClasses "*:*" // always 'Problem\'
                    rows = noOfCandidates

                    filter 'groupId:"'+ problemId +'"'
                }
            }
        }
    }
        
    profile('myProfile') {
        scope('class') { // measurement scope
            type = 'class'
            methodBlacklist = ['main', '<init>', '<clinit>'] // ignore main method
        }
        environment('java17') { // execution environment
            image = 'maven:3.6.3-openjdk-17' // (docker) image template
        }
    }
    
    action(name:'fsExecute',type:'ArenaExecute') {
        // use 'sequences' + 'specification' provided by benchmark
        benchmark = humanEvalBenchmark.name

        generateJUnitTests = false
        exportCsv = false
        
        //features = ['mutation', 'cc'] // measure MS and BC
        features = ['cc']

        dependsOn 'select'
        includeAbstractions '*'
        profile('myProfile')
    }
    
    // proceed with functionally "correct" ones (as determined by set of seed tests)
    
    action(name:'gaiTestGen',type:'GAITestGen') {
        // use 'sequences' + 'specification' provided by benchmark
        benchmark = humanEvalBenchmark.name
        apiUrl = "http://bagdana.informatik.uni-mannheim.de:8080/v1/chat/completions"
        apiKey = "swt4321"
        maxNoOfTests = 100
        noOfPrompts = 1

        dependsOn 'fsExecute'
        includeAbstractions '*'
    }
    
    action(name:'typeAwareTestGen',type:'TypeAwareMutatorTestGen') {
        // use 'sequences' + 'specification' provided by benchmark
        benchmark = humanEvalBenchmark.name
        noOfTests = 100

        dependsOn 'fsExecute'
        includeAbstractions '*'
    }
    
    action(name:'randomTestGen',type:'RandomTestGen') {
        noOfTests = 100

        dependsOn 'fsExecute'
        includeAbstractions '*'
    }
    
    action(name:'amplify',type:'ArenaExecute') {
        // TODO obtain from actions as well (load as JSON from distributed data store)
        testsFromActions = ["typeAwareTestGen", "randomTestGen", "randomTestGen"]
        
        generateJUnitTests = false
        exportCsv = false
        
        //features = ['mutation', 'cc'] // measure MS and BC
        features = ['cc']

        dependsOn 'fsExecute'
        includeAbstractions '*'
        profile('myProfile')
    }
    
    // TODO do deviant testing?
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
