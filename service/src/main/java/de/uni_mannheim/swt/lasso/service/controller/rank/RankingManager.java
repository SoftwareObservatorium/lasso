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
package de.uni_mannheim.swt.lasso.service.controller.rank;

import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRankingStrategy;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.SmoopRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.strategy.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * A manager facade to handle {@link CandidateRankingStrategy}s.
 * 
 * @author Marcus Kessel
 *
 */
public class RankingManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(RankingManager.class);

    private Map<String, CandidateRankingStrategy> rankingStrategies = new HashMap<>();
    
    public static final String RANKING_KEY = "HDS_SMOOP_po";

    public RankingManager() throws IOException {
        init();
    }

    public RankResponse rank(RankRequest rankRequest) throws IOException {
        // nothing to do
        if (CollectionUtils.isEmpty(rankRequest.getCandidates())) {
            return null;
        }

        // get strategy
        CandidateRankingStrategy rankingStrategy = rankingStrategies
                .get(rankRequest.getStrategy());

        Validate.notNull(rankingStrategy, "Ranking strategy is null for %s", rankRequest.getStrategy());

        Criterion[] criteria = toCriteria(rankRequest.getCriteria());

        // do rank
        CandidateRanking candidateRanking = rankingStrategy.rank(criteria,
                toCandidateItems(rankRequest.getCandidates()));

        List<RankedCandidate> rankedCandidates = Arrays.stream(candidateRanking.getCandidateItems()).map(c -> {
            Integer rank = -1;
            if(!c.getRanking().containsKey(RankingManager.RANKING_KEY)) {
                rank = Integer.MAX_VALUE;
            }

            rank = c.getRanking().get(
                            RankingManager.RANKING_KEY)
                    .intValue();

            // identify
            Optional<SystemMeta> sOp = rankRequest.getCandidates().stream().filter(s -> s.getFullId().equals(c.getId())).findFirst();

            RankedCandidate rankedCandidate = new RankedCandidate();
            rankedCandidate.setCandidate(sOp.get());
            rankedCandidate.setRank(rank);

            return rankedCandidate;
        }).toList();

        RankResponse rankResponse = new RankResponse();
        rankResponse.setRankedCandidates(rankedCandidates);

        return rankResponse;
    }

    private Criterion[] toCriteria(List<RankingCriterion> criteria) {
        return criteria.stream().map(c -> {
            Criterion criterion = new Criterion();
            criterion.setObjective(c.getObjective());
            criterion.setId(c.getId());
            criterion.setWeight(c.getWeight());
            criterion.setPriority(c.getPriority());
            return criterion;
        }).toArray(Criterion[]::new);
    }

    private CandidateItem[] toCandidateItems(
            List<SystemMeta> candidates) {
        CandidateItem[] arr = candidates.stream()
                .map(this::toCandidateItem)
                .toArray(CandidateItem[]::new);
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("CandidateItems for ranking " + arr.length);
        }
        
        return arr;
    }

    private CandidateItem toCandidateItem(
            SystemMeta implementation) {
        CandidateItem candidateItem = new CandidateItem();
        candidateItem.setId(
                implementation.getFullId());
        candidateItem.setMetricsMap(implementation.getMeasures());

        return candidateItem;
    }

    /**
     * @return the rankingStrategies
     */
    public Map<String, CandidateRankingStrategy> getRankingStrategies() {
        return rankingStrategies;
    }

    /**
     * @param rankingStrategy
     *            the rankingStrategy to set
     */
    public void addRankingStrategy(CandidateRankingStrategy rankingStrategy) {
        //
        Validate.notNull(rankingStrategy, "RankingStrategy cannot be null");

        this.rankingStrategies.put(rankingStrategy.getId(), rankingStrategy);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Added {}", rankingStrategy.getId());
        }
    }

    protected void init() throws IOException {
        // init js wrapper
        SmoopRanking smoopRanking = new SmoopRanking();
        // set ranking strategies
        addRankingStrategy(new SingleObjectiveRanking(smoopRanking));
        addRankingStrategy(new HybridNonDominatedSortingRanking(smoopRanking));
        addRankingStrategy(new WeightedSumRanking(smoopRanking));
        addRankingStrategy(new WeightedEuclideanRanking(smoopRanking));
        addRankingStrategy(new LRRRanking(smoopRanking));
        addRankingStrategy(new RPHybridNonDominatedSortingRanking(smoopRanking));
        addRankingStrategy(new RPNonDominatedSortingRanking(smoopRanking));
        addRankingStrategy(new NonDominatedSortingRanking(smoopRanking));
        addRankingStrategy(new NormalizedWeightedDistanceRanking(smoopRanking));
    }
}
