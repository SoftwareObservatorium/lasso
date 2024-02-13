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
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.task.Amplify;
import de.uni_mannheim.swt.lasso.arena.task.Execute;
import de.uni_mannheim.swt.lasso.arena.task.load.FileSystemSheetProvider;
import de.uni_mannheim.swt.lasso.arena.task.load.SingleImplementationProvider;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.experimental.JSSSIAutoV2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Marcus Kessel
 */
// FIXME needs update
@Deprecated
public class ExecuteTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_Base64_encode() throws IOException {
        String mql = "Base64{encode(byte[])->byte[]}";
        CodeSearch codeSearch = new CodeSearch();
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/base64");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_base64_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

        // FIXME remove
        execute.setMeasureJaCoCo(true);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("base64");
        resultsWriter.setActionId("execute");

        int limitAdapters = 5;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64_execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_CDS_Stack() throws IOException {
        String mql = "Stack{\n" +
                //"Stack()\n" +
                "push(java.lang.object)->void\n" +
                "peek()->java.lang.object\n" +
                "pop()->java.lang.object\n" +
                "isEmpty()->boolean\n" +
                "size()->int\n" +
                "}";
        CodeSearch codeSearch = new CodeSearch();
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/arena_84db4743-1182-4d37-8fd4-c1e9150c4ed7");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_stack_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        //Execute execute = new Execute(mavenRepository);
        Amplify execute = new Amplify(mavenRepository);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stack");
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
//        execute.execute(provider,
//                new DefaultAdaptationStrategy(),
//                limitAdapters,
//                resultsWriter);

        Set<String> allCuts = provider.findCuts();

        System.out.println(allCuts);

        provider.resolveCuts(allCuts);

        execute.amplify(new ArrayList<>(allCuts),
                provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/stack_execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    // ---------------------------

    @Test
    public void test_run_arraylist_java() throws IOException {
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
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/stack_execute_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_run_arraylist_XLSX() throws IOException {
        String mql = "Stack{" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        //String referenceId = "6ce338e3-3c3c-4f52-b595-9b3ed5bb4025";
        File path = new File("sheets/execute_9e733344-e8a3-4910-93d2-403a302828a6");
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
        provider.setInterfaceSpecification(specification);

        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/stack_execute_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_run_arraylist_XLSX_ORACLE() throws IOException {
        String mql = "Stack{" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch();
        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/oracle");
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
        provider.setInterfaceSpecification(specification);

        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/stack_execute_" + System.currentTimeMillis() + ".csv");
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
        Execute execute = new Execute(mavenRepository);

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

        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64decode_execute_" + System.currentTimeMillis() + ".csv");
    }

    /**
     * Adaptation scenario
     *
     * @throws IOException
     */
    @Test
    public void test_run_Base64_adaptation_convert() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("ca07ef4f-5576-4e8b-8328-6e7bd648ff03");

        File path = new File("sheets/ca07ef4f-5576-4e8b-8328-6e7bd648ff03/src/test/java/org/keycloak/models/utils/Base32Test.java");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_adapt_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setActionId("amplify");
        resultsWriter.setAbstractionId("base64decode");
        int limitAdapters = 1;

        SingleImplementationProvider provider = new SingleImplementationProvider(codeSearch2017, pool, classUnderTest.getId(), path);
        provider.setMethod(true);

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        adaptationStrategy.addSpecFilter((cut, method) -> method.getName().equals("decode"));

        execute.execute(provider,
                adaptationStrategy,
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64decode_execute_adapt_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_run_Base64_adaptation_convert_bug() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("ca07ef4f-5576-4e8b-8328-6e7bd648ff03");

        File path = new File("sheets/ca07ef4f-5576-4e8b-8328-6e7bd648ff03/src/test/java/org/keycloak/models/utils/Base32Test_2.java");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_adapt_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setActionId("amplify");
        resultsWriter.setAbstractionId("base64decode");
        int limitAdapters = 1;

        SingleImplementationProvider provider = new SingleImplementationProvider(codeSearch2017, pool, classUnderTest.getId(), path);
        provider.setMethod(true);

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        adaptationStrategy.addSpecFilter((cut, method) -> method.getName().equals("encode"));

        execute.execute(provider,
                adaptationStrategy,
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64decode_execute_adapt_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_run_Ostermiller_StringHelper_timeout() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("a1a0369e-eb97-4407-a538-01602e3d9f32");

        File path = new File("sheets/ostermiller/a1a0369e-eb97-4407-a538-01602e3d9f32/StringHelperOriginalTests.java");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_adapt_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

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

        execute.execute(provider,
                adaptationStrategy,
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/base64decode_execute_adapt_" + System.currentTimeMillis() + ".csv");
    }

    //-------

    // 7688ec91-4436-4a53-bcb7-76f86303199e
    @Test
    public void test_execute_bug1() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("7688ec91-4436-4a53-bcb7-76f86303199e");

        File path = new File("sheets/7688ec91-4436-4a53-bcb7-76f86303199e/NumberComparerTest.java");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_adapt_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

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

        resultsWriter.getTable().write().csv("/tmp/bug_execute_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_execute_MathUtils_bug_timeoutexceeded_compile_bug() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        ClassUnderTest classUnderTest = codeSearch2017.queryForClass("20af3b70-4dce-4c28-be08-306c3ce4db25");

        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_run_Base64_decode_adapt_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);

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

        resultsWriter.getTable().write().csv("/tmp/bug_execute_" + System.currentTimeMillis() + ".csv");
    }

    @Test
    public void test_execute_experiment1() throws IOException {
        CodeSearch codeSearch2017 = new CodeSearch(SolrInstance.mavenCentral2017());

        Map<String, String> testClasses = JSSSIAutoV2.getTestClasses();

        File work = new File("/tmp/arena_work_test_run_experiment_" + System.currentTimeMillis());

        int c = 0;
        for (String implId : testClasses.keySet()) {
//            if(!implId.equals("044d48bc-7e59-45fd-87ee-260bc3260f0e")) {
//                continue;
//            }

            ClassUnderTest classUnderTest = codeSearch2017.queryForClass(implId);

            String tc = testClasses.get(implId);
//            tc = t;

            File path = File.createTempFile("arena", "java");
            FileUtils.write(path, tc, StandardCharsets.UTF_8);

            System.out.println(path.getAbsolutePath());

            CandidatePool pool = new CandidatePool(mavenRepository);

            work.mkdirs();
            pool.setWorkingDirectory(work);
            Execute execute = new Execute(mavenRepository);

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

            try {
                resultsWriter.toCsv("/tmp/bug_execute_" + System.currentTimeMillis() + ".csv");
            } catch (Throwable e) {
                e.printStackTrace();
            }
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
            "\n" +
            "    @Test\n" +
            "    public void bc2_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float float2 = 2.0f;\n" +
            "        float float3 = 3.0f;\n" +
            "        float float4 = 4.0f;\n" +
            "        float float5 = 5.0f;\n" +
            "        float[] floatArray6 = new float[] { float1, float2, float3, float4, float5 };\n" +
            "        int int7 = 4;\n" +
            "        int int8 = 5;\n" +
            "        int int9 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray6, (float) int7, int8);\n" +
            "        org.junit.Assert.assertNotNull(floatArray6);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray6), \"[1.0, 2.0, 3.0, 4.0, 5.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int9 + \"' != '\" + 3 + \"'\", int9 == 3);\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc3_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float float2 = 2.0f;\n" +
            "        float float3 = 3.0f;\n" +
            "        float float4 = 4.0f;\n" +
            "        float float5 = 5.0f;\n" +
            "        float[] floatArray6 = new float[] { float1, float2, float3, float4, float5 };\n" +
            "        int int7 = 4;\n" +
            "        int int8 = 7;\n" +
            "        int int9 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray6, (float) int7, int8);\n" +
            "        org.junit.Assert.assertNotNull(floatArray6);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray6), \"[1.0, 2.0, 3.0, 4.0, 5.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int9 + \"' != '\" + 3 + \"'\", int9 == 3);\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc4_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float float2 = 2.0f;\n" +
            "        float float3 = 3.0f;\n" +
            "        float float4 = 4.0f;\n" +
            "        float float5 = 5.0f;\n" +
            "        float[] floatArray6 = new float[] { float1, float2, float3, float4, float5 };\n" +
            "        int int7 = 7;\n" +
            "        int int8 = 2;\n" +
            "        int int9 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray6, (float) int7, int8);\n" +
            "        org.junit.Assert.assertNotNull(floatArray6);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray6), \"[1.0, 2.0, 3.0, 4.0, 5.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int9 + \"' != '\" + (-1) + \"'\", int9 == (-1));\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc5_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float float1 = 1.0f;\n" +
            "        float[] floatArray2 = new float[] { float1 };\n" +
            "        int int3 = 1;\n" +
            "        int int4 = 0;\n" +
            "        int int5 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray2, (float) int3, int4);\n" +
            "        org.junit.Assert.assertNotNull(floatArray2);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray2), \"[1.0]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int5 + \"' != '\" + 0 + \"'\", int5 == 0);\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc6_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float[] floatArray1 = new float[] {};\n" +
            "        int int2 = 4;\n" +
            "        int int3 = 2;\n" +
            "        int int4 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray1, (float) int2, int3);\n" +
            "        org.junit.Assert.assertNotNull(floatArray1);\n" +
            "        org.junit.Assert.assertEquals(java.util.Arrays.toString(floatArray1), \"[]\");\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int4 + \"' != '\" + (-1) + \"'\", int4 == (-1));\n" +
            "    }\n" +
            "\n" +
            "    @Test\n" +
            "    public void bc7_044d48bc7e5945fd87ee260bc3260f0e() throws Throwable {\n" +
            "        org.apache.commons.lang.ArrayUtils arrayUtils0 = new org.apache.commons.lang.ArrayUtils();\n" +
            "        float[] floatArray1 = null;\n" +
            "        int int2 = 4;\n" +
            "        int int3 = 2;\n" +
            "        int int4 = org.apache.commons.lang.ArrayUtils.lastIndexOf(floatArray1, (float) int2, int3);\n" +
            "        org.junit.Assert.assertTrue(\"'\" + int4 + \"' != '\" + (-1) + \"'\", int4 == (-1));\n" +
            "    }\n" +
            "}";
}
