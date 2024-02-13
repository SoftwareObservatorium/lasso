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
package de.uni_mannheim.swt.lasso.datasource.maven.build;

import de.uni_mannheim.swt.lasso.core.model.Artifact;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;

import java.io.Serializable;
import java.util.List;

/**
 * A component candidate.
 * 
 * @author Marcus Kessel
 *
 */
public class Candidate implements Serializable {

    /**
     * ID
     */
    private String id;

    /**
     * Compilation unit (e.g., class)
     */
    private CompilationUnit compilationUnit;

    /**
     * Candidate's artifact (e.g. Maven artifact)
     */
    private Artifact artifact;

    /**
     * Own dependencies
     */
    private List<Artifact> dependencies;

    /**
     * Resolved dependencies of unresolved imports
     */
    private List<Artifact> resolvedDependencies;

    /**
     * The candidate's test adapter
     */
    private TestAdapter testAdapter;

    /**
     * The candidate's project
     */
    private MavenProject project;

    /**
     * @return the compilationUnit
     */
    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    /**
     * @param compilationUnit
     *            the compilationUnit to set
     */
    public void setCompilationUnit(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    /**
     * @return the artifact
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * @param artifact
     *            the artifact to set
     */
    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    /**
     * @return the dependencies
     */
    public List<Artifact> getDependencies() {
        return dependencies;
    }

    /**
     * @param dependencies
     *            the dependencies to set
     */
    public void setDependencies(List<Artifact> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * @return the resolvedDependencies
     */
    public List<Artifact> getResolvedDependencies() {
        return resolvedDependencies;
    }

    /**
     * @param resolvedDependencies
     *            the resolvedDependencies to set
     */
    public void setResolvedDependencies(List<Artifact> resolvedDependencies) {
        this.resolvedDependencies = resolvedDependencies;
    }

    /**
     * @return the testAdapter
     */
    public TestAdapter getTestAdapter() {
        return testAdapter;
    }

    /**
     * @param testAdapter
     *            the testAdapter to set
     */
    public void setTestAdapter(TestAdapter testAdapter) {
        this.testAdapter = testAdapter;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }
}
