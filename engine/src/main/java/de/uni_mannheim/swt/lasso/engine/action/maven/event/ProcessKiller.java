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
package de.uni_mannheim.swt.lasso.engine.action.maven.event;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Simple process killer (only Linux-like killing supported).
 *
 * @author Marcus Kessel
 */
public class ProcessKiller {

    private static final Logger LOG = LoggerFactory
            .getLogger(ProcessKiller.class);

    public static void kill9(String executableId) throws IOException {
        File tempScript = createBashCommand(executableId);
        kill9(tempScript);
    }

    public static void kill9(File tempScript) throws IOException {

        if(LOG.isInfoEnabled()) {
            LOG.info("Generated bash script\n{}", FileUtils.readFileToString(tempScript, Charset.defaultCharset()));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", tempScript.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());

            if(LOG.isInfoEnabled()) {
                LOG.info("Process output '{}'", output);
            }

            process.waitFor();
        } catch (Throwable e) {
            throw new IOException(e);
        } finally {
            //tempScript.delete();
        }
    }

    public static File createBashCommand(String executableId) throws IOException {
        File tempScript = File.createTempFile("lasso_bash", null);

        try (Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
                tempScript));
             PrintWriter printWriter = new PrintWriter(streamWriter)) {
            printWriter.println("#!/bin/bash");
            printWriter.println("ps aux | grep \"java\"");
            // XXX potential improvement: grep -E "e8fae666-1f9d-4bca-9653-156b8dbabb78.*surefire.*mymaven"
            // don't accidentally kill the tail command
            // this results in a restart of the tailer command ...
            printWriter.println(
                    String.format("PROC_PID=$(ps aux | grep \"%s\" | grep -v 'grep' | grep -v 'tail' | awk '{print $2}')", executableId));
            printWriter.println("echo \"Killing $PROC_PID\"");
            printWriter.println("kill -9 $PROC_PID");
        }

        return tempScript;
    }
}
