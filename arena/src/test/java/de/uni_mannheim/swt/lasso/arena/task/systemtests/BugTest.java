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

/**
 * @author Marcus Kessel
 */
public class BugTest {

//    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
//    File localRepo = new File("/tmp/lalalamvn/local-repo");
//
//    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
//    MavenRepository mavenRepository = new MavenRepository(resolver);
//
//    @Test
//    public void test_ImmutableSet() throws IOException {
//        String mql = "ImmutableSet {\n" +
//                "            ImmutableSet(java.util.Collection)\n" +
//                "            add(java.lang.Object)->void\n" +
//                "            remove(java.lang.Object)->void\n" +
//                "            iterator()->java.util.Iterator\n" +
//                "            size()->int\n" +
//                "        }";
//
//        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2023());
//
//        List<InterfaceSpecification> parseResults = codeSearch.fromLQL(mql);
//        InterfaceSpecification specification = parseResults.get(0);
//
//        File path = new File("sheets/ImmutableSet");
//        System.out.println(path.getAbsolutePath());
//
//        CandidatePool pool = new CandidatePool(mavenRepository);
//        File work = new File("/tmp/arena_work_test_stringcomparator_" + System.currentTimeMillis());
//        work.mkdirs();
//        pool.setWorkingDirectory(work);
//        Execute execute = new Execute(mavenRepository);
//        execute.setThreads(1);
//        execute.setBySequenceSpecification(false);
//        execute.setMeasureJaCoCo(false);
//
//        //
//        TableSawWriter resultsWriter = new TableSawWriter();
//        resultsWriter.setExecutionId("myexecution");
//        resultsWriter.setAbstractionId("stringcomparator");
//        resultsWriter.setActionId("execute");
//
//        int limitAdapters = 10;
//
//        FileSystemSheetProvider provider = new FileSystemSheetProvider(path, codeSearch, pool);
//        provider.setInterfaceSpecification(specification);
//        execute.execute(provider,
//                new DefaultAdaptationStrategy(),
//                limitAdapters,
//                resultsWriter);
//
//        resultsWriter.getTable().write().csv("/tmp/execute_" + System.currentTimeMillis() + ".csv");
//
//        System.out.println(resultsWriter.getTable().printAll());
//    }
}
