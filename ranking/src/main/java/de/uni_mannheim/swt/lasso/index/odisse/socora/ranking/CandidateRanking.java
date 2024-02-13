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

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A candidate ranking
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateRanking {

    private CandidateItem[] candidateItems;

    /**
     * Measures about ranking performance
     */
    private Map<String, Double> rankingPerformanceMap;

    /**
     * @return the candidateItems
     */
    public CandidateItem[] getCandidateItems() {
        return candidateItems;
    }

    /**
     * @param candidateItems
     *            the candidateItems to set
     */
    public void setCandidateItems(CandidateItem[] candidateItems) {
        this.candidateItems = candidateItems;
    }

    /**
     * @return the rankingPerformanceMap
     */
    public Map<String, Double> getRankingPerformanceMap() {
        return rankingPerformanceMap;
    }

    /**
     * @param rankingPerformanceMap
     *            the rankingPerformanceMap to set
     */
    public void setRankingPerformanceMap(
            Map<String, Double> rankingPerformanceMap) {
        this.rankingPerformanceMap = rankingPerformanceMap;
    }

    /**
     * @param criterion
     *            {@link Criterion} instance
     * @return {@link Stats} instance for given {@link Criterion}
     */
    public Stats getStats(Criterion criterion) {
        if (MapUtils.isEmpty(rankingPerformanceMap)) {
            return null;
        }

        Stats stats = new Stats();
        for (String key : rankingPerformanceMap.keySet()) {
            // metric
            if (StringUtils.startsWith(key, criterion.getId() + "_")) {
                String suffix = StringUtils.substringAfterLast(key, "_");
                Double val = rankingPerformanceMap.get(key);
                // set
                switch (suffix) {
                case "sum":
                    stats.setSum(val);
                    break;
                case "min":
                    stats.setMin(val);
                    break;
                case "max":
                    stats.setMax(val);
                    break;
                case "mean":
                    stats.setMean(val);
                    break;
                case "stdev":
                    stats.setStdev(val);
                    break;
                default:
                    //
                }
            }
        }

        return stats;
    }
}
