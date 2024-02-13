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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.randoop;

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Collects Randoop tests.
 *
 * @author Marcus Kessel
 */
public class RandoopClassCollector extends RecordCollector {

    private static final Logger LOG = LoggerFactory.getLogger(RandoopClassCollector.class);

    public static final String RANDOOP_TESTS = "randoop-tests";

    private final boolean exportTests;

    public RandoopClassCollector(boolean exportTests) {
        this.exportTests = exportTests;
    }

    @Override
    public Result collectData(LSLExecutionContext executionContext, DefaultAction action, System executable) throws IOException {
        Workspace workspace = executionContext.getWorkspace();

        Collection<File> testClasses = workspace.listFilesRecursively(
                executable.getProject(), RANDOOP_TESTS, "java");

        if(testClasses.size() < 1) {
            if(LOG.isInfoEnabled()) {
                LOG.info(String.format("No randoop tests found for implementation %s", executable.getId()));
            }

            return Result.FAILURE;
        }

        // export?
        if(exportTests && CollectionUtils.isNotEmpty(testClasses)) {
            FileUtils.copyDirectory(
                    new File(executable.getProject().getBaseDir(), RANDOOP_TESTS),
                    executable.getProject().getSrcTest());

            testClasses = workspace.listFilesRecursively(
                    executable.getProject().getSrcTest(), "java");
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

        RandoopReport report = new RandoopReport();
        report.setNoTestFiles(testClasses.size());

        // add report
        executionContext.getReportOperations().put(
                executionContext.getExecutionId(),
                ReportKey.of(action, action.getExecutables().getAbstractionName(), executable),
                report);

        return Result.SUCCESS;
    }
}

