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

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Collects EvoSuite reports + tests.
 *
 * Note: Sometimes EvoSuite does not generate statistics, even though tests are generated.
 *
 * @author Marcus Kessel
 */
public class EvosuiteClassCollector extends RecordCollector {

    private static final Logger LOG = LoggerFactory.getLogger(EvosuiteClassCollector.class);

    public static final String EVOSUITE_TESTS = "evosuite-tests";

    private final boolean exportTests;
    private final String reportName;

    private boolean ignoreMissingReport;

    public EvosuiteClassCollector(boolean exportTests, String reportName) {
        this.exportTests = exportTests;
        this.reportName = reportName;
    }

    @Override
    public Result collectData(LSLExecutionContext executionContext, DefaultAction action, System executable) throws IOException {
        File evosuiteReport = new File(executable.getProject().getBaseDir(), "evosuite-report");
        if(!ignoreMissingReport && !evosuiteReport.exists()) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("No evosuite report found for implementation %s", executable.getId()));
            }
            // we don't tolerate that ..
            return Result.FAILURE;
        }

//        File evosuiteTests = new File(executable.getProject().getBaseDir(), "evosuite-tests");
//        if(!evosuiteTests.exists()) {
//            if(LOG.isInfoEnabled()) {
//                LOG.info(String.format("No evosuite tests found for implementation %s", executable.getId()));
//            }
//            // do nothing
//            return Result.FAILURE;
//        }

        Workspace workspace = executionContext.getWorkspace();

        Collection<File> testClasses = workspace.listFilesRecursively(
                executable.getProject(), EVOSUITE_TESTS, "java");

        if(testClasses.size() < 1) {
            if(LOG.isInfoEnabled()) {
                LOG.info(String.format("No evosuite tests found for implementation %s", executable.getId()));
            }

            if(!(action instanceof EvosuiteCoverageClass)) {
                // do nothing
                return Result.FAILURE;
            }
        }

        // ---

        if(evosuiteReport.exists()) {
            ReportOperations reportOperations = executionContext.getReportOperations();

            // stats
            try(CSVParser parser = CSVParser.parse(new File(evosuiteReport, "statistics.csv"), StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for(CSVRecord row : parser.getRecords()) {
                    int no = -1;

                    Map<String, Double> measures = new HashMap<>();

                    Map<String, String> columns = row.toMap();
                    for(String col : columns.keySet()) {
                        if(StringUtils.equalsAny(col, "TARGET_CLASS", "criterion")) {
                            measures.put(col.toUpperCase(), -1d);
                        } else if(StringUtils.equalsIgnoreCase(col, "configuration_id")) {
                            try {
                                no = NumberUtils.createInteger(columns.get(col));
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        } else {
                            String val = columns.get(col);

                            if(StringUtils.isBlank(val)) {
                                measures.put(col.toUpperCase(), -1d);
                            } else {
                                measures.put(col.toUpperCase(), NumberUtils.createDouble(val));
                            }
                        }
                    }

                    reportOperations.putValues(
                            executionContext.getExecutionId(),
                            ReportKey.of(action, action.getExecutables().getAbstractionName(), executable, no),
                            reportName,
                            measures);
                }
            } catch (Throwable e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Failed to read statistics in " + evosuiteReport.getAbsolutePath(), e);
                }

                // do nothing
                return Result.FAILURE;
            }
        }

        // export?
        if(exportTests && CollectionUtils.isNotEmpty(testClasses)) {
            FileUtils.copyDirectory(
                    new File(executable.getProject().getBaseDir(), EVOSUITE_TESTS),
                    executable.getProject().getSrcTest());

            testClasses = workspace.listFilesRecursively(
                    executable.getProject().getSrcTest(), "java");

            // FIXME workaround for NPE in EvoSuite tests
            try {
                // also checks for 'notGeneratedAnyTest' (i.e empty test classes)
                removeNPECall(executable.getProject().getSrcTest());
            } catch (IOException e) {
                LOG.warn("removeNPECall failed", e);

                return Result.FAILURE;
            }
        }

        // make tests available in filesystem
        testClasses.forEach(file -> {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Writing test to remote filesystem '{}'", file.getAbsolutePath());
            }

            try {
                executionContext.getLassoFileSystem().write(file.getAbsolutePath(), file);
            } catch (Throwable e) {
                LOG.warn("Failed to write test '{}'", file.getAbsolutePath());
                LOG.warn("Stack trace:", e);
            }
        });

        return Result.SUCCESS;
    }

    private void removeNPECall(File srcTest) throws IOException {
        // org.evosuite.runtime.GuiSupport.initialize(); fails with NPE

        Collection<File> files = FileUtils.listFiles(
                srcTest,
                new String[] { "java" }, true);

        int noTestsCounter = 0;
        for(File file : files) {
            StringBuilder sb = new StringBuilder();
            try(LineIterator lineIterator = FileUtils.lineIterator(file)) {
                while(lineIterator.hasNext()) {
                    String line = lineIterator.nextLine();

                    // occurs only once in a file
                    if(StringUtils.contains(line, "notGeneratedAnyTest")) {
                        noTestsCounter++;
                    }

                    if(StringUtils.contains(line, "org.evosuite.runtime.GuiSupport.initialize();")) {
                        line = "//" + line;
                    }

                    sb.append(line);
                    sb.append("\n");
                }

                // overwrite file
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Overwriting test class '{}'", file);
                }
                FileUtils.write(file, sb.toString(), StandardCharsets.UTF_8);
            } catch (Throwable e) {
                LOG.warn("removeNPECall failed", e);
            }
        }

        if(noTestsCounter == files.size()) {
            throw new IOException("No tests were generated");
        }
    }

    public boolean isIgnoreMissingReport() {
        return ignoreMissingReport;
    }

    public void setIgnoreMissingReport(boolean ignoreMissingReport) {
        this.ignoreMissingReport = ignoreMissingReport;
    }
}

