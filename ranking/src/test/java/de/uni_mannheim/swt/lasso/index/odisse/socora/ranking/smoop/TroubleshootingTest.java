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
import java.util.Random;

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
public class TroubleshootingTest {

    private SmoopRanking smoopRanking;

    public TroubleshootingTest() throws IOException {
        smoopRanking = new SmoopRanking();
    }

    @Test
    public void testHds() throws IOException {
        Criterion entry1 = new Criterion();
        entry1.setId("m_pa_tc_passed");
        entry1.setObjective(1d);
        entry1.setPriority(1d);
        entry1.setWeight(0.0d);

        Criterion[] criteria = new Criterion[] { entry1 };
        
        double[] rnd = new double[] {0d, 1d, 2d, 3d};
        Random random = new Random();
        
        CandidateItemJs[] CandidateItemJss = new CandidateItemJs[1576];
        for(int i = 0; i < CandidateItemJss.length; i++) {
            CandidateItemJs item1 = new CandidateItemJs();
            Map<String, Double> map1 = new HashMap<>();
            map1.put(entry1.getId(), rnd[random.nextInt(rnd.length)]);
            item1.setMetricsMap(map1);
            
            CandidateItemJss[i] = item1;
        }
        
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
}
