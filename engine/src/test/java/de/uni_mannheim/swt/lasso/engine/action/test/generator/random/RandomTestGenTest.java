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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.random;

import de.uni_mannheim.swt.lasso.core.model.*;

import de.uni_mannheim.swt.lasso.engine.Tester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class RandomTestGenTest {

    String mavenRepoUrl = "https://swtweb.informatik.uni-mannheim.de/nexus/repository/maven-public/";

    @Test
    public void test_gcd() throws IOException {
        RandomTestGen action = new RandomTestGen();
        action.setName("random");
        action.noOfTests = 10;
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
        RandomTestGen action = new RandomTestGen();
        action.setName("random");
        action.noOfTests = 10;
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
    public void test_pair() throws IOException {
        RandomTestGen action = new RandomTestGen();
        action.setName("random");
        action.noOfTests = 10;
        action.dependencies = Arrays.asList("org.javatuples:javatuples:1.2");
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
        action.execute(Tester.ctx(mavenRepoUrl), actionConfiguration);
    }
}
