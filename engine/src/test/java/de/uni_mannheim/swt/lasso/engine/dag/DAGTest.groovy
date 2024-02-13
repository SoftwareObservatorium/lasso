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
package de.uni_mannheim.swt.lasso.engine.dag

import com.fasterxml.jackson.databind.ObjectMapper
import de.uni_mannheim.swt.lasso.engine.LassoEngine
import de.uni_mannheim.swt.lasso.engine.dag.model.LGraph
import de.uni_mannheim.swt.lasso.lsl.LSLDelegatingScript
import de.uni_mannheim.swt.lasso.lsl.LSLRunner
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * @author Marcus Kessel
 */
class DAGTest {

    @Test
    void test() {
        @Language("Groovy")
        String content = '''
        dataSource 'mavenCentral2023' // select from given data source
        /* define new analysis pipeline */
        study(name:'Mine-Base64') {
            /* selects a given stack implementation s */
            action(name:'select', type:'Select') {
                abstraction('Base64') {
                    queryForClasses "*:*"
                    filter 'id:"83640d88-c64a-4013-92b9-c630ea837b00"'
                }
            }
            /* defines an execution profile for the arena */
            profile('myProfile') {
                scope('class') { type = 'class' } // measurement scope
                environment('java11') { // execution environment
                    image = 'maven:3.6.3-openjdk-11' // (docker) image template
                }
            }
            /* mine test sequences */
            action(name:'execute',type:'Mine') {        
                dependsOn 'select'
                includeAbstractions 'Base64'
                profile('myProfile')
            }
        }
        '''

        LSLRunner runner = new LSLRunner()

        LSLDelegatingScript script = runner.runScript(content, new SimpleLogger())

        ExecutionPlan executionPlan = LassoEngine.createActionExecutionPlan(script.lasso)

        LGraph graph = DAG.writeGraphToModel(executionPlan)

        println(new ObjectMapper().writeValueAsString(graph))
    }

    void toJSON() {

    }
}
