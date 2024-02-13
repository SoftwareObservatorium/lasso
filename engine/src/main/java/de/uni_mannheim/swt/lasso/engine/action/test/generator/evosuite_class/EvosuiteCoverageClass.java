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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.evosuite_class;

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Tester;
import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.DefaultMavenActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evosuite coverage class-level action.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://github.com/EvoSuite/evosuite/blob/master/client/src/main/java/org/evosuite/Properties.java">Properties</a>
 */
@LassoAction(desc = "Run Evosuite Coverage")
@Stable
@Tester // handles tests
public class EvosuiteCoverageClass extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(EvosuiteCoverageClass.class);

    public static final String REPORT = "EvosuiteCoverageClass";

    private static final String POM_TEMPLATE_EVOSUITE =
            Mavenizer.getPomTemplate("/mavenizer/pom_evosuite_coverage_class.template");

    @LassoInput(desc = "Evosuite criteria", optional = true)
    public String criteria = "LINE:BRANCH:CBRANCH:WEAKMUTATION:METHODTRACE";

    /** see org.evosuite.statistics.RuntimeVariable.CONSTANTS */
    @LassoInput(desc = "Evosuite report output columns", optional = true)
    public String outputVariables = "TARGET_CLASS,criterion,Coverage,Total_Goals,Covered_Goals,Lines,LineCoverage,Statements_Executed,Total_Branches,BranchCoverage,CBranchCoverage,Total_Methods,Mutants,WeakMutationScore,MutationScore,Size,Result_Size,Length,Result_Length,Total_Time";
    //public String outputVariables = "TARGET_CLASS,criterion,Coverage,Total_Goals,Covered_Goals,LineCoverage,BranchCoverage,CBranchCoverage,WeakMutationScore,MethodTraceCoverage,MutationScore,Total_Time";
    // TODO junit: list of tests to execute

    @LassoInput(desc = "Combine the first N repetitions", optional = true)
    public int firstNRuns = -1;

    @LassoInput(desc = "EvoSuite version", optional = true)
    public String version = "1.2.0";

    @Override
    protected MavenProjectManager createManager(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        TestAdaptationManager testAdaptationManager = new TestAdaptationManager(context);

        ActionNode ancestorNode = context.getExecutionPlan().getAction(actionConfiguration.getDependsOn());
        Systems existingExecutables = context.getLassoOperations().getExecutables(
                context.getExecutionId(), actionConfiguration.getAbstraction().getName(), ancestorNode.getName());
        // reject any not in actionConfiguration
        if(existingExecutables.hasExecutables()) {
            // reject any not part of the "partition block" received by this action
            existingExecutables.getExecutables()
                    .removeIf(executable -> actionConfiguration.getAbstraction().getImplementations().stream().noneMatch(impl -> executable.getId().equals(impl.getId())));
        }

        // only if "semantic"
        // get type
        Class<? extends DefaultAction> ancestorType = context.getActionManager().getRegistry().get(ancestorNode.getType());
        final boolean requireSignatures = false;//!ancestorType.equals(Evosuite.class) && !ancestorType.equals(EvosuiteGenerateClass.class);

        Systems executables = testAdaptationManager.initNew(this,
                getInstanceId(),
                actionConfiguration.getAbstraction(),
                POM_TEMPLATE_EVOSUITE,
                (implementation, candidate, valueMap) -> {
                    // for methods only
                    if(implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                        // limit permutations to method signature only
                        valueMap.put("bytecodename", implementation.getBytecodeName());
                    } else {
                        valueMap.put("bytecodename", "");
                    }

                    // class
                    valueMap.put("lassoClass", implementation.toFQName());
                    valueMap.put("criteria", criteria);
                    valueMap.put("outputVariables", outputVariables);
                },
                executable -> {
                    System existingExecutable = null;
                    try {
                        existingExecutable = existingExecutables.getExecutable(executable.getId());
                    } catch (Throwable e) {
                        //
                        if(LOG.isWarnEnabled()) {
                            LOG.warn("Did not find existing executable for '{}'", executable.getId());
                        }

                        return false;
                    }

//                    //
//                    if(requireSignatures) {
//                        if(!existingExecutable.hasExecutionSignatures()) {
//                            return false;
//                        }
//
//                        // copy over best match json
//                        try {
//                            TestAdaptationManager.copyBestMatchReport(existingExecutable, executable, context);
//                        } catch (Throwable e) {
//                            e.printStackTrace();
//                        }
//
//                        // set best match in model
//                        try {
//                            executable.setExecutionSignatures(Arrays.asList(AdaptationUtils.getBestMatches(existingExecutable)));
//                        } catch (Throwable e) {
//                            e.printStackTrace();
//                        }
//                    }

                    // copy adapter class source + test class source
                    if(firstNRuns > 0) {
                        try {
                            Workspace workspace = context.getWorkspace();
                            Collection<File> testClasses = workspace.listFilesRecursively(
                                    existingExecutable.getProject(), EvosuiteClassCollector.EVOSUITE_TESTS, "java");

                            if(CollectionUtils.isEmpty(testClasses)) {
                                LOG.warn("No test classes found for " + executable.getId());

                                return false;
                            }

                            FileUtils.copyDirectory(
                                    new File(existingExecutable.getProject().getBaseDir(), EvosuiteClassCollector.EVOSUITE_TESTS),
                                    executable.getProject().getSrcTest());


                            Collection<File> copiedClasses = FileUtils.listFiles(
                                    executable.getProject().getSrcTest(),
                                    new String[] { "java" }, true);

                            selectTestSuites(copiedClasses, firstNRuns);
                        } catch (Throwable e) {
                            LOG.warn("Merging failed for " + executable.getId(), e);

                            return false;
                        }
                    } else {
                        try {
                            executable.getProject().copySrcFrom(existingExecutable.getProject(), true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return true;
                });

        // set
        setExecutables(executables);

        if(getExecutables().hasExecutables()) {
            // write values report
            // publish schema
            try {
                ReportOperations reportOperations = context.getReportOperations();

                String[] variable = StringUtils.split(outputVariables, ",");
                reportOperations.newValuesReport(context.getExecutionId(), REPORT, Arrays.stream(variable).collect(Collectors.toMap(String::toUpperCase, v -> "java.lang.Double")));
            } catch (Throwable e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Failed to create report class", e);
                }
            }
        }

        return testAdaptationManager;
    }

    public static void selectTestSuites(Collection<File> testClasses, int maxRuns) throws IOException {
        if(CollectionUtils.isEmpty(testClasses)) {
            throw new IOException("No test classes found");
        }

        // sort
        List<File> sorted = new ArrayList<>(testClasses);
        sorted.sort(new Comparator<File>() {

            /**
             * Ascending order
             *
             * @param o1
             * @param o2
             * @return
             */
            @Override
            public int compare(File o1, File o2) {
                return ComparatorUtils.<String>naturalComparator().compare(o1.getName(), o2.getName());
            }
        });

        // remove all those we want!
        sorted.removeIf(testClass -> {
            int repetition = 0;
            try {
                repetition = NumberUtils.createInteger(StringUtils.substringBetween(testClass.getName(), "_", "_"));
            } catch (Throwable e) {
                LOG.warn("Could not process test class {}", testClass.getAbsolutePath());

                // remove
                return false;
            }

            return repetition < maxRuns;
        });

        // remove all remaining test class files
        sorted.forEach(File::delete);
    }

    @Override
    protected DefaultMavenActionExecutionListener createListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        // grant more timeout
        long millis = (long) (DefaultMavenActionExecutionListener.DEFAULT_PROJECT_TIMEOUT_IN_MILLIS * 4d);

        // also fires action listener ..
        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this, millis);
        testListener.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));
        testListener.setAllowedMojos(Arrays.asList("evosuite-maven-plugin:coverage"));

        return testListener;
    }

    @Override
    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        mavenDefaultCommand.addAll(Arrays.asList(
                "test-compile", // target/test-classes is required
                "evosuite:coverage"
        ));

        return mavenDefaultCommand;
    }

    @Override
    public void postExecute(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, boolean success) {
        if (!success) {
            // remove
            removeExecutableConditionally(actionConfiguration, executableId, "Collection failed");

            return;
        }
    }

    @Override
    public List<RecordCollector> createCollectors() {
        return Arrays.asList(
                new EvosuiteClassCollector(false, REPORT));
    }
}
