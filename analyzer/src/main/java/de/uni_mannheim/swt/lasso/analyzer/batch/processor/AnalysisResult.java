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
package de.uni_mannheim.swt.lasso.analyzer.batch.processor;

import java.util.List;
import java.util.Map;

import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.MetaData;

/**
 * Result of an analysis
 * 
 * @author Marcus Kessel
 *
 */
public class AnalysisResult {

    private MavenArtifact mavenArtifact;
    private List<CompilationUnit> compilationUnits;

    private MetaData metaData;

    private Map<String, String> meta;

    /**
     * @return the mavenArtifact
     */
    public MavenArtifact getMavenArtifact() {
        return mavenArtifact;
    }

    /**
     * @param mavenArtifact
     *            the mavenArtifact to set
     */
    public void setMavenArtifact(MavenArtifact mavenArtifact) {
        this.mavenArtifact = mavenArtifact;
    }

    /**
     * @return the compilationUnits
     */
    public List<CompilationUnit> getCompilationUnits() {
        return compilationUnits;
    }

    /**
     * @param compilationUnits
     *            the compilationUnits to set
     */
    public void setCompilationUnits(List<CompilationUnit> compilationUnits) {
        this.compilationUnits = compilationUnits;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }
}
