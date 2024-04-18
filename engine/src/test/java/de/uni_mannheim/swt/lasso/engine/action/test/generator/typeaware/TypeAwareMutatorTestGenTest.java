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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.typeaware;

import de.uni_mannheim.swt.lasso.benchmark.*;
import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.classloader.Container;
import de.uni_mannheim.swt.lasso.core.model.*;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.Tester;

import de.uni_mannheim.swt.lasso.engine.action.utils.ClazzContainerUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class TypeAwareMutatorTestGenTest {

    String mavenRepoUrl = "https://swtweb.informatik.uni-mannheim.de/nexus/repository/maven-public/";

    @Test
    public void test_gcd() throws IOException {
        Sequence sequence = new Sequence();
        sequence.setId("test");
        Statement statement = new Statement();
        statement.setOperation("greatestCommonDivisor");
        statement.setExpectedOutputs(new LinkedList<>());
        Value value1 = new Value();
        value1.setType("long");
        value1.setValue(5);
        Value value2 = new Value();
        value2.setType("long");
        value2.setValue(10);
        statement.setInputs(Arrays.asList(value1, value2));
        sequence.setStatements(Arrays.asList(statement));

        TypeAwareMutatorTestGen action = new TypeAwareMutatorTestGen() {
            @Override
            protected List<Sequence> collectSequences(LSLExecutionContext context, Systems systems) {
                return Arrays.asList(sequence);
            }
        };
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
        abstraction.setSpecification(specification);

        actionConfiguration.setAbstraction(abstraction);
        action.execute(Tester.ctx(mavenRepoUrl), actionConfiguration);
    }

    @Test
    public void test_Stack() throws IOException {
        Sequence sequence = new Sequence();
        sequence.setId("test");
        Statement statement = new Statement();
        statement.setOperation("push");
        statement.setExpectedOutputs(new LinkedList<>());
        Value value1 = new Value();
        value1.setType("long");
        value1.setValue(5);
        statement.setInputs(Arrays.asList(value1));

        Statement pop = new Statement();
        pop.setOperation("pop");
        pop.setExpectedOutputs(new LinkedList<>());
        pop.setInputs(new LinkedList<>());

        sequence.setStatements(Arrays.asList(statement, pop));

        TypeAwareMutatorTestGen action = new TypeAwareMutatorTestGen() {
            @Override
            protected List<Sequence> collectSequences(LSLExecutionContext context, Systems systems) {
                return Arrays.asList(sequence);
            }
        };
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

        actionConfiguration.setAbstraction(abstraction);
        action.execute(Tester.ctx(mavenRepoUrl), actionConfiguration);
    }

    /**
     * Type tokens.
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws IOException
     */
    @Test
    public void test_pair() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Sequence sequence = new Sequence();
        sequence.setId("test");
        Statement statement = new Statement();
        statement.setOperation("tuple");
        statement.setExpectedOutputs(new LinkedList<>());
        Value value1 = new Value();
        value1.setType("org.javatuples.Pair");

        LSLExecutionContext context = Tester.ctx(mavenRepoUrl);
        Container container = ClazzContainerUtils.createClazzContainer(context, Arrays.asList("org.javatuples:javatuples:1.2"));

        Class<?> tupleType = container.loadClass(value1.getType());

        Object tupleValue = tupleType.getConstructor(Object.class, Object.class).newInstance(5l,6l);
        value1.setValue(tupleValue);

        statement.setInputs(Arrays.asList(value1));
        sequence.setStatements(Arrays.asList(statement));

        TypeAwareMutatorTestGen action = new TypeAwareMutatorTestGen() {
            @Override
            protected List<Sequence> collectSequences(LSLExecutionContext context, Systems systems) {
                return Arrays.asList(sequence);
            }
        };
        action.setName("typeAware");
        action.noOfTests = 1;
        //action.seed = 42;

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        Abstraction abstraction = new Abstraction();
        abstraction.setName("greatestCommonDivisor");
        abstraction.setSystems(Arrays.asList(Tester.system("1", "Clazz", "pkg")));
        Specification specification = new Specification();
        // type token
        Interface iFace = Tester.parse("Problem{ tuple(org.javatuples.Pair<java.lang.Long,java.lang.Long>)->long }").getInterfaceSpecification();
        specification.setInterfaceSpecification(iFace);
        abstraction.setSpecification(specification);

        actionConfiguration.setAbstraction(abstraction);
        action.execute(context, actionConfiguration);
    }

    // load benchmark tests
    @Test
    public void test_gcd_benchmark() throws IOException {
        Sequence sequence = new Sequence();
        sequence.setId("test");
        Statement statement = new Statement();
        statement.setOperation("greatestCommonDivisor");
        statement.setExpectedOutputs(new LinkedList<>());
        Value value1 = new Value();
        value1.setType("long");
        value1.setValue(5);
        Value value2 = new Value();
        value2.setType("long");
        value2.setValue(10);
        statement.setInputs(Arrays.asList(value1, value2));
        sequence.setStatements(Arrays.asList(statement));

        TypeAwareMutatorTestGen action = new TypeAwareMutatorTestGen();
        action.setName("typeAware");
        action.noOfTests = 1;
        action.benchmark = "humaneval-java-reworded";

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        Abstraction abstraction = new Abstraction();
        abstraction.setName("HumanEval_13_greatest_common_divisor");
        abstraction.setSystems(Arrays.asList(Tester.system("1", "Clazz", "pkg")));

        // benchmark
        BenchmarkManager benchmarkManager = new BenchmarkManager();
        Benchmark b = benchmarkManager.load(action.benchmark);
        FunctionalAbstraction ab = b.getAbstractions().get(abstraction.getName());

        Specification specification = new Specification();
        Interface iFace = Tester.parse(ab.getLql()).getInterfaceSpecification();
        specification.setInterfaceSpecification(iFace);
        abstraction.setSpecification(specification);

        actionConfiguration.setAbstraction(abstraction);

        LSLExecutionContext context = Tester.ctx(mavenRepoUrl);
        context.setBenchmarkManager(benchmarkManager);

        action.execute(context, actionConfiguration);
    }
}
