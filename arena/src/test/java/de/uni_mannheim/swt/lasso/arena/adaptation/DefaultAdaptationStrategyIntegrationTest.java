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
package de.uni_mannheim.swt.lasso.arena.adaptation;

import com.google.gson.GsonBuilder;
import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.core.adapter.AdapterDesc;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Note: Contains integration tests that rely on internal services.
 *
 * @author Marcus Kessel
 */
public class DefaultAdaptationStrategyIntegrationTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);
    CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

    // -- NEW

    @Test
    public void test_subset_ng() throws IOException, ParseException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "isEmpty()->boolean\n" + // does not exist .. but mapped to others
                "}");

        ClassUnderTest classUnderTest = codeSearch.queryForClass("a1d09619-dbe0-401c-a147-efe9049dc522");
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

        int limitPerms = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(parseResults.get(0), classUnderTest, limitPerms);

        assertEquals(1, adaptedImplementations.size());

        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        System.out.println(adaptedImplementation);

        assertEquals(5, adaptedImplementation.getNoOfMethods());
    }

    @Test
    public void test_missing_constructor() throws IOException, ParseException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("Stack {\n" +
                "Stack(int)\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}");

        ClassUnderTest classUnderTest = codeSearch.queryForClass("a1d09619-dbe0-401c-a147-efe9049dc522");
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

        int limitPerms = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(parseResults.get(0), classUnderTest, limitPerms);

        assertEquals(1, adaptedImplementations.size());

        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        System.out.println(adaptedImplementation);

        assertEquals(4, adaptedImplementation.getNoOfMethods());
    }

    @Test
    public void test_subset_void() throws IOException, ParseException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("Stack {\n" +
                "push(java.lang.Object)->void\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}");

        ClassUnderTest classUnderTest = codeSearch.queryForClass("a1d09619-dbe0-401c-a147-efe9049dc522");
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

        int limitPerms = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(parseResults.get(0), classUnderTest, limitPerms);

        assertEquals(1, adaptedImplementations.size());

        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        System.out.println(adaptedImplementation);

        assertEquals(4, adaptedImplementation.getNoOfMethods());
    }

//    @Test
//    public void test_method_filter() throws IOException {
//        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());
//
//        List<InterfaceSpecification> parseResults = codeSearch2017.fromLQL("Base32 {\n" +
//                "encode(byte[])->java.lang.String\n" +
//                "decode(java.lang.String)->byte[]\n" +
//                "}");
//
//        InterfaceSpecification specification = parseResults.get(0);
//
//        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("ca07ef4f-5576-4e8b-8328-6e7bd648ff03");
//
//        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
//        // automatically resolves project-related artifacts
//        pool.initProjects();
//
//        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
//        // set filter
//        adaptationStrategy.addSpecFilter(new ImplementationUnitFilter());
//
//        int limitPerms = 10;
//        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, classUnderTest, limitPerms);
//
////        //assertEquals(1, adaptedImplementations.size());
////
////        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);
////
////        System.out.println(adaptedImplementation);
////
////        assertEquals(4, adaptedImplementation.getNoOfMethods());
//
//        for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
//            System.out.println(adaptedImplementation.getMethod(specification, 0).getMethod()); // must be encode
//            System.out.println(adaptedImplementation.getMethod(specification, 1).getMethod()); // must be encode as well
//        }
//    }

    @Test
    public void test_subset_many() throws IOException, ParseException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("Stack {\n" +
                "push(java.lang.Object)->void\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}");

        ClassUnderTest classUnderTest = codeSearch.queryForClass("a1d09619-dbe0-401c-a147-efe9049dc522");
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

        int limitPerms = 10;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(parseResults.get(0), classUnderTest, limitPerms);

        System.out.println(adaptedImplementations.size());

        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(parseResults.get(0).toDescription()));

        for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
            System.out.println("----------");
            PermutatorAdaptedImplementation pimpl = (PermutatorAdaptedImplementation) adaptedImplementation;

            System.out.println(pimpl.getAdapter().getNameScore());

            for(int m = 0; m < parseResults.get(0).getConstructors().size(); m++) {
                System.out.println(adaptedImplementation.getInitializer(parseResults.get(0), m).getInitializer());
            }

            for(int m = 0; m < parseResults.get(0).getMethods().size(); m++) {
                System.out.println(adaptedImplementation.getMethod(parseResults.get(0), m).getMethod());
            }

            System.out.println("----------");

            AdapterDesc adapterDesc = ((PermutatorAdaptedImplementation) adaptedImplementation).toDescription(parseResults.get(0));

            System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(adapterDesc));

            System.out.println("----------");
        }
    }

    @Test
    public void testWrappersToPrimitives() {
        Class<?>[] params = new Class<?>[] { Double.class, Boolean.class, Character.class };

        Class<?>[] nParams = DefaultAdaptationStrategy.wrappersToPrimitives(params);

        assertArrayEquals(new Class<?>[] { double.class, boolean.class, char.class }, nParams);
    }

    @Test
    public void testIsAssignable() {
        // primitive to primitive
        assertTrue(DefaultAdaptationStrategy.isAssignable(new Class<?>[] { int.class }, new Class<?>[] { long.class }));
        // wrapper to primitive
        assertTrue(DefaultAdaptationStrategy.isAssignable(new Class<?>[] { Integer.class }, new Class<?>[] { long.class }));
        // primitive to wrapper
        assertTrue(DefaultAdaptationStrategy.isAssignable(new Class<?>[] { int.class }, new Class<?>[] { Long.class }));

        // double to int primitive
        assertFalse(DefaultAdaptationStrategy.isAssignable(new Class<?>[] { double.class }, new Class<?>[] { int.class }));
    }

    @Test
    public void testObject() {
        // any object
        assertTrue(DefaultAdaptationStrategy.isAssignable(String.class, Object.class, false));

        assertTrue(DefaultAdaptationStrategy.isAssignable(Integer.class, Object.class, false));

        // itself
        assertTrue(DefaultAdaptationStrategy.isAssignable(Object.class, Object.class, false));

        // number
        assertTrue(DefaultAdaptationStrategy.isAssignable(Integer.class, Number.class, false));

        // String
        assertTrue(DefaultAdaptationStrategy.isAssignable(String.class, CharSequence.class, false));

        assertTrue(DefaultAdaptationStrategy.isAssignable(ArrayList.class, List.class, false));
    }

    @Test
    public void testIsNumber() {
        // primitive to Number
        assertTrue(DefaultAdaptationStrategy.isAssignable(int.class, Number.class, false));
        // wrapper to Number
        assertTrue(DefaultAdaptationStrategy.isAssignable(Integer.class, Number.class, false));
    }
}
