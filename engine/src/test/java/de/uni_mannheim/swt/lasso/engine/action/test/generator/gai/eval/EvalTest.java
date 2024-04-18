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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.gai.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class EvalTest {

    @Test
    public void test_arr_init() {
        String arrInit = "float float1 = 1.0f\n" +
                "float float2 = 2.0f\n" +
                "float float3 = 3.0f\n" +
                "float float4 = 4.0f\n" +
                "float float5 = 5.0f\n" +
                "return new float[] { float1, float2, float3, float4, float5 }";

        Eval eval = new Eval();

        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f}, (float[]) eval.exprRaw(arrInit));
    }
}
