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
package de.uni_mannheim.swt.lasso.arena.sequence.miner.guess;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
public class GuesserTest {

    String mavenRepoUrl = "http://lassohp12.informatik.uni-mannheim.de:8081/repository/maven-public/";
    File localRepo = new File("/tmp/blubblubblubmvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

    @Test
    public void test_Base64_combined() throws IOException, DependencyResolutionException {
        ClassUnderTest testClass = codeSearch.queryForClass("83640d88-c64a-4013-92b9-c630ea837b00");

        CombinedGuesser guesser = new CombinedGuesser();

        assertEquals("org.apache.commons.net.util.Base64", guesser.guess(testClass));
    }

    @Test
    public void test_Base64_heuristic() throws IOException, DependencyResolutionException {
        ClassUnderTest testClass = codeSearch.queryForClass("83640d88-c64a-4013-92b9-c630ea837b00");

        HeuristicGuesser testClassAnalyzer = new HeuristicGuesser();
        testClassAnalyzer.analyze(testClass);

        TestClassAnalysisContext classAnalysisContext = testClassAnalyzer.getTestClassAnalysisContext();
        classAnalysisContext.getTestMethods().stream().forEach(t -> {
            System.out.println(t.getName() + " " + t.getClassUnderTest());
        });
    }

    @Test
    public void test_Base64_tfidf() throws IOException, DependencyResolutionException {
        ClassUnderTest testClass = codeSearch.queryForClass("83640d88-c64a-4013-92b9-c630ea837b00");

        TfIdfGuesser testClassAnalyzer = new TfIdfGuesser();
        HashMap<String, List<TfIdfGuesser.TfIdfCUT>> tfIdfCUTMap = testClassAnalyzer.analyze(testClass);

        tfIdfCUTMap.get("org/apache/commons/net/util/Base64Test").stream().forEach(c -> {
            System.out.println(c.getTfIdf() + " " + c.getCut());
        });
    }
}
