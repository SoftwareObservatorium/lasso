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
package de.uni_mannheim.swt.lasso.datasource.maven.socora;

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRankingStrategy;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A manager facade to handle {@link CandidateRankingStrategy}s.
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateRankingManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(CandidateRankingManager.class);

    private Map<String, CandidateRankingStrategy> rankingStrategies = new HashMap<>();
    
    public static final String RANKING_KEY = "HDS_SMOOP_po"; 

    public Map<String, Integer> rank(List<System> candidates,
                                     String strategy, Criterion[] criteria) throws IOException {
        // nothing to do
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        // get strategy
        CandidateRankingStrategy rankingStrategy = rankingStrategies
                .get(strategy);

        Validate.notNull(rankingStrategy, "Ranking strategy is null for %s", strategy);

        // do rank
        CandidateRanking candidateRanking = rankingStrategy.rank(criteria,
                toCandidateItems(candidates));

        // back to candidate mapping
        return sort(candidates,
                candidateRanking.getCandidateItems());
    }

    private CandidateItem[] toCandidateItems(
            List<System> candidates) {
        CandidateItem[] arr = candidates.stream()
                .map(c -> toCandidateItem(c))
                .toArray(CandidateItem[]::new);
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("CandidateItems for ranking " + arr.length);
        }
        
        return arr;
    }

    private CandidateItem toCandidateItem(
            System implementation) {
        CandidateItem candidateItem = new CandidateItem();
        candidateItem.setId(
                implementation.getId());
        candidateItem.setMetricsMap(implementation.getCode().getMeasures());

        return candidateItem;
    }

    private Map<String, Integer> sort(List<System> unrankedCandidates,
                                      CandidateItem[] candidateItems) {
        // sort using comparator
        if(ArrayUtils.isEmpty(candidateItems)) {
            // do nothing
            return null;
        }

        Map<String, Integer> sortMap = Arrays.stream(candidateItems).collect(Collectors.toMap(c -> c.getId(), c -> {
            if(!c.getRanking().containsKey(CandidateRankingManager.RANKING_KEY)) {
                return Integer.MAX_VALUE;
            }

            Integer rank = c.getRanking().get(
                    CandidateRankingManager.RANKING_KEY)
                    .intValue();

            return rank;
        }));

        // sort
        unrankedCandidates.sort((o1, o2) -> sortMap.get(o1.getId()) - sortMap.get(o2.getId()));

        return sortMap;
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
}
