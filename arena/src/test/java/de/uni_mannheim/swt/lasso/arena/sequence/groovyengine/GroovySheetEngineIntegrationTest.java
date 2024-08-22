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
package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test adaptation capabilities with Examples
 *
 * @author Marcus Kessel
 */
public class GroovySheetEngineIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(GroovySheetEngineIntegrationTest.class);

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);
    CodeSearch codeSearch = new CodeSearch(SolrInstance.local());

    @Test
    public void test_Stack_empty_constructor() throws IOException, NoSuchMethodException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("Stack {\n" +
                "push(java.lang.String)->java.lang.String\n" +
                "size()->int\n" +
                "}");

        InterfaceSpecification interfaceSpecification = parseResults.get(0);

        ClassUnderTest classUnderTest = createExample(Stack.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

        AdaptedImplementation first = adaptedImplementations.get(0);
        System.out.println(ToStringBuilder.reflectionToString(first));

        //
        GroovySheetEngine groovySheetEngine = new GroovySheetEngine(classUnderTest.getProject().getContainer());

        CallableSequence callableSequence = new CallableSequence();
        CallableStatement stmt0 = callableSequence.fromCode("Stack()", interfaceSpecification, 0);
        // FIXME may also be inlined (string concat)
        CallableStatement stmt1 = callableSequence.fromCode("\"Hello\"");
        CallableStatement stmt2 = callableSequence.fromCode("push()", interfaceSpecification, 0, Arrays.asList(stmt0, stmt1));
        CallableStatement stmt3 = callableSequence.fromCode("size()", interfaceSpecification, 1, Arrays.asList(stmt0));

        ExecutedSequence executedSequence = groovySheetEngine.run(callableSequence, interfaceSpecification, first);

        debug(executedSequence);

        Assertions.assertEquals(4, executedSequence.getNoOfExecutedStatements());
        Assertions.assertEquals("[]", executedSequence.getStatement(0).getSerializedOutput());
        Assertions.assertEquals("\"Hello\"", executedSequence.getStatement(1).getSerializedOutput());
        Assertions.assertEquals("\"Hello\"", executedSequence.getStatement(2).getSerializedOutput());
        Assertions.assertEquals("1", executedSequence.getStatement(3).getSerializedOutput());
    }

    @Test
    public void test_List_nonempty_constructor() throws IOException, NoSuchMethodException {
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL("List {\n" +
                "List(int)\n" +
                "add(java.lang.String)->boolean\n" +
                "size()->int\n" +
                "}");

        InterfaceSpecification interfaceSpecification = parseResults.get(0);

        // FIXME cut
        ClassUnderTest classUnderTest = createExample(ArrayList.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

        // FIXME
        AdaptedImplementation first = adaptedImplementations.get(0);
        System.out.println(ToStringBuilder.reflectionToString(first));

        //
        GroovySheetEngine groovySheetEngine = new GroovySheetEngine(classUnderTest.getProject().getContainer());

        CallableSequence callableSequence = new CallableSequence();
        CallableStatement stmt0 = callableSequence.fromCode("1");
        CallableStatement stmt1 = callableSequence.fromCode("List()", interfaceSpecification, 0, Arrays.asList(stmt0)); // FIXME get instance
        // FIXME may also be inlined (string concat) / or provided as code "add(\"Hello\")"
        CallableStatement stmt2 = callableSequence.fromCode("\"Hello\"");
        CallableStatement stmt3 = callableSequence.fromCode("add()", interfaceSpecification, 0, Arrays.asList(stmt1, stmt2));
        CallableStatement stmt4 = callableSequence.fromCode("size()", interfaceSpecification, 1, Arrays.asList(stmt1));

        ExecutedSequence executedSequence = groovySheetEngine.run(callableSequence, interfaceSpecification, first);

        debug(executedSequence);

        Assertions.assertEquals(5, executedSequence.getNoOfExecutedStatements());
        Assertions.assertEquals("1", executedSequence.getStatement(0).getSerializedOutput());
        Assertions.assertEquals("[]", executedSequence.getStatement(1).getSerializedOutput());
        Assertions.assertEquals("\"Hello\"", executedSequence.getStatement(2).getSerializedOutput());
        Assertions.assertEquals("true", executedSequence.getStatement(3).getSerializedOutput());
        Assertions.assertEquals("1", executedSequence.getStatement(4).getSerializedOutput());
    }

    public static ClassUnderTest createExample(Class<?> exampleClass) {
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

    static void debug(ExecutedSequence executedSequence) {
        for(ExecutedStatement executedStatement : executedSequence.getStatements()) {
            LOG.info(ToStringBuilder.reflectionToString(executedStatement, ToStringStyle.JSON_STYLE));
        }
    }
}
