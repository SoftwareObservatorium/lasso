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

import java.io.IOException;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.AbstractSmoopRankingStrategy;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.CandidateItemJs;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.SmoopRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.perf.RankingPerformanceCollector;

/**
 * Linear recursive ranking strategy. Sorts non-distinguishable candidates
 * iteratively using a sequence of criteria.
 * 
 * @author Marcus Kessel
 *
 */
public class LRRRanking extends AbstractSmoopRankingStrategy {

    public static final String ID = "LRR_SMOOP";

    /**
     * Constructor
     * 
     * @param smoopRanking
     *            {@link SmoopRanking} instance
     */
    public LRRRanking(SmoopRanking smoopRanking) {
        super(smoopRanking);
    }

    /**
     * {@inheritDoc}
     */
    protected CandidateItemJs[] doRank(Criterion[] criteria,
            CandidateItemJs[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException {
        //
        getSmoopRanking().lrr(criteria, candidateItems,
                rankingPerformanceCollector);

        return candidateItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

}
