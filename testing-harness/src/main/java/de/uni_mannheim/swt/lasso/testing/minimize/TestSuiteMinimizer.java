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

import java.util.*;

/**
 * Greedy algorithm for test suite minimization based on coverage criteria.
 *
 * @author Marcus Kessel
 */
public class TestSuiteMinimizer {

    /**
     * Find a minimal test set in a greedy manner (find {@link TestCase} with highest coverage first etc.).
     * If there is more than one best {@link TestCase}, it simply takes the first one observed.
     *
     * This algorithm works for any coverage idea that is
     * encoded in {@link BitSet} in {@link CodeElements#getElements()}.
     *
     *
     *
     * @param testCases
     * @param noOfCodeElements
     * @return
     */
    public MinimalTestSuite findMinimalTestSet(List<TestCase> testCases, int noOfCodeElements) {
        // initialize data structures
        BitSet codeElementsRemaining = new BitSet(noOfCodeElements);
        // all true
        codeElementsRemaining.flip(0, noOfCodeElements);

        BitSet coveredElements = new BitSet(noOfCodeElements);
        List<TestCase> selectedTests = new ArrayList<>();

        // iterate until all code elements are covered
        while (!codeElementsRemaining.isEmpty()) {
            TestCase bestTest = null;
            int maxCoverage = 0;

            // find the test case that covers the most uncovered code elements
            for (TestCase testCase : testCases) {
                BitSet tcBits = (BitSet) testCase.getCoveredCodeElements().getElements().clone();
                tcBits.and(codeElementsRemaining);
                int coverage = tcBits.cardinality();

                if (coverage > maxCoverage) {
                    bestTest = testCase;
                    maxCoverage = coverage;
                }
            }

            if(bestTest != null) {
                // add the selected test case to the list of selected tests
                if(!selectedTests.contains(bestTest)) {
                    selectedTests.add(bestTest);
                }

                // update the covered elements set
                coveredElements.or(bestTest.getCoveredCodeElements().getElements());

                // remove the covered code elements from the list of uncovered code elements
                codeElementsRemaining.andNot(bestTest.getCoveredCodeElements().getElements());
            } else {
                // finished (some uncovered code elements remain)
                break;
            }
        }

        MinimalTestSuite minimalTestSuite = new MinimalTestSuite();
        minimalTestSuite.setSuite(selectedTests);
        minimalTestSuite.setCoveredElements(coveredElements);
        minimalTestSuite.setTotalElements(noOfCodeElements);

        return minimalTestSuite;
    }
}

