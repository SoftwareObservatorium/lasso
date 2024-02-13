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
 * Demonstrates Test action
 *
 * @author mkessel
 */
class TestSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    /**
     * Surefire test
     *
     * @throws IOException
     * @throws DataSourceNotFoundException
     */
    @Test
    void test_class_stack() throws IOException, DataSourceNotFoundException {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2020' // define data source to use
        def stackMap = [:]
        stackMap.put('6ce338e3-3c3c-4f52-b595-9b3ed5bb4025', """
        package edu.mines.jtk.sgl;
        import org.junit.Test;
        import static org.junit.Assert.*;
        
        public class StackTest {
            @Test
            public void test() throws Throwable {
                ArrayStack<String> stack = new ArrayStack<String>();
                String input = "hi!";
                String str2 = stack.push(input);
                String str3 = stack.peek();
                int int4 = stack.size();
                String str5 = stack.pop();
                int int6 = stack.size();
                assertEquals(str2, "hi!");
                assertEquals(str3, "hi!");
                assertTrue(int4 == 1);
                assertEquals(str5, "hi!");
                assertTrue(int6 == 0);
            }
        }
        """
        )
        /** Define a new study */
        study(name: 'Select') {
            /** Retrieve class implementations */
            action(name: 'select', type: 'Select') {
                abstraction('Stack') {
                    queryForClasses '*:*', 'concept'
                    rows = 1
                    
                    directly = true // without cursor functionality
                    
                    // pick a known stack
                    filter 'id:"6ce338e3-3c3c-4f52-b595-9b3ed5bb4025"'
                }
            }
            
            action(name:'test',type:'Test') { //
                testClasses = stackMap
        
                dependsOn 'select'
                includeAbstractions 'Stack'
                includeTests '*'
                profile {
                    environment('java8') {
                        image = 'maven:3.5.4-jdk-8-alpine'
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
        verifyAbstraction(lslExecutionContext, 'select', 'Stack', 1)
        verifyAbstraction(lslExecutionContext, 'test', 'Stack', 1)
    }
}
