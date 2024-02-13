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
package de.uni_mannheim.swt.lasso.arena.sequence.eval;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.classloader.Containers;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class EvalTest {

    @Test
    public void test_arr_init() throws DuplicateRealmException {
        String arrInit = "float float1 = 1.0f\n" +
                "float float2 = 2.0f\n" +
                "float float3 = 3.0f\n" +
                "float float4 = 4.0f\n" +
                "float float5 = 5.0f\n" +
                "return new float[] { float1, float2, float3, float4, float5 }";

        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f}, (float[]) eval.exprRaw(arrInit));
    }

    @Test
    public void test_unary() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertEquals(2, eval.expr("1+1"));
    }

    @Test
    public void test_weird_string() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertEquals("Pinh+$z$/<).)5f~", eval.expr("\"Pinh+$z$/<).)5f~\""));
        assertEquals("Pinh+$z$/<).)5f~'", eval.expr("\"Pinh+$z$/<).)5f~'\""));
    }

    @Test
    public void test_arr() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertArrayEquals(new int[]{1,2}, (int[]) eval.expr("new int[]{1,2}"));
        assertArrayEquals(new double[6], (double[]) eval.expr("new double[6]"));

        // FIXME short-hand notation currently unsupported
        //assertArrayEquals(new int[]{1,2}, (int[]) eval.expr("{1,2}"));
    }

    @Test
    public void test_multi_arr() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        System.out.println(eval.expr("(int[][]) [[1],[2]]"));

        System.out.println(eval.expr("new int[1][1]"));

        // null init
        assertArrayEquals(new int[1][1], (int[][]) eval.expr("new int[1][1]"));

        // init with values
        assertArrayEquals(new int[][]{{1},{2}}, (int[][]) eval.expr("new int[][]{{1},{2}}"));
    }

    @Test
    public void testDouble() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertEquals(2426.846650691062, ((BigDecimal)eval.expr("2426.846650691062")).doubleValue(), 0d);
    }

    @Test
    public void testInt() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertEquals(2426, eval.expr("2426"));
    }

    @Test
    public void test_constants() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertEquals(Math.PI, eval.expr("Math.PI"));
    }

    @Test
    public void test_unknown() throws DuplicateRealmException {
        Containers containers = new Containers();
        Container container = containers.newContainer("mytestcontainer");

        Eval eval = new Eval(container);

        assertThrows(groovy.lang.MissingPropertyException.class, () -> {
            eval.expr("blub");
        });
    }
}
