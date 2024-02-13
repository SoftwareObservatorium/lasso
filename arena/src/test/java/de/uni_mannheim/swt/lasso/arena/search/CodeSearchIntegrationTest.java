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
package de.uni_mannheim.swt.lasso.arena.search;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class CodeSearchIntegrationTest {

    @Test
    public void test_getClass() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest method = codeSearch.queryForClass("564debc5-124a-439f-b9f2-6a247dfc1c20");
        ClassUnderTest ownerClass = codeSearch.queryForClass(method.getImplementation().getCode().getParentId());

        System.out.println(ownerClass.getImplementation().getCode().getContent());
    }

    @Test
    public void test_getClassesDirectly() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2020());

        String mql = "Stack{\n" +
                "Stack(int)\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int" +
                "}";

        List<ClassUnderTest> classesUnderTest = codeSearch.queryForClassesDirectly(mql, 10, "class"); // retrieve classes

        for(ClassUnderTest cut : classesUnderTest) {
            System.out.println(ToStringBuilder.reflectionToString(cut.getImplementation().getCode()));
        }
    }

    @Test
    public void test_getClassesDirectly_secorpora2021() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.secorpora2021());

        String mql = "Stack{\n" +
                "Stack(int)\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int" +
                "}";

        // let's say we only want code from 50-K
        String[] filters = {
                "groupId:\"50kc.projects\""
        };
        List<ClassUnderTest> classesUnderTest = codeSearch.queryForClassesDirectly(mql, 10, "class", filters); // retrieve classes

        for(ClassUnderTest cut : classesUnderTest) {
            System.out.println(ToStringBuilder.reflectionToString(cut.getImplementation().getCode()));
        }
    }

    @Test
    public void testConstructor() throws IOException {
        String mql = "Stack{\n" +
                "Stack(int)\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int" +
                "}";

        CodeSearch codeSearch = new CodeSearch();
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);

        List<ClassUnderTest> classesUnderTest = codeSearch.queryForClasses(mql, 5); // retrieve classes

        for(ClassUnderTest cut : classesUnderTest) {
            System.out.println(ToStringBuilder.reflectionToString(cut.getImplementation()));
        }
    }

    /**
     * Retrieve JUnit4 classes
     *
     * @throws IOException
     */
    @Test
    public void testRetrieve_JUnitClasses() throws IOException {
        String query = "*:*";

        // which data source? here mavenCentral2020
        SolrInstance mavenCentral2020 = SolrInstance.mavenCentral2020();
        CodeSearch codeSearch = new CodeSearch(mavenCentral2020);

        List<ClassUnderTest> classesUnderTest = codeSearch.queryForClasses(query, 10,
                // JUnit3 tests
                //"dep_exact:\"junit/framework/TestCase\"",
                // JUnit4 tests
                "dep_exact:\"org/junit/Test\""
                // JUnit5 tests
                //"dep_exact:\"org/junit/jupiter/api/Test\"",
                // TestNG
                //"dep_exact:\"org/testng/annotations/Test\""
                ); // retrieve classes

        for(ClassUnderTest cut : classesUnderTest) {
            System.out.println(ToStringBuilder.reflectionToString(cut.getImplementation()));
        }
    }
}
