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
package de.uni_mannheim.swt.lasso.llm.test;


import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.llm.problem.MultiPLE;
import de.uni_mannheim.swt.lasso.llm.problem.Problem;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
public class ProblemToSheetParserTest {

    @Test
    public void testParseLQL_HumanEval_53_add() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/humaneval-java-reworded.json"));

        Problem problem = problemsMap.get("HumanEval_53_add");

        LQLParseResult lqlParseResult = parse.parseLQL(problem);

        assertEquals("add", lqlParseResult.getInterfaceSpecification().getMethods().get(0).getName());
    }

    @Test
    public void testParseLQL() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        LQLParseResult lqlParseResult = parse.parseLQL(problemsList.get(1));

        assertEquals("encrypt", lqlParseResult.getInterfaceSpecification().getMethods().get(0).getName());
    }

    @Test
    public void testParseDescription() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        String description = parse.parseDescription(problemsList.get(2));
        System.out.println(description);
    }

    @Test
    public void testParseProblemMethodSignature() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        String lql = parse.parseMethodSignature(problemsList.get(1));

        System.out.println(lql);
        assertEquals("HumanEval_89_encrypt {\n" +
                "  encrypt(java.lang.String)->java.lang.String\n" +
                "}", lql);
    }

    @Test
    public void testParseProblem_equals_primitives() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        List<Sequence> ss = parse.parse(problemsList.get(1));

        FunctionalAbstraction fa = new FunctionalAbstraction();
        fa.setId(problemsList.get(1).getName());
        fa.setLql(parse.parseMethodSignature(problemsList.get(1)));
        fa.setDescription(parse.parseDescription(problemsList.get(1)));
        fa.setSequences(ss);

        // FIXME store FA somewhere

        debug(ss);

        System.out.println(ProblemToAbstraction.toJson(fa));
    }

    static void debug(List<Sequence> ss) {
        for(Sequence s : ss) {
            System.out.println("----- Sheet -----");
            System.out.println(s);
            System.out.println("-----  -----");
        }
    }

    @Test
    public void testParseProblem_hashmap() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        List<Sequence> ss = parse.parse(problemsList.get(2));

        FunctionalAbstraction fa = new FunctionalAbstraction();
        fa.setId(problemsList.get(2).getName());
        fa.setLql(parse.parseMethodSignature(problemsList.get(2)));
        fa.setDescription(parse.parseDescription(problemsList.get(2)));
        fa.setSequences(ss);

        // FIXME store FA somewhere

        debug(ss);

        System.out.println(ProblemToAbstraction.toJson(fa));
    }

    // this is faulty, so not our fault
    // public static ArrayList<Pair<Long, Long, Long>> findTuples(ArrayList<Pair<Long, Long, Long>> test_list, long K) {
    // should be ArrayList<Pair<Long, Long>>

    /**
     * applies for
     *
     * Issue with Pair and only two types possible for generics
     *
     * <pre>
     *     mbpp_75_find_tuples
     *     mbpp_607_find_literals
     *     mbpp_424_extract_rear
     *     mbpp_261_division_elements
     *     mbpp_616_tuple_modulo
     *     mbpp_273_substract_elements
     *     mbpp_429_and_tuples
     *     mbpp_440_find_adverb_position
     *     mbpp_399_bitwise_xor
     *     mbpp_744_check_none
     *     mbpp_773_occurance_substring
     *     mbpp_791_remove_nested
     *     mbpp_720_add_dict_to_tuple
     *     mbpp_413_extract_nth_element
     *     mbpp_809_check_smaller
     *     mbpp_785_tuple_str_int
     *     mbpp_116_tuple_to_int
     *     mbpp_470_add_pairwise
     *     mbpp_579_find_dissimilar
     *     mbpp_580_extract_even
     *     mbpp_421_concatenate_tuple
     *     mbpp_106_add_lists
     *     mbpp_788_new_tuple
     *     mbpp_740_tuple_to_dict
     *     mbpp_272_rear_extract
     * </pre>
     *
     * Issue with long numbers (too large) - 6775685320645824322581483068371419745979053216268760300l not possible
     *
     * <pre>
     *     mbpp_67_bell_number
     * </pre>
     *
     * Wrong Generics Optional.empty()
     *
     * <pre>
     *     mbpp_568_empty_list
     * </pre>
     *
     * No No signature of method: static org.javatuples.Pair.with() is applicable for argument types: (Long, Long, Long, Long, Long, Long) values: [5, 10, 7, 4, 15, 3]
     *
     * <pre>
     *     mbpp_587_list_tuple
     *     mbpp_446_count_Occurrence
     *     mbpp_222_check_type
     * </pre>
     *
     * No signature of method: static java.util.Map.of() is applicable for argument types: (Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long...) values: [1, 1, 2, 1, 3, 1, 4, 1, 5, 1, 6, 1, 7, 1, 8, 1, 9, 1, 10, 1, ...]
     *
     * <pre>
     *     mbpp_97_frequency_lists
     * </pre>
     *
     * Not possible Object(1l).
     *
     * <pre>
     *     mbpp_425_count_element_in_list
     *     mbpp_407_rearrange_bigger
     *     mbpp_284_check_element
     *     mbpp_595_min_Swaps
     * </pre>
     *
     * Unknown (encoding?)
     *
     * <pre>
     *      mbpp_230_replace_blank
     * </pre>
     *
     * @throws IOException
     */
    @Test
    public void testParseProblem_tuples_mbpp() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/mbpp-java-reworded.json"));

        Problem problem = problemsMap.get("mbpp_425_count_element_in_list");

        System.out.println(problem.getTests());

        List<Sequence> ss = parse.parse(problem);

        FunctionalAbstraction fa = new FunctionalAbstraction();
        fa.setId(problem.getName());
        fa.setLql(parse.parseMethodSignature(problem));
        fa.setDescription(parse.parseDescription(problem));
        fa.setSequences(ss);

        // FIXME store FA somewhere

        debug(ss);

        System.out.println(ProblemToAbstraction.toJson(fa));
    }

    @Test
    public void testParseProblem_mbpp_more() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/mbpp-java-reworded.json"));

        Problem problem = problemsMap.get("mbpp_568_empty_list");

        List<Sequence> ss = parse.parse(problem);

        FunctionalAbstraction fa = new FunctionalAbstraction();
        fa.setId(problem.getName());
        fa.setLql(parse.parseMethodSignature(problem));
        fa.setDescription(parse.parseDescription(problem));
        fa.setSequences(ss);

        // FIXME store FA somewhere

        debug(ss);

        System.out.println(ProblemToAbstraction.toJson(fa));
    }

    /**
     * MultiPLE uses javatuples. Special handling required.
     *
     * @throws IOException
     */
    @Test
    public void testParseProblem_javatuples_Pair() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        Map<String, Problem> problemsMap = MultiPLE.toMap(multiple.getProblems("/problems/humaneval-java-reworded.json"));

        // another one is HumanEval_87_get_row
        List<Sequence> ss = parse.parse(problemsMap.get("HumanEval_107_even_odd_palindrome"));

        debug(ss);
    }

    @Test
    public void testParseAllHumanEvalProblems() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/humaneval-java-reworded.json");

        int fail = 0;
        List<String> failed = new ArrayList<>();
        for(Problem problem : problemsList) {
            System.out.println("PROBLEM " + problem.getName());
            try {
                List<Sequence> ss = parse.parse(problem);

                debug(ss);
            } catch (Throwable e) {
                fail++;
                failed.add(problem.getName());
            }
        }

        System.out.println("Total fail = " + fail);
        System.out.println("Success rate = " + ((double) fail / problemsList.size()));
        System.out.println(failed.stream().collect(Collectors.joining("\n")));
    }

    @Test
    public void testParseAllMBPPProblems() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);
        List<Problem> problemsList = multiple.getProblems("/problems/mbpp-java-reworded.json");

        int fail = 0;
        List<String> failed = new ArrayList<>();
        for(Problem problem : problemsList) {
            System.out.println("PROBLEM " + problem.getName());
            try {
                List<Sequence> ss = parse.parse(problem);

                debug(ss);
            } catch (Throwable e) {
                fail++;
                failed.add(problem.getName());
            }
        }

        System.out.println("Total fail = " + fail);
        System.out.println("Success rate = " + ((double) fail / problemsList.size()));
        System.out.println(failed.stream().collect(Collectors.joining("\n")));
    }
}
