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
package de.uni_mannheim.swt.lasso.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Reflects the entry component of a system (e.g., Java class or method).
 *
 * @author Marcus Kessel
 */
public class System implements Serializable {

    private final CodeUnit code;
    private MavenProject project;

    /**
     * Adapter
     */
    private Adapter adapter;

    private Variant variant;

    public System(CodeUnit code) {
        this.code = code;
    }

    public System(CodeUnit code, MavenProject project) {
        this(code);
        this.project = project;
    }

    public System(CodeUnit code, MavenProject project, Adapter adapter) {
        this(code, project);
        this.adapter = adapter;
    }

    public boolean isAdapted() {
        return adapter != null;
    }

    public boolean isVariant() {
        return variant != null;
    }

    public CodeUnit getCode() {
        return code;
    }

    // FIXME more specific wrt wrapper
    public String getId() {
        return code.getId();
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        System that = (System) o;
//        return Objects.equals(getId(), that.getId());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(getId());
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        System that = (System) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getAdapter(), that.getAdapter()) && Objects.equals(getVariant(), that.getVariant());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAdapter(), getVariant());
    }
}
