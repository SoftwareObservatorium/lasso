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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop;

import java.util.Map;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.js.MapScriptable;

/**
 * JS-enabled model representing a component candidate in a ranking
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateItemJs extends CandidateItem {

    /**
     * Representation of {@link #metricsMap} in JavaScript (sort of a bridge)
     */
    private MapScriptable metrics;
    
    private MapScriptable normalized;

    /**
     * @param metricsMap
     *            the metricsMap to set
     */
    public void setMetricsMap(Map<String, Double> metricsMap) {
        // call super
        super.setMetricsMap(metricsMap);

        // set JS equivalent
        setMetrics(new MapScriptable(super.getMetricsMap()));
    }

    /**
     * @param metrics
     *            the metrics to set
     */
    public void setMetrics(MapScriptable metrics) {
        this.metrics = metrics;
    }

    /**
     * @return the metrics
     */
    public MapScriptable getMetrics() {
        return metrics;
    }
}
