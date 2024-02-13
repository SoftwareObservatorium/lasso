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
package de.uni_mannheim.swt.lasso.cluster.client;

import de.uni_mannheim.swt.lasso.core.model.Scope;
import de.uni_mannheim.swt.lasso.core.model.System;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class ArenaJob {

    private String id;
    private String executionId;
    private String workerNode;

    private String abstractionId;
    private String actionId;

    private JobStatus status = JobStatus.CREATED;

    private Date lastModified = new Date();
    private List<System> implementations;
    private Map<String,String> sheets;

    private String cut;

    private String specification;

    private Map<String, Object> configuration = new LinkedHashMap<>();

    private boolean referenceImplementationOnly;

    /**
     * Adapt by sequence specification or by entire set of sequence specifications.
     */
    private boolean bySequenceSpecification;

    private Scope scope;

    private int threads = -1;

    public int getMaxPermutations() {
        if(configuration.containsKey("perms")) {
            return (int) configuration.get("perms");
        } else {
            return -1;
        }
    }

    public boolean isGenerateJUnitTests() {
        if(configuration.containsKey("generateTests")) {
            return (boolean) configuration.get("generateTests");
        } else {
            return false;
        }
    }

    public boolean isIgnoreVisibility() {
        if(configuration.containsKey("ignoreVisibility")) {
            return (boolean) configuration.get("ignoreVisibility");
        } else {
            return false;
        }
    }

    public boolean isWriteSequenceRecords() {
        if(configuration.containsKey("writeSequenceRecords")) {
            return (boolean) configuration.get("writeSequenceRecords");
        } else {
            return false;
        }
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getWorkerNode() {
        return workerNode;
    }

    public void setWorkerNode(String workerNode) {
        this.workerNode = workerNode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<System> getImplementations() {
        return implementations;
    }

    public void setImplementations(List<System> implementations) {
        this.implementations = implementations;
    }

    public Map<String,String> getSheets() {
        return sheets;
    }

    public void setSheets(Map<String,String> sheets) {
        this.sheets = sheets;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getAbstractionId() {
        return abstractionId;
    }

    public void setAbstractionId(String abstractionId) {
        this.abstractionId = abstractionId;
    }

    public String getCut() {
        return cut;
    }

    public void setCut(String cut) {
        this.cut = cut;
    }

    public boolean isReferenceImplementationOnly() {
        return referenceImplementationOnly;
    }

    public void setReferenceImplementationOnly(boolean referenceImplementationOnly) {
        this.referenceImplementationOnly = referenceImplementationOnly;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean isBySequenceSpecification() {
        return bySequenceSpecification;
    }

    public void setBySequenceSpecification(boolean bySequenceSpecification) {
        this.bySequenceSpecification = bySequenceSpecification;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
