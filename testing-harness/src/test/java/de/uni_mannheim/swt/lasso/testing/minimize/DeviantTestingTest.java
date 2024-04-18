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
package de.uni_mannheim.swt.lasso.testing.minimize;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marcus Kessel
 */
public class DeviantTestingTest {

    TestSuiteMinimizer testSuiteMinimizer = new TestSuiteMinimizer();

    @Test
    public void testFindMinimalTestSet() {
        List<TestCase> testCases = new ArrayList<>();

        // 1. we need to identify the code elements - in this case: N versions
        // 2. we apply oracle strategy - e.g. test-based voting - e.g., 80% agree (=TRUE), 20% disagree (=FALSE)
        // in mutation testing, the goal is KILL ALL MUTANTS
        // in deviant testing, the goal is to KILL ALL DEVIANTS (assuming ground truth)
        // which tests kill the most deviants?

        // agree (=FALSE), disagree = deviant (=TRUE)
        testCases.add(new TestCase("T1", new CodeElements(new boolean[]{true, false, false})));
        testCases.add(new TestCase("T2", new CodeElements(new boolean[]{false, true, false})));
        testCases.add(new TestCase("T3", new CodeElements(new boolean[]{false, false, true})));

        MinimalTestSuite minimalTestSuite = testSuiteMinimizer.findMinimalTestSet(testCases, 3);

        assertEquals(3, minimalTestSuite.getSuite().size());
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(0))); // T1
        assertEquals(3, minimalTestSuite.getTotalElements());
        assertEquals(3, minimalTestSuite.getTotalCovered());
        assertEquals(0, minimalTestSuite.getTotalUncovered());
    }

    @Test
    public void testFindMinimalTestSet_paper() {
        List<TestCase> testCases = new ArrayList<>();

        // 1. we need to identify the code elements - in this case: N versions
        // 2. we apply oracle strategy - e.g. test-based voting - e.g., 80% agree (=TRUE), 20% disagree (=FALSE)
        // in mutation testing, the goal is KILL ALL MUTANTS
        // in deviant testing, the goal is to KILL ALL DEVIANTS (assuming ground truth)
        // which tests kill the most deviants?

        // agree (=FALSE), disagree = deviant (=TRUE)
        testCases.add(new TestCase("T1", new CodeElements(new boolean[]{false, true, true, false, false})));
        testCases.add(new TestCase("T2", new CodeElements(new boolean[]{false, false, true, false, false})));
        testCases.add(new TestCase("T3", new CodeElements(new boolean[]{false, false, false, true, false})));

        MinimalTestSuite minimalTestSuite = testSuiteMinimizer.findMinimalTestSet(testCases, 5);

        assertEquals(2, minimalTestSuite.getSuite().size());
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(0))); // T1
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(2))); // T3
        assertEquals(5, minimalTestSuite.getTotalElements());
        assertEquals(3, minimalTestSuite.getTotalCovered());
        assertEquals(2, minimalTestSuite.getTotalUncovered());
    }
}
