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
package de.uni_mannheim.swt.lasso.benchmark;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class ClasspathBenchmarkLoaderTest {

    @Test
    public void testLoad_humaneval() throws IOException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        assertEquals("humaneval-java-reworded", benchmark.getName());
        assertEquals(158, benchmark.getAbstractions().size());
    }

    @Test
    public void testLoad_mbpp() throws IOException {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("mbpp-java-reworded");

        assertEquals("mbpp-java-reworded", benchmark.getName());
        assertEquals(350, benchmark.getAbstractions().size());
    }
}
