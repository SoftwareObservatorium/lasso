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
package de.uni_mannheim.swt.lasso.service.systemtests.integration.current

import de.uni_mannheim.swt.lasso.benchmark.Benchmark
import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertFalse

/**
 * Demonstrates GAITestGen action.
 *
 * @author mkessel
 */
class AllBenchmarksTest {

    @Test
    void testBenchmarks() {
        BenchmarkManager benchmarkManager = new BenchmarkManager();
        Benchmark benchmark = benchmarkManager.load("humaneval-java-reworded")
        println(benchmark.problems.keySet())

        println(benchmark.problems['HumanEval_24_largest_divisor'].getDescription())
        println(benchmark.problems['HumanEval_24_largest_divisor'].getLql())
    }

}
