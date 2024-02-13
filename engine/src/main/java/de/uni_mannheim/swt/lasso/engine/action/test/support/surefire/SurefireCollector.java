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
package de.uni_mannheim.swt.lasso.engine.action.test.support.surefire;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link SurefireParser} collector.
 * 
 * @author Marcus Kessel
 *
 */
public class SurefireCollector extends RecordCollector {

    private static final Logger LOG = LoggerFactory
            .getLogger(SurefireCollector.class);

    public static final String SUREFIRE_REPORTS = "surefire-reports";

    public static final String EXTENSION = "xml";

    /**
     * Increase memory efficiency
     */
    private boolean collectFeedback;

    public SurefireCollector(boolean collectFeedback) {
        this.collectFeedback = collectFeedback;
    }

    /**
     * Read surefire reports to get test results
     * @return
     */
    @Override
    public Result collectData(LSLExecutionContext executionContext, DefaultAction action, System executable) throws IOException {
        Workspace workspace = executionContext.getWorkspace();

//        try {
//            Thread.sleep(30 * 1000L);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // read surefire test reports
        Collection<File> xmlFiles = workspace
                .listFilesRecursively(executable.getProject().getTarget(), SUREFIRE_REPORTS, EXTENSION);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Found '{}' surefire reports", xmlFiles.size());
        }

        if (CollectionUtils.isNotEmpty(xmlFiles)) {
            List<SurefireReport> surefireReportList = new LinkedList<>();

            for (File xmlFile : xmlFiles) {
                if(StringUtils.startsWith(xmlFile.getName(), "testng-results")) {
                    // skip
                    continue;
                }

                try {
                    SurefireReport surefireReport = new SurefireParser(
                            FileUtils.openInputStream(xmlFile), !collectFeedback)
                                    .parse();
                    surefireReport.setFilename(xmlFile.getName());

                    surefireReportList.add(surefireReport);
                } catch (Throwable e) {
                    //
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(String.format("Could not parse surefire record '%s'", xmlFile.getAbsolutePath()), e);
                    }
                }

            }

            SurefireReports surefireReports = new SurefireReports();
            surefireReports.setSurefireReports(surefireReportList);

            // add report
            executionContext.getReportOperations().put(
                    executionContext.getExecutionId(),
                    ReportKey.of(action, action.getExecutables().getAbstractionName(), executable),
                    surefireReports);

            // make tests available in filesystem
            List<File> testClasses = executable.getProject().getFiles(executable.getProject().getSrcTest(), "java");
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

        return Result.FAILURE;
    }
}
