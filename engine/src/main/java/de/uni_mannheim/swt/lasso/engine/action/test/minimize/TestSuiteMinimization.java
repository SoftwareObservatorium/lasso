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
package de.uni_mannheim.swt.lasso.engine.action.test.minimize;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;

import de.uni_mannheim.swt.lasso.testing.minimize.CodeElements;
import de.uni_mannheim.swt.lasso.testing.minimize.MinimalTestSuite;
import de.uni_mannheim.swt.lasso.testing.minimize.TestCase;
import de.uni_mannheim.swt.lasso.testing.minimize.TestSuiteMinimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * See {@link de.uni_mannheim.swt.lasso.testing.minimize.TestSuiteMinimizer}.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Minimize test suites based on coverage data")
@Stable
@Local
public class TestSuiteMinimization extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(TestSuiteMinimization.class);

    // FIXME
    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        LOG.info("Abstraction = {}", abstraction.getName());
        LOG.info("Systems = {}", abstraction.getImplementations().size());
        for(System implementation : abstraction.getImplementations()) {
            LOG.info(">> System = {}, {}", implementation.getId(), implementation.getCode().toFQName());
        }

        setExecutables(Systems.fromAbstraction(abstraction, getName()));

        // FIXME implement
        TestSuiteMinimizer testSuiteMinimizer = new TestSuiteMinimizer();
        List<TestCase> testCases = new ArrayList<>();

        // obtain from SRM? or do directly in arena?
        // FIXME see de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco.TestSuiteMinimizationIntegrationTest
        testCases.add(new TestCase("T1", new CodeElements(new boolean[]{true, true, true, false, false, false})));
        testCases.add(new TestCase("T2", new CodeElements(new boolean[]{false, true, true, true, false, false})));
        testCases.add(new TestCase("T3", new CodeElements(new boolean[]{true, false, false, true, true, false})));
        testCases.add(new TestCase("T4", new CodeElements(new boolean[]{false, false, false, true, true, true})));

        MinimalTestSuite minimalTestSuite = testSuiteMinimizer.findMinimalTestSet(testCases, 6);
    }
}
