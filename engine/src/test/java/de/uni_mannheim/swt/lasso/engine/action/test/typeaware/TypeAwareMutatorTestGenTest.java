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
package de.uni_mannheim.swt.lasso.engine.action.test.typeaware;

import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.Tester;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Marcus Kessel
 */
public class TypeAwareMutatorTestGenTest {

    String mavenRepoUrl = "https://swtweb.informatik.uni-mannheim.de/nexus/repository/maven-public/";

    @Test
    public void test_gcd() throws IOException {
        TypeAwareMutatorTestGen action = new TypeAwareMutatorTestGen();
        action.setName("typeAware");
        action.noOfTests = 1;
        //action.seed = 42;

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        Abstraction abstraction = new Abstraction();
        abstraction.setName("greatestCommonDivisor");
        abstraction.setSystems(Arrays.asList(Tester.system("1", "Clazz", "pkg")));
        Specification specification = new Specification();
        Interface iFace = Tester.parse("Problem{ greatestCommonDivisor(long,long)->long }").getInterfaceSpecification();
        specification.setInterfaceSpecification(iFace);

        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Problem"}}
                {"cells": {"A2": {}, "B2": "greatestCommonDivisor", "C2": "2", "D2": "4"}}
                """;
        Sheet sheet = new Sheet("test1()", ssnJsonlStr, iFace.getLqlQuery());
        specification.getTests().add(sheet);
        abstraction.setSpecification(specification);

        actionConfiguration.setAbstraction(abstraction);
        action.execute(Tester.ctx(mavenRepoUrl), actionConfiguration);

        // debug
        action.getExecutables().getSpecification().getTests().forEach(s -> java.lang.System.out.println(s.getBody()));
    }

    @Test
    public void test_Stack() throws IOException {
        TypeAwareMutatorTestGen action = new TypeAwareMutatorTestGen();
        action.setName("typeAware");
        action.noOfTests = 1;
        //action.seed = 42;

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        Abstraction abstraction = new Abstraction();
        abstraction.setName("stack");
        abstraction.setSystems(Arrays.asList(Tester.system("1", "Clazz", "pkg")));
        Specification specification = new Specification();

        String lql = "Stack{" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        Interface iFace = Tester.parse(lql).getInterfaceSpecification();
        specification.setInterfaceSpecification(iFace);
        abstraction.setSpecification(specification);

        @Language("jsonl")
        String ssnJsonlStr = """
                {"cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
                {"cells": {"A2": {}, "B2": "push", "C2": "A1", "D2": "\\"Hello World!\\""}}
                {"cells": {"A3": {}, "B3": "size", "C3": "A1"}}
                """;
        Sheet sheet = new Sheet("test1()", ssnJsonlStr, iFace.getLqlQuery());
        specification.getTests().add(sheet);

        actionConfiguration.setAbstraction(abstraction);
        action.execute(Tester.ctx(mavenRepoUrl), actionConfiguration);

        // debug
        action.getExecutables().getSpecification().getTests().forEach(s -> java.lang.System.out.println(s.getBody()));
    }

//    /**
//     * Type tokens.
//     *
//     * @throws InstantiationException
//     * @throws IllegalAccessException
//     * @throws NoSuchFieldException
//     * @throws IOException
//     */
//    @Test
//    public void test_pair() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        Sequence sequence = new Sequence();
//        sequence.setId("test");
//        Statement statement = new Statement();
//        statement.setOperation("tuple");
//        statement.setExpectedOutputs(new LinkedList<>());
//        Value value1 = new Value();
//        value1.setType("org.javatuples.Pair");
//
//        LSLExecutionContext context = Tester.ctx(mavenRepoUrl);
//        Container container = ClazzContainerUtils.createClazzContainer(context, Arrays.asList("org.javatuples:javatuples:1.2"));
//
//        Class<?> tupleType = container.loadClass(value1.getType());
//
//        Object tupleValue = tupleType.getConstructor(Object.class, Object.class).newInstance(5l,6l);
//        value1.setValue(tupleValue);
//
//        statement.setInputs(Arrays.asList(value1));
//        sequence.setStatements(Arrays.asList(statement));
//
//        TypeAwareMutatorTestGenSSN action = new TypeAwareMutatorTestGenSSN();
//        action.setName("typeAware");
//        action.noOfTests = 1;
//        //action.seed = 42;
//
//        ActionConfiguration actionConfiguration = new ActionConfiguration();
//        Abstraction abstraction = new Abstraction();
//        abstraction.setName("greatestCommonDivisor");
//        abstraction.setSystems(Arrays.asList(Tester.system("1", "Clazz", "pkg")));
//        Specification specification = new Specification();
//        // type token
//        Interface iFace = Tester.parse("Problem{ tuple(org.javatuples.Pair<java.lang.Long,java.lang.Long>)->long }").getInterfaceSpecification();
//        specification.setInterfaceSpecification(iFace);
//        abstraction.setSpecification(specification);
//
//        actionConfiguration.setAbstraction(abstraction);
//        action.execute(context, actionConfiguration);
//    }

//    // load benchmark tests
//    @Test
//    public void test_gcd_benchmark() throws IOException {
//        Sequence sequence = new Sequence();
//        sequence.setId("test");
//        Statement statement = new Statement();
//        statement.setOperation("greatestCommonDivisor");
//        statement.setExpectedOutputs(new LinkedList<>());
//        Value value1 = new Value();
//        value1.setType("long");
//        value1.setValue(5);
//        Value value2 = new Value();
//        value2.setType("long");
//        value2.setValue(10);
//        statement.setInputs(Arrays.asList(value1, value2));
//        sequence.setStatements(Arrays.asList(statement));
//
//        TypeAwareMutatorTestGenSSN action = new TypeAwareMutatorTestGenSSN();
//        action.setName("typeAware");
//        action.noOfTests = 1;
//        action.benchmark = "humaneval-java-reworded";
//
//        ActionConfiguration actionConfiguration = new ActionConfiguration();
//        Abstraction abstraction = new Abstraction();
//        abstraction.setName("HumanEval_13_greatest_common_divisor");
//        abstraction.setSystems(Arrays.asList(Tester.system("1", "Clazz", "pkg")));
//
//        // benchmark
//        BenchmarkManager benchmarkManager = new BenchmarkManager();
//        Benchmark b = benchmarkManager.load(action.benchmark);
//        FunctionalAbstraction ab = b.getAbstractions().get(abstraction.getName());
//
//        Specification specification = new Specification();
//        Interface iFace = Tester.parse(ab.getLql()).getInterfaceSpecification();
//        specification.setInterfaceSpecification(iFace);
//        abstraction.setSpecification(specification);
//
//        actionConfiguration.setAbstraction(abstraction);
//
//        LSLExecutionContext context = Tester.ctx(mavenRepoUrl);
//        context.setBenchmarkManager(benchmarkManager);
//
//        action.execute(context, actionConfiguration);
//    }
}
