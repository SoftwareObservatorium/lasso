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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A method
 * 
 * @author Marcus Kessel
 * 
 */
public class Method {

    private long id;
    private String hash;
    private String name;
    private int fromLine;
    private int toLine;
    private List<ModifierType> modifiers;
    private List<String> exceptions;
    private Parameter returnParameter;
    private List<Parameter> parameters;
    private String ownerClass;
    private List<String> annotations;
    private Date lastModified = new Date();

    private List<String> javaKeywords;
    
    private String content;
    
    private Map<String, Double> measures = new HashMap<>();
    
    private String byteCodeName;
    
    private Set<String> dependencies;
    private Set<String> calls;

    /**
     * @return the javaKeywords
     */
    public List<String> getJavaKeywords() {
        return javaKeywords;
    }

    /**
     * @param javaKeywords
     *            the javaKeywords to set
     */
    public void setJavaKeywords(List<String> javaKeywords) {
        this.javaKeywords = javaKeywords;
    }

    public boolean isInheritable(boolean samePackage) {
        if(samePackage && (javaKeywords == null || !javaKeywords.contains("private"))) {
            return true;
        }

        // not same package
        return javaKeywords != null && !javaKeywords.contains("private");
    }

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

    public void addParameter(Parameter parameter) {
        if (parameters == null) {
            parameters = new ArrayList<Parameter>();
        }

        parameters.add(parameter);
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
     * @return the returnParameter
     */
    public Parameter getReturnParameter() {
        return returnParameter;
    }

    /**
     * @param returnParameter
     *            the returnParameter to set
     */
    public void setReturnParameter(Parameter returnParameter) {
        this.returnParameter = returnParameter;
    }

    /**
     * @return the parameters
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * @param parameters
     *            the parameters to set
     */
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the ownerClass
     */
    public String getOwnerClass() {
        return ownerClass;
    }

    /**
     * @param ownerClass
     *            the ownerClass to set
     */
    public void setOwnerClass(String ownerClass) {
        this.ownerClass = ownerClass;
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
     * @return the constructor
     */
    public boolean isConstructor() {
        return StringUtils.equals(name, "<init>");
    }

    /**
     * @return the constructor
     */
    public boolean isStaticInit() {
        return StringUtils.equals(name, "<clinit>");
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

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(ownerClass);
        sb.append(" ");
        sb.append(StringUtils.join(modifiers, " "));
        sb.append(" ");
        sb.append(name);
        sb.append("(");

        if (CollectionUtils.isNotEmpty(parameters)) {
            List<String> params = new ArrayList<String>();
            for (Parameter paramField : parameters) {
                String param = paramField.getType();

                if (paramField.getArrayDim() > 0) {
                    param += StringUtils.repeat("[]", paramField.getArrayDim());
                    // param += StringUtils.repeat("]",
                    // paramField.getArrayDim());
                }

                params.add(param);
            }
            sb.append(StringUtils.join(params, ", "));
        }
        sb.append(") ");
        sb.append(StringUtils.join(exceptions, ", "));

        return sb.toString();
    }

    /**
     * @return the measures
     */
    public Map<String, Double> getMeasures() {
        return measures;
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((byteCodeName == null) ? 0 : byteCodeName.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Method other = (Method) obj;
        if (byteCodeName == null) {
            if (other.byteCodeName != null)
                return false;
        } else if (!byteCodeName.equals(other.byteCodeName))
            return false;
        return true;
    }

    /**
     * @return the dependencies
     */
    public Set<String> getDependencies() {
        return dependencies;
    }

    /**
     * @param dependencies the dependencies to set
     */
    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * @return the calls
     */
    public Set<String> getCalls() {
        return calls;
    }

    /**
     * @param calls the calls to set
     */
    public void setCalls(Set<String> calls) {
        this.calls = calls;
    }
}
