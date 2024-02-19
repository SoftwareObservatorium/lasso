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
package de.uni_mannheim.swt.lasso.srm.operators;

import joinery.DataFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marcus Kessel
 */
public class HeuristicsBasedCorrectnessTest {

    static DataFrame srm3 = null;

    @BeforeAll
    static void init() {
        srm3 = new DataFrame<>("STATEMENT", "SYSTEMID", "VALUE");
        srm3.append(Arrays.asList("sheet1@0,1", "system1", "[117,115,101,114,58,112,97,115,115]"));
        srm3.append(Arrays.asList("sheet1@0,0", "system1", "_INSTANCE_"));
        srm3.append(Arrays.asList("sheet1@0,1", "system2", "[1,2,3]"));
        srm3.append(Arrays.asList("sheet1@0,0", "system2", "_INSTANCE_"));
        srm3.append(Arrays.asList("sheet1@0,1", "system3", "[117,115,101,114,58,112,97,115,115]"));
        srm3.append(Arrays.asList("sheet1@0,0", "system3", "_INSTANCE_"));
    }

    @Test
    public void testAssertStringEqualsList() {
        DataFrame wide = srm3.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");
        System.out.println(wide.toString());

        HeuristicsBasedCorrectness correctness = new HeuristicsBasedCorrectness();
        Similarity sim1 = correctness.assertStringEquals(wide.col("STATEMENT"), wide.col("system1"), wide.col("system2"));

        System.out.println(wide.col("STATEMENT").size());

        assertEquals(2, sim1.getTotal());
        assertEquals(1, sim1.getMatches());
        assertEquals(0.5d, sim1.getSimilarity());
        assertFalse(sim1.isEquivalent());

        Similarity sim2 = correctness.assertStringEquals(wide.col("STATEMENT"), wide.col("system1"), wide.col("system3"));

        assertEquals(2, sim2.getTotal());
        assertEquals(2, sim2.getMatches());
        assertEquals(1d, sim2.getSimilarity());
        assertTrue(sim2.isEquivalent());
    }

    @Test
    public void testAssertStringEquals() {
        HeuristicsBasedCorrectness correctness = new HeuristicsBasedCorrectness();

        assertTrue(correctness.assertStringEquals("sheet1@0,0",
                HeuristicsBasedCorrectness._INSTANCE_,  HeuristicsBasedCorrectness._INSTANCE_));
        assertTrue(correctness.assertStringEquals("sheet1@0,0",
                HeuristicsBasedCorrectness._NA_,  HeuristicsBasedCorrectness._INSTANCE_));
        assertTrue(correctness.assertStringEquals("sheet1@0,0",
                HeuristicsBasedCorrectness._NA_,  HeuristicsBasedCorrectness._NA_));
    }

    @Test
    public void testAssertObjectEqualsList() {
        DataFrame wide = srm3.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");
        System.out.println(wide.toString());

        HeuristicsBasedCorrectness correctness = new HeuristicsBasedCorrectness();
        Similarity sim1 = correctness.assertObjectEquals(wide.col("STATEMENT"), wide.col("system1"), wide.col("system2"));

        System.out.println(wide.col("STATEMENT").size());

        assertEquals(2, sim1.getTotal());
        assertEquals(1, sim1.getMatches());
        assertEquals(0.5d, sim1.getSimilarity());
        assertFalse(sim1.isEquivalent());

        Similarity sim2 = correctness.assertObjectEquals(wide.col("STATEMENT"), wide.col("system1"), wide.col("system3"));

        assertEquals(2, sim2.getTotal());
        assertEquals(2, sim2.getMatches());
        assertEquals(1d, sim2.getSimilarity());
        assertTrue(sim2.isEquivalent());
    }

    @Test
    public void testAssertObjectEquals() {
        HeuristicsBasedCorrectness correctness = new HeuristicsBasedCorrectness();

        assertTrue(correctness.assertObjectEquals("sheet1@0,0",
                HeuristicsBasedCorrectness._INSTANCE_,  HeuristicsBasedCorrectness._INSTANCE_));
        assertTrue(correctness.assertObjectEquals("sheet1@0,0",
                HeuristicsBasedCorrectness._NA_,  HeuristicsBasedCorrectness._INSTANCE_));
        assertTrue(correctness.assertObjectEquals("sheet1@0,0",
                HeuristicsBasedCorrectness._NA_,  HeuristicsBasedCorrectness._NA_));
    }
}
