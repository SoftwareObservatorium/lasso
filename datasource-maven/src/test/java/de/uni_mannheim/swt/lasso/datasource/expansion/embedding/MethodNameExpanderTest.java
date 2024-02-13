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
package de.uni_mannheim.swt.lasso.datasource.expansion.embedding;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class MethodNameExpanderTest {

    static {
        System.setProperty("models.embedding.code2vec", "/home/marcus/Downloads/target_vecs.txt");
    }

    @Test
    public void test_encode() {
        String methodName = "encode";
        LinkedHashMap<String, Double> methods = MethodNameExpander.getInstance().getNearestMethodNames(methodName, 10);

        System.out.println(methods);

        assertTrue(methods.size() > 0);
        assertFalse(methods.containsKey(methodName));
    }

    @Test
    public void test_toLower() {
        String methodName = "toLower";
        LinkedHashMap<String, Double> methods = MethodNameExpander.getInstance().getNearestMethodNames(methodName, 10);

        System.out.println(methods);

        assertTrue(methods.size() > 0);
        assertFalse(methods.containsKey(methodName));
    }
}
