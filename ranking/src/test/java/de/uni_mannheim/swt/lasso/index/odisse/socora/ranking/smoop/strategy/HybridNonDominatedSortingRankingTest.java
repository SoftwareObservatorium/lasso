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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.strategy;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Stats;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Tester;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.SmoopRanking;

/**
 * {@link HybridNonDominatedSortingRanking} tests
 * 
 * @author Marcus Kessel
 *
 */
public class HybridNonDominatedSortingRankingTest {

    private SmoopRanking smoopRanking;

    public HybridNonDominatedSortingRankingTest() throws IOException {
        smoopRanking = new SmoopRanking();
    }

    @Test
    public void test() throws IOException {
        HybridNonDominatedSortingRanking rankingStrategy = new HybridNonDominatedSortingRanking(
                smoopRanking);

        // enable rank perf
        rankingStrategy.setMeasureRankingPerformance(true);

        Criterion[] criteria = new Criterion[] {
                Tester.toCriterion("metric1", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("metric2", Criterion.OBJECTIVE_MAX, 1d, 1d) };

        CandidateItem[] candidateItems = new CandidateItem[] {
                Tester.toCandidateItem("c1", new String[] { "metric1",
                        "metric2" }, new double[] { 1, 2 }),
                Tester.toCandidateItem("c2", new String[] { "metric1",
                        "metric2" }, new double[] { 2, 2 }),
                Tester.toCandidateItem("c3", new String[] { "metric1",
                        "metric2" }, new double[] { 3, 3 }),
                Tester.toCandidateItem("c4", new String[] { "metric1",
                        "metric2" }, new double[] { 4, 4 }),
                Tester.toCandidateItem("c5", new String[] { "metric1",
                        "metric2" }, new double[] { 5, 5 }) };

        // rank
        CandidateRanking candidateRanking = rankingStrategy.rank(criteria,
                candidateItems);

        assertEquals(5, candidateRanking.getCandidateItems().length);

        // assert scores
        for (int i = 1; i <= candidateRanking.getCandidateItems().length; i++) {
            CandidateItem candidateItem = candidateRanking.getCandidateItems()[i - 1];
            assertEquals(
                    i,
                    candidateItem.getRanking()
                            .get(HybridNonDominatedSortingRanking.ID)
                            .intValue());

            // get normalized measures
            Map<String, Double> normalized = candidateItem
                    .getNormalizedMeasures();

            assertTrue(!normalized.isEmpty());

            for (String key : normalized.keySet()) {
                System.out.println(key + " = " + normalized.get(key));
            }
            System.out.println("----");
        }

        // criteria stats
        for (Criterion criterion : criteria) {
            Stats stats = candidateRanking.getStats(criterion);
            System.out.println(criterion.getId() + " = "
                    + ToStringBuilder.reflectionToString(stats));
        }
    }

    @Test
    public void test_rankperf_enabled() throws IOException {
        HybridNonDominatedSortingRanking rankingStrategy = new HybridNonDominatedSortingRanking(
                smoopRanking);

        // enable rank perf
        rankingStrategy.setMeasureRankingPerformance(true);

        Criterion[] criteria = new Criterion[] {
                Tester.toCriterion("metric1", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("metric2", Criterion.OBJECTIVE_MAX, 1d, 1d) };

        CandidateItem[] candidateItems = new CandidateItem[] {
                Tester.toCandidateItem("c1", new String[] { "metric1",
                        "metric2" }, new double[] { 1, 2 }),
                Tester.toCandidateItem("c2", new String[] { "metric1",
                        "metric2" }, new double[] { 2, 2 }),
                Tester.toCandidateItem("c3", new String[] { "metric1",
                        "metric2" }, new double[] { 3, 3 }),
                Tester.toCandidateItem("c4", new String[] { "metric1",
                        "metric2" }, new double[] { 4, 4 }),
                Tester.toCandidateItem("c5", new String[] { "metric1",
                        "metric2" }, new double[] { 5, 5 }) };

        // rank
        CandidateRanking candidateRanking = rankingStrategy.rank(criteria,
                candidateItems);

        assertEquals(5, candidateRanking.getCandidateItems().length);

        // assert scores
        for (int i = 1; i <= candidateRanking.getCandidateItems().length; i++) {
            assertEquals(i, candidateRanking.getCandidateItems()[i - 1]
                    .getRanking().get(HybridNonDominatedSortingRanking.ID)
                    .intValue());
        }

        // rank perf measures
        for (String rankMetricName : candidateRanking
                .getRankingPerformanceMap().keySet()) {
            System.out.println(rankMetricName
                    + " = "
                    + candidateRanking.getRankingPerformanceMap().get(
                            rankMetricName));
        }

        //
        assertEquals(5d,
                candidateRanking.getRankingPerformanceMap().get("nds_size"), 0d);
    }
    
    /**
     * Binary functions test.
     * 
     * @throws IOException
     */
    @Test
    public void test_binaryfunctions() throws IOException {
        HybridNonDominatedSortingRanking rankingStrategy = new HybridNonDominatedSortingRanking(
                smoopRanking);

        // enable rank perf
        rankingStrategy.setMeasureRankingPerformance(true);

        // features
        Criterion[] criteria = new Criterion[] {
                Tester.toCriterion("f1", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("f2", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("f3", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("f4", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("f5", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("f6", Criterion.OBJECTIVE_MAX, 1d, 1d)
        };

        CandidateItem[] candidateItems = new CandidateItem[] {
                // pi_0
                Tester.toCandidateItem("c1", new String[] { "f1", "f2", "f3", "f4", "f5", "f6"},
                        new double[] { 1, 1, 1, 1, 1, 1 }),    
                // pi_1
                Tester.toCandidateItem("c2", new String[] { "f1", "f2", "f3", "f4", "f5", "f6"},
                        new double[] { 1, 1, 1, 1, 1, 0 }),
                // pi_0
                Tester.toCandidateItem("c3", new String[] { "f1", "f2", "f3", "f4", "f5", "f6"},
                        new double[] { 1, 1, 1, 1, 1, 1 }),
                // pi_1
                Tester.toCandidateItem("c4", new String[] { "f1", "f2", "f3", "f4", "f5", "f6"},
                        new double[] { 0, 1, 1, 1, 1, 1 })
        };

        // rank
        CandidateRanking candidateRanking = rankingStrategy.rank(criteria,
                candidateItems);

        assertEquals(4, candidateRanking.getCandidateItems().length);

        // assert scores
        for (int i = 1; i <= candidateRanking.getCandidateItems().length; i++) {
            CandidateItem item = candidateRanking.getCandidateItems()[i - 1];
            assertEquals(i, item
                    .getRanking().get(HybridNonDominatedSortingRanking.ID)
                    .intValue());
            
            System.out.println(item
                    .getRanking().get(HybridNonDominatedSortingRanking.ID)
                    .intValue() + " " + item.getId());
        }

        // rank perf measures
        for (String rankMetricName : candidateRanking
                .getRankingPerformanceMap().keySet()) {
            System.out.println(rankMetricName
                    + " = "
                    + candidateRanking.getRankingPerformanceMap().get(
                            rankMetricName));
        }

        //
        assertEquals(2d,
                candidateRanking.getRankingPerformanceMap().get("nds_size"), 0d);
    }
}
