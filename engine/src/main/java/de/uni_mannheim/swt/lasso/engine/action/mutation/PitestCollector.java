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
package de.uni_mannheim.swt.lasso.engine.action.mutation;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;

import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Pitest report collector.
 * 
 * @author Marcus Kessel
 *
 */
public class PitestCollector extends RecordCollector {

    private static final Logger LOG = LoggerFactory
            .getLogger(PitestCollector.class);

    public static final String REPORT_DIR = "pit-reports";
    public static final String MUTATIONS_XML = "mutations.xml";

    public PitestCollector() {
    }

    /**
     * Read surefire reports to get test results
     * @return
     */
    @Override
    public Result collectData(LSLExecutionContext executionContext, DefaultAction action, System executable) throws IOException {
        Workspace workspace = executionContext.getWorkspace();

        // read surefire test reports
        Collection<File> xmlFiles = workspace
                .listFilesRecursively(executable.getProject().getTarget(), REPORT_DIR, "xml");

        if(LOG.isDebugEnabled()) {
            LOG.debug("Found '{}' mutation reports", xmlFiles.size());
        }

        if (CollectionUtils.isNotEmpty(xmlFiles)) {
            for (File xmlFile : xmlFiles) {
                if(!StringUtils.equals(xmlFile.getName(), MUTATIONS_XML)) {
                    // skip
                    continue;
                }


                try(InputStream in1 = FileUtils.openInputStream(xmlFile)) {
                    PitestReport pitestReport = new PitestParser(
                            in1)
                            .parse();

                    // add report
                    executionContext.getReportOperations().put(
                            executionContext.getExecutionId(),
                            ReportKey.of(action, action.getExecutables().getAbstractionName(), executable),
                            pitestReport);

                    try(InputStream inM = FileUtils.openInputStream(xmlFile)) {
                        // get mutations
                        List<PitMutation> mutationList = new PitestParser(
                                inM).parseMutations();

                        int mutationId = 0;
                        for(PitMutation pitMutation : mutationList) {
                            // add mutation report
                            executionContext.getReportOperations().put(
                                    executionContext.getExecutionId(),
                                    ReportKey.of(action, action.getExecutables().getAbstractionName(), executable, mutationId),
                                    pitMutation);

                            mutationId++;
                        }

                        //return Result.SUCCESS;
                    } catch (Throwable e) {
                        //
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(String.format("Could not parse mutants '%s'", xmlFile.getAbsolutePath()), e);
                        }
                    }

                    if(executable.getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                        try(InputStream in2 = FileUtils.openInputStream(xmlFile)) {
                            MethodPitestReport methodPitestReport = new PitestParser(
                                    in2)
                                    .parse(Arrays.asList(executable.getCode().getBytecodeName()));

                            // add report
                            executionContext.getReportOperations().put(
                                    executionContext.getExecutionId(),
                                    ReportKey.of(action, action.getExecutables().getAbstractionName(), executable),
                                    methodPitestReport);

                            //return Result.SUCCESS;
                        } catch (Throwable e) {
                            //
                            if (LOG.isWarnEnabled()) {
                                LOG.warn(String.format("Could not parse MethodPitestReport record '%s'", xmlFile.getAbsolutePath()), e);
                            }
                        }
                    }

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
                } catch (Throwable e) {
                    //
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(String.format("Could not parse pitest record '%s'", xmlFile.getAbsolutePath()), e);
                    }
                }
            }
        }

        return Result.FAILURE;
    }
}
