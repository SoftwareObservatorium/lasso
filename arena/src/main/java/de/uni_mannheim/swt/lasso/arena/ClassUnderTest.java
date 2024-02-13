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

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Model of a class under test.
 *
 * @author Marcus Kessel
 */
public class ClassUnderTest {

    private String id;
    private String className;

    private Project project;

    private /*transient*/ System implementation;

    private MavenProject localProject;

    private String variantId = "original";

    private boolean pseudo;

    public ClassUnderTest(System implementation) {
        this(implementation.getId(),
                implementation.getCode().toFQName(),
                new Project(implementation.getCode().toUri()));

        this.implementation = implementation;
    }

    public ClassUnderTest(String id, String className, Project project) {
        this.id = id;
        this.className = className;
        this.project = project;
    }

    public String toUri() {
        return String.join(":", implementation.getCode().getGroupId(),
                implementation.getCode().getArtifactId(),
                implementation.getCode().getClassifier(),
                "jar",
                implementation.getCode().getVersion());
    }

    public String getId() {
        return id;
    }

    public String getFullId() {
        return StringUtils.join(new String[]{ id, variantId }, '_');
    }

    public void setId(String id) {
        this.id = id;
    }

    public System getImplementation() {
        return implementation;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public String toString() {
        return "ClassUnderTest{" +
                "id='" + id + '\'' +
                ", variantId='" + variantId + '\'' +
                ", className='" + className + '\'' +
                ", project=" + project +
                '}';
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    /**
     * Load class under test.
     *
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> loadClass() throws ClassNotFoundException {
        if(isPseudo()) {
            throw new ClassNotFoundException("PSEUDO!");
        }

        return getProject().getContainer().loadClass(getClassName());
    }

    public Class<?> loadClassUnsafe() {
        try {
            return loadClass();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassUnderTest that = (ClassUnderTest) o;
        return Objects.equals(id, that.id) && Objects.equals(className, that.className) && Objects.equals(project, that.project) && Objects.equals(variantId, that.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, className, project, variantId);
    }

    public MavenProject getLocalProject() {
        return localProject;
    }

    public void setLocalProject(MavenProject localProject) {
        this.localProject = localProject;
    }

    public boolean isPseudo() {
        return pseudo;
    }

    public void setPseudo(boolean pseudo) {
        this.pseudo = pseudo;
    }
}
