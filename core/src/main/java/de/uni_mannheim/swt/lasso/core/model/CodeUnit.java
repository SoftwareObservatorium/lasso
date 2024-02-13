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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * A code unit (e.g. class or method).
 *
 * @author Marcus Kessel
 */
public class CodeUnit implements Serializable {

    public boolean hasKeyword(String keyword) {
        if(unitType == CodeUnitType.METHOD) {
            if(CollectionUtils.isEmpty(getMethodSignatureParamsOrderedKeywordsFq())) {
                return false;
            }

            String keywords = getMethodSignatureParamsOrderedKeywordsFq().get(0);
            return StringUtils.contains(keywords, String.format("kw_%s", keyword));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public String getType1Hash() {
        return type1Hash;
    }

    public void setType1Hash(String type1Hash) {
        this.type1Hash = type1Hash;
    }

    public List<CodeUnit> getType1Clones() {
        return type1Clones;
    }

    public void setType1Clones(List<CodeUnit> type1Clones) {
        this.type1Clones = type1Clones;
    }

    private List<CodeUnit> clonesDetected;

    public List<CodeUnit> getClonesDetected() {
        return clonesDetected;
    }

    public void setClonesDetected(List<CodeUnit> clonesDetected) {
        this.clonesDetected = clonesDetected;
    }

    public String getLql() {
        return lql;
    }

    public void setLql(String lql) {
        this.lql = lql;
    }

    /**
     * Code unit.
     *
     * @author Marcus Kessel
     */
    public enum CodeUnitType {
        CLASS,
        METHOD,
        //MODULE // a coherent system of classes (e.g., project)
    }

    private String workerNodeId;

    private String id;

    private String dataSource;

    private String parentId;

    private String name;

    private String packagename;

    private String bytecodeName;

    private String groupId;

    private String artifactId;

    private String version;

    private String classifier;

    private double score;

    private String content;

    private String hash;

    private String type1Hash;

    private String docType;

    private String type;

    private List<String> methods;

    private List<String> superClasses;

    private List<String> interfaces;

    private List<String> dependencies;

    private Map<String, Double> measures;

    private Map<String, Object> metaData;

    private List<String> inheritedMethods;

    /**
     * Alternatives to current document (e.g., from same project)
     *
     * @return
     */
    private List<CodeUnit> alternatives;

    /**
     * Get clone documents (e.g., hash clones)
     *
     * @return
     */
    private List<CodeUnit> clones;

    /**
     * Get similar documents (e.g., likely clones)
     *
     * @return
     */
    private List<CodeUnit> similar;

    private List<CodeUnit> type1Clones;

    private CodeUnitType unitType;

    private List<String> methodSignatureParamsOrderedKeywordsFq;
    private List<String> methodNames;
    private List<String> methodBytecodeNames;

    private String lql;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeUnit that = (CodeUnit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * @return Artifact identifier
     */
    public String toUri() {
        return StringUtils.join(Arrays.asList(getGroupId(), getArtifactId(), getVersion()), ':');
    }

    /**
     * @return Fully-qualified class name of entry class of this candidate in
     *         Java-like format
     */
    public String toFQName() {
        return StringUtils.join(Arrays.asList(getPackagename(), getName()), '.');
    }

    /**
     * @param other
     *            {@link CodeUnit} instance
     * @return true if they are similar based on groupId, artifactId, name and
     *         packagename
     */
    public boolean isSimilar(CodeUnit other) {
        return other != null && StringUtils.equals(getGroupId(), other.getGroupId())
                && StringUtils.equals(getArtifactId(), other.getArtifactId())
                && StringUtils.equals(getName(), other.getName())
                && StringUtils.equals(getPackagename(), other.getPackagename());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackagename() {
        return packagename;
    }

    public void setPackagename(String packagename) {
        this.packagename = packagename;
    }

    public String getBytecodeName() {
        return bytecodeName;
    }

    public void setBytecodeName(String bytecodeName) {
        this.bytecodeName = bytecodeName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getSuperClasses() {
        return superClasses;
    }

    public void setSuperClasses(List<String> superClasses) {
        this.superClasses = superClasses;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, Double> getMeasures() {
        return measures;
    }

    public void setMeasures(Map<String, Double> measures) {
        this.measures = measures;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }

    public List<String> getInheritedMethods() {
        return inheritedMethods;
    }

    public void setInheritedMethods(List<String> inheritedMethods) {
        this.inheritedMethods = inheritedMethods;
    }

    public List<CodeUnit> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<CodeUnit> alternatives) {
        this.alternatives = alternatives;
    }

    public List<CodeUnit> getClones() {
        return clones;
    }

    public void setClones(List<CodeUnit> clones) {
        this.clones = clones;
    }

    public List<CodeUnit> getSimilar() {
        return similar;
    }

    public void setSimilar(List<CodeUnit> similar) {
        this.similar = similar;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public CodeUnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(CodeUnitType unitType) {
        this.unitType = unitType;
    }

    public List<String> getMethodSignatureParamsOrderedKeywordsFq() {
        return methodSignatureParamsOrderedKeywordsFq;
    }

    public void setMethodSignatureParamsOrderedKeywordsFq(List<String> methodSignatureParamsOrderedKeywordsFq) {
        this.methodSignatureParamsOrderedKeywordsFq = methodSignatureParamsOrderedKeywordsFq;
    }

    public List<String> getMethodNames() {
        return methodNames;
    }

    public void setMethodNames(List<String> methodNames) {
        this.methodNames = methodNames;
    }

    public List<String> getMethodBytecodeNames() {
        return methodBytecodeNames;
    }

    public void setMethodBytecodeNames(List<String> methodBytecodeNames) {
        this.methodBytecodeNames = methodBytecodeNames;
    }

    @Deprecated
    public String getWorkerNodeId() {
        return workerNodeId;
    }

    @Deprecated
    public void setWorkerNodeId(String workerNodeId) {
        this.workerNodeId = workerNodeId;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}
