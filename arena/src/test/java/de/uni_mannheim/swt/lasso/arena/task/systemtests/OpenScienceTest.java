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
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import de.uni_mannheim.swt.lasso.benchmark.*;

import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.junit.jupiter.api.Test;
import randoop.org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author Marcus Kessel
 */
public class OpenScienceTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_execute_HumanEval_23_strlen_generated() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

//        ClassUnderTest classUnderTest = codeSearch.queryForClasses("*:*", 1, new String[]{
//                "benchmark:\"humaneval-java-reworded\"",
//                "problem:\"HumanEval_23_strlen\"",
//                "generator:\"humaneval-java-davinci-0.2-reworded\"",
//                "k:0"
//        }).get(0);

        //System.out.println(classUnderTest);

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_23_strlen");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval/execute_c3dedcb0-0d94-4914-9c34-586815701aa2/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);

        // FIXME remove
        execute.setMeasureJaCoCo(false);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_execute_HumanEval_23_strlen() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

//        ClassUnderTest classUnderTest = codeSearch.queryForClasses("*:*", 1, new String[]{
//                "benchmark:\"humaneval-java-reworded\"",
//                "problem:\"HumanEval_23_strlen\"",
//                "generator:\"humaneval-java-davinci-0.2-reworded\"",
//                "k:0"
//        }).get(0);

        //System.out.println(classUnderTest);

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_23_strlen");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval//HumanEval_23_strlen/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);

        // FIXME remove
        execute.setMeasureJaCoCo(false);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_execute_HumanEval_23_strlen_JaCoCo() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

//        ClassUnderTest classUnderTest = codeSearch.queryForClasses("*:*", 1, new String[]{
//                "benchmark:\"humaneval-java-reworded\"",
//                "problem:\"HumanEval_23_strlen\"",
//                "generator:\"humaneval-java-davinci-0.2-reworded\"",
//                "k:0"
//        }).get(0);

        //System.out.println(classUnderTest);

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_23_strlen");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval//HumanEval_23_strlen/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);

        // enable
        execute.setMeasureJaCoCo(true);

        // set scope
        Scope scope = new Scope();
        scope.setType("class");
        List<String> methodBlacklist = new ArrayList<>();
        methodBlacklist.add("main");
        methodBlacklist.add("<init>");
        methodBlacklist.add("<clinit>");
        scope.addConfiguration("methodBlacklist", (Serializable) methodBlacklist);
        execute.setScope(scope);

        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_execute_HumanEval_1_separate_paren_groups() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_1_separate_paren_groups");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval/HumanEval_1_separate_paren_groups/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);

        // FIXME remove
        execute.setMeasureJaCoCo(false);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_execute_HumanEval_3_below_zero() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_3_below_zero");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval/HumanEval_3_below_zero/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);

        // FIXME remove
        execute.setMeasureJaCoCo(false);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_execute_HumanEval_95_check_dict_case() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_95_check_dict_case");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval/HumanEval_95_check_dict_case/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);

        // FIXME remove
        execute.setMeasureJaCoCo(false);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_execute_HumanEval_107_even_odd_palindrome() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_107_even_odd_palindrome");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/humaneval/HumanEval_107_even_odd_palindrome/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Execute execute = new Execute(mavenRepository);

        // FIXME remove
        execute.setMeasureJaCoCo(false);
        execute.setBySequenceSpecification(false);

        execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    //
    @Test
    public void test_execute_HumanEval_23_strlen_AMPLIFY() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.multipleBenchmark23());

//        ClassUnderTest classUnderTest = codeSearch.queryForClasses("*:*", 1, new String[]{
//                "benchmark:\"humaneval-java-reworded\"",
//                "problem:\"HumanEval_23_strlen\"",
//                "generator:\"humaneval-java-davinci-0.2-reworded\"",
//                "k:0"
//        }).get(0);

        //System.out.println(classUnderTest);

        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");
        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_23_strlen");

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(ab.getLql());
        InterfaceSpecification specification = parseResults.get(0);

        for(Sequence seq : ab.getSequences()) {
            for(Statement stmt : seq.getStatements()) {
                System.out.println(ToStringBuilder.reflectionToString(stmt));
            }
        }

        File path = new File("sheets/execute_b824023c-c6d5-4db3-8083-d12353d06506/");

//        File path = new File("sheets/20af3b70-4dce-4c28-be08-306c3ce4db25/MathUtils_0_Test.java");
//        System.out.println(path.getAbsolutePath());
//
        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_benchmark_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);

//        pool.addClasses(Arrays.asList(classUnderTest));
//
//        pool.initProjects();

        Amplify execute = new Amplify(mavenRepository);

        // enable
        execute.setMeasureJaCoCo(false);

        // set scope
        Scope scope = new Scope();
        scope.setType("class");
        List<String> methodBlacklist = new ArrayList<>();
        methodBlacklist.add("main");
        methodBlacklist.add("<init>");
        methodBlacklist.add("<clinit>");
        scope.addConfiguration("methodBlacklist", (Serializable) methodBlacklist);
        execute.setScope(scope);

        execute.setBySequenceSpecification(false);

        // FIXME
        //execute.setWriteSequenceRecords(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId(ab.getId());
        resultsWriter.setActionId("execute");

        int limitAdapters = 1;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.amplify(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/arena_benchmark_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }
}
