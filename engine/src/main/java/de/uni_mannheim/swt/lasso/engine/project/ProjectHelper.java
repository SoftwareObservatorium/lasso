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
package de.uni_mannheim.swt.lasso.engine.project;

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.data.fs.LassoFileSystem;
import de.uni_mannheim.swt.lasso.engine.matcher.TestMatcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marcus Kessel
 */
public class ProjectHelper {

    private static final Logger LOG = LoggerFactory
            .getLogger(ProjectHelper.class);

    public static void copyTestsFromRemote(LSLExecutionContext context, ActionConfiguration actionConfiguration, System executable, System existingExecutable) throws IOException {
        LassoFileSystem fileSystem = context.getLassoFileSystem();

        File srcTestPath = existingExecutable.getProject().getSrcTest();

        List<String> files = fileSystem.listFiles(srcTestPath.getAbsolutePath());
        // copy over
        TestMatcher testMatcher = new TestMatcher();

        List<String> filtered = files.stream()
                .filter(f -> testMatcher.match(actionConfiguration.getIncludeTestsPattern(), f))
                .collect(Collectors.toList());

        for (String file : filtered) {
            File to = new File(executable.getProject().getSrcTest(), StringUtils.substringAfter(file, srcTestPath.getAbsolutePath() + "/"));

            if(LOG.isDebugEnabled()) {
                LOG.debug("Copying test from remote '{}' to local '{}'", file, to.getAbsolutePath());
            }

            try (InputStream in = fileSystem.read(file);
                 FileOutputStream out = FileUtils.openOutputStream(to)) {
                IOUtils.copy(in, out);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyTestsFromRemoteWithPostfix(LSLExecutionContext context, ActionConfiguration actionConfiguration, System executable, System existingExecutable) throws IOException {
        LassoFileSystem fileSystem = context.getLassoFileSystem();

        File srcTestPath = existingExecutable.getProject().getSrcTest();

        List<String> files = fileSystem.listFiles(srcTestPath.getAbsolutePath());
        // copy over
        TestMatcher testMatcher = new TestMatcher();

        List<String> filtered = files.stream()
                .filter(f -> testMatcher.match(actionConfiguration.getIncludeTestsPattern(), f))
                .collect(Collectors.toList());

        for (String file : filtered) {
            File to = new File(executable.getProject().getSrcTest(), StringUtils.substringAfter(file, srcTestPath.getAbsolutePath() + "/"));

            // change post fix of Java file to make it distinct
            String newFileName = StringUtils.replace(to.getName(), ".java", "_" + LassoUtils.compactUUID(existingExecutable.getId()) + ".java");

            to = new File(StringUtils.replace(to.getAbsolutePath(), to.getName(), newFileName));

            if(LOG.isDebugEnabled()) {
                LOG.debug("Copying test from remote '{}' to local '{}'", file, to.getAbsolutePath());
            }

            try (InputStream in = fileSystem.read(file);
                 FileOutputStream out = FileUtils.openOutputStream(to)) {
                IOUtils.copy(in, out);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
