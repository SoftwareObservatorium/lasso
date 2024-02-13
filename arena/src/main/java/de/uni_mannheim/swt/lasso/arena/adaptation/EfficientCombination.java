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
package de.uni_mannheim.swt.lasso.arena.adaptation;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.ClassPermutation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.Logger;
import de.uni_mannheim.swt.lasso.runner.permutator.rank.CandidateDistanceRanking;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A more efficient strategy which puts a cap on max. permutations and max. candidates (method).
 * 
 * @author Marcus Kessel
 *
 */
public class EfficientCombination {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(EfficientCombination.class);

    /*FIXME adapterLimit overall*/
    private final int adapterLimit;

    private int methodMax;

    private int candidateMax;

    /**
     * @param methodMax max amount of unique methods to consider
     * @param adapterLimit Maximum of permutations overall
     */
    public EfficientCombination(int methodMax, int adapterLimit) {
        this.methodMax = methodMax;

        // set the same
        this.candidateMax = methodMax;

        this.adapterLimit = adapterLimit;
    }

    /**
     * 1) Rank candidates for each method.
     * 2) Create combinations based on best ranked candidates for each method.
     * 3) Favor direct matches.
     * 4) Put cap on top in order to reduce no. of permutations.
     * x) Alternative approach: feedback directed from testing. Start with
     * permutation and observe which methods passed, then adapt.
     * 
     */
    public List<ClassPermutation> createPermutations(List<List<Candidate>> constructorCandidates,
                                                     List<List<Candidate>> candidates, InterfaceSpecification specification) {
        //
        List<List<Candidate>> candidateList = new LinkedList<>();

        String[] methodNames = specification.getMethodNames();

        // dynamic
        int max = Math.max(methodNames.length, methodMax);

        if(LOG.isDebugEnabled()) {
            for(int c = 0; c < constructorCandidates.size(); c++) {
                MethodSignature con = specification.getConstructors().get(c);
                LOG.debug("----> Constructor under search: " + con.toLQL());
                List<Candidate> constructors = constructorCandidates.get(c);
                LOG.debug("CANDIDATES " + constructors.size());
                for(Candidate cc : constructors) {
                    //LOG.debug(cc.getMethod() == null ? "n/a" : cc.getMethod().toString());

                    try {
                        LOG.debug("{} / {} / {}", cc.getMethod() == null ? "n/a" : cc.getMethod().toString(), cc.getPositions(), cc.getProducerStrategy());
                    } catch (Throwable e) {
                        //
                    }
                }
            }
        }

        // sort the candidates for each method under search
        long N = 1;
        for (int i = 0; i < candidates.size(); i++) {
            LOG.debug("CANDIDATES " + candidates.size());
            LOG.debug("methodNames " + methodNames.length);

            if (candidates.get(i) == null) {
                candidateList.add(new LinkedList<>());
            } else {
                // sort and limit by cap size and make distinct
                List<Candidate> element = candidates.get(i).stream()
                        .sorted(new CandidateDistanceRanking(methodNames[i]))
                        // limit the no. of permutations allowed by method and no of methods overall
                        .filter(limitCountByKey(max, max, c -> c.getMethod()))
                        .collect(Collectors.toList());

                candidateList.add(element);
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("----> Method under search: " + specification.getMethods().get(i).toLQL() + " / (max = " + max + ")");
                candidateList.get(i).stream()
                        .forEach(m -> {
                            try {
                                LOG.debug("{} / {} / {}", m.getMethod().toString(), m.getPositions(), m.getAdaptationStrategy());
                            } catch (Throwable e) {
                                //
                            }
                        });
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

        // FIXME multiplyExact throws Caused by: java.lang.ArithmeticException: integer overflow

        try {
            N = candidateList.stream().mapToLong(c -> c.size() == 0 ? 1 : c.size()).reduce(1, Math::multiplyExact);
        } catch (ArithmeticException e) {
            LOG.warn("Too big", e);
        }
//
//        if(LOG.isInfoEnabled()) {
//            LOG.info("----> Raw Combinations: " + N);
//        }
//
//        if(N > Permutator.MAX_PERMUTATIONS) {
////            // reduce
////            int max = methodMax;
////
////            while(N > Permutator.MAX_PERMUTATIONS) {
////                for(List<Candidate> list : candidateList) {
////                    if(list.size() == max) {
////                        list.remove(list.size() - 1);
////                    }
////
////                    N = candidateList.stream().mapToInt(c -> c.size()).reduce(1, Math::multiplyExact);
////
////                    Logger.info("Reduced to " + N);
////
////                    max--;
////                }
////            }
//
//            Logger.debug("----> Top Combinations: " + N);
//        }

        Logger.debug("----> Raw Combinations: " + N);

        int headMax = max;

        List<ClassPermutation> top = topNCombinations(specification, constructorCandidates, candidateList, headMax);

        Logger.debug("----> Top Combinations: " + top.size());

        return top;

        //return createMethodPermutations(constructor, candidateList, specification.getMethodNames());
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

    @Deprecated
    private List<ClassPermutation> pickBestMatch(InterfaceSpecification specification, List<List<Candidate>> constructorCandidates,
                                            List<List<Candidate>> candidates) {
        // FIXME determine best match for constructors ...
        List<List<Candidate>> constructors;
        if(CollectionUtils.isEmpty(constructorCandidates)) {
            constructors = new LinkedList<>();
        } else {
            constructors = constructorCandidates;//.get(0);
        }

        List<ClassPermutation> permutations = new ArrayList<>();
        ClassPermutation p = new ClassPermutation(constructors,
                new ArrayList<>(specification.getMethods().size()));
        permutations.add(p);

        // by method
        for(int m = 0; m < specification.getMethods().size(); m++) {
            List<Candidate> mCandList = candidates.get(m);
            if(mCandList.size() == 0) {
                // FIXME no method matched
                p.getMethods().add(m, null);
            } else {
                // pick top match
                Candidate candidate = mCandList.get(0);
                p.getMethods().add(m, candidate);
            }
        }

        return permutations;
    }

    private List<ClassPermutation> topNCombinations(InterfaceSpecification specification, List<List<Candidate>> constructorCandidates,
                                                 List<List<Candidate>> candidates, int headMax) {
//        // FIXME determine best match for constructors ...
//        List<Candidate> constructors;
//        if(CollectionUtils.isEmpty(constructorCandidates)) {
//            constructors = new LinkedList<>();
//        } else {
//            constructors = constructorCandidates.get(0);
//        }
//
//        List<ClassPermutation> permutations = new ArrayList<>();
//        ClassPermutation p = new ClassPermutation(constructors,
//                new ArrayList<>(specification.getMethods().size()));
//        permutations.add(p);
//
//        // by method
//        for(int m = 0; m < specification.getMethods().size(); m++) {
//            List<Candidate> mCandList = candidates.get(m);
//            if(mCandList.size() == 0) {
//                // FIXME no method matched
//                p.getMethods().add(m, null);
//            } else {
//                // pick top match
//                Candidate candidate = mCandList.get(0);
//                p.getMethods().add(m, candidate);
//            }
//        }

        LinkedList<List<Candidate>> candidatesList = new LinkedList<>(
                candidates);

        List<ClassPermutation> permutations = new ArrayList<>();

        // call chain mode, compute all combinations
        List<ClassPermutation> newPermutations;

        List<List<Candidate>> constructors;
        if(CollectionUtils.isEmpty(constructorCandidates)) {
            constructors = new LinkedList<>();
        } else {
            // each index == one required constructor with candidates
            constructors = constructorCandidates;//.get(0);
        }

        // first list: methodId == 0
        int methodId = 0;
        for (Candidate c : candidatesList.removeFirst()) {
            ClassPermutation p = new ClassPermutation(constructors,
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
            for (ClassPermutation p1 : permutations) {
                int cc = 0;
                for (Candidate c : next) {
                    if(cc++ >= headMax) {
                        //Logger.debug("Ignoring further methods");
                        break;
                    }
                    List<Candidate> nList = new ArrayList<>(p1.getMethods());

                    ClassPermutation np = new ClassPermutation(constructors, nList);

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

}
