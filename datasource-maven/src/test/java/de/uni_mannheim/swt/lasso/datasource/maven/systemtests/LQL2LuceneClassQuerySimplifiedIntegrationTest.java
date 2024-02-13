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
package de.uni_mannheim.swt.lasso.datasource.maven.systemtests;

import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.datasource.maven.support.RandomMavenCentralRepository;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Marcus Kessel
 */
public class LQL2LuceneClassQuerySimplifiedIntegrationTest {

    private static RandomMavenCentralRepository mavenCentralRepository;
    private static MavenCentralIndex mavenCentralIndex;

    static {
        System.setProperty("models.embedding.code2vec", "/home/marcus/Downloads/target_vecs.txt");
    }

    @BeforeAll
    public static void before() {
        HttpClient client = HttpUtils.createHttpClient("", "");

        SolrClient solrClient = new HttpSolrClient.Builder("http://lassohp10.informatik.uni-mannheim.de:8983/solr/mavencentral2023/")
                .withHttpClient(client).build();
        mavenCentralRepository = new RandomMavenCentralRepository(solrClient);

        assertNotNull(mavenCentralRepository);

        mavenCentralIndex = new MavenCentralIndex(mavenCentralRepository,
                new QueryBuilder());
    }

    @Test
    public void test_Stack_LQL2LuceneClassQuerySimplified() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Stack {\n" +
                "            push(java.lang.Object)->java.lang.Object\n" +
                "            pop()->java.lang.Object\n" +
                "            peek()->java.lang.Object\n" +
                "            size()->int}", "class-simple"); // STRATEGY
        mavenQuery.setRows(200);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");
        //mavenQuery.filter("!name_fq:Queue");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    /**
     * With "negative" list.
     *
     * @throws IOException
     */
    @Test
    public void test_Stack_LQL2LuceneClassQuerySimplified_filters() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Stack {\n" +
                "            push(java.lang.Object)->java.lang.Object\n" +
                "            pop()->java.lang.Object\n" +
                "            peek()->java.lang.Object\n" +
                "            size()->int}\n" +
                "!name_fq:Queue !name_fq:Deque", "class-simple"); // STRATEGY
        mavenQuery.setRows(200);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Stack_LQL2LuceneClassQuerySimplified_relaxed() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Stack {\n" +
                "            push(java.lang.String)->java.lang.String\n" +
                "            pop()->java.lang.String\n" +
                "            peek()->java.lang.String\n" +
                "            size()->int}", "class-simple"); // STRATEGY
        mavenQuery.setRows(200);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Strlen_LQL2LuceneClassQuerySimplified() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Problem { strlen(java.lang.String)->long }", "class-simple"); // STRATEGY
        mavenQuery.setRows(100);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    /**
     * $
     *
     * @throws IOException
     */
    @Test
    public void test_Strlen_LQL2LuceneClassQuerySimplified_placeholder() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("$ { strlen(java.lang.String)->long }", "class-simple"); // STRATEGY
        mavenQuery.setRows(100);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Base64_LQL2LuceneClassQuerySimplified() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Base64{encode(byte[])->java.lang.String}", "class-simple"); // STRATEGY
        mavenQuery.setRows(500);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore() + " / " + implementation.getId());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Json_LQL2LuceneClassQuerySimplified() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Json{toJson(java.util.Map)->java.lang.String}", "class-simple"); // STRATEGY
        mavenQuery.setRows(200);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore() + " / " + implementation.getId());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Matrix_LQL2LuceneClassQuerySimplified() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Matrix {\n" +
                "    Matrix(double[][],int,int)\n" +
                "    set(int,int,double)->void\n" +
                "    get(int, int)->double\n" +
                "    sum(Matrix)->Matrix\n" +
                "}", "class-simple"); // STRATEGY
        mavenQuery.setRows(200);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore() + " / " + implementation.getId());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Ngram_LQL2LuceneClassQuerySimplified() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Ngram {\n" +
                "    Ngram(int)\n" +
                "    similarity(java.lang.String,java.lang.String)->float\n" +
                "}", "class-simple"); // STRATEGY
        mavenQuery.setRows(100);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore() + " / " + implementation.getId());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Ngram_LQL2LuceneClassQuerySimplified_MethodNameExpansion() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Ngram {\n" +
                "    Ngram(int)\n" +
                "    similarity(java.lang.String,java.lang.String)->float\n" +
                "}", "class-simple"); // STRATEGY
        mavenQuery.setRows(100);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        // expand method names
        mavenQuery.expandMethodNames(5);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore() + " / " + implementation.getId());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Stack_LQL2LuceneClassQuerySimplified_MethodNameExpansion() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Stack {\n" +
                "            push(java.lang.Object)->java.lang.Object\n" +
                "            pop()->java.lang.Object\n" +
                "            peek()->java.lang.Object\n" +
                "            size()->int}", "class-simple"); // STRATEGY
        mavenQuery.setRows(200);
        mavenQuery.setJavaTypeFilter(false);
        mavenQuery.filter("{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}");

        // expand method names
        mavenQuery.expandMethodNames(5);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        System.out.println("Total: " + queryResult.getNumFound());
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("-------- " + implementation.getScore());
            System.out.println(implementation.toFQName() + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }
}
