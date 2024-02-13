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

import com.google.common.collect.ImmutableMap;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.SmoopRanking;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.strategy.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.*;

/**
 *
 * @author Marcus Kessel
 */
public class CandidateRankingManagerTest {

    @Test
    public void test_ExactMethodSignatureQuery() throws IOException {
        System impl1 = impl("1", "One", "pkg", ImmutableMap.<String, Double> builder().put("metric1", 5d).build());
        System impl2 = impl("2", "Two", "pkg", ImmutableMap.<String, Double> builder().put("metric1", 10d).build());

        CandidateRankingManager candidateRankingManager = candidateRankingManager();

        List<System> implementations = new ArrayList<>(Arrays.asList(impl1, impl2));

        Criterion criterion1 = new Criterion();
        criterion1.setId("metric1");
        criterion1.setObjective(Criterion.OBJECTIVE_MAX);
        criterion1.setPriority(1);

        Map<String, Integer> sortMap = candidateRankingManager.rank(implementations, HybridNonDominatedSortingRanking.ID, new Criterion[]{criterion1});

        assertThat(implementations, contains(impl2, impl1));

        assertThat(sortMap.get(impl1.getId()), is(2));
        assertThat(sortMap.get(impl2.getId()), is(1));
    }

    public CandidateRankingManager candidateRankingManager() throws IOException {
        CandidateRankingManager rankingManager = new CandidateRankingManager();

        // init js wrapper
        SmoopRanking smoopRanking = new SmoopRanking();
        // set ranking strategies
        rankingManager.addRankingStrategy(new SingleObjectiveRanking(smoopRanking));
        rankingManager.addRankingStrategy(new HybridNonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new WeightedSumRanking(smoopRanking));
        rankingManager.addRankingStrategy(new WeightedEuclideanRanking(smoopRanking));
        rankingManager.addRankingStrategy(new LRRRanking(smoopRanking));
        rankingManager.addRankingStrategy(new RPHybridNonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new RPNonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new NonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new NormalizedWeightedDistanceRanking(smoopRanking));

        return rankingManager;
    }

    private static System impl(String id, String name, String pkg, Map<String, Double> measures) {
        CodeUnit impl = new CodeUnit();
        impl.setId(id);
        impl.setName(name);
        impl.setPackagename(pkg);

        impl.setMeasures(measures);

        return new System(impl);
    }
}
