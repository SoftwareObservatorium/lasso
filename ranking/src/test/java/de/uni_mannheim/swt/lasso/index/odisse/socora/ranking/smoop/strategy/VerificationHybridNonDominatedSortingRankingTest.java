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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
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
public class VerificationHybridNonDominatedSortingRankingTest {

    private SmoopRanking smoopRanking;

    public VerificationHybridNonDominatedSortingRankingTest()
            throws IOException {
        smoopRanking = new SmoopRanking();
    }

    @Test
    public void test_hds() throws IOException {
        HybridNonDominatedSortingRanking hdsRankingStrategy = new HybridNonDominatedSortingRanking(
                smoopRanking);

        NonDominatedSortingRanking ndsRankingStrategy = new NonDominatedSortingRanking(
                smoopRanking);

        // enable rank perf
        hdsRankingStrategy.setMeasureRankingPerformance(true);

        Criterion[] criteria = new Criterion[] {
                Tester.toCriterion("leanness", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("throughput", Criterion.OBJECTIVE_MAX, 1d,
                        1d),
         Tester.toCriterion("cohesion", Criterion.OBJECTIVE_MIN, 1d, 1d),
         Tester.toCriterion("coupling", Criterion.OBJECTIVE_MIN, 1d, 1d)

        };

        CandidateItem[] candidateItems = new CandidateItem[] {
                // Tester.toCandidateItem("ideal", new String[] { "leanness",
                // "throughput", "cohesion", "coupling" }, new double[] {
                // 1d, Integer.MAX_VALUE * 1d, 0d, 0d }),
                Tester.toCandidateItem("worse", new String[] { "leanness",
                        "throughput", "cohesion", "coupling" }, new double[] {
                        0.5517970402, 1d, 0.78d, 1d }),
                Tester.toCandidateItem("c1", new String[] { "leanness",
                        "throughput", "cohesion", "coupling" }, new double[] {
                        0.6929824561, 234136.216177264, 0.7777777778, 0d }),
                Tester.toCandidateItem("c2", new String[] { "leanness",
                        "throughput", "cohesion", "coupling" }, new double[] {
                        1d, 226823.944732622, 0.6666666667, 0d }) };
        //

        // HDS ranking
        CandidateRanking candidateRanking = hdsRankingStrategy.rank(criteria,
                candidateItems);

        // NDS ranking
        CandidateRanking ndsCandidateRanking = ndsRankingStrategy.rank(
                criteria, candidateItems);

        // assertEquals(2, candidateRanking.getCandidateItems().length);

        // assert scores
        for (int i = 1; i <= candidateRanking.getCandidateItems().length; i++) {
            assertEquals(i, candidateRanking.getCandidateItems()[i - 1]
                    .getRanking().get(HybridNonDominatedSortingRanking.ID)
                    .intValue());

            System.out.println(candidateRanking.getCandidateItems()[i - 1]
                    .getRanking()
                    .get(HybridNonDominatedSortingRanking.ID + "_po")
                    .intValue()
                    + ". position: "
                    + candidateRanking.getCandidateItems()[i - 1].getId()
                    + ". NDS "
                    + ndsCandidateRanking.getCandidateItems()[i - 1].getId());
        }
    }

    @Test
    public void test_hds_CSV() throws IOException {
        HybridNonDominatedSortingRanking hdsRankingStrategy = new HybridNonDominatedSortingRanking(
                smoopRanking);

        NonDominatedSortingRanking ndsRankingStrategy = new NonDominatedSortingRanking(
                smoopRanking);

        // enable rank perf
        hdsRankingStrategy.setMeasureRankingPerformance(true);

        Criterion[] criteria = new Criterion[] {
                Tester.toCriterion("leanness", Criterion.OBJECTIVE_MAX, 1d, 1d),
                Tester.toCriterion("throughput", Criterion.OBJECTIVE_MAX, 1d,
                        1d),
                Tester.toCriterion("cohesion", Criterion.OBJECTIVE_MIN, 1d, 1d),
                Tester.toCriterion("coupling", Criterion.OBJECTIVE_MIN, 1d, 1d)

        };

        List<CandidateItem> candidateList = new ArrayList<>();

        LineIterator lineIt = FileUtils
                .lineIterator(new File(
                        "ranking_HDS_SMOOP_-_0508e01e-27f4-4488-8b55-4a7d01832e5d_-_MAX_sf_instruction_leanness,MAX_jmh_thrpt_score_mean,MIN_entryClass_ckjm_ext_lcom3,MIN_entryClass_ckjm_ext_ce.csv"));
        boolean first = true;
        while (lineIt.hasNext()) {
            String line = lineIt.next();
            
            if(first) {
                first = !first;
                //
                continue;
            }

            String[] columns = StringUtils.split(line, ";");

            try {
                candidateList.add(Tester.toCandidateItem(
                        columns[0],
                        new String[] { "leanness", "throughput", "cohesion",
                                "coupling" },
                        new double[] { Double.parseDouble(columns[1]),
                                Double.parseDouble(columns[2]),
                                Double.parseDouble(columns[3]),
                                Double.parseDouble(columns[4]) }));
            } catch (NumberFormatException e) {
                // ignore
                e.printStackTrace();
            }
        }

        // shuffle
        //Collections.shuffle(candidateList);
        CandidateItem[] candidateItems = candidateList
                .toArray(new CandidateItem[0]);
        //
        candidateItems = Arrays.copyOfRange(candidateItems, 0, 10);

        // HDS ranking
        CandidateRanking candidateRanking = hdsRankingStrategy.rank(criteria,
                candidateItems);

        // NDS ranking
        CandidateRanking ndsCandidateRanking = ndsRankingStrategy.rank(
                criteria, candidateItems);

        // assertEquals(2, candidateRanking.getCandidateItems().length);

        // assert scores
        for (int i = 1; i <= candidateRanking.getCandidateItems().length; i++) {
            assertEquals(i, candidateRanking.getCandidateItems()[i - 1]
                    .getRanking().get(HybridNonDominatedSortingRanking.ID)
                    .intValue());

            System.out.println(candidateRanking.getCandidateItems()[i - 1]
                    .getRanking()
                    .get(HybridNonDominatedSortingRanking.ID + "_po")
                    .intValue()
                    + ". position: "
                    + candidateRanking.getCandidateItems()[i - 1].getId()
                    + ". NDS "
                    + ndsCandidateRanking.getCandidateItems()[i - 1].getId());
        }
    }
}
