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

import de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature;
import org.apache.lucene.search.BooleanQuery;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author Marcus Kessel
 */
public class SignatureExpansionTest {

    @Test
    public void test_bytes() {
        SignatureExpansion queryExpansion = new SignatureExpansion();

        List<Class> mapping = queryExpansion.getMapping(byte[].class);

        System.out.println(mapping.stream().map(Class::getCanonicalName).collect(Collectors.joining("\n")));

        assertThat(mapping,
                containsInAnyOrder(
                        Byte[].class,
                        String.class,
                        CharSequence.class,
                        char[].class,
                        Character[].class));
    }

    @Test
    public void test_number() {
        SignatureExpansion queryExpansion = new SignatureExpansion();

        List<Class> mapping = queryExpansion.getMapping(int.class);

        System.out.println(mapping.stream().map(Class::getCanonicalName).collect(Collectors.joining("\n")));

        assertThat(mapping,
                containsInAnyOrder(
                        Integer.class,
                        long.class,
                        float.class,
                        double.class,
                        Long.class,
                        Float.class,
                        Double.class));
    }

    @Test
    public void test_int() {
        SignatureExpansion queryExpansion = new SignatureExpansion();

        MethodSignature methodSignature = new MethodSignature("public",
                "size",
                Collections.emptyList(),
                "int");

        List<MethodSignature> expansions = queryExpansion.expand(methodSignature);

        for(MethodSignature m : expansions) {
            System.out.println(m);
        }

        System.out.println(int.class.getCanonicalName());
    }

    @Test
    public void test_int_relax() {
        SignatureExpansion queryExpansion = new SignatureExpansion();

        List<List<Class>> expansions = queryExpansion.expand(new ArrayList<>(Arrays.asList("int")));

        BooleanQuery.Builder expBool = new BooleanQuery.Builder();
        for(List<Class> l : expansions) {
            System.out.println(l.get(0).getCanonicalName());
        }
    }
}
