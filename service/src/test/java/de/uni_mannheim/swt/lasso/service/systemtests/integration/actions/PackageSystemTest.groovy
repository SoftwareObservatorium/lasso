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
 * Demonstrates Compile action
 *
 * @author mkessel
 */
class PackageSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * Just package system artifact.
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_package() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'dummy' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** dummy data source */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    source = """package my.pkg;
                                class Stack {}
                             """
                    className = "Stack"
                    packageName = "my.pkg"
                }
            }
            
            /** defines profile (compiler etc.) */
            profile('java11Profile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            /** Package (compile) source code */
            action(name: 'package', type: 'Package') {
                dependsOn 'select'
                includeAbstractions 'Stack'
                
                profile('java11Profile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
    }

    /**
     * Also deploy system artifact.
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_deploy() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'dummy' // define data source to use
        /** Define a new study */
        study(name: 'Select') {
            /** dummy data source */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    source = """package my.pkg;
                                class Stack {}
                             """
                    className = "Stack"
                    packageName = "my.pkg"
                }
            }
            
            /** defines profile (compiler etc.) */
            profile('java11Profile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            
            /** Package (compile) source code */
            action(name: 'package', type: 'Package') {
                deploy = true // enable automatic deployment
            
                dependsOn 'select'
                includeAbstractions 'Stack'
                
                profile('java11Profile')
            }
        }
        '''

        //
        LSLScript scriptUnderTest = createScript(content)

        // DO EXECUTE
        LSLExecutionResult lslExecutionResult = lassoEngine.execute(scriptUnderTest);
        LSLExecutionContext lslExecutionContext = lassoEngine.getLastContext();

        // assertions
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
    }
}
