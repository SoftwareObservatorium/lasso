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
package de.uni_mannheim.swt.lasso.engine;

import de.uni_mannheim.swt.lasso.lsl.LSLLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Marcus Kessel
 */
public class LSLFileLogger implements LSLLogger {

    private final FileWriter out;

    public LSLFileLogger(File log) throws IOException {
        this.out = new FileWriter(log, true);

    }

    @Override
    public void log(Object object) {
        try {
            out.append(String.format("%s", object));
            out.append(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        out.close();
    }
}
