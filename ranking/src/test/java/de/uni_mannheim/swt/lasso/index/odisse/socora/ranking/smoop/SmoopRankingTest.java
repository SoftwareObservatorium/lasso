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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.perf.RankingPerformanceCollector;

/**
 * {@link SmoopRanking} tests
 * 
 * @author Marcus Kessel
 *
 */
public class SmoopRankingTest {

    private SmoopRanking smoopRanking;

    public SmoopRankingTest() throws IOException {
        smoopRanking = new SmoopRanking();
    }

    @Test
    public void testHds() throws IOException {
        Criterion entry1 = new Criterion();
        entry1.setId("metric1");
        entry1.setObjective(0d);
        entry1.setPriority(1d);
        entry1.setWeight(0.5d);

        Criterion entry2 = new Criterion();
        entry2.setId("metric2");
        entry2.setObjective(0d);
        entry2.setPriority(1d);
        entry2.setWeight(0.5d);

        Criterion[] criteria = new Criterion[] { entry1, entry2 };

        CandidateItemJs item1 = new CandidateItemJs();
        Map<String, Double> map1 = new HashMap<>();
        map1.put("metric1", 2d);
        map1.put("metric2", 2d);
        item1.setMetricsMap(map1);

        CandidateItemJs item2 = new CandidateItemJs();
        Map<String, Double> map2 = new HashMap<>();
        map2.put("metric1", 4d);
        map2.put("metric2", 4d);
        item2.setMetricsMap(map2);

        CandidateItemJs[] CandidateItemJss = new CandidateItemJs[] { item1,
                item2 };

        RankingPerformanceCollector rankingPerformanceCollector = new RankingPerformanceCollector();

        // rank
        smoopRanking.hds(criteria, CandidateItemJss,
                rankingPerformanceCollector);

        // check if passed objects changed
        for (CandidateItemJs item : CandidateItemJss) {
            System.out.println(ToStringBuilder.reflectionToString(item));

            for (String key : item.getRanking().keySet()) {
                System.out.println(key + " = " + item.getRanking().get(key));
            }
        }

        // rank perf measures
        for (String rankMetricName : rankingPerformanceCollector
                .getMetricsMap().keySet()) {
            System.out.println(rankMetricName
                    + " = "
                    + rankingPerformanceCollector.getMetricsMap().get(
                            rankMetricName));
        }
    }

    @Test
    public void testSingle() throws IOException {
        // max
        Criterion entry1 = new Criterion();
        entry1.setId("metric1");
        entry1.setObjective(1d);
        entry1.setPriority(1d);
        entry1.setWeight(0.5d);

        // max, but not really needed for single as only first objective is
        // evaluated
        Criterion entry2 = new Criterion();
        entry2.setId("metric2");
        entry2.setObjective(1d);
        entry2.setPriority(1d);
        entry2.setWeight(0.5d);

        Criterion[] criteria = new Criterion[] { entry1, entry2 };

        CandidateItemJs item1 = new CandidateItemJs();
        Map<String, Double> map1 = new HashMap<>();
        map1.put("metric1", 2d);
        map1.put("metric2", 2d);
        item1.setMetricsMap(map1);

        CandidateItemJs item2 = new CandidateItemJs();
        Map<String, Double> map2 = new HashMap<>();
        map2.put("metric1", 4d);
        map2.put("metric2", 4d);
        item2.setMetricsMap(map2);

        CandidateItemJs[] candidateItemJss = new CandidateItemJs[] { item1,
                item2 };

        RankingPerformanceCollector rankingPerformanceCollector = new RankingPerformanceCollector();

        // rank
        smoopRanking.single(criteria, candidateItemJss,
                rankingPerformanceCollector);

        // check if passed objects changed
        for (CandidateItemJs item : candidateItemJss) {
            System.out.println(ToStringBuilder.reflectionToString(item));

            for (String key : item.getRanking().keySet()) {
                System.out.println(key + " = " + item.getRanking().get(key));
            }
        }
        
        // rank perf measures
        for (String rankMetricName : rankingPerformanceCollector
                .getMetricsMap().keySet()) {
            System.out.println(rankMetricName
                    + " = "
                    + rankingPerformanceCollector.getMetricsMap().get(
                            rankMetricName));
        }
    }
}
