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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.common.hash.Hashing;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.mutation.MethodPitestReport;

import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marcus Kessel
 */
@LassoAction(desc = "Minimize test suites based on mutations")
@Stable
@Local
@Deprecated
public class TestSuiteMinimization extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(TestSuiteMinimization.class);

    //
//    @LassoInput(desc = "minimum test coverage", optional = false)
//    public double minimumTestCoverage = 0d;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

//        // double-check ancestor node
//        ActionNode testFilterNode = context.getExecutionPlan().getAction(actionConfiguration.getDependsOn());
//        if (!testFilterNode.getType().equals(TestFilter.class.getSimpleName()) && !testFilterNode.getType().equals(CrossCheck.class.getSimpleName())) {
//            throw new IllegalArgumentException(String.format("The node this node depends is not of required type '%s'", testFilterNode.getType()));
//        }

        try {
            // fetch ALL previous implementations
            Systems candidateExecutables = context.getLassoOperations()
                    .getExecutables(context.getExecutionId(), actionConfiguration.getAbstraction().getName(), actionConfiguration.getDependsOn());

            //
            //Executable refExecutable = candidateExecutables.getExecutables().get(0);
            Optional<System> refExecutableOp = candidateExecutables.getExecutables()
                    .stream()
                    .filter(executable -> StringUtils.equals(executable.getId(), actionConfiguration.getAbstraction().getName())).findFirst();

            System refExecutable = refExecutableOp.orElseThrow(() -> new RuntimeException(String.format("Did not find ref '%s'", actionConfiguration.getAbstraction().getName())));

            // attempt to retrieve last report
            ReportKey key = ReportKey.of((String) null, actionConfiguration.getAbstraction().getName(), refExecutable);

            MethodPitestReport methodPitestReport = context.getReportOperations().getLast(context.getExecutionId(), key, MethodPitestReport.class);
            //AdapterReport adapterReport = context.getReportOperations().getLast(context.getExecutionId(), key, AdapterReport.class);

            // minimize
            TestSuiteMinimizationReport report = minimize(refExecutable, methodPitestReport);

            Validate.notNull(report, "TestSuiteMinimizationReport was null for %s", refExecutable.getId());

            // add report
            context.getReportOperations().put(
                    context.getExecutionId(),
                    ReportKey.of(this, actionConfiguration.getAbstraction().getName(), refExecutable),
                    report);

            Systems executables = new Systems();
            executables.setAbstractionName(actionConfiguration.getAbstraction().getName());
            executables.setExecutables(new ArrayList<>(Arrays.asList(refExecutable)));
            setExecutables(executables);
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Action failed for " + actionConfiguration.getAbstraction().getName(), e);
            }

            throw new RuntimeException(e);
        }
    }

    /**
     * Returns number of unique tests.
     *
     * @param testClassSource
     * @return
     */
    UniqueTestsReport uniqueTests(String testClassSource) {
        JavaParser javaParser = new JavaParser();

        // ignore comments
        javaParser.getParserConfiguration().setAttributeComments(false);

        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(testClassSource).getResult().get();
        TypeDeclaration<?> tc = cu.getType(0);

        Set<String> seen = new HashSet<>();
        for(MethodDeclaration member : tc.getMethods()) {
            Optional<AnnotationExpr> optionalAnnotationDeclaration = member.getAnnotationByName("Test");
            if(!optionalAnnotationDeclaration.isPresent()) {
                continue; // skip if not test method
            }

            // FIXME problematic, we miss several things: how is cut called etc.
            Optional<BlockStmt> blockOp = member.getBody();
            if(blockOp.isPresent()) {
                BlockStmt block = blockOp.get();

                String blockStr = StringUtils.replaceChars(block.toString(), " \t\n\r", "");

                //System.out.println("----");
                //System.out.println(blockStr);

                seen.add(Hashing.sha256().hashString(blockStr, StandardCharsets.UTF_8).toString());
            }
        }

        UniqueTestsReport uniqueTestsReport = new UniqueTestsReport();
        uniqueTestsReport.setTests(tc.getMethods().size());
        uniqueTestsReport.setUniqueTests(seen.size());

        return uniqueTestsReport;
    }

    TestSuiteMinimizationReport minimize(System executable, MethodPitestReport methodPitestReport) {
        MultiValuedMap<String, String> killers = methodPitestReport.getKillers();

        if (killers == null || killers.isEmpty()) {
            // nothing to do
            return null;
        }

        // 0) we need to keep seed tests
        String compactId = LassoUtils.compactUUID(executable.getId());

        // 1) sort by "best" killing tests (i.e number of mutations killed)
        MultiValuedMap<String, String> freq = new ArrayListValuedHashMap<>();

        // testName to mutation mapping
        Map<String, Collection<String>> map = killers.asMap();
        Set<String> mutationsKilledBySingleTest = new HashSet<>();
        Set<String> mutationsKilledByMultipleTests = new HashSet<>();
        for (String mutationId : map.keySet()) {
            Collection<String> killingTests = map.get(mutationId);

            for (String killingTest : killingTests) {
                // e.g, org.apache.commons.codec.binary.Base64Test.test_3
                String testName = StringUtils.substringAfterLast(killingTest, ".");

                freq.put(testName, mutationId);
            }

            if(killingTests.size() == 1) {
                mutationsKilledBySingleTest.add(mutationId);
            } else {
                mutationsKilledByMultipleTests.add(mutationId);
            }
        }

        // now sort frequencies sort by "best" killing tests (i.e number of mutations killed)
        LinkedHashMap<String, Collection<String>> sortedFreqDescending = freq.asMap()
                .entrySet()
                .stream()
                .sorted(((Comparator<Map.Entry<String, Collection<String>>>) (o1, o2) -> ComparatorUtils.<Integer>naturalComparator().compare(o1.getValue().size(), o2.getValue().size())).reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        //
        Set<String> killingSeedTests = new HashSet<>();

        // algorithm is simple: look at impls with highest killings first and subsequently remove all killed mutants until none left
        // first, we favor seed tests
        for (String testName : sortedFreqDescending.keySet()) {
            // we favor seed tests
            boolean seedTest = StringUtils.endsWith(testName, compactId);

            if(!seedTest) {
                continue;
            }

            // mutations killed by this test
            Collection<String> killedMutations = sortedFreqDescending.get(testName);

            boolean added = false;
            // mutations which are only killed by one test
            if(killedMutations.stream().anyMatch(mutationsKilledBySingleTest::contains)) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Single kill by '{}'", testName);
                }

                added = true;
            }

            // mutations which are killed by multiple tests
            if(mutationsKilledByMultipleTests.size() > 0) {
                for(String killedMutation : killedMutations) {
                    //
                    boolean killed = mutationsKilledByMultipleTests.remove(killedMutation);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Picked first seed test for multiple kill '{}' of '{}'", testName, killedMutation);
                    }

                    if(killed && !added) {
                        added = true;
                    }
                }
            }

            if(added) {
                killingSeedTests.add(testName);
            }
        }

        Set<String> killingAmpTests = new HashSet<>();

        // REMAINDER up to AMP tests
        // algorithm is simple: look at impls with highest killings first and subsequently remove all killed mutants until none left
        for (String testName : sortedFreqDescending.keySet()) {
            //
            boolean seedTest = StringUtils.endsWith(testName, compactId);

            if(seedTest) {
                // we already favored those before
                continue;
            }

            // mutations killed by this test
            Collection<String> killedMutations = sortedFreqDescending.get(testName);

            boolean added = false;
            // mutations which are only killed by one test
            if(killedMutations.stream().anyMatch(mutationsKilledBySingleTest::contains)) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Single kill by '{}'", testName);
                }

                added = true;
            }

            // mutations which are killed by multiple tests
            if(mutationsKilledByMultipleTests.size() > 0) {
                for(String killedMutation : killedMutations) {
                    //
                    boolean killed = mutationsKilledByMultipleTests.remove(killedMutation);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Picked first amp test for multiple kill '{}' of '{}'", testName, killedMutation);
                    }

                    if(killed && !added) {
                        added = true;
                    }
                }
            }

            if(added) {
                killingAmpTests.add(testName);
            }
        }

        Set<String> killingImpls = killingAmpTests.stream()
                .map(t -> LassoUtils.decompactUUID(StringUtils.substringAfterLast(t, "_")))
                .collect(Collectors.toSet());

        TestSuiteMinimizationReport report = new TestSuiteMinimizationReport();
        //report.setAllTestsTotal(adapterReport.getTestCasesPassedTotal());

        //report.setMinTestsTotal();
        report.setMinKillingSeedTestsTotal(killingSeedTests.size());
        report.setMinKillingAmpTestsTotal(killingAmpTests.size());
        report.setKillingAmpTests(StringUtils.join(killingAmpTests, ";"));
        report.setKillingSeedTests(StringUtils.join(killingSeedTests, ";"));
        report.setKillingImplementationsTotal(killingImpls.size());
        report.setKillingImplementations(StringUtils.join(killingImpls, ";"));

        return report;
    }
}
