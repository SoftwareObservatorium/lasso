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
package de.uni_mannheim.swt.lasso.runner.permutator.combination;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.Permutation;

/**
 * Classic merobase strategy (brute-force approach which is all-combinations).
 * 
 * @author Marcus Kessel
 *
 */
public class AllCombinations implements PermutationStategy {

    /**
     * 
     */
    @Override
    public List<Permutation> createMethodPermutations(Candidate constructor,
            List<List<Candidate>> candidates, String[] methodNames) {
        LinkedList<List<Candidate>> candidatesList = new LinkedList<List<Candidate>>(
                candidates);

        // System.out.println("Size " + candidatesList.get(0).size());

        List<Permutation> permutations = new ArrayList<>();

        // call chain mode, compute all combinations
        List<Permutation> newPermutations;

        // first list: methodId == 0
        int methodId = 0;
        for (Candidate c : candidatesList.removeFirst()) {
            Permutation p = new Permutation(constructor,
                    new ArrayList<>());
            // keep care of index (== methodId)
            p.getMethods().add(methodId, c);
            permutations.add(p);
        }

        while (!candidatesList.isEmpty()) {
            List<Candidate> next = candidatesList.removeFirst();
            // inc methodId
            methodId++;
            newPermutations = new ArrayList<>();
            for (Permutation p1 : permutations) {
                for (Candidate c : next) {

                    List<Candidate> nList = new ArrayList<>();
                    for (Candidate oc : p1.getMethods()) {
                        nList.add(oc);
                    }

                    Permutation np = new Permutation(constructor, nList);

                    // add candidate (respecting current methodId)
                    np.getMethods().add(methodId, c);

                    if (!newPermutations.contains(np)) {
                        newPermutations.add(np);
                    }
                }
            }

            permutations = newPermutations;
        }

        return permutations;
    }
}
