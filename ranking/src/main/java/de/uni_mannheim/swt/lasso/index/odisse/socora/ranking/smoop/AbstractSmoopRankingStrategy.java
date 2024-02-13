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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.perf.RankingPerformanceCollector;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateItem;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRanking;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.CandidateRankingStrategy;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;

/**
 * Abstract {@link SmoopRanking} based {@link CandidateRankingStrategy}.
 * 
 * @author Marcus Kessel
 *
 */
public abstract class AbstractSmoopRankingStrategy implements
        CandidateRankingStrategy {

    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractSmoopRankingStrategy.class);

    private static final String SAFE_MEASURE_FLAG_SUFFIX = "_safe";

    private final SmoopRanking smoopRanking;

    /**
     * Flag to indicate of ranking performance metrics should be measured
     */
    private boolean measureRankingPerformance;

    /**
     * Constructor
     * 
     * @param smoopRanking
     *            {@link SmoopRanking} instance
     */
    public AbstractSmoopRankingStrategy(SmoopRanking smoopRanking) {
        Validate.notNull(smoopRanking, "SmoopRanking cannot be null");
        this.smoopRanking = smoopRanking;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CandidateRanking rank(Criterion[] criteria,
            CandidateItem[] candidateItems) throws IOException {
        Validate.notEmpty(criteria, "Criteria cannot be empty");
        Validate.notEmpty(candidateItems, "Candidate items cannot be empty");

        // convert
        CandidateItemJs[] convertedCandidateItems = convert(candidateItems);

        // check flags if all measures are available
        CandidateFitness candidateFitness = evaluateFitness(criteria,
                convertedCandidateItems);

        // nothing to rank?
        if (ArrayUtils.isEmpty(candidateFitness.getFitCandidateItems())) {
            return new CandidateRanking();
        }

        // rank
        RankingPerformanceCollector rankingPerformanceCollector = null;
        if (isMeasureRankingPerformance()) {
            rankingPerformanceCollector = new RankingPerformanceCollector();
        }

        CandidateItemJs[] retCandidateItems = doRank(criteria,
                candidateFitness.getFitCandidateItems(),
                rankingPerformanceCollector);

        // sort ascending
        Arrays.sort(retCandidateItems, new Comparator<CandidateItem>() {

            @Override
            public int compare(CandidateItem o1, CandidateItem o2) {
                int score1 = o1.getRanking().get(getId()).intValue();
                int score2 = o2.getRanking().get(getId()).intValue();

                if (score1 == score2) {
                    return 0;
                }

                return score1 > score2 ? 1 : -1;
            }
        });

        //
        CandidateRanking ranking = new CandidateRanking();
        // join ranked fit ones + unfit ones
        if (ArrayUtils.isNotEmpty(candidateFitness.getUnfitCandidateItems())) {
            // add at the end of list
            ranking.setCandidateItems(ArrayUtils.addAll(retCandidateItems,
                    candidateFitness.getUnfitCandidateItems()));
        } else {
            // set
            ranking.setCandidateItems(retCandidateItems);
        }

        // add rank perf measures
        if (isMeasureRankingPerformance()
                && rankingPerformanceCollector != null) {
            ranking.setRankingPerformanceMap(rankingPerformanceCollector
                    .getMetricsMap());
        }

        return ranking;
    }

    /**
     * Evaluate candidate fitness based on given criteria.
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            Array of {@link CandidateItemJs}s
     * @return {@link CandidateFitness} instance
     * 
     * @see MathUtils#isSafeMeasure(String, java.util.Map)
     */
    private CandidateFitness evaluateFitness(Criterion[] criteria,
            CandidateItemJs[] candidateItems) {
        //
        List<CandidateItemJs> fitList = new LinkedList<CandidateItemJs>();
        List<CandidateItemJs> unfitList = new LinkedList<CandidateItemJs>();
        for (CandidateItemJs candidate : candidateItems) {
            // flag
            boolean isCriteriaSafe = true;
            // failed if at least one criterion unsafe
            for (Criterion criterion : criteria) {
                // is safe to be used for ranking?
                boolean isSafe = isSafeMeasure(criterion.getId(),
                        candidate.getMetricsMap());

                if (!isSafe) {
                    isCriteriaSafe = false;
                    // return
                    break;
                }
            }

            // add safe flag to candidate item
            Map<String, Boolean> safeRankingCriteria = candidate
                    .getSafeRankingCriteria();
            if (safeRankingCriteria == null) {
                safeRankingCriteria = new HashMap<>();
                candidate.setSafeRankingCriteria(safeRankingCriteria);
            }
            // store flag
            safeRankingCriteria.put(getId(), isCriteriaSafe);

            if (isCriteriaSafe) {
                fitList.add(candidate);
            } else {
                unfitList.add(candidate);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring " + candidate.getId()
                            + " for ranking as measure is unsafe");
                }
            }
        }

        // return
        CandidateFitness candidateFitness = new CandidateFitness();
        candidateFitness.setFitCandidateItems(fitList
                .toArray(new CandidateItemJs[0]));
        candidateFitness.setUnfitCandidateItems(unfitList
                .toArray(new CandidateItemJs[0]));

        return candidateFitness;
    }

    /**
     * Actually ranking logic, should be implemented by sub-class
     * 
     * @param criteria
     *            Array of {@link Criterion}s
     * @param candidateItems
     *            Array of {@link CandidateItem}s
     * @param rankingPerformanceCollector
     *            {@link RankingPerformanceCollector} instance (optional)
     * @return Array of {@link CandidateItemJs}s
     * @throws IOException
     *             I/O in ranking
     */
    protected abstract CandidateItemJs[] doRank(Criterion[] criteria,
            CandidateItemJs[] candidateItems,
            RankingPerformanceCollector rankingPerformanceCollector)
            throws IOException;

    /**
     * @param candidateItems
     *            Array of {@link CandidateItem}s
     * @return Array of JS-compliant {@link CandidateItemJs}
     */
    protected CandidateItemJs[] convert(CandidateItem[] candidateItems) {
        CandidateItemJs[] candidateItemsJs = new CandidateItemJs[candidateItems.length];

        int i = 0;
        for (CandidateItem item : candidateItems) {
            CandidateItemJs itemJs = new CandidateItemJs();
            itemJs.setId(item.getId());
            itemJs.setMetricsMap(item.getMetricsMap());

            candidateItemsJs[i++] = itemJs;
        }

        return candidateItemsJs;
    }

    /**
     * @return {@link SmoopRanking}
     */
    protected SmoopRanking getSmoopRanking() {
        return smoopRanking;
    }

    /**
     * @return the measureRankingPerformance
     */
    public boolean isMeasureRankingPerformance() {
        return measureRankingPerformance;
    }

    /**
     * @param measureRankingPerformance
     *            the measureRankingPerformance to set
     */
    public void setMeasureRankingPerformance(boolean measureRankingPerformance) {
        this.measureRankingPerformance = measureRankingPerformance;
    }

    /**
     * Check candidate fitness based on given criteria
     * 
     * @author Marcus Kessel
     *
     */
    private class CandidateFitness {
        private CandidateItemJs[] fitCandidateItems;
        private CandidateItemJs[] unfitCandidateItems;

        /**
         * @return the fitCandidateItems
         */
        public CandidateItemJs[] getFitCandidateItems() {
            return fitCandidateItems;
        }

        /**
         * @param fitCandidateItems
         *            the fitCandidateItems to set
         */
        public void setFitCandidateItems(CandidateItemJs[] fitCandidateItems) {
            this.fitCandidateItems = fitCandidateItems;
        }

        /**
         * @return the unfitCandidateItems
         */
        public CandidateItemJs[] getUnfitCandidateItems() {
            return unfitCandidateItems;
        }

        /**
         * @param unfitCandidateItems
         *            the unfitCandidateItems to set
         */
        public void setUnfitCandidateItems(CandidateItemJs[] unfitCandidateItems) {
            this.unfitCandidateItems = unfitCandidateItems;
        }
    }

    /**
     * Is given measure safe for given measures?
     * 
     * @param metricName
     *            Metric name
     * @param metrics
     *            {@link Map} of String, Double mappings
     * @return true if safe flag is true of safe flag does not exist, otherwise
     *         false
     */
    private static boolean isSafeMeasure(String metricName,
            Map<String, Double> metrics) {
        Validate.notBlank(metricName, "Metric name cannot be blank");

        // false if not available
        if (MapUtils.isEmpty(metrics)) {
            return false;
        }

        // check for safe flat
        String metricNameSafe = metricName + SAFE_MEASURE_FLAG_SUFFIX;
        if (metrics.containsKey(metricNameSafe)) {
            // check flag
            return BooleanUtils.toBoolean(metrics.get(metricNameSafe)
                    .intValue());
        } else {
            // true as there is no safe flag defined!
            return true;
        }
    }
}
