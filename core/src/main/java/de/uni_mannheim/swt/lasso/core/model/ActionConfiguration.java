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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class ActionConfiguration implements Serializable {

    public static final String DROP_FAILED = "dropFailed";
    public static final String DISABLE_PARTITIONING = "disablePartitioning";

    // used for all FAs
    private List<String> dependsOnActions;

    // used per FA
    private String dependsOn;
    private Map<String, Serializable> configuration = createDefaultSettings();
    private Profile profile;

    private Scope scope;

    private Abstraction abstraction;

    private String includeTestsPattern;

    private static Map<String, Serializable> createDefaultSettings() {
        Map<String, Serializable> defaultConfiguration = new HashMap<>();

        //-- defaults
        // drop candidates with no report by default
        defaultConfiguration.put(ActionConfiguration.DROP_FAILED, true);
        defaultConfiguration.put(ActionConfiguration.DISABLE_PARTITIONING, false);

        return defaultConfiguration;
    }

    public ActionConfiguration fromAbstraction(Abstraction abstraction) {
        ActionConfiguration cfg = new ActionConfiguration();
        cfg.setDependsOn(dependsOn);
        cfg.setProfile(profile);
        cfg.setScope(scope);
        cfg.configuration = new HashMap<>(configuration);
        cfg.setAbstraction(abstraction);
        cfg.setIncludeTestsPattern(includeTestsPattern);
        cfg.setDependsOnActions(dependsOnActions);

        return cfg;
    }

    public boolean hasIncludeTestsPattern() {
        return StringUtils.isNotBlank(includeTestsPattern);
    }

    public void resetConfiguration(Map<String, Serializable> configuration) {
        this.configuration = configuration;
    }

    public Map<String, Serializable> getConfiguration() {
        return configuration;
    }

    public void addConfiguration(Map<String, Serializable> configuration) {
        //
        this.configuration.putAll(configuration);
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Abstraction getAbstraction() {
        return abstraction;
    }

    public void setAbstraction(Abstraction abstraction) {
        this.abstraction = abstraction;
    }

    /**
     * Drop failed candidates?
     *
     * @return
     */
    public boolean isDropFailed() {
        if(configuration.containsKey(DROP_FAILED)) {
            return (boolean) configuration.get(DROP_FAILED);
        }

        return false;
    }

    /**
     * Disable partitioning of implementations?
     *
     * @return
     */
    public boolean isDisablePartitioning() {
        if(configuration.containsKey(DISABLE_PARTITIONING)) {
            return (boolean) configuration.get(DISABLE_PARTITIONING);
        }

        return false;
    }

    public String getIncludeTestsPattern() {
        return includeTestsPattern;
    }

    public void setIncludeTestsPattern(String includeTestsPattern) {
        this.includeTestsPattern = includeTestsPattern;
    }

    public List<String> getDependsOnActions() {
        return dependsOnActions;
    }

    public void setDependsOnActions(List<String> dependsOnActions) {
        this.dependsOnActions = dependsOnActions;
    }
}
