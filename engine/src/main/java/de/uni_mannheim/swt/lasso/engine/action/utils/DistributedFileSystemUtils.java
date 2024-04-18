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
package de.uni_mannheim.swt.lasso.engine.action.utils;

import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.core.model.Action;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper methods for DFS (distributed file system).
 *
 * @author Marcus Kessel
 */
public class DistributedFileSystemUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(DistributedFileSystemUtils.class);

    /**
     * Write test sequences to remote file system
     *
     * @param context
     * @param action
     * @param abstractionId
     * @param testClasses
     */
    public static void writeSequenceFiles(LSLExecutionContext context, Action action, String abstractionId, List<File> testClasses) {
        // resolve path where to store tests
        String path = getTestsPath(context.getExecutionId(), action.getName(), abstractionId);

        // make tests available in filesystem
        testClasses.forEach(file -> {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Writing test to remote filesystem '{}'", file.getAbsolutePath());
            }

            try {
                context.getLassoFileSystem().write(path, file);
            } catch (Throwable e) {
                LOG.warn("Failed to write test '{}'", file.getAbsolutePath());
                LOG.warn("Stack trace:", e);
            }
        });
    }

    /**
     * Write test sequences to remote file system
     *
     * @param context
     * @param action
     * @param abstractionId
     * @param sequences
     */
    public static void writeSequences(LSLExecutionContext context, Action action, String abstractionId, List<Sequence> sequences) {
        // resolve path where to store tests
        String path = getTestsPath(context.getExecutionId(), action.getName(), abstractionId);

        // make tests available in filesystem
        sequences.forEach(s -> {
            File file = new File(path, String.format("%s.json", s.getId()));

            if(LOG.isDebugEnabled()) {
                LOG.debug("Writing test to remote filesystem '{}'", file.getAbsolutePath());
            }

            try {
                String json = SequenceUtils.toJson(s);
                OutputStream out = context.getLassoFileSystem()
                        .writeToOutputStream(file.getAbsolutePath());
                IOUtils.write(json, out, StandardCharsets.UTF_8);
                IOUtils.closeQuietly(out);
            } catch (Throwable e) {
                LOG.warn("Failed to write test '{}'", s.getId());
                LOG.warn("Stack trace:", e);
            }
        });
    }

    /**
     * List all sequences
     *
     * @param context
     * @param actionId
     * @param abstractionId
     * @return
     * @throws IOException
     */
    public static List<String> listSequences(LSLExecutionContext context, String actionId, String abstractionId) throws IOException {
        return listSequences(context, context.getExecutionId(), actionId, abstractionId);
    }

    /**
     * List all sequences
     *
     * @param context
     * @param executionId
     * @param actionId
     * @param abstractionId
     * @return
     * @throws IOException
     */
    public static List<String> listSequences(LSLExecutionContext context, String executionId, String actionId, String abstractionId) throws IOException {
        // resolve path where to store tests
        String path = getTestsPath(executionId, actionId, abstractionId);

        try {
            return context.getLassoFileSystem().listFiles(path);
        } catch (Throwable e) {
            LOG.warn("Failed to list files '{}'", path);
            LOG.warn("Stack trace:", e);

            throw new IOException(e);
        }
    }

    /**
     * Read list of {@link Sequence}s.
     *
     * @param context
     * @param executionId
     * @param actionId
     * @param abstractionId
     * @return
     * @throws IOException
     */
    public static List<Sequence> readSequences(LSLExecutionContext context, String executionId, String actionId, String abstractionId) throws IOException {
        List<String> files = listSequences(context, executionId, actionId, abstractionId);
        // filter by suffix (.json)
        List<String> jsonFiles = files.stream().filter(s -> StringUtils.endsWithIgnoreCase(s, ".json")).collect(Collectors.toList());
        return jsonFiles.stream().map(file -> {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Reading test from remote filesystem '{}'", file);
            }

            try {
                return SequenceUtils.fromJson(IOUtils.toString(context.getLassoFileSystem().read(file), StandardCharsets.UTF_8));
            } catch (Throwable e) {
                LOG.warn("Failed to write test '{}'", file);
                LOG.warn("Stack trace:", e);

                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * Get path where a certain action stores its tests
     *
     * @param executionId
     * @param actionId
     * @param abstractionId
     * @return
     */
    public static String getTestsPath(String executionId, String actionId, String abstractionId) {
        return String.format("/workspace/%s/tests/%s/%s", executionId, abstractionId, actionId);
    }
}
