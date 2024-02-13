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
package de.uni_mannheim.swt.lasso.analyzer.model;

import java.util.Date;
import java.util.List;

/**
 * A method/return parameter.
 * 
 * @author Marcus Kessel
 *
 */
public class Parameter {

    private long id;

    private String name;

    private String hash;

    private int fromLine;
    private int toLine;

    private String type;
    private int arrayDim;
    private int typeDim;

    private List<ModifierType> modifiers;

    private List<String> annotations;

    private Date lastModified = new Date();

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified
     *            the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the annotations
     */
    public List<String> getAnnotations() {
        return annotations;
    }

    /**
     * @param annotations
     *            the annotations to set
     */
    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @param hash
     *            the hash to set
     */
    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * @return the fromLine
     */
    public int getFromLine() {
        return fromLine;
    }

    /**
     * @param fromLine
     *            the fromLine to set
     */
    public void setFromLine(int fromLine) {
        this.fromLine = fromLine;
    }

    /**
     * @return the toLine
     */
    public int getToLine() {
        return toLine;
    }

    /**
     * @param toLine
     *            the toLine to set
     */
    public void setToLine(int toLine) {
        this.toLine = toLine;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the arrayDim
     */
    public int getArrayDim() {
        return arrayDim;
    }

    /**
     * @param arrayDim
     *            the arrayDim to set
     */
    public void setArrayDim(int arrayDim) {
        this.arrayDim = arrayDim;
    }

    /**
     * @return the typeDim
     */
    public int getTypeDim() {
        return typeDim;
    }

    /**
     * @param typeDim
     *            the typeDim to set
     */
    public void setTypeDim(int typeDim) {
        this.typeDim = typeDim;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the modifiers
     */
    public List<ModifierType> getModifiers() {
        return modifiers;
    }

    /**
     * @param modifiers
     *            the modifiers to set
     */
    public void setModifiers(List<ModifierType> modifiers) {
        this.modifiers = modifiers;
    }
}
