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

import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Demonstrates LSL Pipelines
 *
 * @author mkessel
 */
class PipeLineSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * Amplification pipeline.
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_amplification() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'
        
        def refEvoSuiteTimeBudget = 30
        def altEvoSuiteTimeBudget = 30
        def altImpls = 50 // over budget
        def stopAfterClasses = 15 // stop after X successful classes
        def totalNoOfRandomClasses = 10
        
        def adapterImplementations = 1 // how many adapter implementations to try
        
        def studyRepetitions = 1
        
        study(name:'AMPLIFICATION') {
        
            //-- START reference impls
        
            /**
             * LOCAL, MASTER
             */
            action(name: 'selectRandom', type: 'Select') {
                abstraction('Random') {
                    queryForClasses '*:*', 'concept'
                    rows = 3
                    
                    directly = true // without cursor functionality

                    // pick two known stacks
                    filter 'id:("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025")'
                }
            }
        
            //-- START alternative impls
        
            // continue with retrieving alternative implementations
        
            /**
             * LOCAL, MASTER
             */
            action(name: "selectAlt", type: 'Select') {
                dependsOn "selectRandom"
                includeAbstractions 'Random'
        
                execute() {
                    List refImpls = abstractions['Random'].implementations
                    refImpls.each { impl ->
                        abstraction(impl) { // by example
                            queryForClasses '*:*', 'concept'
                            rows = 2
                            
                            directly = true // without cursor functionality
        
                            // pick two known stacks
                            filter 'id:("4e73bb0d-f01f-43e5-bf46-7ab7870a289f" OR "91a8e57d-cffe-4bd6-b1f0-1859366e8f5e")'
                        }
                    }
                }
            }
        
            /**
             * LOCAL, MASTER
             */
            action(name: "clonesAlt", type: 'Nicad6') {
                cloneType = "type2" // see nicad "config/" for possible clone types (1-3)
                collapseClones = true // collapse (i.e remove)
                refActionRef = "selectRandom" // also drop clones of reference implementation
        
                dependsOn "selectAlt" // mandatory
                includeAbstractions '*-*'
                profile {
                    environment('nicad') {
                        image = 'nicad:6.2'
                    }
                }
            }
        
            // repetitions
            for(int repetition = 0; repetition < studyRepetitions; repetition++) {
        
                /**
                 * DISTRIBUTED, WORKER
                 */
                action(name:"evosuiteRef_${repetition}",type:'EvosuiteGenerateClass') {
                    // configuration
        
                    ignoreMissingReport = true
                    searchBudget = refEvoSuiteTimeBudget
                    stoppingCondition = "MaxTime"
                    criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"
        
                    // kill process after searchBudget * timeoutMultiplier
                    timeoutMultiplier = 3
        
                    dependsOn 'selectRandom' // mandatory
                    includeAbstractions 'Random'
                    includeImplementations {abName ->
                        // check if alts exist, if not remove ref
                        abstractions[abName].implementations.removeAll {impl ->
                            // alts empty
                            !actions["clonesAlt"].abstractions[impl.id]?.implementations
                        }
        
                        abstractions[abName].implementations
                    }
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8'
                        }
                    }
                }
        
                action(name:"pitestOriginal_${repetition}",type:'Pitest') {
                    dropFailed = true // drop if we cannot measure PIT
        
                    dependsOn "evosuiteRef_${repetition}"
                    includeAbstractions 'Random'
                    includeTests '*' // include original tests
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8-alpine'
                        }
                    }
        
                    whenAbstractionsReady() {
                        // FIXME remove IDEAL
                    }
                }
        
                action(name:"jacocoOriginal_${repetition}",type:'JaCoCo') {
                    dropFailed = false
        
                    minimumTestCoverage = 0d
                    generateReport = false
                    
                    // helper variable (scoping issues)
                    def currentRepetition = repetition
        
                    dependsOn "evosuiteRef_${repetition}"
                    includeAbstractions 'Random'
                    includeTests '*' // include original tests
                    includeImplementations {abName ->
                        String actionRef = "pitestOriginal_${currentRepetition}".toString()
                        actions[actionRef].abstractions[abName].implementations // only run those which passed PIT
                    }
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8-alpine'
                        }
                    }
                }
        
                /**
                 * DISTRIBUTED, WORKER
                 */
                action(name:"evosuiteAlt_${repetition}",type:'EvosuiteGenerateClass') {
                    // configuration
                    ignoreMissingReport = true
                    searchBudget = altEvoSuiteTimeBudget
                    stoppingCondition = "MaxTime"
                    criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"
        
                    stopAfter = stopAfterClasses // stop after X successful classes
        
                    // kill process after searchBudget * timeoutMultiplier
                    timeoutMultiplier = 3
        
                    // helper variable (scoping issues)
                    def currentRepetition = repetition
        
                    dependsOn "clonesAlt" // mandatory
                    includeAbstractions '*-*'
                    includeImplementations {abName ->
                        // only return alt impls if ref exists
                        String actionRef = "pitestOriginal_${currentRepetition}".toString()
                        if(actions[actionRef].abstractions['Random'].implementations?.find { it.id == abName }) {
                            return abstractions[abName].implementations
                        } else {
                            return []
                        }
                    }
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8'
                        }
                    }
        
                    whenAbstractionsReady() {
                        Map abs = abstractions as Map
        
                        // get down to stopAfterClasses and add reference implementation.
                        abs.each{ abName, abstraction ->
                            abstraction.implementations =
                                    abstraction.implementations.take(stopAfterClasses) // take up to 'stopAfterClasses'
        
                            // add ref impl
                            // only if at least one implementation
                            if(abstraction.implementations) {
                                String refAction = "evosuiteRef_${currentRepetition}".toString()
                                def refImpl = actions[refAction].abstractions['Random'].implementations?.find { it.id == abName }
                                if(refImpl) {
                                    abstraction.implementations.add(refImpl)
                                }
                            }
                        }
                    }
                }
        
                /**
                 * LOCAL, WORKER
                 *
                 * Amplification Action
                 */
                action(name:"arena_${repetition}",type:'Arena') {
                    disablePartitioning = true
                    maxPermutations = adapterImplementations // this assumes that we try only the "best match"
                    task = 'Amplify'
                    exportCsv = true
        
                    containerTimeout = 1 * 60 * 60 * 1000L // 1hour
        
                    referenceImplementationOnly = true // required
        
                    dependsOn "evosuiteAlt_${repetition}"
                    includeAbstractions '*-*'
                    includeImplementations {abName ->
                        // reference implementations only
                        if(abstractions[abName].implementations?.size < 2) {
                            return [] // don't execute this action if no alts
                        }
        
                        abstractions[abName].implementations
                    }
                    includeTests '*' // take any
                    profile {
                        environment('java8') {
                            image = 'openjdk:8-jdk-alpine'
                        }
                    }
                }
        
                // merge again so that we can run all refs in parallel (performance improvement in distributed environment)
                action(name: "merge_${repetition}") { // plain action
                    dependsOn "arena_${repetition}"
                    includeAbstractions '*-*'
        
                    execute {
                        Map abs = abstractions as Map
                        List refs = abs.collect { name, ab ->
                            ab.implementations?.find { it.id == name }
                        }.findAll { it != null}
        
                        // create new abstraction
                        def amplifiedAbstraction = abstraction(refs, "Amplified")
        
                        log("abstraction ${amplifiedAbstraction.name} has ${amplifiedAbstraction.implementations.size()} implementations")
                    }
                }
        
        
                action(name:"pitestAmplify_${repetition}",type:'Pitest') {
                    dropFailed = false
        
                    dependsOn "merge_${repetition}" // mandatory
                    includeAbstractions 'Amplified'
                    includeTests '*' // include all
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8-alpine'
                        }
                    }
                }
        
                action(name:"jacocoAmplify_${repetition}",type:'JaCoCo') {
                    dropFailed = false
        
                    minimumTestCoverage = 0d
                    generateReport = false
        
                    dependsOn "merge_${repetition}" // mandatory
                    includeAbstractions 'Amplified'
                    includeTests '*' // include all
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8-alpine'
                        }
                    }
                }
        
                // EvoTime
                action(name:"evoTime_${repetition}",type:'EvosuiteGenerateClass') {
                    def actionRef = "arena_${repetition}".toString()
        
                    // configuration
                    configure {
                        ignoreMissingReport = true
                        searchBudget = refEvoSuiteTimeBudget // we need this as upper bound for timeouts
                        stoppingCondition = "MaxTime"
                        criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"
        
                        // get no of alt. implementations by abstraction (i.e impls size * default budget)
                        List refImpls = abstractions['Amplified'].implementations as List
                        timeBudgetProviderByImpl = refImpls.collectEntries { impl ->
                            List allImpls = actions[actionRef].abstractions[impl.id].implementations as List
                            if(allImpls && allImpls.size() > 1) {
                                return [impl.id, (allImpls.size() - 1) * refEvoSuiteTimeBudget] // exclude ref impl
                            } else {
                                return [impl.id, 1]
                            }
                        }
        
                        timeBudgetProviderDefault = 120 // fallback for missing implementation
        
                        // kill process after searchBudget * timeoutMultiplier
                        timeoutMultiplier = 2
                    }
        
                    dependsOn "merge_${repetition}" // mandatory
                    includeAbstractions 'Amplified'
                    includeTests 'NONE' // include NONE (dummy placeholder, means EXCLUDE any)
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8'
                        }
                    }
                }
        
                action(name:"pitestEvoTime_${repetition}",type:'Pitest') {
                    dropFailed = false
        
                    dependsOn "evoTime_${repetition}" // mandatory
                    includeAbstractions 'Amplified'
                    includeTests '*' // include all
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8-alpine'
                        }
                    }
                }
        
                action(name:"jacocoEvoTime_${repetition}",type:'JaCoCo') {
                    dropFailed = false
        
                    minimumTestCoverage = 0d
                    generateReport = false
        
                    dependsOn "evoTime_${repetition}" // mandatory
                    includeAbstractions 'Amplified'
                    includeTests '*' // include all
                    profile {
                        environment('java8') {
                            image = 'maven:3.5.4-jdk-8-alpine'
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

        // assertions
        verifyAbstraction(lslExecutionContext, 'pitestOriginal_0', 'Random', 1)
        verifyAbstraction(lslExecutionContext, 'jacocoOriginal_0', 'Random', 1)
        // including alts + ref
        verifyAbstraction(lslExecutionContext, 'evosuiteAlt_0', '6ce338e3-3c3c-4f52-b595-9b3ed5bb4025', 3)
        verifyAbstraction(lslExecutionContext, 'arena_0', '6ce338e3-3c3c-4f52-b595-9b3ed5bb4025', 3)

        verifyAbstraction(lslExecutionContext, 'merge_0', 'Amplified', 1)
        verifyAbstraction(lslExecutionContext, 'pitestAmplify_0', 'Amplified', 1)
        verifyAbstraction(lslExecutionContext, 'jacocoAmplify_0', 'Amplified', 1)

        verifyAbstraction(lslExecutionContext, 'evoTime_0', 'Amplified', 1)
        verifyAbstraction(lslExecutionContext, 'pitestEvoTime_0', 'Amplified', 1)
        verifyAbstraction(lslExecutionContext, 'jacocoEvoTime_0', 'Amplified', 1)

        Table pitTable = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from PitestReport")
        println(pitTable.printAll())

        Table jacocoTable = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from ClassLevelJaCoCoReport")
        println(jacocoTable.printAll())

        assert pitTable.size() == 3
        assert jacocoTable.size() == 3
    }

    /**
     * Retrieve by example
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_retrieval_by_example_random() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020'

        def altImpls = 1
        def totalNoOfRandomClasses = 10
        
        study(name:'AMPLIFICATION') {
        
            //-- START reference impls
        
            /**
             * LOCAL, MASTER
             */
            action(name: 'selectRandom', type: 'Select') {
                abstraction('Random') {
                    queryForClasses '*:*', 'concept'
                    rows = totalNoOfRandomClasses
        
                    directly = true
        
                    random = true
        
                    javaTypeFilter = true // java types only
        
                    dropPojo = true // drop Pojos
        
                    collapseBy("hash")
                    useAlternatives = false
        
                    // EvoSuite is compatible up to Java 8 (i.e bytecode version 52)
                    filter 'bytecodeversion_i:[* TO 52]' // FIXME
                    // visible classes only
                    // CLASS must be visible
                    filter '-keyword_ss:("private" OR "abstract")' // non abstract
        
                    // not from tests classifier
                    filter '-classifier_s:"tests"'
                    // no JUnit3 tests
                    filter '-dep_exact:"junit/framework/TestCase"'
                    // no JUnit4 tests
                    filter '-dep_exact:"org/junit/Test"'
                    // no JUnit5 tests
                    filter '-dep_exact:"org/junit/jupiter/api/Test"'
                    // no TestNG
                    filter '-dep_exact:"org/testng/annotations/Test"'
                    // no JEE and internal packages
                    filter '-packagename_fq:("java.*" OR "javax.*" OR "com.sun.*" OR "sun.*" OR "org.evosuite.*")'
        
                    // no exceptions
                    filter '-superclass_exact:"java/lang/Exception"'
                    filter '-superclass_exact:"java/lang/Throwable"'
        
                    // -- rule out trivial classes
                    // minimum number of methods
                    filter 'm_static_methods_td:[1 TO *]'
                    // minimum number of branches
                    filter 'm_static_branch_td:[10 TO *]'
        
                    // FIXME remove me, just for debugging
                    //filter 'id:"e6b6ecfa-d5b4-4b8e-a2f6-fc2684bf469a"'
        
                    filter '-groupId:"software.amazon.awssdk"' // large
                }
            }
        
            //-- START alternative impls
        
            // continue with retrieving alternative implementations
        
            /**
             * LOCAL, MASTER
             */
            action(name: "selectAlt", type: 'Select') {
                dependsOn "selectRandom"
                includeAbstractions 'Random'
        
                execute() {
                    List refImpls = abstractions['Random'].implementations
                    refImpls.each { impl ->
                        abstraction(impl) { // by example
                            queryByExample impl, 'class', false
                            rows = altImpls
        
                            directly = true
        
                            dropPojo = true // drop Pojos
        
                            collapseBy("hash")
                            useAlternatives = false
        
                            // EvoSuite is compatible up to Java 8 (i.e bytecode version 52)
                            filter 'bytecodeversion_i:[* TO 52]' // FIXME
                            // visible classes only
                            // CLASS must be visible
                            filter '-keyword_ss:("private" OR "abstract")' // non abstract
        
                            // not from tests classifier
                            filter '-classifier_s:"tests"'
                            // no JUnit3 tests
                            filter '-dep_exact:"junit/framework/TestCase"'
                            // no JUnit4 tests
                            filter '-dep_exact:"org/junit/Test"'
                            // no JUnit5 tests
                            filter '-dep_exact:"org/junit/jupiter/api/Test"'
                            // no TestNG
                            filter '-dep_exact:"org/testng/annotations/Test"'
                            // no JEE and internal packages
                            filter '-packagename_fq:("java.*" OR "javax.*" OR "com.sun.*" OR "sun.*" OR "org.evosuite.*")'
        
                            // -- rule out trivial classes
                            // minimum number of methods
                            filter 'm_static_methods_td:[1 TO *]'
                            // minimum number of branches
                            filter 'm_static_branch_td:[1 TO *]'
        
                            // ignore impls which break EvoSuite
                            filter '!id:"5bce88a7-b927-422b-8d10-754e418309e5"'
        
                            filter '-groupId:"software.amazon.awssdk"' // large
                        }
                    }
                }
            }
        
            /**
             * LOCAL, MASTER
             */
            action(name: "clonesAlt", type: 'Nicad6') {
                cloneType = "type2" // see nicad "config/" for possible clone types (1-3)
                collapseClones = true // collapse (i.e remove)
                refActionRef = "selectRandom" // also drop clones of reference implementation
        
                dependsOn "selectAlt" // mandatory
                includeAbstractions '*-*'
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

        // assertions
        verifyAbstraction(lslExecutionContext, 'selectRandom', 'Random', 10)
    }
}
