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
package de.uni_mannheim.swt.lasso.analyzer.analyzer;

import java.io.IOException;
import java.util.List;

import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;

/**
 * Interface for static class analysis (class hierarchy, metrics etc.)
 * 
 * @author Marcus Kessel
 *
 */
public interface ProjectAnalyzer {

    /**
     * Analyse project including sources and bytecode.
     * 
     * @param mavenArtifact
     *            a MavenArtifact instance
     * @return {@link List} if {@link CompilationUnit}s.
     * @throws IOException
     *             Cannot read given file locations, analysis errors
     */
    List<CompilationUnit> analyze(MavenArtifact mavenArtifact)
            throws IOException;
}
