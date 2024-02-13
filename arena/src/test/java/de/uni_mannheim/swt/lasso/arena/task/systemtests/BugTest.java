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
import de.uni_mannheim.swt.lasso.arena.task.Execute;
import de.uni_mannheim.swt.lasso.arena.task.load.FileSystemSheetProvider;
import de.uni_mannheim.swt.lasso.arena.writer.TableSawWriter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Marcus Kessel
 */
public class BugTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_ImmutableSet() throws IOException {
        String mql = "ImmutableSet {\n" +
                "            ImmutableSet(java.util.Collection)\n" +
                "            add(java.lang.Object)->void\n" +
                "            remove(java.lang.Object)->void\n" +
                "            iterator()->java.util.Iterator\n" +
                "            size()->int\n" +
                "        }";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/ImmutableSet");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);
        execute.setMeasureJaCoCo(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_Matrix() throws IOException {
        String mql = "Matrix {\n" +
                "            Matrix(double[][],int,int)\n" +
                "            set(int,int,double)->void\n" +
                "            get(int, int)->double\n" +
                "            add(Matrix)->Matrix\n" +
                "            transpose()->Matrix\n" +
                "        }";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/Matrix");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);
        execute.setMeasureJaCoCo(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_BoundedStack() throws IOException {
        String mql = "BoundedStack {\n" +
                "            BoundedStack(int)\n" +
                "            push(java.lang.Object)->java.lang.Object\n" +
                "            pop()->java.lang.Object\n" +
                "            peek()->java.lang.Object\n" +
                "            size()->int\n" +
                "            isFull()->boolean\n" +
                "            isEmpty()->boolean\n" +
                "        }";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/filter_6b156154-5e96-4e9f-80ca-327406b9863d");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);
        execute.setMeasureJaCoCo(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_StringComparator() throws IOException {
        String mql = "StringComparator {\n" +
                "            compare(java.lang.String,java.lang.String)->int}";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/filter_ec649c44-63da-461a-91e7-12b991b564ba");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);
        execute.setMeasureJaCoCo(true);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_JSON() throws IOException {
        String mql = "Json {\n" +
                "            fromJson(java.lang.String)->java.util.Map}";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/filter_b9fc4871-189c-4871-88fd-43a381ae86fe");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_toJSON() throws IOException {
        String mql = "Json {\n" +
                "            toJson(java.util.Map)->java.lang.String}";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("sheets/filter_0fc5a5c5-6966-4690-b066-2062c2ff7092");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_FileExt() throws IOException {
        String mql = "Filename{\n" +
                "    getExtension(java.io.File)->java.lang.String\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("/home/marcus/development/repositories/lasso/arena/sheets/FileExt/");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }

    @Test
    public void test_TreeNode() throws IOException {
        String mql = "TreeNode{\n" +
                "    setName(java.lang.String)->void\n" +
                "    addChild(TreeNode)->void\n" +
                "    getChildren()->java.util.List\n" +
                "}";

        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());

        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
        InterfaceSpecification specification = parseResults.get(0);

        File path = new File("/home/marcus/development/repositories/lasso/arena/sheets/TreeNode/");
        System.out.println(path.getAbsolutePath());

        CandidatePool pool = new CandidatePool(mavenRepository);
        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
        work.mkdirs();
        pool.setWorkingDirectory(work);
        Execute execute = new Execute(mavenRepository);
        execute.setThreads(1);
        execute.setBySequenceSpecification(false);

        //
        TableSawWriter resultsWriter = new TableSawWriter();
        resultsWriter.setExecutionId("myexecution");
        resultsWriter.setAbstractionId("stringcomparator");
        resultsWriter.setActionId("execute");

        int limitAdapters = 10;

        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
        provider.setInterfaceSpecification(specification);
        execute.execute(provider,
                new DefaultAdaptationStrategy(),
                limitAdapters,
                resultsWriter);

        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");

        System.out.println(resultsWriter.getTable().printAll());
    }
}
