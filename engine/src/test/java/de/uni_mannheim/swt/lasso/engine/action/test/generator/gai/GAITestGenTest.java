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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.gai;

import de.uni_mannheim.swt.lasso.benchmark.*;
import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.Tester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class GAITestGenTest {

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
        GAITestGen action = new GAITestGen() {
            @Override
            protected List<Sequence> collectSequences(LSLExecutionContext context, Systems systems) {
                return Arrays.asList(sequence);
            }
        };
        // OpenAiClient client = new OpenAiClient("http://bagdana.informatik.uni-mannheim.de:8080/v1/chat/completions",
        //            "swt4321");
        action.setName("gai");
        action.apiUrl = "http://bagdana.informatik.uni-mannheim.de:8080/v1/chat/completions";
        action.apiKey = "swt4321";
        action.maxNoOfTests = 100;
        action.noOfPrompts = 1;

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
        GAITestGen action = new GAITestGen();
        // OpenAiClient client = new OpenAiClient("http://bagdana.informatik.uni-mannheim.de:8080/v1/chat/completions",
        //            "swt4321");
        action.setName("gai");
        action.apiUrl = "http://bagdana.informatik.uni-mannheim.de:8080/v1/chat/completions";
        action.apiKey = "swt4321";
        action.maxNoOfTests = 100;
        action.noOfPrompts = 1;
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
