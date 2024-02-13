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
 * Demonstrates Merge action
 *
 * @author mkessel
 */
class MergeSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_merge_two_functional_abstractions_no_ref() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select1', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses 'Stack', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
            
            /** Retrieve class implementations */
            action(name: 'select2', type: 'Select') {
                abstraction('List') {
                    queryForClasses 'List', 'concept'
                    rows = 10
                    directly = true // without cursor functionality
                }
            }
            
            action(name:"merge", type:'Merge') {
                from = [
                        'List': "select2"
                ]
                referenceImplementationOnly = false
    
                dependsOn "select1"
                includeAbstractions 'Stack'
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext, 'merge', 'Stack', 20)
    }
}
