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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.perf;

import java.util.HashMap;
import java.util.Map;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.js.MapScriptable;

/**
 * Ranking performance measures
 * 
 * @author Marcus Kessel
 *
 */
public class RankingPerformanceCollector {

    /**
     * Ranking performance measures
     */
    private Map<String, Double> metricsMap = new HashMap<String, Double>();

    /**
     * Representation of {@link #metricsMap} in JavaScript (sort of a bridge)
     */
    private MapScriptable metrics = new MapScriptable(metricsMap);

    /**
     * @return the metrics
     */
    public MapScriptable getMetrics() {
        return metrics;
    }

    /**
     * @param metrics
     *            the metrics to set
     */
    public void setMetrics(MapScriptable metrics) {
        this.metrics = metrics;
    }

    /**
     * @return the metricsMap
     */
    public Map<String, Double> getMetricsMap() {
        return metricsMap;
    }
}
