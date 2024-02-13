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

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Model representing a class
 * 
 * @author Marcus Kessel
 *
 */
public class CompilationUnit implements Serializable {

    private String name;
    private String pkg;

    private String sourceCode;

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
     * @return the pkg
     */
    public String getPkg() {
        return pkg;
    }

    /**
     * @param pkg
     *            the pkg to set
     */
    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    /**
     * @return fully-qualified Java class name
     */
    public String getFQName() {
        return toJavaFullyQualifedName(pkg, name);
    }

    /**
     * @return byte code class name including file suffix
     */
    public String getByteCodeClassName() {
        return toByteCodeClass(getFQName());
    }

    /**
     * @return relative Java path including suffix *.java
     */
    public String getJavaClassPath() {
        return toJavaPkgPath(getFQName());
    }

    /**
     * @return the sourceCode
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * @param sourceCode
     *            the sourceCode to set
     */
    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    /**
     * Convert Java fully-qualified class name to byte code class name.
     *
     * @param fqName
     *            Java fully-qualified class name
     * @return byte code representation including *.class suffix
     */
    public static String toByteCodeClass(String fqName) {
        return StringUtils.replace(fqName, ".", "/") + ".class";
    }

    /**
     * From byte code notation to Java notation.
     *
     * @param bFqName
     *            Byte code fully-qualified name
     * @return Java fully-qualified name
     */
    public static String toJavaFullyQualifiedName(String bFqName) {
        return StringUtils.replace(bFqName, "/", ".");
    }

    /**
     * To local Java file including pkg path.
     *
     * @param fqName
     *            Java fully-qualified class name
     * @return pkg path including *.java suffix
     */
    public static String toJavaPkgPath(String fqName) {
        return StringUtils.replace(fqName, ".", "/") + ".java";
    }

    /**
     * Get fully qualified class name based on package + class name.
     *
     * @param packageName
     *            Java package name
     * @param className
     *            Java class name
     * @return packageName.ClassName
     */
    public static String toJavaFullyQualifedName(String packageName,
                                                 String className) {
        if (StringUtils.isBlank(packageName)) {
            return className;
        } else {
            return packageName + "." + className;
        }
    }
}
