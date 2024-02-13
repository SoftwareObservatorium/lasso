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

import java.util.Arrays;
import java.util.Comparator;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.Permutation;

/**
 * Rank permutations by distance.
 * 
 * @author Marcus Kessel
 *
 */
public class PermutatorDistanceRanking implements Comparator<Permutation> {

    private final Comparator<Permutation> comparator;

    /**
     * First rank by direct type matches, then rank by {@link NameDistance}.
     * 
     * Note: This is a comparator-like chain.
     * 
     * @param methodNames
     */
    public PermutatorDistanceRanking(String[] methodNames) {
        this.comparator = Comparator.<Permutation> comparingInt(p -> {
            return (int) p.getMethods().stream()
                    .filter(c -> c.getParamConverterClasses() == null
                            && c.getReturnParamConverterClass() == null)
                    .count();
        }).thenComparingDouble(p -> getCummulatedDistance(p, methodNames));
    }

    /**
     * Rank by direct type matches
     */
    public PermutatorDistanceRanking() {
        this.comparator = Comparator.<Permutation> comparingInt(p -> {
            return (int) p.getMethods().stream()
                    .filter(c -> c.getParamConverterClasses() == null
                            && c.getReturnParamConverterClass() == null)
                    .count();
        });
    }

    @Override
    public int compare(Permutation o1, Permutation o2) {
        //
        return comparator.compare(o1, o2);
    }

    /**
     * Cummulate distance.
     * 
     * @param permutation
     * @param methodNames
     * @return
     */
    private static double getCummulatedDistance(Permutation permutation,
            String[] methodNames) {
        if (permutation.getNameScore() != null) {
            return permutation.getNameScore();
        }

        double nameScore = Arrays.stream(getDistances(permutation, methodNames))
                .sum();
        permutation.setNameScore(nameScore);

        return nameScore;
    }

    /**
     * Get vector of {@link NameDistance#similarity(String, String)}s.
     * 
     * @param permutation
     * @param methodNames
     * @return
     */
    private static double[] getDistances(Permutation permutation,
            String[] methodNames) {
        int i = 0;

        double[] distances = new double[methodNames.length];

        for (Candidate method : permutation.getMethods()) {
            String methodName = methodNames[i];

            distances[i] = NameDistance.similarity(methodName,
                    method.getMethod().getName());

            i++;
        }

        return distances;
    }
}
