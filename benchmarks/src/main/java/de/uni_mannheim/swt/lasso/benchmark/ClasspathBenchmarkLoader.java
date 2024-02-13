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
package de.uni_mannheim.swt.lasso.benchmark;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Load benchmarks from classpath.
 *
 * @author Marcus Kessel
 */
public class ClasspathBenchmarkLoader {

    private static Map<String, String> LOOKUP = new HashMap<>() {
        {
            put("humaneval-java-reworded", "classpath*:**/HumanEval_*.json");
            put("mbpp-java-reworded", "classpath*:**/mbpp_*.json");
        }
    };

    public Benchmark load(String benchmarkName) throws IOException {
        Resource[] resources = loadResources(LOOKUP.get(benchmarkName));

        //System.out.println(Arrays.toString(resources));

        Benchmark benchmark = new Benchmark();
        benchmark.setName(benchmarkName);

        Map<String, FunctionalAbstraction> functionalAbstractions = Arrays.stream(resources).map(r -> {
            try {
                return load(r);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toMap(a -> a.getId(), a -> a));

        benchmark.setAbstractions(functionalAbstractions);

        return benchmark;
    }

    private FunctionalAbstraction load(Resource resource) throws IOException {
        return ProblemToAbstraction.fromJson(
                IOUtils.toString(resource.getInputStream(), Charset.defaultCharset()));
    }

    private Resource[] loadResources(String resourceName) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                ClasspathBenchmarkLoader.class.getClassLoader());
        Resource[] resources = resolver.getResources(resourceName);

        return resources;
    }
}
