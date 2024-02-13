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
package de.uni_mannheim.swt.lasso.maven;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;

/**
 *
 * @author Marcus Kessel
 */
public class EventStream {

    private final PrintWriter printWriter;
    private final String filename;

    public EventStream(String filename) throws IOException {
        this.filename = filename;
        printWriter = new PrintWriter(new FileWriter(filename));
    }

    public void addEvent(String[] cols) {
        LocalTime time = LocalTime.now();
        printWriter.printf("%tT;%s\n", time, String.join(";", cols));
        // write instantly to file
        printWriter.flush();
    }

    public void close() throws Exception {
        printWriter.close();
    }

    public String getFilename() {
        return filename;
    }
}
