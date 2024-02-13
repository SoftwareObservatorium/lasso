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

import de.uni_mannheim.swt.lasso.engine.action.maven.event.ProcessKiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Simple process killer for PIT processes (only Linux-like killing supported).
 *
 * @author Marcus Kessel
 */
public class PitestProcessKiller {

    private static final Logger LOG = LoggerFactory
            .getLogger(PitestProcessKiller.class);

    public static void kill9() throws IOException {
        File tempScript = createBashCommand();
        ProcessKiller.kill9(tempScript);
    }

    /**
     * @return
     * @throws IOException
     */
    public static File createBashCommand() throws IOException {
        File tempScript = File.createTempFile("lasso_bash", null);

        try (Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
                tempScript));
             PrintWriter printWriter = new PrintWriter(streamWriter)) {
            printWriter.println("#!/bin/bash");
            printWriter.println("ps aux | grep \"java\"");
            // sort by earliest start time, return first process only
            printWriter.println(
                    "PROC_PID=$(ps aux --sort=start | grep \"org.pitest:pitest-maven:mutationCoverage\" | grep -v 'grep' | grep -v 'tail' | awk '{print $2}' | head -n 1)");
            printWriter.println("echo \"Killing $PROC_PID\"");
            printWriter.println("kill -9 $PROC_PID");
        }

        return tempScript;
    }
}
