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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Model representing a component candidate in a ranking
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateItem {

    /**
     * Candidate id
     */
    private String id;

    /**
     * Metric name + measured value
     */
    private Map<String, Double> metricsMap;

    /**
     * Map of ranking method + score
     */
    private Map<String, Double> ranking;

    /**
     * Ranking criteria can be safely used for this candidate?
     */
    private Map<String, Boolean> safeRankingCriteria;

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
     * @return the ranking
     */
    public Map<String, Double> getRanking() {
        return ranking;
    }

    /**
     * @return normalized ranking criteria measures in range [0, 100] (MIN
     *         objectives are converted in MAX objectives)
     */
    public Map<String, Double> getNormalizedMeasures() {
        if (MapUtils.isEmpty(ranking)) {
            return Collections.emptyMap();
        }

        Map<String, Double> normMap = new LinkedHashMap<String, Double>();
        for (String key : ranking.keySet()) {
            if (StringUtils.endsWith(key, "_norm")) {
                normMap.put(StringUtils.substringBeforeLast(key, "_norm"),
                        ranking.get(key));
            }
        }

        return normMap;
    }

    /**
     * @param ranking
     *            the ranking to set
     */
    public void setRanking(Map<String, Double> ranking) {
        this.ranking = ranking;
    }

    /**
     * @return the metricsMap
     */
    public Map<String, Double> getMetricsMap() {
        return metricsMap;
    }

    /**
     * @param metricsMap
     *            the metricsMap to set
     */
    public void setMetricsMap(Map<String, Double> metricsMap) {
        this.metricsMap = metricsMap;
    }

    /**
     * @return the safeRankingCriteria
     */
    public Map<String, Boolean> getSafeRankingCriteria() {
        return safeRankingCriteria;
    }

    /**
     * @param safeRankingCriteria
     *            the safeRankingCriteria to set
     */
    public void setSafeRankingCriteria(Map<String, Boolean> safeRankingCriteria) {
        this.safeRankingCriteria = safeRankingCriteria;
    }
}
