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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signature tests.
 *
 * @author Marcus Kessel
 */
public class SignatureFilterTest {

    SignatureFilter signatureFilter = new SignatureFilter();

    @Test
    public void test1() {
        assertTrue(signatureFilter.accept(create(Arrays.asList("java.lang.Integer", "double"), "int")));
        assertFalse(signatureFilter.accept(create(Arrays.asList("java.lang.Integer", "double"), "void")));
        assertTrue(signatureFilter.accept(create(Arrays.asList("double[]"), "int")));
        assertFalse(signatureFilter.accept(create(Arrays.asList("java.lang.Object"), "java.lang.String")));
    }

    static Signature create(List<String> inputTypes, String returnType) {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(inputTypes);
        signature.setReturnType(returnType);

        return signature;
    }
}
