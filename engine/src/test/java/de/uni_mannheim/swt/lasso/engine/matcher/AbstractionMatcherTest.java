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
package de.uni_mannheim.swt.lasso.engine.matcher;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class AbstractionMatcherTest {

    @Test
    public void testMatch() {
        AbstractionMatcher abstractionMatcher = new AbstractionMatcher();

        assertTrue(abstractionMatcher.match("*", abstraction("test")));
        assertFalse(abstractionMatcher.match("test", abstraction("notest")));

        assertTrue(abstractionMatcher.match("*-*", abstraction("4128ac81-2a90-46bc-a22c-6c9f557730b6")));
    }

    @Test
    public void testAnyMatch() {
        AbstractionMatcher abstractionMatcher = new AbstractionMatcher();

        assertTrue(abstractionMatcher.match("*,bla", abstraction("test")));
        assertTrue(abstractionMatcher.match("test,notest", abstraction("notest")));
        assertFalse(abstractionMatcher.match("test,notest", abstraction("bla")));
    }


    private static Abstraction abstraction(String name) {
        Abstraction abstraction = new Abstraction();
        abstraction.setName(name);

        return abstraction;
    }
}
