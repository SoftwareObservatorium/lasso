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
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Demonstrates querying Lasso Reports and saving new ones.
 *
 * @author mkessel
 */
class LassoReportSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * <pre>
     *     queryReport
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_queryReport_tablesaw() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
                
                whenAbstractionsReady {
                    def stack = abstractions['Stack']
                    // get table and do something
                    // either use SQL for aggregation or use the Table structure
                    Table table = queryReport("select * from SelectReport where action = '${getName()}' and abstraction = '${stack.name}'")
                    log(table?.printAll())
                    double meanScore = table.doubleColumn("SCORE").mean()
                    log("average score is '${meanScore}'")
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from SelectReport where action = 'select' and abstraction = 'Stack'")
        println(table.printAll())

        assert table.size() == 10
    }

    /**
     * <pre>
     *     saveReport
     * </pre>
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_saveReport() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        import tech.tablesaw.api.Table // helps in writing statically typed with suggestions
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
                
                whenAbstractionsReady {
                    def stack = abstractions['Stack']
                    // save
                    List impls = abstractions['Stack'].implementations as List
                    impls.each {impl ->
                        Map myReport = [:]
                        myReport.put("myScoreMetric", impl.code.score)
                        saveReport("MyScoreReport", stack, impl, myReport)
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)

        Table table = lslExecutionContext.getReportOperations().select(scriptUnderTest.getExecutionId(),
                "select * from MyScoreReport where action = 'select' and abstraction = 'Stack'")
        println(table.printAll())

        assert table.size() == 10
    }
}
