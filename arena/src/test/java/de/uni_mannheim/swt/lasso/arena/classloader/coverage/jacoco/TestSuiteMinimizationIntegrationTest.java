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
package de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SheetSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SpreadSheet;
import de.uni_mannheim.swt.lasso.arena.writer.CellWriter;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import de.uni_mannheim.swt.lasso.testing.minimize.CodeElements;
import de.uni_mannheim.swt.lasso.testing.minimize.MinimalTestSuite;
import de.uni_mannheim.swt.lasso.testing.minimize.TestCase;
import de.uni_mannheim.swt.lasso.testing.minimize.TestSuiteMinimizer;
import examples.*;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 *
 * @author Marcus Kessel
 */
public class TestSuiteMinimizationIntegrationTest {

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);
    CodeSearch codeSearch = new CodeSearch(SolrInstance.local());

    /**
     *
     * @throws IOException
     */
    @Test
    public void test_Calculator_full_coverage() throws IOException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("Calculator {\n" +
                "divide(int,int)->int\n" +
                "}");

        ClassUnderTest classUnderTest = createExample(Calculator.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        Scope scope = new Scope();
        scope.setType("class");
        pool.setContainerFactory(ContainerFactory.jacoco(scope));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setName("jacoco");
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        int limitAdapters = 1;

        //
        SpreadSheet sheet1 = new SpreadSheet(new File("/home/marcus/development/repositories/github/lasso/arena/examples/CalculatorDivideNormal.xlsx"));
        SpreadSheet sheet2 = new SpreadSheet(new File("/home/marcus/development/repositories/github/lasso/arena/examples/CalculatorDivideByZero.xlsx"));

        SheetSequenceSpecificationParser parser = new SheetSequenceSpecificationParser();

        Map<String, SequenceSpecification> ssMap1 = parser.toSequenceSpecifications(sheet1, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap1.size());

        Map<String, SequenceSpecification> ssMap2 = parser.toSequenceSpecifications(sheet2, parseResults.get(0), classUnderTest, "postfix");
        assertEquals(1, ssMap2.size());

        List<SequenceSpecification> sheets = new ArrayList<>(ssMap1.values());
        sheets.addAll(ssMap2.values());

        List<TestCase> testCases = new ArrayList<>();
        for(SequenceSpecification sheet : sheets) {
            CellWriter cellWriter = new TableSawWriter();
            Map<AdaptedImplementation, SequenceExecutionRecords> results = arena.execute(new ArrayList<>(Collections.singletonList(classUnderTest)),
                    parseResults.get(0),
                    Arrays.asList(sheet),
                    cellWriter,
                    limitAdapters);

            // jacoco
            JaCoCoContainer jaCoCoContainer = (JaCoCoContainer) classUnderTest.getProject().getContainer();

            ExecutionDataStore executionDataStore = jaCoCoContainer.getExecutionDataStore();
            for(ExecutionData executionData : executionDataStore.getContents()) {
                TestCase testCase = new TestCase(sheet.getName(), new CodeElements(executionData.getProbes()));
                testCases.add(testCase);
            }
        }

        TestSuiteMinimizer testSuiteMinimizer = new TestSuiteMinimizer();
        MinimalTestSuite minimalTestSuite = testSuiteMinimizer.findMinimalTestSet(testCases, testCases.get(0).getCoveredCodeElements().getTotal());

        assertEquals(2, minimalTestSuite.getSuite().size());
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(0)));
        assertTrue(minimalTestSuite.getSuite().contains(testCases.get(1)));
        assertEquals(4, minimalTestSuite.getTotalElements());
        assertEquals(4, minimalTestSuite.getTotalCovered());
        assertEquals(0, minimalTestSuite.getTotalUncovered());
    }

    static ClassUnderTest createExample(Class<?> exampleClass) {
        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());
        implementation.setName(exampleClass.getSimpleName());
        implementation.setPackagename(exampleClass.getPackage().getName());
        implementation.setGroupId("examples.lasso");
        implementation.setArtifactId("examples");
        implementation.setVersion("1.0.0-SNAPSHOT");
        ClassUnderTest classUnderTest = new ClassUnderTest(new de.uni_mannheim.swt.lasso.core.model.System(implementation));
        //classUnderTest.setPseudo(true);

        // one workaround to avoid resolution of artifacts
        classUnderTest.getProject().setDependencyResult(new DependencyResult(new DependencyRequest()));

        return classUnderTest;
    }
}
