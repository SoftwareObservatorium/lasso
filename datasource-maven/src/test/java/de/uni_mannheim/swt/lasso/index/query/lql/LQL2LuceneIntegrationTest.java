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
package de.uni_mannheim.swt.lasso.index.query.lql;

import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.systemtests.HttpUtils;
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
public class LQL2LuceneIntegrationTest {

    private static RandomMavenCentralRepository mavenCentralRepository;
    private static MavenCentralIndex mavenCentralIndex;

    @BeforeAll
    public static void before() {
        HttpClient client = HttpUtils.createHttpClient("solr", "");

        SolrClient solrClient = new HttpSolrClient.Builder("http://lassohp10.informatik.uni-mannheim.de:8983/solr/mavencentral2023/")
                .withHttpClient(client).build();
        mavenCentralRepository = new RandomMavenCentralRepository(solrClient);

        assertNotNull(mavenCentralRepository);

        mavenCentralIndex = new MavenCentralIndex(mavenCentralRepository,
                new QueryBuilder());
    }

    @Test
    public void test_Base64_ext() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Base64{encode(byte[])->java.lang.String}", "class-ext");
        mavenQuery.setRows(25);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("--------");
            System.out.println(implementation.toFQName() + " ("+ implementation.getScore()+") " + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_Stack_ext() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("Stack{\n" +
                "    Stack()\n" +
                "    push(java.lang.Object)->java.lang.Object\n" +
                "    pop()->java.lang.Object\n" +
                "    peek()->java.lang.Object\n" +
                "    size()->int\n" +
                "}", "class-ext");
        mavenQuery.setRows(25);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("--------");
            System.out.println(implementation.toFQName() + " ("+ implementation.getScore()+") " + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_MultiMap_ext() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("MultiMap {\n" +
                "    put(java.lang.Object,java.lang.Object)->java.lang.Object\n" +
                "    getValues(java.lang.Object)->java.util.List\n" +
                "    size()->int\n" +
                "}", "class-ext");
        mavenQuery.setRows(25);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("--------");
            System.out.println(implementation.toFQName() + " ("+ implementation.getScore()+") " + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }

    @Test
    public void test_StringComparator_ext() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("StringComparator {\n" +
                "            compare(java.lang.String,java.lang.String)->int}", "class-ext");
        mavenQuery.setRows(25);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("--------");
            System.out.println(implementation.toFQName() + " ("+ implementation.getScore()+") " + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");
        });
    }
}
