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
package de.uni_mannheim.swt.lasso.llm.eval;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Marcus Kessel
 */
public class EvalReaderIntegrationTest {

    @Disabled
    @Test
    public void testResults() throws IOException {
        EvalReader evalReader = new EvalReader();
        Results results = evalReader.getResults(new File("/home/marcus/development/repositories/huggingface/MultiPL-E-raw-data/humaneval-java-davinci-0.2-reworded/HumanEval_23_strlen.results.json.gz"));

//        for(ExecutedSolution executedSolution : results.getResults()) {
//            System.out.println(ToStringBuilder.reflectionToString(executedSolution));
//        }

        System.out.println(results.getName());

        System.out.println(results.getResults().size());
    }

    @Disabled
    @Test
    public void testHumanEval_95() throws IOException {
        EvalReader evalReader = new EvalReader();
        Results results = evalReader.getResults(new File("/home/marcus/development/repositories/huggingface/MultiPL-E-raw-data/humaneval-java-davinci-0.2-reworded/HumanEval_95_check_dict_case.results.json.gz"));

        System.out.println(ToStringBuilder.reflectionToString(results.getResults().get(0)));

        System.out.println(results.getResults().get(0).getProgram());
        System.out.println(results.getResults().get(0).getStderr());

        System.out.println("------");

        System.out.println(results.getResults().get(1).getProgram());
        System.out.println(results.getResults().get(1).getStderr());
    }
}
