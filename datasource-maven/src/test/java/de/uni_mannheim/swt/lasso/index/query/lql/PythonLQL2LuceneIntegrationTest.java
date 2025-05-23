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

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.datasource.maven.support.RandomMavenCentralRepository;
import de.uni_mannheim.swt.lasso.datasource.maven.systemtests.HttpUtils;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class PythonLQL2LuceneIntegrationTest {

    private static RandomMavenCentralRepository mavenCentralRepository;
    private static MavenCentralIndex mavenCentralIndex;

    @BeforeAll
    public static void before() {
        HttpClient client = HttpUtils.createHttpClient("", "");

        SolrClient solrClient = new HttpSolrClient.Builder("http://localhost:8983/solr/lasso_quickstart/")
                .withHttpClient(client).build();
        mavenCentralRepository = new RandomMavenCentralRepository(solrClient);

        assertNotNull(mavenCentralRepository);

        mavenCentralIndex = new MavenCentralIndex(mavenCentralRepository,
                new QueryBuilder());
    }

    @Test
    public void test_greet_MODULE() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);

        mavenQuery.lang("python"); // set Python language
        mavenQuery.unitType("module"); // Python Module

        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("typeannotations {greet(str)->str}", "class-simple");
        mavenQuery.setRows(25);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("--------");
            System.out.println(implementation.toFQName() + " ("+ implementation.getScore()+") " + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");

            assertEquals(CodeUnit.PYTHON, implementation.getLang());
            assertTrue(implementation.isPython());
            assertFalse(implementation.isJava());

            assertEquals(CodeUnit.CodeUnitType.MODULE, implementation.getUnitType());
        });
    }

    @Test
    public void test_greet_MODULE_OR_CLASS() throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);

        mavenQuery.lang("python"); // set Python language
        // FIXME feature: set as default when lang python is set?
        mavenQuery.unitType("module_or_class"); // Python Module or Python Class, BUT NOT FUNCTION

        mavenQuery.setDirectly(true);
        mavenQuery.queryForClasses("$ {get_coordinates()->object}", "class-simple");
        mavenQuery.setRows(25);

        QueryResult queryResult = mavenDataSource.query(mavenQuery);
        queryResult.getImplementations().forEach(implementation -> {
            System.out.println("--------");
            System.out.println(implementation.toFQName() + " ("+ implementation.getScore()+") " + "=>" + SignatureUtils.create(implementation).toLQL(true));
            System.out.println("--------");

            assertEquals(CodeUnit.PYTHON, implementation.getLang());
            assertTrue(implementation.isPython());
            assertFalse(implementation.isJava());

            assertTrue(implementation.getUnitType() == CodeUnit.CodeUnitType.MODULE || implementation.getUnitType() == CodeUnit.CodeUnitType.CLASS);
        });
    }
}
