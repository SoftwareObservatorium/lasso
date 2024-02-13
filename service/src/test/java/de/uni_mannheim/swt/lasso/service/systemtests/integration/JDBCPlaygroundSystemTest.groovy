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
import de.uni_mannheim.swt.lasso.srm.JDBC
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Playground for JDBC (debugging purposes)
 *
 * @author mkessel
 */
class JDBCPlaygroundSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_DEBUG_SCHEMA() throws IOException, DataSourceNotFoundException {
        JDBC jdbc = new JDBC();

        Table schemaTable = jdbc.sqlToTable("SELECT * FROM INFORMATION_SCHEMA.TABLES");
        System.out.println(schemaTable.printAll());
    }

    /**
     * Arena EXECUTE Sequence task (resembles a test-driven search based on a given sheet)
     *
     * @throws IOException
     * @throws de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
     */
    @Test
    void test_create_CSV() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        // interface of a Stack in LQL notation
        def interfaceSpec = """Stack {
            push(java.lang.Object)->java.lang.Object
            pop()->java.lang.Object
            peek()->java.lang.Object
            size()->int}"""
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses interfaceSpec, 'class'
                    rows = 10
                    
                    directly = true // without cursor functionality
                    
                    // pick a known stack
                    //filter 'id:"4e73bb0d-f01f-43e5-bf46-7ab7870a289f"'
                }
            }
            
            action(name:'execute',type:'Arena') { // execute
                specification = interfaceSpec
                sheets = [
                        sheet1: sheet() {
                            row  '',  'CREATE', 'Stack'
                            row 'hi!',  'push',   'A1',     'hi!'
                            row 'hi!',  'peek',   'A1'
                            row     1,  'size',   'A1'
                            row 'hi!',  'pop',    'A1'
                            row     0,  'size',   'A1'
                        }
                ]
        
                maxPermutations = 1
                task = 'Execute'
                features = ['mutation', 'cc'] // enable mutation and code coverage
        
                exportCsv = true
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                includeTests '*'
                profile {
                    environment('java8') {
                        image = 'openjdk:8-jdk-alpine'
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
        //verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
        //verifyAbstraction(lslExecutionContext, 'execute', 'Stack', 1)

        // TODO verify SRM
        // put
        ClusterEngine clusterEngine = lslExecutionContext.getConfiguration().getService(ClusterEngine.class);

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", lslExecutionContext.getExecutionId());
        System.out.println(table.printAll());

        table.write().csv("/tmp/stack.csv")
    }
}
