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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.uni_mannheim.swt.lasso.runner.permutator.Logger;
import de.uni_mannheim.swt.lasso.runner.permutator.rank.CandidateDistanceRanking;
import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.Permutation;

/**
 * Some more efficient strategy which puts a cap on max permutations and max candidates (method).
 * 
 * @author Marcus Kessel
 *
 */
public class EfficientCombination implements PermutationStategy {

    /**
     * Maximum no. of permutations to test (upper bound).
     */
    public static Integer MAX_PERMUTATIONS = 250;

    private int methodMax;

    private int candidateMax;
    
    private PermutationStategy delegate = new AllCombinations();

    /**
     *
     *
     * @param methodMax max amount of unique methods to consider
     */
    public EfficientCombination(int methodMax) {
        this.methodMax = methodMax;

        // set the same
        this.candidateMax = methodMax;
    }

    /**
     * 1) Rank candidates for each method.
     * 
     * 2) Create combinations based on best ranked candidates for each method.
     * 
     * 3) Favor direct matches.
     * 
     * 4) Put cap on top in order to reduce no. of permutations.
     * 
     * *) Alternative approach: feedback directed from testing. Start with
     * permutation and observe which methods passed, then adapt.
     * 
     */
    @Override
    public List<Permutation> createMethodPermutations(Candidate constructor,
                                                      List<List<Candidate>> candidates, String[] methodNames) {
        //
        List<List<Candidate>> candidateList = new LinkedList<List<Candidate>>();

        // sort the candidates for each method under search
        int N = 1;
        for (int i = 0; i < candidates.size(); i++) {
            Logger.info("CANDIDATES " + candidates.size());
            Logger.info("methodNames " + methodNames.length);

            if (candidates.get(i) == null) {
                candidateList.add(new LinkedList<>());
            } else {
                // sort and limit by cap size and make distinct
                List<Candidate> element = candidates.get(i).stream()
                        .sorted(new CandidateDistanceRanking(methodNames[i]))
                        // limit the no. of permutations allowed by method and no of methods overall
                        .filter(limitCountByKey(methodMax, candidateMax, c -> c.getMethod()))
                        .collect(Collectors.toList());

                candidateList.add(element);
            }

            if(Logger.isDebugEnabled()) {
                Logger.debug("----> Method under search: " + methodNames[i] + "(max = " + methodMax + ")");
                candidateList.get(i).stream()
                        .forEach(m -> Logger.debug(m.getMethod().toString()));
            }

//            N *= candidateList.get(i).size();
//
//            if(N > Permutator.MAX_PERMUTATIONS) {
//                // we do not need to compute any additional ones, since we reached the threshold
//                if(Logger.isDebugEnabled()) {
//                    Logger.debug("Max. Permutations threshold reached: " + N);
//                }
//
//                //break;
//            }
        }

        N = candidateList.stream().mapToInt(c -> c.size()).reduce(1, Math::multiplyExact);

        if(Logger.isDebugEnabled()) {
            Logger.debug("----> Raw Combinations: " + N);
        }

        if(N > MAX_PERMUTATIONS) {
//            // reduce
//            int max = methodMax;
//
//            while(N > Permutator.MAX_PERMUTATIONS) {
//                for(List<Candidate> list : candidateList) {
//                    if(list.size() == max) {
//                        list.remove(list.size() - 1);
//                    }
//
//                    N = candidateList.stream().mapToInt(c -> c.size()).reduce(1, Math::multiplyExact);
//
//                    Logger.info("Reduced to " + N);
//
//                    max--;
//                }
//            }

            Logger.debug("----> Top Combinations: " + N);
        }

        return delegate.createMethodPermutations(constructor, candidateList,
                methodNames);
    }

    /**
     * Limit amount of candidates accepted by passed methodMax (maximum no of methods allowed in total) as well as
     * candidateMax (max. amount of candidates allowed by method).
     *
     * @param methodMax
     * @param candidateMax
     * @param ke
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> limitCountByKey(int methodMax, int candidateMax,
            Function<? super T, ?> ke) {

        Map<Object, MethodCandidatesCounter> seen = new ConcurrentHashMap<>();

        return t -> {
            // TODO we could also implement a strategy in which a global counter is used ..

            // strategy: either methodMax reached or candidateMax/method reached in total
            if(seen.size() >= methodMax) {
                return false;
            }

            MethodCandidatesCounter counter = seen.putIfAbsent(ke.apply(t), new MethodCandidatesCounter());
            if(counter == null) {
                return true;
            }

            return counter.incrementAndGet() <= candidateMax;
        };
    }

    /**
     * Simple counter
     *
     * @author Marcus Kessel
     */
    static class MethodCandidatesCounter {
        int value = 1;
        public int incrementAndGet() {
            return ++value;
        }
    }

    public PermutationStategy getDelegate() {
        return delegate;
    }

    public void setDelegate(PermutationStategy delegate) {
        this.delegate = delegate;
    }

}
