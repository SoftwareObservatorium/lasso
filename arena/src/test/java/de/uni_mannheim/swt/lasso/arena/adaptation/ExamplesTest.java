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

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SheetSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SpreadSheet;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;

import examples.*;

import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test adaptation capabilities with Examples
 *
 * @author Marcus Kessel
 */
public class ExamplesTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);
    CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

    /**
     * ignore visibility of constructors/methods.
     *
     * {@link SequenceSpecification#isIgnoreVisibility()}
     *
     * @throws IOException
     */
    @Test
    public void test_InvisibleExample() throws IOException, NoSuchMethodException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}");

        ClassUnderTest classUnderTest = createExample(InvisibleExample.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        int limitAdapters = 1;

        //
        SpreadSheet sheet1 = new SpreadSheet(new File("examples/SingletonExample.xlsx"));
        SheetSequenceSpecificationParser parser = new SheetSequenceSpecificationParser();

        Map<String, SequenceSpecification> ssMap = parser.toSequenceSpecifications(sheet1, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap.size());

        //ClassUnderTest

        List<AdaptedImplementation> adaptedImplementations = arena.adapt(classUnderTest, parseResults.get(0), limitAdapters);

        // ASSERTS
        AdaptedImplementation first = adaptedImplementations.get(0);
        assertEquals(first.getInitializer(parseResults.get(0), 0).getAsConstructor(), InvisibleExample.class.getDeclaredConstructor());
        assertEquals(first.getMethod(parseResults.get(0), 0).getMethod(), InvisibleExample.class.getDeclaredMethod("sum", int.class, int.class));

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                SequenceSpecification ss = ssMap.get(name);
                // IGNORE
                ss.setIgnoreVisibility(true);

                try {
                    SequenceExecutionRecord sequenceExecutionRecord = ss.instantiate(parseResults.get(0), adaptedImplementation);
                    System.out.println(sequenceExecutionRecord.getSequence());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * see {@link de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.FactoryMethodStrategy}
     *
     * @throws IOException
     */
    @Test
    public void test_FactoryMethodStrategy() throws IOException, NoSuchMethodException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}");

        ClassUnderTest classUnderTest = createExample(SingletonExample.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        int limitAdapters = 1;

        //
        SpreadSheet sheet1 = new SpreadSheet(new File("examples/SingletonExample.xlsx"));
        SheetSequenceSpecificationParser parser = new SheetSequenceSpecificationParser();

        Map<String, SequenceSpecification> ssMap = parser.toSequenceSpecifications(sheet1, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap.size());

        //ClassUnderTest

        List<AdaptedImplementation> adaptedImplementations = arena.adapt(classUnderTest, parseResults.get(0), limitAdapters);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                try {
                    SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(parseResults.get(0), adaptedImplementation);
                    System.out.println(sequenceExecutionRecord.getSequence());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * see {@link de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.FactoryMethodStrategy}
     *
     * @throws IOException
     */
    @Test
    public void test_FactoryMethodStrategy_NoDefaultConstructor() throws IOException, NoSuchMethodException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}");

        ClassUnderTest classUnderTest = createExample(NonDefaultConstructorSingletonExample.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        int limitAdapters = 1;

        //
        SpreadSheet sheet1 = new SpreadSheet(new File("examples/SingletonExample.xlsx"));
        SheetSequenceSpecificationParser parser = new SheetSequenceSpecificationParser();

        Map<String, SequenceSpecification> ssMap = parser.toSequenceSpecifications(sheet1, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap.size());

        //ClassUnderTest

        List<AdaptedImplementation> adaptedImplementations = arena.adapt(classUnderTest, parseResults.get(0), limitAdapters);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                try {
                    SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(parseResults.get(0), adaptedImplementation);
                    System.out.println(sequenceExecutionRecord.getSequence());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * see {@link de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.StaticFieldInstance}
     *
     * @throws IOException
     */
    @Test
    public void test_StaticFieldInstance() throws IOException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}");

        ClassUnderTest classUnderTest = createExample(PublicFieldInstanceExample.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        int limitAdapters = 1;

        //
        SpreadSheet sheet1 = new SpreadSheet(new File("examples/SingletonExample.xlsx"));
        SheetSequenceSpecificationParser parser = new SheetSequenceSpecificationParser();

        Map<String, SequenceSpecification> ssMap = parser.toSequenceSpecifications(sheet1, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap.size());

        //ClassUnderTest

        List<AdaptedImplementation> adaptedImplementations = arena.adapt(classUnderTest, parseResults.get(0), limitAdapters);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                try {
                    SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(parseResults.get(0), adaptedImplementation);
                    System.out.println(sequenceExecutionRecord.getSequence());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void test_StaticMethod() throws IOException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("SingletonExample {\n" +
                "sum(int,int)->int\n" +
                "}");

        ClassUnderTest classUnderTest = createExample(StaticMethodExample.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        int limitAdapters = 1;

        //
        SpreadSheet sheet1 = new SpreadSheet(new File("examples/SingletonExample.xlsx"));
        SheetSequenceSpecificationParser parser = new SheetSequenceSpecificationParser();

        Map<String, SequenceSpecification> ssMap = parser.toSequenceSpecifications(sheet1, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap.size());

        //ClassUnderTest

        List<AdaptedImplementation> adaptedImplementations = arena.adapt(classUnderTest, parseResults.get(0), limitAdapters);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            for(AdaptedImplementation adaptedImplementation : adaptedImplementations) {
                try {
                    SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(parseResults.get(0), adaptedImplementation);
                    System.out.println(sequenceExecutionRecord.getSequence());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static ClassUnderTest createExample(Class<?> exampleClass) {
        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());
        implementation.setName(exampleClass.getSimpleName());
        implementation.setPackagename(exampleClass.getPackage().getName());
        implementation.setGroupId("examples.lasso");
        implementation.setArtifactId("examples");
        implementation.setVersion("1.0.0-SNAPSHOT");
        ClassUnderTest classUnderTest = new ClassUnderTest(new de.uni_mannheim.swt.lasso.core.model.System(implementation));
        //classUnderTest.setPseudo(true);

        // one workaround to avoid resolution of artifacts
        classUnderTest.getProject().setDependencyResult(new DependencyResult(new DependencyRequest()));

        return classUnderTest;
    }
}
