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

import java.util.HashMap;
import java.util.Map;

/**
 * Testing utilities
 * 
 * @author Marcus Kessel
 *
 */
public class Tester {

    public static Criterion toCriterion(String id, double objective,
            double priority, double weight) {
        Criterion criterion = new Criterion();
        criterion.setId(id);
        criterion.setObjective(objective);
        criterion.setPriority(priority);
        criterion.setWeight(weight);

        return criterion;
    }

    public static CandidateItem toCandidateItem(String id,
            String[] metricNames, double[] measures) {
        CandidateItem item = new CandidateItem();
        item.setId(id);

        Map<String, Double> map = new HashMap<>();
        int i = 0;
        for (String metricName : metricNames) {
            map.put(metricName, measures[i++]);
        }
        item.setMetricsMap(map);

        return item;
    }
}
