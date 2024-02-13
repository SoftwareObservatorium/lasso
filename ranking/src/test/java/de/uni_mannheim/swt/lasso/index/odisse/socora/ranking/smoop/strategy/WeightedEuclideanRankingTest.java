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

import org.junit.Test;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Tester;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.SmoopRanking;

/**
 * {@link HybridNonDominatedSortingRanking} tests
 * 
 * @author Marcus Kessel
 *
 */
public class WeightedEuclideanRankingTest {

    private SmoopRanking smoopRanking;

    public WeightedEuclideanRankingTest() throws IOException {
        smoopRanking = new SmoopRanking();
    }

    @Test
    public void test() throws IOException {
        WeightedEuclideanRanking rankingStrategy = new WeightedEuclideanRanking(
                smoopRanking);

        Criterion[] criteria = new Criterion[] { Tester.toCriterion("metric1",
                Criterion.OBJECTIVE_MAX, 1d, 1d) };

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
        for (int i = 1; i < candidateRanking.getCandidateItems().length; i++) {
            assertEquals(i, candidateRanking.getCandidateItems()[i - 1]
                    .getRanking().get(WeightedEuclideanRanking.ID).intValue());
        }
    }
}
