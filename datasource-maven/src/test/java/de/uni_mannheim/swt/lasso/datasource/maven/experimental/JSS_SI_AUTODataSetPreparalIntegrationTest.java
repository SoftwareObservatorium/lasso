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
package de.uni_mannheim.swt.lasso.datasource.maven.experimental;

import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.systemtests.HttpUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.filter.MethodSignatureFilter;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.experimental.JSSSIAuto;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.datasource.maven.support.RandomMavenCentralRepository;
import de.uni_mannheim.swt.lasso.index.CandidateQueryResult;
import de.uni_mannheim.swt.lasso.index.SearchOptions;

import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Marcus Kessel
 */
public class JSS_SI_AUTODataSetPreparalIntegrationTest {

    private static RandomMavenCentralRepository mavenCentralRepository;
    private static MavenCentralIndex mavenCentralIndex;

    private MethodSignatureFilter methodSignatureFilter = new MethodSignatureFilter();

    @BeforeAll
    public static void before() {
        HttpClient client = HttpUtils.createHttpClient("foqsadmin", "hgj490sajcf4309");

        SolrClient solrClient = new HttpSolrClient.Builder("http://swt100.informatik.uni-mannheim.de:8983/solr/candidates/")
                .withHttpClient(client).build();
        mavenCentralRepository = new RandomMavenCentralRepository(solrClient);

        assertNotNull(mavenCentralRepository);

        mavenCentralIndex = new MavenCentralIndex(mavenCentralRepository,
                new QueryBuilder());
    }

    @Test
    public void merge_DivAmp() throws IOException {
        File dir = new File("/home/marcus/development/experiments/JSS_SI_AUTO_21/dataset/temp/divamp");

        Collection<File> files = FileUtils.listFiles(
                dir,
                new String[] { "java" }, true);

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        Map<String, File> out = new HashMap<>();

        // prepare
        File outDir = new File(dir, "amplified_tests");
        outDir.mkdirs();

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            for(File file : files) {
                //System.out.println(file.getAbsolutePath());

                if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                    String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                    System.out.println(implId + " => " + testSuite + " " + file.length());

                    out.put(implId, file);

                    File cand = new File(outDir, row.getInt(0) + "");

                    // double-blind
                    String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    source = StringUtils.replace(source, "LASSO", "XXXX");

                    //FileUtils.copyFile(file, new File(cand, file.getName()));
                    FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println(out.size());
    }

    @Test
    public void merge_DivGen() throws IOException {
        File dir = new File("/home/marcus/development/experiments/JSS_SI_AUTO_21/dataset/temp/divgen");

        Collection<File> files = FileUtils.listFiles(
                dir,
                new String[] { "java" }, true);

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        Map<String, File> out = new HashMap<>();

        // prepare
        File outDir = new File(dir, "amplified_tests");
        outDir.mkdirs();

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            for(File file : files) {
                //System.out.println(file.getAbsolutePath());

                if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                    String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                    System.out.println(implId + " => " + testSuite + " " + file.length());

                    out.put(implId, file);

                    File cand = new File(outDir, row.getInt(0) + "");

                    // double-blind
                    String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    source = StringUtils.replace(source, "LASSO", "XXXX");

                    //FileUtils.copyFile(file, new File(cand, file.getName()));
                    FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println(out.size());
    }

    @Test
    public void merge_MulGen30() throws IOException {
        File dir = new File("/home/marcus/development/experiments/JSS_SI_AUTO_21/dataset/temp/MulGen30");

        Collection<File> files = FileUtils.listFiles(
                dir,
                new String[] { "java" }, true);

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        Map<String, File> out = new HashMap<>();

        // prepare
        File outDir = new File(dir, "amplified_tests");
        outDir.mkdirs();

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            for(File file : files) {
                //System.out.println(file.getAbsolutePath());

                if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                    String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                    System.out.println(implId + " => " + testSuite + " " + file.length());

                    out.put(implId, file);

                    File cand = new File(outDir, row.getInt(0) + "");

                    // double-blind
                    String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    source = StringUtils.replace(source, "LASSO", "XXXX");

                    //FileUtils.copyFile(file, new File(cand, file.getName()));
                    FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println(out.size());
    }

    @Test
    public void merge_MulGenR() throws IOException {
        File dir = new File("/home/marcus/development/experiments/JSS_SI_AUTO_21/dataset/temp/MulGenR");

        Collection<File> files = FileUtils.listFiles(
                dir,
                new String[] { "java" }, true);

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        Map<String, File> out = new HashMap<>();

        // prepare
        File outDir = new File(dir, "amplified_tests");
        outDir.mkdirs();

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            for(File file : files) {
                //System.out.println(file.getAbsolutePath());

                if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                    String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                    System.out.println(implId + " => " + testSuite + " " + file.length());

                    out.put(implId, file);

                    File cand = new File(outDir, row.getInt(0) + "");

                    // double-blind
                    String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    source = StringUtils.replace(source, "LASSO", "XXXX");

                    //FileUtils.copyFile(file, new File(cand, file.getName()));
                    FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println(out.size());
    }

    @Test
    public void merge_EvoAmpT() throws IOException {
        File dir = new File("/home/marcus/development/experiments/JSS_SI_AUTO_21/dataset/temp/EvoAmpT");

        Collection<File> files = FileUtils.listFiles(
                dir,
                new String[] { "java" }, true);

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        Map<String, File> out = new HashMap<>();

        // prepare
        File outDir = new File(dir, "amplified_tests");
        outDir.mkdirs();

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            for(File file : files) {
                //System.out.println(file.getAbsolutePath());

                if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                    String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                    System.out.println(implId + " => " + testSuite + " " + file.length());

                    out.put(implId, file);

                    File cand = new File(outDir, row.getInt(0) + "");

                    // double-blind
                    String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    source = StringUtils.replace(source, "LASSO", "XXXX");

                    //FileUtils.copyFile(file, new File(cand, file.getName()));
                    FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println(out.size());
    }

    @Test
    public void merge_GenAmpT() throws IOException {
        File dir = new File("/home/marcus/development/experiments/JSS_SI_AUTO_21/dataset/temp/AmpGenT");

        Collection<File> files = FileUtils.listFiles(
                dir,
                new String[] { "java" }, true);

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        Map<String, File> out = new HashMap<>();

        // prepare
        File outDir = new File(dir, "amplified_tests");
        outDir.mkdirs();

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            for(File file : files) {
                //System.out.println(file.getAbsolutePath());

                if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                    String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                    System.out.println(implId + " => " + testSuite + " " + file.length());

                    out.put(implId, file);

                    File cand = new File(outDir, row.getInt(0) + "");

                    // double-blind
                    String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    source = StringUtils.replace(source, "LASSO", "XXXX");

                    //FileUtils.copyFile(file, new File(cand, file.getName()));
                    FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println(out.size());
    }

    // ---------------

    @Test
    public void merge_MulGen() throws IOException {
        File pdir = new File("/home/marcus/development/ICST21_dataset/MulGen");

        int[] range = new int[]{1,5,10,30,50,100};

        for(int r : range) {
            File dir = new File(pdir, "MulGen" + r);

            Collection<File> files = FileUtils.listFiles(
                    dir,
                    new String[] { "java" }, true);

            Table table = Table.read().csv(new File("/home/marcus/Downloads/super_table.csv"));

            Map<String, File> out = new HashMap<>();

            // prepare
            File outDir = new File(dir, "amplified_tests");
            outDir.mkdirs();

            for(Row row : table) {
                String implId = row.getString("IMPLEMENTATION");

                for(File file : files) {
                    //System.out.println(file.getAbsolutePath());

                    if(StringUtils.contains(file.getAbsolutePath(), implId)) {
                        String testSuite = StringUtils.substringAfterLast(file.getAbsolutePath(), "/src/test/java/");

                        System.out.println(implId + " => " + testSuite + " " + file.length());

                        out.put(implId, file);

                        File cand = new File(outDir, row.getInt(0) + "");

                        // double-blind
                        String source = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        source = StringUtils.replace(source, "LASSO", "XXXX");

                        //FileUtils.copyFile(file, new File(cand, file.getName()));
                        FileUtils.write(new File(cand, file.getName()),source, StandardCharsets.UTF_8);
                    }
                }
            }

            System.out.println(out.size());
        }
    }

    @Test
    public void test_bla() throws IOException {
        Map<String, String> testClasses = JSSSIAuto.getTestClasses();

        Table table = Table.read().csv(new File("/home/marcus/Downloads/amplified_table.csv"));

        File outputDir = new File("/tmp/jsssiauto_dataset");
        outputDir.mkdirs();

        File functionalAbs = new File(outputDir, "functional_abstractions");

        for(Row row : table) {
            String implId = row.getString("IMPLEMENTATION");

            CandidateDocument candidateDocument = getImpl(implId);
            CandidateDocument parentDocument = getImpl(candidateDocument.getParentId());

            StringBuilder sb = new StringBuilder();
            sb.append("Information\n===========\n");
            sb.append("Index ID: " + implId + "\n");
            sb.append("Maven Artifact: " + parentDocument.getUri() + "\n");
            //sb.append("Maven Central Coordinates (Artifact Download): " + String.format("https://search.maven.org/search?q=g:%s", URLEncoder.encode(parentDocument.getUri())));
            sb.append("Fully-qualified Class Name: " + candidateDocument.getFQName()  + "\n");
            sb.append("Method Signature: " + candidateDocument.getValues().get("methodOrigSignatureFq_ssigs_sexact") + "\n");

            String testSource = testClasses.get(implId);

            File dir = new File(functionalAbs, row.getInt(0) + "");
            dir.mkdirs();

            FileUtils.write(new File(dir, "info.txt"), sb.toString(), StandardCharsets.UTF_8);
            FileUtils.write(new File(dir, "method_source.txt"), candidateDocument.getContent(), StandardCharsets.UTF_8);
            FileUtils.write(new File(dir, "class_source.txt"), parentDocument.getContent(), StandardCharsets.UTF_8);
            FileUtils.write(new File(dir, "manual_tests_source.txt"), testSource, StandardCharsets.UTF_8);
        }
    }

    private CandidateDocument getImpl(String implId ) throws IOException {
        CandidateQueryResult candidateQueryResult = mavenCentralIndex
                .query("*:*",
                        new SearchOptions(),
                        new String[]{"id:\""+implId+"\""},
                        0,
                        1,
                        null);
        return candidateQueryResult.getCandidates().get(0);
    }

    @Test
    public void test() throws IOException {
        CandidateQueryResult candidateQueryResult = mavenCentralIndex
                .query("*:*",
                        new SearchOptions(),
                        new String[]{"id:\"0be199da-1339-4ffe-a151-0343e287c6eb\""},
                        0,
                        1,
                        null);

        candidateQueryResult.getCandidates().forEach(candidate -> {
            System.out.println(candidate.getId());
        });
    }

    @Test
    public void test_ds() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.queryForMethods("*:*");
        mavenQuery.filter("id:\"0be199da-1339-4ffe-a151-0343e287c6eb\"");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println(ToStringBuilder.reflectionToString(implementation));
        });
    }
}
