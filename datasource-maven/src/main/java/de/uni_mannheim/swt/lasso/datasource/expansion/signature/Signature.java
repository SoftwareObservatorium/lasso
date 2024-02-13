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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A method/constructor signature.
 *
 * @author Marcus Kessel
 */
public class Signature implements Serializable {

    private static final long serialVersionUID = 2584203323009775708L;

    /**
     * Fallback for unresolved types (use root object instead).
     */
    public static String UNRESOLVED_TYPE_FALLBACK = "java.util.Object";

    public static String UNRESOLVED_PKG = "unknown.pkg";

    private String name;
    private String returnType;
    private List<String> inputTypes;

    private String typeDescriptor;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(List<String> inputTypes) {
        this.inputTypes = inputTypes;
    }

    /**
     * e.g. Base64(encode(char[]):char[];)
     *
     * @param fullyQualifiedTypes
     * @return
     */
    @Deprecated
    public String toMQL(boolean fullyQualifiedTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        if(CollectionUtils.isNotEmpty(getInputTypes())) {
            sb.append(getInputTypes().stream()
                    .map(this::getTypeSafe)
                    .map(type -> getSimpleName(type, fullyQualifiedTypes))
                    .collect(Collectors.joining(",")));
        }
        sb.append("):");
        sb.append(getSimpleName(getTypeSafe(returnType), fullyQualifiedTypes));

        return sb.toString();
    }

    /**
     * e.g. Base64{encode(char[])->char[]}
     *
     * @param fullyQualifiedTypes
     * @param constructor
     * @return
     */
    public String toLQL(boolean fullyQualifiedTypes, boolean constructor) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        if(CollectionUtils.isNotEmpty(getInputTypes())) {
            sb.append(getInputTypes().stream()
                    .map(this::getTypeSafe)
                    .map(type -> getSimpleName(type, fullyQualifiedTypes))
                    .collect(Collectors.joining(",")));
        }

        sb.append(")");

        if(!constructor) {
            sb.append("->");
            sb.append(getSimpleName(getTypeSafe(returnType), fullyQualifiedTypes));
        }

        return sb.toString();
    }

    /**
     * Simply checks for blank types and returns {@link #UNRESOLVED_TYPE_FALLBACK} instead.
     *
     * @param type
     * @return
     */
    protected String getTypeSafe(String type) {
        if(StringUtils.isBlank(type)) {
            return UNRESOLVED_TYPE_FALLBACK;
        }

        return type;
    }

    /**
     * Return simple name (removing package name).
     *
     * @param type Any Java type including primitives.
     * @param fullyQualifiedTypes
     * @return
     */
    protected String getSimpleName(String type, boolean fullyQualifiedTypes) {
        // remove generics?
        if(StringUtils.contains(type, "<")) {
            type = StringUtils.substringBefore(type, "<");
        }

        if(!fullyQualifiedTypes) {
            if(StringUtils.contains(type, '.')) {
                type = StringUtils.substringAfterLast(type, ".");
            }
        } else {
            // remove unknown pkg
            if(StringUtils.startsWith(type, UNRESOLVED_PKG)) {
                type = StringUtils.substringAfterLast(type, ".");
            }
        }

        return type;
    }

    public String getTypeDescriptor() {
        return typeDescriptor;
    }

    public void setTypeDescriptor(String typeDescriptor) {
        this.typeDescriptor = typeDescriptor;
    }
}
