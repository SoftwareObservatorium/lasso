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
package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.compile.TestSupport;
import de.uni_mannheim.swt.lasso.arena.task.Execute;
import de.uni_mannheim.swt.lasso.arena.task.load.FileSystemSheetProvider;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
// FIXME needs update
public class PseudoImplementationTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void testToSequenceSpecification_ArrayStack__PSEUDO() throws IOException {
        String mql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();

        // CREATE PSEUDO CUT
        ClassUnderTest pseudo = TestSupport.createPseudoImplementation("Stack");

        // actual cut
        ClassUnderTest classUnderTest = codeSearch.queryForClass("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025");

        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Arrays.asList(pseudo, classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        DefaultArena arena = new DefaultArena();
        arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

        String testClass = FileUtils.readFileToString(new File("sheets/PseudoStack.java"), StandardCharsets.UTF_8);

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

        //InterfaceSpecification specification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, specification, pseudo,pseudo.getClassName(), "");

        assertEquals(1, ssMap.size());

        //ClassUnderTest

        AdaptedImplementation adaptedImplementation = arena.adapt(classUnderTest, specification, 1).get(0);

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            SequenceExecutionRecord sequenceExecutionRecord = ssMap.get(name).instantiate(specification, adaptedImplementation);
            System.out.println(sequenceExecutionRecord.getSequence());
        }

    }

    @Test
    public void test_MANUAL_SPECIFICATION_ArrayStack() throws IOException {
        String mql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();
        //String referenceId = "6ce338e3-3c3c-4f52-b595-9b3ed5bb4025";
        File path = new File("sheets/evosuiteRef_f204cfa2-7b5e-4d4b-acce-20e7e048b2f1");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_arraylist_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stack");
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        // SET MANUAL SPECIFICATION
        provider.setInterfaceSpecification(codeSearch.fromLQL(mql).get(0));

        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/stack_execute_" + System.currentTimeMillis() + ".csv");
    }
}
