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
package de.uni_mannheim.swt.lasso.datasource.maven.filter;

import de.uni_mannheim.swt.lasso.datasource.expansion.signature.Clazz;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.Signature;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class MethodSignatureFilterIntegrationTest {

    private MethodSignatureFilter methodSignatureFilter = new MethodSignatureFilter();

    @Test
    public void test_String() throws IOException {
        Clazz clazz = new Clazz();
        clazz.setName("MyClass");
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("int", "java.lang.String"));
        signature.setReturnType("java.lang.String");
        clazz.setMethods(Arrays.asList(signature));

        assertTrue(methodSignatureFilter.accept(clazz));
    }

    @Test
    public void test_byte_int() throws IOException {
        Clazz clazz = new Clazz();
        clazz.setName("MyClass");
        Signature signature = new Signature();
        signature.setName("endSliceOf");
        signature.setInputTypes(Arrays.asList("byte[]", "int"));
        signature.setReturnType("byte[]");
        clazz.setMethods(Arrays.asList(signature));

        assertTrue(methodSignatureFilter.accept(clazz));
    }
}
