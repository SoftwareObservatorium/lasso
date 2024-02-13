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
package de.uni_mannheim.swt.lasso.service.systemtests.integration

import de.uni_mannheim.swt.lasso.benchmark.Benchmark
import de.uni_mannheim.swt.lasso.benchmark.ClasspathBenchmarkLoader
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import java.util.regex.Pattern

/**
 * Just spawn a test platform for debugging purposes
 *
 * @author mkessel
 */
class SpawnPlatformTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine

    @Test
    void spawn() throws Exception {
        Pattern pattern = Pattern.compile(".*");
        final Collection<String> list = ResourceList.getResources(pattern);
        for(final String name : list){
            if(name.endsWith(".json")) {
                System.out.println(name);
            }
        }

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        // requires external interrupt
        while(true) {
            Thread.sleep(100);
        }
    }
}
