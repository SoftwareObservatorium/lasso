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
import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.JUnitSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.task.Amplify;
import de.uni_mannheim.swt.lasso.arena.task.load.SingleImplementationProvider;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.experimental.JSSSIAutoV2;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marcus Kessel
 */
// FIXME needs update
@Deprecated
public class Experiment1Test {

    private static final Logger LOG = LoggerFactory
            .getLogger(Experiment1Test.class);

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_dataset() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017());

        Map<String, String> testClasses = JSSSIAutoV2.getTestClasses();

        int c = 0;
        for (String implId : testClasses.keySet()) {
            // cannot resolve
            // 2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570 JRStringUtil
            // d083e652-739d-44a5-b435-07f0289893eb StringUtils
            // 3cf7db4f-407b-44e7-9398-abaedc4747d5 org.jgrasstools.gears.utils.geometry.GeometryUtilities
            // 16eb796d-1533-4ff8-9be4-318d52a09a50 org.apache.poi.util.StringUtil cannot resolve cut method
            if (StringUtils.equalsAny(implId, "2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570", "d083e652-739d-44a5-b435-07f0289893eb")) {
                LOG.debug("Skipping {}", implId);
                continue;
            }

//            // FIXME REMVOE
//            if(!implId.equals("570386ad-99a1-43fb-b854-d1679e269d1e")) {
//                continue;
//            }


            try {
                ClassUnderTest classUnderTest = codeSearch.queryForClass(implId);

                LOG.debug("Implementation " + classUnderTest);
                LOG.debug("FQ " + classUnderTest.getClassName());

                CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
                // automatically resolves project-related artifacts
                pool.initProjects();

                String testSource = testClasses.get(implId);

                LOG.debug("TEST CLASS \n==========\n" + testSource);

                JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

                Map<String, InterfaceSpecification> specification = importJUnitClass.toSpecification(testSource, classUnderTest);

                for (Map.Entry<String, InterfaceSpecification> cut : specification.entrySet()) {
                    LOG.debug("CUT " + cut.getKey());
                    LOG.debug("SPEC MQL " + cut.getValue().toLQL());
                }

                //
                Map<String, SequenceSpecification> sheets = importJUnitClass.toSequenceSpecifications(testSource, specification.get(classUnderTest.getClassName()), classUnderTest, classUnderTest.getClassName(), "");

//                if (++c == 3) {
//                    break;
//                }
            } catch (Throwable e) {
                LOG.warn(implId + " FAILED", e);
            }

            try {
                Thread.sleep(2 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test_dataset_execute() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017());

        Map<String, String> testClasses = JSSSIAutoV2.getTestClasses();

        TableSawWriter writer = new TableSawWriter();

        int c = 0;
        for (String implId : testClasses.keySet()) {
            // cannot resolve
            // 2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570 JRStringUtil
            // d083e652-739d-44a5-b435-07f0289893eb StringUtils
            if (StringUtils.equalsAny(implId, "2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570", "d083e652-739d-44a5-b435-07f0289893eb")) {
                LOG.debug("Skipping {}", implId);
                continue;
            }

//                        // FIXME REMVOE
//            if(!implId.equals("2114889a-fd89-4ab5-9a4c-f1062cb71545")) {
//                continue;
//            }

            try {
                ClassUnderTest classUnderTest = codeSearch.queryForClass(implId);

                LOG.debug("Implementation " + classUnderTest);
                LOG.debug("FQ " + classUnderTest.getClassName());

                CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
                // automatically resolves project-related artifacts
                pool.initProjects();

                String testSource = testClasses.get(implId);

                LOG.debug("TEST CLASS \n==========\n" + testSource);

                JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

                Map<String, InterfaceSpecification> specification = importJUnitClass.toSpecification(testSource, classUnderTest);

                for (Map.Entry<String, InterfaceSpecification> cut : specification.entrySet()) {
                    LOG.debug("CUT " + cut.getKey());
                    LOG.debug("SPEC MQL " + cut.getValue().toLQL());
                }

                InterfaceSpecification specification1 = new ArrayList<>(specification.values()).get(0);

                //
                Map<String, SequenceSpecification> sheets = importJUnitClass.toSequenceSpecifications(testSource,
                        specification.get(classUnderTest.getClassName()),
                        classUnderTest, classUnderTest.getClassName(),
                        classUnderTest.getId());

                DefaultArena arena = new DefaultArena();
                arena.setAdaptationStrategy(new DefaultAdaptationStrategy());

                Map<AdaptedImplementation, SequenceExecutionRecords> results = arena.execute(
                        new ArrayList<>(Collections.singleton(classUnderTest)),
                        specification1,
                        new ArrayList<>(sheets.values()),
                        writer,
                        1);

//                if (++c == 1) {
//                    break;
//                }
            } catch (Throwable e) {
                LOG.warn(implId + " FAILED", e);
            }
        }

        writer.getTable().write().csv("/tmp/exp1_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_dataset_single() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017());

        Map<String, String> testClasses = JSSSIAutoV2.getTestClasses();

        // cannot resolve
        // 2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570 JRStringUtil
        // d083e652-739d-44a5-b435-07f0289893eb StringUtils

        //String implId = "16eb796d-1533-4ff8-9be4-318d52a09a50"; unresolved also JRStringUtil
        String implId = "d083e652-739d-44a5-b435-07f0289893eb";
        ClassUnderTest classUnderTest = codeSearch.queryForClass(implId);

        System.out.println("Implementation " + classUnderTest);
        System.out.println("FQ " + classUnderTest.getClassName());

        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        String testSource = testClasses.get(implId);

        System.out.println("TEST CLASS \n==========\n" + testSource);

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

        Map<String, InterfaceSpecification> specification = importJUnitClass.toSpecification(testSource, classUnderTest);

        for (Map.Entry<String, InterfaceSpecification> cut : specification.entrySet()) {
            LOG.debug("CUT " + cut.getKey());
            LOG.debug("SPEC MQL " + cut.getValue().toLQL());
        }
//
//            System.out.println(specification.get(classUnderTest.getClassName()).getMethods());
//
//            //
//            Map<String, ExecutableSheet> sheets = importJUnitClass.toSheets(testSource, specification.get(classUnderTest.getClassName()), classUnderTest,classUnderTest.getClassName(), "");
    }

    @Test
    public void test_run_matrix() throws IOException {
        // FIXME take generated tests from data set
        // best to take them directly in project layout stored on cluster machines.
        // reuse existing tests

        String referenceId = "379e30b8-cb5f-45c4-93b9-d592450d2743";
        File path = new File("sheets/jss_si_21_exp1/amplified_tests/MulGen30/amplified_tests/24/MatrixTest.java");
        System.out.println(path.getAbsolutePath());

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017()); // mavenCentral2017

        CandidatePool pool = new CandidatePool(mavenRepository);
        //arena.setAdaptationStrategy(new DefaultAdaptationStrategy());
        Amplify harvest = new Amplify(mavenRepository);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        int limitAdapters = 1;

        SingleImplementationProvider provider = new SingleImplementationProvider(codeSearch, pool, referenceId, path);
        provider.setMethod(true); // is method
        harvest.amplify(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/matrix_amplified_" + System.currentTimeMillis() + ".csv");
    }
}
