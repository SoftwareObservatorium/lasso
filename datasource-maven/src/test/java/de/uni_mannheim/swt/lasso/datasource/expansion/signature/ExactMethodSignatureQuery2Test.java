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

import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.CandidateQuery;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Marcus Kessel
 */
public class ExactMethodSignatureQuery2Test {

    @Test
    public void test_ExactMethodSignatureQuery_hlechte() throws IOException {
        QueryBuilder queryBuilder = new QueryBuilder();

        Clazz clazz = new Clazz();
        clazz.setName("Util");
        Signature signature = new Signature();
        signature.setName("md5");
        signature.setInputTypes(Arrays.asList("java.lang.String"));
        signature.setReturnType("java.lang.String");
        clazz.setMethods(Arrays.asList(signature));

        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(ExactMethodSignatureQuery2.class);
        CandidateQuery candidateQuery = queryBuilder.build(clazz.toMQL(true), Arrays.asList("field1:lala"), searchOptions);
    }

    @Test
    public void test_ExactMethodSignatureQuery_hlechte_2() throws IOException {
        QueryBuilder queryBuilder = new QueryBuilder();

        Clazz clazz = new Clazz();
        clazz.setName("PasswordUtil");
        Signature signature = new Signature();
        signature.setName("createMd5Hash");
        signature.setInputTypes(Arrays.asList("java.lang.String"));
        signature.setReturnType("java.lang.String");
        clazz.setMethods(Arrays.asList(signature));

        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(ExactMethodSignatureQuery2.class);
        CandidateQuery candidateQuery = queryBuilder.build(clazz.toMQL(true), Arrays.asList("field1:lala"), searchOptions);
    }

    @Test
    public void test_ExactMethodSignatureQuery() throws IOException {
        QueryBuilder queryBuilder = new QueryBuilder();

        Clazz clazz = new Clazz();
        clazz.setName("Base64Utils");
        Signature signature = new Signature();
        signature.setName("decode");
        signature.setInputTypes(Arrays.asList("byte[]"));
        signature.setReturnType("byte[]");
        clazz.setMethods(Arrays.asList(signature));

        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(ExactMethodSignatureQuery2.class);
        CandidateQuery candidateQuery = queryBuilder.build(clazz.toMQL(true), Arrays.asList("field1:lala"), searchOptions);
    }

    @Test
    public void test_ExactMethodSignatureQuery_endSliceOf() throws IOException {
        QueryBuilder queryBuilder = new QueryBuilder();

        Clazz clazz = new Clazz();
        clazz.setName("MyClass");
        Signature signature = new Signature();
        signature.setName("endSliceOf");
        signature.setInputTypes(Arrays.asList("byte[]", "int"));
        signature.setReturnType("byte[]");
        clazz.setMethods(Arrays.asList(signature));

        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(ExactMethodSignatureQuery2.class);
        CandidateQuery candidateQuery = queryBuilder.build(clazz.toMQL(true), Arrays.asList("field1:lala"), searchOptions);
    }

    @Test
    public void test_ExactMethodSignatureQuery_TooManyClauses() throws IOException {
        QueryBuilder queryBuilder = new QueryBuilder();

        Clazz clazz = new Clazz();
        clazz.setName("MyClass");
        Signature signature = new Signature();
        signature.setName("read");
        signature.setInputTypes(Arrays.asList("byte[]","int","int"));
        signature.setReturnType("int");
        clazz.setMethods(Arrays.asList(signature));

        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(ExactMethodSignatureQuery2.class);
        CandidateQuery candidateQuery = queryBuilder.build(clazz.toMQL(true), Arrays.asList("field1:lala"), searchOptions);

        System.out.println(candidateQuery.getLuceneQuery().toString());
    }
}
