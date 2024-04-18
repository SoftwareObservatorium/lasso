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
package de.uni_mannheim.swt.lasso.service.systemtests.integration.current

import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.service.systemtests.integration.AbstractGroovySystemTest
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Demonstrates Select action
 *
 * @author mkessel
 */
class CsvSelectSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_select_csv() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
import de.uni_mannheim.swt.lasso.engine.action.select.data.CsvLoader

dataSource 'lasso_quickstart' // define data source to use

String benchmarkId = "humaneval-java-reworded"
/** Define a new study */
study(name: 'Select') {
    def humanEvalBenchmark = loadBenchmark(benchmarkId)

    /** Retrieve class implementations */
    action(name: 'select', type: 'Select') {
        String csvContents = """"","IMPLEMENTATIONID","CLUSTERID","SYSTEMID","ADAPTERID","ABSTRACTIONID","ISEQ"
"1","4b824c04-1c9f-434f-907f-194f8b79a344_0",18,"4b824c04-1c9f-434f-907f-194f8b79a344","0","HumanEval_13_greatest_common_divisor",TRUE"""

        abstraction('HumanEval_13_greatest_common_divisor', CsvLoader.load(csvContents), humanEvalBenchmark.abstractions['HumanEval_13_greatest_common_divisor'].lql) {
        }
    }
    
    action(name:'debug') {
        dependsOn 'select'
        includeAbstractions '*'
        
        execute {
            println(abstractions['HumanEval_13_greatest_common_divisor'].specification.interfaceSpecification)
        }
    }
}
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest)
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext()
    }
}
