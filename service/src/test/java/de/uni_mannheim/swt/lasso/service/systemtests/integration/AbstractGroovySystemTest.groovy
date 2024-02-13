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

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext
import de.uni_mannheim.swt.lasso.engine.LSLScript
import de.uni_mannheim.swt.lasso.core.model.Systems
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec
import de.uni_mannheim.swt.lasso.service.systemtests.AbstractDistributedSystemTest
import org.apache.commons.collections4.CollectionUtils

/**
 * Abstract system tests for groovy LSL tests
 *
 * @author mkessel
 */
class AbstractGroovySystemTest extends AbstractDistributedSystemTest {

    LSLScript createScript(String content) {
        String executionId = UUID.randomUUID().toString();

        //
        LSLScript scriptUnderTest = new LSLScript()
        scriptUnderTest.setLogger(new SimpleLogger())
        scriptUnderTest.setContent(content)
        scriptUnderTest.setExecutionId(executionId)

        return scriptUnderTest
    }

    void verifyAbstraction(LSLExecutionContext lslExecutionContext, String actionName, String abstractionName, int expectedSize) {
        // verify in-memory model of select action
        AbstractionSpec abstractionSpec = lslExecutionContext.lassoContext.
                actionContainerSpec.actions[actionName].abstractions[abstractionName]
        assert abstractionSpec.implementations.size() == expectedSize

        // verify in-memory cache (Ignite)
        Systems executables = lslExecutionContext.getLassoOperations()
                .getExecutables(lslExecutionContext.executionId, abstractionName, actionName)
        assert executables.getExecutables().size() == expectedSize

        // must be in sync
        assert CollectionUtils.isEqualCollection(abstractionSpec.implementations, executables.getExecutables())
    }
}
