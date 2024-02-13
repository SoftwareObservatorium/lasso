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
import static org.junit.Assert.assertThat;

/**
 * Signature tests.
 *
 * @author Marcus Kessel
 */
public class SignatureTest {

    @Test
    public void test_permutations() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("int", "java.lang.String"));
        signature.setReturnType("java.lang.String");

        //assertThat(signature.toMQL(true), is("doSomething(java.lang.Integer,double):List"));

        List<Signature> signatureList = SignatureUtils.createPermutations(signature);

        signatureList.forEach(s -> {
            StringBuilder sb = new StringBuilder();
            sb.append("methodSignatureParamsOrderedSyntaxFq_ssigs_sexact")
                    .append(":")
                    .append("rv_")
                    .append(s.getReturnType().toLowerCase());

            s.getInputTypes().stream().forEach(p -> {
                sb.append(";")
                        .append("pt_")
                        .append(p.toLowerCase());
            });

            System.out.println(sb.toString());
        });
    }

    @Test
    public void test_fqname_unknown_pkg() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("java.lang.Integer", "double"));
        signature.setReturnType(Signature.UNRESOLVED_PKG + ".List");

        assertThat(signature.toMQL(true), is("doSomething(java.lang.Integer,double):List"));
    }

    @Test
    public void test_fqname() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("java.lang.Integer", "double"));
        signature.setReturnType("java.util.List");

        assertThat(signature.toMQL(true), is("doSomething(java.lang.Integer,double):java.util.List"));
    }

    @Test
    public void test_simplename() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("int", "double"));
        signature.setReturnType("java.util.List");

        assertThat(signature.toMQL(false), is("doSomething(int,double):List"));
    }

    @Test
    public void test_simplename_generics() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("int", "double"));
        signature.setReturnType("java.util.List<String>");

        assertThat(signature.toMQL(false), is("doSomething(int,double):List"));
    }

    @Test
    public void test_constructor_init() {
        Clazz clazz = new Clazz();
        clazz.setName("MyClass");
        Signature signature = new Signature();
        signature.setName("<init>");
        signature.setInputTypes(Arrays.asList("int", "double"));
        signature.setReturnType("void");

        clazz.setConstructors(Arrays.asList(signature));

        assertThat(clazz.toMQL(false), is("MyClass(\nMyClass(int,double):void;\n)"));
    }

    @Test
    public void test_simplename_generics2() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("int", "double"));
        signature.setReturnType("java.util.Map<String, List<Double>>");

        assertThat(signature.toMQL(false), is("doSomething(int,double):Map"));
    }

    @Test
    public void test_unresolved() {
        Signature signature = new Signature();
        signature.setName("doSomething");
        signature.setInputTypes(Arrays.asList("int", null));
        signature.setReturnType("java.util.List");

        assertThat(signature.toMQL(false), is("doSomething(int,Object):List"));
    }
}
