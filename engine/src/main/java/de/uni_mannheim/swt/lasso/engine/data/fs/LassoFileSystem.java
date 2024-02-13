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
package de.uni_mannheim.swt.lasso.engine.data.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Interface for distributed file systems.
 *
 * @author Marcus Kessel
 */
public interface LassoFileSystem {

    static String CACHE_NAME = "lasso_files";

    void write(String path, File file) throws IOException;

    OutputStream writeToOutputStream(String path) throws IOException;

    InputStream read(String path) throws IOException;

    boolean exists(String path) throws IOException;

    boolean delete(String path) throws IOException;

    long length(String path) throws IOException;

    List<String> listFiles(String path) throws IOException;

    List<String> listFiles(String path, int limit) throws IOException;

    void clear() throws IOException;
}
