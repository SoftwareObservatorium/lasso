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
package de.uni_mannheim.swt.lasso.analyzer.systemtests;

import de.uni_mannheim.swt.lasso.analyzer.maven.MavenPomParser;
import de.uni_mannheim.swt.lasso.analyzer.model.MetaData;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class MavenPomParserTest {

    @Test
    public void testParsePom() throws IOException, XmlPullParserException {
        //
        MetaData meta = MavenPomParser.parsePom(new File("pom.xml"));

        meta.getValues().forEach((k, v) -> {
            System.out.println(k + " => " + (v instanceof List<?> ? ((List<?>)v).stream().map(s -> s.toString()).collect(Collectors.joining(",")) : v.toString()));
        });
    }

    @Test
    public void testParseBenchmarkMetaData() throws IOException, XmlPullParserException {
        //
        Map<String, String> meta = MavenPomParser.parseBenchmarkPom(new File("HumanEval_0_has_close_elements_humaneval-java-codegen-0.2-reworded_0_humaneval-java-codegen-0.2-reworded-0.pom"));

        System.out.println(meta);
    }
}
