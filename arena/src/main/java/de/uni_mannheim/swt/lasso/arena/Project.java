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
package de.uni_mannheim.swt.lasso.arena;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;

import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.eclipse.aether.resolution.DependencyResult;

import java.util.Objects;

/**
 * Models a project artifact.
 *
 * @author Marcus Kessel
 */
public class Project {

    private final String groupId;
    private final String artifactId;
    private final String version;

    private DependencyResult dependencyResult;

    private Container container;

    public Project(String mavenUri) {
        try {
            String[] parts = mavenUri.split(":");

            this.groupId = parts[0];
            this.artifactId = parts[1];
            this.version = parts[2];
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to split maven URI " + mavenUri);
        }
    }

    public Project(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String toMavenUri() {
        return String.join(":", groupId, artifactId, version);
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(groupId, project.groupId) && Objects.equals(artifactId, project.artifactId) && Objects.equals(version, project.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    public DependencyResult getDependencyResult() {
        return dependencyResult;
    }

    public void setDependencyResult(DependencyResult dependencyResult) {
        this.dependencyResult = dependencyResult;
    }

    public boolean isResolved() {
        return dependencyResult != null;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Remove container (i.e its {@link ClassLoader}
     *
     */
    public void removeContainer() {
        if(getContainer() != null) {
            try {
                getContainer().getWorld().disposeRealm(container.getId());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        setContainer(null);
    }

    @Override
    public String toString() {
        return "Project{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
