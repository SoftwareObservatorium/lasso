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
package de.uni_mannheim.swt.lasso.arena.sequence;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.JUnitSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import randoop.ExecutionVisitor;
import randoop.sequence.ExecutableSequence;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
// FIXME needs update
public class SequenceSpecificationIntegrationTest {

    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(NexusInstance.LASSOHP12_URL, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_java() throws NoSuchMethodException {
        SequenceSpecification ss = new SequenceSpecification();
        ss.setName("customtest");

        ConstructorCallStatement constructorCallStatement = new ConstructorCallStatement(
                new ReflectionConstructorSignature(String.class.getConstructor(String.class)));
        ss.addStatement(constructorCallStatement, ss.getNextPosition());

        ValueStatement valueStatement = new ValueStatement(String.class, "hi");
        constructorCallStatement.addInput(valueStatement);

        MethodCallStatement methodCallStatement = new MethodCallStatement(
                new ReflectionMethodSignature(String.class.getMethod("getBytes")));
        ss.addStatement(methodCallStatement, ss.getNextPosition());

        // first input always subject
        methodCallStatement.addInput(constructorCallStatement); // subject

        ss.addStatement(new ValueStatement(int.class, 5), ss.getNextPosition()); // omitted by randoop (since not used)
        ss.addStatement(new ValueStatement(int[].class, new int[]{1,2}), ss.getNextPosition());
        ss.addStatement(new ValueStatement(int[].class, new int[5]), ss.getNextPosition());
        ss.addStatement(new ValueStatement(int[][].class, new int[][]{{1}, {2}}), ss.getNextPosition());

        ValueStatement myArr = new ValueStatement(String[].class, new String[]{"hello", "world"});
        ss.addStatement(myArr, ss.getNextPosition());

        ValueStatement arrElement = new ValueStatement(String.class, "marcus");
        ss.addStatement(arrElement, ss.getNextPosition());

        ArraySetStatement arraySetStatement = new ArraySetStatement(1);
        ss.addStatement(arraySetStatement, ss.getNextPosition());
        // add array
        arraySetStatement.addInput(myArr);
        arraySetStatement.addInput(arrElement);

        ss.addStatement(new ValueStatement(int[].class, null), ss.getNextPosition());

        SequenceExecutionRecord sequenceExecutionRecord = ss.instantiate(null,null);

        System.out.println(sequenceExecutionRecord.getSequence());
    }

    @Test
    public void test_conver_listtoarray() throws NoSuchMethodException {
        SequenceSpecification ss = new SequenceSpecification();
        ss.setName("customtest");

        ConstructorCallStatement constructorCallStatement = new ConstructorCallStatement(
                new ReflectionConstructorSignature(ArrayList.class.getConstructor()));
        ss.addStatement(constructorCallStatement, ss.getNextPosition());

        MethodCallStatement methodCallStatement = new MethodCallStatement(
                new ReflectionMethodSignature(ArrayList.class.getMethod("add", Object.class)));
        ss.addStatement(methodCallStatement, ss.getNextPosition());

        // first input always subject
        methodCallStatement.addInput(constructorCallStatement); // subject
        methodCallStatement.addInput(new ValueStatement(String.class, "marcus"));


        SequenceExecutionRecord sequenceExecutionRecord = ss.instantiate(null,null);

        System.out.println(sequenceExecutionRecord.getSequence());
    }

    @Test
    public void test_adapt_Stack() throws NoSuchMethodException, IOException {
        String mql = "Stack{" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();

        InterfaceSpecification specification = codeSearch.fromLQL(mql).get(0);

        ClassUnderTest classUnderTest = codeSearch.queryForClass("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025");

        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        //
        AdaptedImplementation adaptedImplementation = arena.adapt(classUnderTest, specification, 1).get(0);

        SequenceSpecification ss = new SequenceSpecification();

        ConstructorCallStatement constructorCallStatement = new ConstructorCallStatement(
                specification.getConstructors().get(0));
        constructorCallStatement.setClassUnderTest(true);
        ss.addStatement(constructorCallStatement, ss.getNextPosition());

        MethodCallStatement push = new MethodCallStatement(
                specification.getMethods().stream().filter(m -> StringUtils.equals(m.getName(), "push")).findFirst().get());
        push.setClassUnderTest(true);
        ss.addStatement(push, ss.getNextPosition());

        ValueStatement valueStatement = new ValueStatement(String.class, "hi");
        // first input always subject
        push.addInput(constructorCallStatement); // subject
        push.addInput(valueStatement);

        MethodCallStatement size = new MethodCallStatement(
                specification.getMethods().stream().filter(m -> StringUtils.equals(m.getName(), "size")).findFirst().get());
        size.setClassUnderTest(true);
        ss.addStatement(size, ss.getNextPosition());

        size.addInput(constructorCallStatement); // subject

        SequenceExecutionRecord sequenceExecutionRecord = ss.instantiate(specification, adaptedImplementation);

        System.out.println(sequenceExecutionRecord.getSequence());

        ExecutableSequence executableSequence = arena.execute(sequenceExecutionRecord.getSequence(), new ExecutionVisitor() {
            @Override
            public void visitBeforeStatement(ExecutableSequence executableSequence,int i) {
            }

            @Override
            public void visitAfterStatement(ExecutableSequence executableSequence, int i) {
            }

            @Override
            public void initialize(ExecutableSequence executableSequence) {
            }

            @Override
            public void visitAfterSequence(ExecutableSequence executableSequence) {
            }
        });

        System.out.println(executableSequence.toCodeString());

        sequenceExecutionRecord.setExecutableSequence(executableSequence);

        TableSawWriter writer = new TableSawWriter();
        writer.writeExecutedSequence(sequenceExecutionRecord, new DefaultArena());

        System.out.println(writer.getTable().printAll());
    }

    @Test
    public void testToSequenceSpecification_ArrayStack() throws IOException {
        String mql = "Stack{" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();

        ClassUnderTest classUnderTest = codeSearch.queryForClass("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025");
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        String testClass = FileUtils.readFileToString(new File("sheets/ArrayStack_0_Test.java"), StandardCharsets.UTF_8);

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

        InterfaceSpecification specification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());

        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, specification, classUnderTest,classUnderTest.getClassName(), "");

        assertEquals(12, ssMap.size());

        AdaptedImplementation adaptedImplementation = arena.adapt(classUnderTest, specification, 1).get(0);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(specification, adaptedImplementation);
            System.out.println(sequenceExecutionRecord.getSequence());
        }

    }

    @Test
    public void testToSequenceSpecification_ArrayStack_play() throws IOException {
        CodeSearch codeSearch = new CodeSearch();

        ClassUnderTest classUnderTest = codeSearch.queryForClass("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025");
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        String testClass = FileUtils.readFileToString(new File("sheets/PlayArrayStack_0_Test.java"), StandardCharsets.UTF_8);

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

        InterfaceSpecification specification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());

        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, specification, classUnderTest,classUnderTest.getClassName(), "");

        //assertEquals(12, ssMap.size());

        AdaptedImplementation adaptedImplementation = arena.adapt(classUnderTest, specification, 1).get(0);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(specification, adaptedImplementation);
            System.out.println(sequenceExecutionRecord.getSequence());

            System.out.println(sequenceExecutionRecord.getSequenceSpecification().toInterfaceSpecification().toLQL());
        }

    }

//    @Test
//    public void test_toSequenceSpecification_Matrix() throws IOException {
//        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017());
//        ClassUnderTest classUnderTest = codeSearch.queryForClass("379e30b8-cb5f-45c4-93b9-d592450d2743");
//
//        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
//        // automatically resolves project-related artifacts
//        pool.initProjects();
//
//        DefaultArena arena = new DefaultArena();
//        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());
//
//        String testClass = FileUtils.readFileToString(new File("sheets/MatrixTest_array.java"), StandardCharsets.UTF_8);
//
//        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();
//
//        InterfaceSpecification specification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());
//
//        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, specification, classUnderTest,classUnderTest.getClassName(), "");
//
//        //assertEquals(12, ssMap.size());
//
//        System.out.println("SPEC " + specification.toMQL());
//
//        AdaptedImplementation adaptedImplementation = arena.adapt(classUnderTest, specification, 1).get(0);
//
//        for(String name : ssMap.keySet()) {
//            System.out.println("SS " + name);
//            System.out.println("SS " + ssMap.get(name).toString());
//
//            SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(specification, adaptedImplementation);
//            System.out.println(sequenceExecutionRecord.getSequence());
//        }
//    }
}
