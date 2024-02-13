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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The class under test
 *
 * @author Marcus Kessel
 */
public class Clazz implements Serializable {

    private static final long serialVersionUID = 2584203323009771108L;

    private String name;
    private String packageName;

    private List<Signature> constructors = new LinkedList<>();
    private List<Signature> methods = new LinkedList<>();

    public String getFQName() {
        return String.format("%s.%s", packageName, name);
    }

    /**
     * Resolved dependency types part of this clazz's signatures.
     */
    private Map<String, Clazz> dependencies;

    /**
     * Any properties
     */
    private Map<String, Object> properties;

    public List<Signature> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<Signature> constructors) {
        this.constructors = constructors;
    }

    public List<Signature> getMethods() {
        return methods;
    }

    public void setMethods(List<Signature> methods) {
        this.methods = methods;
    }

    public Map<String, Clazz> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, Clazz> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * e.g. Base64(encode(char[]):char[];)
     *
     * @param fullyQualifiedTypes
     * @return
     */
    @Deprecated
    public String toMQL(boolean fullyQualifiedTypes) {
        // XXX MQL currently supports NO fully-qualified names.
        // FIXME still valid?
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(\n");
        if(CollectionUtils.isNotEmpty(constructors)) {
            // make sure to use Clazz name and not <init>
            constructors.forEach(c -> {
                c.setName(getName());
                c.setReturnType("void");
            });

            sb.append(constructors.stream().map(c -> c.toMQL(fullyQualifiedTypes)).collect(Collectors.joining(";\n")));

            sb.append(";\n");
        }
        if(CollectionUtils.isNotEmpty(methods)) {
            sb.append(methods.stream().map(c -> c.toMQL(fullyQualifiedTypes)).collect(Collectors.joining(";\n")));

            sb.append(";\n");
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * e.g. Base64{encode(char[])->char[]}
     *
     * @param fullyQualifiedTypes
     * @return
     */
    public String toLQL(boolean fullyQualifiedTypes) {
        return toLQL(fullyQualifiedTypes, false);
    }

    /**
     * e.g. Base64{encode(char[])->char[]}
     *
     * @param fullyQualifiedTypes
     * @param pretty Pretty formatting
     * @return
     */
    public String toLQL(boolean fullyQualifiedTypes, boolean pretty) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("{\n");

        String delim = "\n";
        String spacing = "\t";

        if(CollectionUtils.isNotEmpty(constructors)) {
            // make sure to use Clazz name and not <init>
            constructors.forEach(c -> {
                c.setName(getName());
                c.setReturnType(name);
            });

            constructors.forEach(c -> {
                if(pretty) {
                    sb.append(spacing);
                }
                sb.append(c.toLQL(fullyQualifiedTypes, true))
                        .append(delim);
            });

            //sb.append(constructors.stream().map(c -> c.toLQL(fullyQualifiedTypes, true)).collect(Collectors.joining(delim)));

            sb.append(delim);
        }
        if(CollectionUtils.isNotEmpty(methods)) {
            methods.forEach(c -> {
                if(pretty) {
                    sb.append(spacing);
                }
                sb.append(c.toLQL(fullyQualifiedTypes, false))
                        .append(delim);
            });

            //sb.append(methods.stream().map(c -> c.toLQL(fullyQualifiedTypes, false)).collect(Collectors.joining(delim)));
            //sb.append(delim);
        }

        sb.append("}");

        return sb.toString();
    }
}
