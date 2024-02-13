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
package de.uni_mannheim.swt.lasso.arena.sequence.minimize;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.task.Amplify;
import de.uni_mannheim.swt.lasso.arena.task.Execute;
import de.uni_mannheim.swt.lasso.arena.task.load.FileSystemSheetProvider;
import de.uni_mannheim.swt.lasso.arena.task.load.SingleImplementationProvider;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marcus Kessel
 */
// FIXME needs update
public class SequenceSpecificationMinimizerTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_execute_duplicate() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        File work = new File("/tmp/arena_work_test_run_experiment_" + System.currentTimeMillis());

        String implId = "044d48bc-7e59-45fd-87ee-260bc3260f0e";

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass(implId);

        String tc = t;

        File path = File.createTempFile("arena", "java");
        FileUtils.write(path, tc, StandardCharsets.UTF_8);

        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);

        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

        // enable JUnitRunner
        execute.setRemoveCompileErrors(true);
        execute.setRemoveFlakyTests(true);
        execute.setMinimizeSequences(true); // ENABLED

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setActionId("execute");
        resultsWriter.setAbstractionId("bug");
        int limitAdapters = 1;

        SingleImplementationProvider provider = new SingleImplementationProvider(codeSearch2017, pool, classUnderTest.getId(), path);
        provider.setMethod(true);

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        //adaptationStrategy.addSpecFilter((cut, method) -> method.getName().equals("decode"));

        execute.execute(provider,
                adaptationStrategy,
                limitAdapters,
                resultsWriter);

        ClassUnderTest cut = provider.getPool().getClassUnderTest(classUnderTest.getId()).get();

        List<File> testFiles = cut.getLocalProject().getFiles(cut.getLocalProject().getSrcTest(), "java");

        assertTrue(FileUtils.readFileToString(testFiles.get(0), StandardCharsets.UTF_8).contains("bc1_044d48bc7e5945fd87ee260bc3260f0e"));
        assertFalse(FileUtils.readFileToString(testFiles.get(0), StandardCharsets.UTF_8).contains("bc1_044d48bc7e5945fd87ee260bc3260f0e_duplicate"));
        assertTrue(FileUtils.readFileToString(testFiles.get(0), StandardCharsets.UTF_8).contains("bc2_044d48bc7e5945fd87ee260bc3260f0e"));


        try {
            resultsWriter.toCsv("/tmp/bug_execute_" + System.currentTimeMillis() + ".csv");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String t = "package org.apache.commons.lang;\n" +
            "\n" +
            "import org.junit.After;\n" +
            "import org.junit.AfterClass;\n" +
            "import org.junit.Before;\n" +
            "import org.junit.BeforeClass;\n" +
            "import org.junit.FixMethodOrder;\n" +
            "import org.junit.Test;\n" +
            "import org.junit.runners.MethodSorters;\n" +
            "\n" +
            "@FixMethodOrder(MethodSorters.NAME_ASCENDING)\n" +
            "public class ArrayUtilsOriginalTests {\n" +
            "\n" +
            "    @BeforeClass\n" +
            "    public static void setupAll() {\n" +
            "    }\n" +
            "\n" +
            "    @AfterClass\n" +
            "    public static void teardownAll() {\n" +
            "    }\n" +
            "\n" +
            "    @Before\n" +
            "    public void setup() {\n" +
            "    }\n" +
            "\n" +
            "    @After\n" +
            "    public void teardown() {\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc1_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float float2 = 2.0f;\n" +
            "        float float3 = 3.0f;\n" +
            "        float float4 = 4.0f;\n" +
            "        float float5 = 5.0f;\n" +
            "        float[] floatArray6 = new float[] { float1, float2, float3, float4, float5 };\n" +
            "        int int7 = 4;\n" +
            "        int int8 = 4;\n" +
            "        int int9 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray6, (float) int7, int8);\n" +
            "        org.junit.Assert.assertNotNull(floatArray6);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray6), \"[1.0, 2.0, 3.0, 4.0, 5.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int9 + \"' != '\" + 3 + \"'\", int9 == 3);\n" +
            "    }\n" +
            "    @Test\n" +
            "    public void bc1_044d48bc7e5945fd87ee260bc3260f0e_duplicate() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float float2 = 2.0f;\n" +
            "        float float3 = 3.0f;\n" +
            "        float float4 = 4.0f;\n" +
            "        float float5 = 5.0f;\n" +
            "        float[] floatArray6 = new float[] { float1, float2, float3, float4, float5 };\n" +
            "        int int7 = 4;\n" +
            "        int int8 = 4;\n" +
            "        int int9 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray6, (float) int7, int8);\n" +
            "        org.junit.Assert.assertNotNull(floatArray6);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray6), \"[1.0, 2.0, 3.0, 4.0, 5.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int9 + \"' != '\" + 3 + \"'\", int9 == 3);\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc2_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float float2 = 2.0f;\n" +
            "        float float3 = 3.0f;\n" +
            "        float float4 = 4.0f;\n" +
            "        float float5 = 10.0f;\n" +
            "        float[] floatArray6 = new float[] { float1, float2, float3, float4, float5 };\n" +
            "        int int7 = 4;\n" +
            "        int int8 = 5;\n" +
            "        int int9 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray6, (float) int7, int8);\n" +
            "        org.junit.Assert.assertNotNull(floatArray6);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray6), \"[1.0, 2.0, 3.0, 4.0, 10.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int9 + \"' != '\" + 3 + \"'\", int9 == 3);\n" +
            "    }\n" +
            "\n" +
            "}";

    @Test
    public void test_run_arraylist_minimize() throws IOException {
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
        harvest.setMinimizeSequences(true);

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
}
