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
package de.uni_mannheim.swt.lasso.arena.task.systemtests;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.task.Amplify;
import de.uni_mannheim.swt.lasso.arena.task.load.FileSystemSheetProvider;
import de.uni_mannheim.swt.lasso.arena.task.load.SingleImplementationProvider;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
// FIXME needs update
@Deprecated
public class AmplifyTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_run_arraylist() throws IOException {
        CodeSearch codeSearch = new CodeSearch();
        //String referenceId = "6ce338e3-3c3c-4f52-b595-9b3ed5bb4025";
        File path = new File("sheets/evosuiteRef_f204cfa2-7b5e-4d4b-acce-20e7e048b2f1");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_arraylist_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        //pool.setAdaptationStrategy(new DefaultAdaptationStrategy());
        Amplify harvest = new Amplify(mavenRepository);

        //harvest.setBySequenceSpecification(false);

        harvest.setThreads(1);
//        harvest.setMeasureJaCoCo(false);
//        harvest.setMeasurePIT(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stack");
        resultsWriter.setActionId("amplify");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        harvest.amplify(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/stack_amplified_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_run_Base64_decode() throws IOException {
        CodeSearch codeSearch2020 = new CodeSearch();
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        String referenceId = "564debc5-124a-439f-b9f2-6a247dfc1c20";
        File path = new File("sheets/jss_si_21_exp1/" + referenceId);
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        //pool.setAdaptationStrategy(new DefaultAdaptationStrategy());
        Amplify harvest = new Amplify(mavenRepository);

        //harvest.setThreads(1);
//        harvest.setMeasureJaCoCo(false);
//        harvest.setMeasurePIT(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setActionId("amplify");
        resultsWriter.setAbstractionId("base64decode");
        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch2020, pool) {

            @Override
            protected ClassUnderTest retrieve(String implementation) throws IOException {
                // XXX use method's parent class to get Class contents (simple hack)
                if(StringUtils.equals(referenceId, implementation)) {
                    ClassUnderTest classUnderTest = codeSearch2017.queryForClass(implementation);
                    ClassUnderTest parent = codeSearch2017.queryForClass(classUnderTest.getImplementation().getCode().getParentId());
                    classUnderTest.getImplementation().getCode().setContent(parent.getImplementation().getCode().getContent());
                    return classUnderTest;
                }

                ClassUnderTest classUnderTest = codeSearch2020.queryForClass(implementation);
                ClassUnderTest parent = codeSearch2020.queryForClass(classUnderTest.getImplementation().getCode().getParentId());
                classUnderTest.getImplementation().getCode().setContent(parent.getImplementation().getCode().getContent());
                return classUnderTest;
            }
        };
        provider.setMethod(true);

        harvest.amplify(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64decode_amplified_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_run_Ostermiller_StringHelper_timeout_bug() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("a1a0369e-eb97-4407-a538-01602e3d9f32");

        File path = new File("sheets/ostermiller/a1a0369e-eb97-4407-a538-01602e3d9f32/StringHelperOriginalTests.java");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_adapt_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Amplify amplify = new Amplify(mavenRepository);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setActionId("amplify");
        resultsWriter.setAbstractionId("base64decode");
        int limitAdapters = 1;

        SingleImplementationProvider provider = new SingleImplementationProvider(codeSearch2017, pool, classUnderTest.getId(), path);
        provider.setMethod(true);

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        //adaptationStrategy.addSpecFilter((cut, method) -> method.getName().equals("decode"));

        amplify.amplify(provider,
                adaptationStrategy,
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64decode_execute_adapt_" + System.currentTimeMillis() + ".csv");
    }
}
