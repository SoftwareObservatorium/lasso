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
package de.uni_mannheim.swt.lasso.engine.action.test.minimize;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.Tester;
import de.uni_mannheim.swt.lasso.engine.action.mutation.MethodPitestReport;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
@Deprecated
public class TestSuiteMinimizationTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(TestSuiteMinimizationTest.class);

    @Test
    public void test() {
        TestSuiteMinimization testSuiteMinimization = new TestSuiteMinimization();

        CodeUnit implementation = new CodeUnit();
        implementation.setId("38eca277-e888-4f5e-9a77-a37b5e496c5c");
        System executable = new System(implementation);

        MethodPitestReport methodPitestReport = new MethodPitestReport();
        MultiValuedMap<String, String> killers = new ArrayListValuedHashMap<>();
        methodPitestReport.setKillers(killers);

        // killed by single seed test
        killers.put("mutant_1", "some.pkg.test1_" + LassoUtils.compactUUID(implementation.getId()));
        // killed by single amp test
        killers.put("mutant_2", "some.pkg.test1_f63a1a8995bb4f83ba2a94f1f15a075a");
        // killed by multiple tests
        killers.put("mutant_3", "some.pkg.test1_1other8995bb4f83ba2a94f1f15a075a");
        killers.put("mutant_3", "some.pkg.test1_2other8995bb4f83ba2a94f1f15a075a");
        killers.put("mutant_3", "some.pkg.test1_3other8995bb4f83ba2a94f1f15a075a");
        //
        killers.put("mutant_4", "some.pkg.test1_1other8995bb4f83ba2a94f1f15a075a");
        killers.put("mutant_4", "some.pkg.test1_2other8995bb4f83ba2a94f1f15a075a");

        TestSuiteMinimizationReport report = testSuiteMinimization.minimize(executable, methodPitestReport);

        java.lang.System.out.println(ToStringBuilder.reflectionToString(report));

        assertEquals(1, report.getMinKillingSeedTestsTotal());
        assertEquals(2, report.getMinKillingAmpTestsTotal());
        assertEquals(2, report.getKillingImplementationsTotal());

        assertEquals("test1_1other8995bb4f83ba2a94f1f15a075a;test1_f63a1a8995bb4f83ba2a94f1f15a075a", report.getKillingAmpTests());
        assertEquals("f63a1a89-95bb-4f83-ba2a-94f1f15a075a;1other89-95bb-4f83-ba2a-94f1f15a075a", report.getKillingImplementations());
        assertEquals("test1_38eca277e8884f5e9a77a37b5e496c5c", report.getKillingSeedTests());
    }

    @Test
    public void testUniqueTests() throws IOException {
        TestSuiteMinimization testSuiteMinimization = new TestSuiteMinimization();

        String testClassSource = Tester.getResource("/testdata/testsuiteminimization/example1.txt");

        UniqueTestsReport uniqueTestsReport = testSuiteMinimization.uniqueTests(testClassSource);

        assertEquals(129, uniqueTestsReport.getTests());
        assertEquals(115, uniqueTestsReport.getUniqueTests());
    }
}
