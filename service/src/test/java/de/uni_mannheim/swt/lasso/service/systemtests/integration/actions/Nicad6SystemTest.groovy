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
 * Demonstrates Nicad6 action
 *
 * @author mkessel
 */
class Nicad6SystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_drop_stack() throws IOException, DataSourceNotFoundException {
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
                    queryForClasses interfaceSpec // query for classes by interface specification
                    rows = 10
                    
                    directly = true // without cursor functionality

                    excludeClassesByKeywords(['private', 'abstract'])
                    excludeTestClasses()
                    excludeInternalPkgs()
                    // optionally, we don't want it to be a collection
                    //excludeSuperClass("java.util.Collection", true)
                    // non empty classes, i.e having complexity > 1
                    filter 'm_static_complexity_td:[1 TO *]'
                }
            }
            
            /** Drop Code Clones */
            action(name: 'clones', type: 'Nicad6') {
                cloneType = "type2"
        
                dependsOn 'select'
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 10)
    }
}
