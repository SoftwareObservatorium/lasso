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
public class MutationTestingTest {

    TestSuiteMinimizer testSuiteMinimizer = new TestSuiteMinimizer();

    @Test
    public void testFindMinimalTestSet() {
        List<TestCase> testCases = new ArrayList<>();

        // array contains 6 mutants, TRUE = killed, FALSE, not killed
        testCases.add(new TestCase("T1", new CodeElements(new boolean[]{true, true, true, false, false, false})));
        testCases.add(new TestCase("T2", new CodeElements(new boolean[]{false, true, true, true, false, false})));
        testCases.add(new TestCase("T3", new CodeElements(new boolean[]{true, false, false, true, true, false})));
        testCases.add(new TestCase("T4", new CodeElements(new boolean[]{false, false, false, true, true, true})));

        MinimalTestSuite minimalTestSuite = testSuiteMinimizer.findMinimalTestSet(testCases, 6);

        assertEquals(2, minimalTestSuite.getSuite().size());
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(0))); // T1
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(3))); // T4
        assertEquals(6, minimalTestSuite.getTotalElements());
        assertEquals(6, minimalTestSuite.getTotalCovered());
        assertEquals(0, minimalTestSuite.getTotalUncovered());
    }
}
