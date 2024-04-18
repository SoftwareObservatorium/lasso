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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.gai.parser;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 *
 * @author Marcus Kessel
 */
public class TestParserTest {

    @Test
    public void testParentheses() {
        String content = "Here are some complex inputs for testing the function:\n" +
                "\n" +
                "(10, 20) // sum of two positive numbers\n" +
                "(-5, 5) // sum of a negative and positive number\n" +
                "(0, 0) // sum of zero with itself\n" +
                "(-3, -7) // sum of two negative numbers\n" +
                "(8, 0) // sum of a positive and zero";

        TestParser contentParser = new TestParser();

        List<String> matches = contentParser.extractTests(content);

        assertEquals(5, matches.size());

        assertIterableEquals(Arrays.asList("(10, 20)", "(-5, 5)", "(0, 0)", "(-3, -7)", "(8, 0)"), matches);
    }
}
