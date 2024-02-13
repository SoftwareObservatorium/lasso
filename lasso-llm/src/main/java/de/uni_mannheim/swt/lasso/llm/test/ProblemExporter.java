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
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Export problems to FAs on disk.
 *
 * @author Marcus Kessel
 */
public class ProblemExporter {

    private static final Logger LOG = LoggerFactory
            .getLogger(ProblemExporter.class);

    public static void main(String[] args) throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();
        ProblemToSheetParser parse = new ProblemToSheetParser(container);

        //String problems = "humaneval-java-reworded";
        String problems = "mbpp-java-reworded";
        String path = "/tmp/mybenchmarks/" + problems;
        File baseDir = new File(path);
        baseDir.mkdirs();

        List<Problem> problemsList = multiple.getProblems("/problems/" + problems + ".json");

        List<String> failed = new ArrayList<>();

        for(Problem problem : problemsList) {
            LOG.info("Processing problem '{}'", problem.getName());

            try {
                List<Sequence> ss = parse.parse(problem);

                FunctionalAbstraction fa = new FunctionalAbstraction();
                fa.setId(problem.getName());
                fa.setLql(parse.parseMethodSignature(problem));
                fa.setDescription(parse.parseDescription(problem));
                fa.setSequences(ss);

                // store FA somewhere
                FileUtils.writeStringToFile(new File(baseDir, problem.getName() + ".json"),
                        ProblemToAbstraction.toJson(fa),
                        StandardCharsets.UTF_8.name());
            } catch (Throwable e) {
                LOG.warn("Problem failed: " +  problem.getName(), e);

                failed.add(problem.getName());
            }
        }

        LOG.info("Summary");
        LOG.info("Problems: {}", problemsList.size());
        LOG.info("Failed: {}, {}", failed.size(), failed.stream().collect(Collectors.joining(",")));
    }
}
