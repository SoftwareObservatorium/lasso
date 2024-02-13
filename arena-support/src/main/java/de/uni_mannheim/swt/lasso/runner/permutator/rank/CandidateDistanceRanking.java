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
package de.uni_mannheim.swt.lasso.runner.permutator.rank;

import java.util.Comparator;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;

/**
 * Rank permutations by distance.
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateDistanceRanking implements Comparator<Candidate> {

    private final Comparator<Candidate> comparator;

    /**
     * First rank by direct type matches, then rank by {@link NameDistance}.
     * 
     * Note: This is a comparator-like chain.
     * 
     * @param methodName
     */
    public CandidateDistanceRanking(String methodName) {
        this.comparator = Comparator.<Candidate> comparingInt(c -> {
            return (c.getParamConverterClasses() == null
                    && c.getReturnParamConverterClass() == null) ? 1 : 0;
        }).thenComparingDouble(c -> getCummulatedDistance(c, methodName));
    }

    @Override
    public int compare(Candidate o1, Candidate o2) {
        //
        return comparator.compare(o1, o2);
    }

    /**
     * Cummulate distance.
     * 
     * @param candidate
     * @param methodName
     * @return
     */
    private static double getCummulatedDistance(Candidate candidate,
            String methodName) {
        double nameScore = NameDistance.similarity(methodName,
                candidate.getMethod().getName());

        return nameScore;
    }
}
