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
package de.uni_mannheim.swt.lasso.llm.problem;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 *
 * @author Marcus Kessel
 */
public class MultiPLETest {

    @Test
    public void testCreateContainer() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        assertNotNull(container);
    }

    @Test
    public void testHumanEvalProblems() throws IOException {
        MultiPLE multiple = new MultiPLE();
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        for(Problem problem : problemsList) {
            System.out.println(ToStringBuilder.reflectionToString(problem));
        }

        System.out.println(problemsList.size());
    }

    @Test
    public void testMap() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/humaneval-java-reworded.json"));

        System.out.println(ToStringBuilder.reflectionToString(problemsMap.get("HumanEval_13_greatest_common_divisor")));
    }

    @Test
    public void testMBPPProblems() throws IOException {
        MultiPLE multiple = new MultiPLE();
        List<Problem> problemsList = multiple.getProblems("/problems/mbpp-java-reworded.json");

        for(Problem problem : problemsList) {
            System.out.println(ToStringBuilder.reflectionToString(problem));
        }

        System.out.println(problemsList.size());
    }

    @Test
    public void testParseProblem_javatuples_Pair() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/mbpp-java-reworded.json"));

        System.out.println(ToStringBuilder.reflectionToString(problemsMap.get("mbpp_75_find_tuples")));
    }
}
