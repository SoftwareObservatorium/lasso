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

import java.util.*;

import org.apache.commons.lang3.StringUtils;

/**
 * A compilation unit.
 * 
 * @author Marcus Kessel
 * 
 */
public class CompilationUnit {
    
    private int byteCodeVersion;
    private String byteCodeName;
    
    private List<String> javaKeywords;
    
    private List<String> dependencies;

    private String id;
    private String hash;
    private String name;
    private String packageName;
    private int fromLine;
    private int toLine;
    private boolean isInnerClass;
    private boolean isAnonymousClass;

    private ClassType type;

    private List<ModifierType> modifiers;

    private String superClassName;

    private Set<String> interfaceNames;

    private Set<String> superClassNames;

    private Set<String> interfaceClassNames;

    private List<String> annotations;

    private List<Method> methods;

    private Date lastModified = new Date();

    private String sourceCode;

    private boolean source;

    private String uri;
    private String origin;
    
    private String artifactId;
    private String groupId;
    private String version;

    private boolean generic;

    private Map<String, Double> measures = new HashMap<>();

    private List<Method> inheritedMethods;

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

    public void addMethod(Method method) {
        if (methods == null) {
            methods = new ArrayList<Method>();
        }

        methods.add(method);
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
     * @return the type
     */
    public ClassType getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(ClassType type) {
        this.type = type;
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

    /**
     * @return the isInnerClass
     */
    public boolean isInnerClass() {
        return isInnerClass;
    }

    /**
     * @param isInnerClass
     *            the isInnerClass to set
     */
    public void setInnerClass(boolean isInnerClass) {
        this.isInnerClass = isInnerClass;
    }

    /**
     * @return the isAnonymousClass
     */
    public boolean isAnonymousClass() {
        return isAnonymousClass;
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
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @param packageName
     *            the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @param isAnonymousClass
     *            the isAnonymousClass to set
     */
    public void setAnonymousClass(boolean isAnonymousClass) {
        this.isAnonymousClass = isAnonymousClass;
    }

    /**
     * @return the methods
     */
    public List<Method> getMethods() {
        return methods;
    }

    /**
     * @param methods
     *            the methods to set
     */
    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    /**
     * @return the superClassName
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * @param superClassName
     *            the superClassName to set
     */
    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    /**
     * @return the interfaceNames
     */
    public Set<String> getInterfaceNames() {
        return interfaceNames;
    }

    /**
     * @param interfaceNames
     *            the interfaceNames to set
     */
    public void setInterfaceNames(Set<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    public Set<String> getSuperClassNames() {
        return superClassNames;
    }

    public void setSuperClassNames(Set<String> superClassNames) {
        this.superClassNames = superClassNames;
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

        // set source flag to true if source code available
        this.source = StringUtils.isNotEmpty(sourceCode);
    }

    /**
     * @return the source
     */
    public boolean isSource() {
        return source;
    }

    /**
     * @param source
     *            the source to set
     */
    public void setSource(boolean source) {
        this.source = source;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri
     *            the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the origin
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * @param origin
     *            the origin to set
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return the javaKeywords
     */
    public List<String> getJavaKeywords() {
        return javaKeywords;
    }

    /**
     * @param javaKeywords the javaKeywords to set
     */
    public void setJavaKeywords(List<String> javaKeywords) {
        this.javaKeywords = javaKeywords;
    }

    /**
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the byteCodeVersion
     */
    public int getByteCodeVersion() {
        return byteCodeVersion;
    }

    /**
     * @param byteCodeVersion the byteCodeVersion to set
     */
    public void setByteCodeVersion(int byteCodeVersion) {
        this.byteCodeVersion = byteCodeVersion;
    }

    /**
     * @return the byteCodeName
     */
    public String getByteCodeName() {
        return byteCodeName;
    }

    /**
     * @param byteCodeName the byteCodeName to set
     */
    public void setByteCodeName(String byteCodeName) {
        this.byteCodeName = byteCodeName;
    }

    /**
     * @return the measures
     */
    public Map<String, Double> getMeasures() {
        return measures;
    }

    /**
     * @return the dependencies
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * @param dependencies the dependencies to set
     */
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public boolean isGeneric() {
        return generic;
    }

    public void setGeneric(boolean generic) {
        this.generic = generic;
    }

    public Set<String> getInterfaceClassNames() {
        return interfaceClassNames;
    }

    public void setInterfaceClassNames(Set<String> interfaceClassNames) {
        this.interfaceClassNames = interfaceClassNames;
    }

    public List<Method> getInheritedMethods() {
        return inheritedMethods;
    }

    public void setInheritedMethods(List<Method> inheritedMethods) {
        this.inheritedMethods = inheritedMethods;
    }
}
