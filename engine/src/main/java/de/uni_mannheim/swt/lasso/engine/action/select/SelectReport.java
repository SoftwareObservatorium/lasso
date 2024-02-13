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
package de.uni_mannheim.swt.lasso.engine.action.select;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.index.collect.CandidateResultCollector;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author Marcus Kessel
 */
public class SelectReport extends LassoReport {

    @QuerySqlField(index = true)
    private String name;
    @QuerySqlField(index = true)
    private String packageName;
    @QuerySqlField(index = true)
    private double score;
    @QuerySqlField(index = true)
    private String uri;
    @QuerySqlField(index = true)
    private String signature;

    /**
     * @see de.uni_mannheim.swt.lasso.datasource.maven.MavenImplementation#hash
     */
    @QuerySqlField(index = false)
    private String hash;

    /**
     * 
     * @see de.uni_mannheim.swt.lasso.datasource.maven.clone.CloneDetection#type1Hash(String) 
     */
    @QuerySqlField(index = true)
    private String cloneType1Hash;
    /**
     * @see CandidateResultCollector#collapseByHashClone(SolrDocumentList)
     */
    @QuerySqlField(index = false)
    private int hashClones;
    /**
     * @see CandidateResultCollector#collapseBySimilarNaming(SolrDocumentList)
     */
    @QuerySqlField(index = false)
    private int namingClones;

    @QuerySqlField(index = false)
    private int type1Clones;

    /**
     * Solr collapse
     */
    @QuerySqlField(index = false)
    private int collapseByAlternatives;

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

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCloneType1Hash() {
        return cloneType1Hash;
    }

    public void setCloneType1Hash(String cloneType1Hash) {
        this.cloneType1Hash = cloneType1Hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getHashClones() {
        return hashClones;
    }

    public void setHashClones(int hashClones) {
        this.hashClones = hashClones;
    }

    public int getNamingClones() {
        return namingClones;
    }

    public void setNamingClones(int namingClones) {
        this.namingClones = namingClones;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getCollapseByAlternatives() {
        return collapseByAlternatives;
    }

    public void setCollapseByAlternatives(int collapseByAlternatives) {
        this.collapseByAlternatives = collapseByAlternatives;
    }

    public int getType1Clones() {
        return type1Clones;
    }

    public void setType1Clones(int type1Clones) {
        this.type1Clones = type1Clones;
    }
}
