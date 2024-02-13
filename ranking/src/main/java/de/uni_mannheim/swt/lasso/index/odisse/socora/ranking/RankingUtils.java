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

import org.apache.commons.lang3.Validate;

/**
 * Ranking utilities
 * 
 * @author Marcus Kessel
 *
 */
public abstract class RankingUtils {

    /**
     * Private
     */
    private RankingUtils() {
        // private
    }

    /**
     * Return rank for {@link CandidateItem} based on given
     * {@link CandidateRankingStrategy}.
     * 
     * @param candidateItem
     *            {@link CandidateItem} instance
     * @param candidateRankingStrategy
     *            {@link CandidateRankingStrategy} ID
     * @param partialOrder
     *            true if partial order should be returned (disables strict
     *            order!). Candidates in non-distinguishable sets possess the
     *            same ranks.
     * @return rank for given {@link CandidateItem} based on passed
     *         {@link CandidateRankingStrategy}.
     */
    public static int getRank(CandidateItem candidateItem,
            String candidateRankingStrategy, boolean partialOrder) {
        return getRank(candidateItem.getRanking(), candidateRankingStrategy,
                partialOrder);
    }

    /**
     * Return rank for passed {@link Map} based on given
     * {@link CandidateRankingStrategy}.
     * 
     * @param ranking
     *            {@link Map} instance
     * @param candidateRankingStrategy
     *            {@link CandidateRankingStrategy} ID
     * @param partialOrder
     *            true if partial order should be returned (disables strict
     *            order!). Candidates in non-distinguishable sets possess the
     *            same ranks.
     * @return rank for given {@link CandidateItem} based on passed
     *         {@link CandidateRankingStrategy}.
     */
    public static int getRank(Map<String, Double> ranking,
            String candidateRankingStrategy, boolean partialOrder) {
        Validate.notBlank(candidateRankingStrategy,
                "CandidateRankingStrategy ID cannot be blank");

        String key = candidateRankingStrategy;
        if (partialOrder) {
            key += "_po";
        }

        return ranking.get(key).intValue();
    }

    /**
     * Get partial order cardinalities (sizes of non-distinguishable sets in
     * ranked order!)
     * 
     * @param candidateRanking
     *            {@link CandidateRanking} instance
     * @return sizes of non-distinguishable sets in ranked order!
     */
    public static int[] getPartialOrderCardinalities(
            CandidateRanking candidateRanking) {
        return getPartialOrderCardinalities(candidateRanking
                .getRankingPerformanceMap());
    }

    /**
     * Get partial order cardinalities (sizes of non-distinguishable sets in
     * ranked order!)
     * 
     * @param rankingPerformance
     *            {@link Map} instance
     * @return sizes of non-distinguishable sets in ranked order!
     */
    public static int[] getPartialOrderCardinalities(
            Map<String, Double> rankingPerformance) {
        int noOfSets = rankingPerformance.get("nds_size")
                .intValue();

        int[] cardinalities = new int[noOfSets];
        for (int i = 0; i < noOfSets; i++) {
            cardinalities[i] = rankingPerformance.get("pi_" + i).intValue();
        }

        return cardinalities;
    }
}
