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
package de.uni_mannheim.swt.lasso.arena.search;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenCodeUnitUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import de.uni_mannheim.swt.lasso.datasource.maven.support.RandomMavenCentralRepository;
import de.uni_mannheim.swt.lasso.datasource.maven.util.HttpUtils;
import de.uni_mannheim.swt.lasso.index.CandidateQueryResult;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.collect.CandidateResultCollector;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.lql.parser.LQL;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Simple code search facade.
 *
 * @author Marcus Kessel
 */
public class CodeSearch {

    private static final Logger LOG = LoggerFactory
            .getLogger(CodeSearch.class);

    private final RandomMavenCentralRepository mavenCentralRepository;
    private final MavenCentralIndex mavenCentralIndex;
    private final SolrInstance solrInstance;

    public CodeSearch() {
        this(SolrInstance.mavenCentral2023());
    }

    public CodeSearch(SolrInstance instance) {
        HttpClient client = HttpUtils.createHttpClient(instance.getUser(), instance.getPass());

        SolrClient solrClient = new HttpSolrClient.Builder(instance.getUrl())
                .withHttpClient(client).build();
        mavenCentralRepository = new RandomMavenCentralRepository(solrClient);

        assertNotNull(mavenCentralRepository);

        mavenCentralIndex = new MavenCentralIndex(mavenCentralRepository,
                new QueryBuilder());

        // SET FETCH SIZE
        CandidateResultCollector.FETCH_SIZE = 100;

        this.solrInstance = instance;
    }

    public List<ClassUnderTest> queryForMethods(String mql, int rows, String... constraints) throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.queryForMethods(mql, "exact2");
        mavenQuery.setRows(rows);
        mavenQuery.filter("!classifier_s:\"tests\"");

        if (ArrayUtils.isNotEmpty(constraints)) {
            Arrays.stream(constraints).forEach(mavenQuery::filter);
        }

        QueryResult queryResult = mavenDataSource.query(mavenQuery);

        return queryResult.getImplementations().stream().map(System::new).map(ClassUnderTest::new
        ).collect(Collectors.toList());
    }

    public List<ClassUnderTest> toClasses(ArenaJob arenaJob) throws IOException {
        return arenaJob.getImplementations().stream().map(ClassUnderTest::new
        ).collect(Collectors.toList());
    }

    public List<ClassUnderTest> queryForClasses(String mql, int rows, String... constraints) throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        MavenQuery mavenQuery = (MavenQuery) mavenDataSource.createQueryModelForLSL();
        LassoContext ctx = new LassoContext();
        ctx.setLogger(new SimpleLogger());
        ctx.register(mavenQuery);
        mavenQuery.queryForClasses(mql, "class");
        mavenQuery.setRows(rows);
        mavenQuery.filter("!classifier_s:\"tests\"");

        if (ArrayUtils.isNotEmpty(constraints)) {
            Arrays.stream(constraints).forEach(mavenQuery::filter);
        }

        QueryResult queryResult = mavenDataSource.query(mavenQuery);

        return queryResult.getImplementations().stream().map(System::new).map(ClassUnderTest::new
        ).collect(Collectors.toList());
    }

    public List<ClassUnderTest> queryForClassesDirectly(String mql, int rows, String strategy, String... constraints) throws IOException {
        MavenDataSource mavenDataSource = new MavenDataSource(mavenCentralIndex);

        //
        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(MavenQuery.queryStrategies.get(strategy));

        MavenCentralIndex mavenCentralIndex = mavenDataSource.getMavenCentralIndex();

        CandidateQueryResult result = mavenCentralIndex.queryDirectly(mql,
                searchOptions,
                constraints,
                0,
                rows,
                Collections.emptyList());

        return result.getCandidates().stream().map(c -> {
            CodeUnit implementation = MavenCodeUnitUtils.toImplementation(((SolrCandidateDocument) c).getSolrDocument());
            implementation.setDataSource(mavenDataSource.getId());

            // copy over methods
            List<String> methods = new LinkedList<>();
            if (implementation.getUnitType() == CodeUnit.CodeUnitType.CLASS) {
                methods.addAll(((SolrCandidateDocument) c).getSolrDocument().getFieldValues("methodOrigSignatureFq_sigs_exact").stream().map(s -> (String) s).collect(Collectors.toList()));
            } else {
                methods.addAll(((SolrCandidateDocument) c).getSolrDocument().getFieldValues("methodOrigSignatureFq_ssigs_sexact").stream().map(s -> (String) s).collect(Collectors.toList()));
            }

            implementation.setMethods(methods);

            return implementation;
        }).map(System::new).map(ClassUnderTest::new).collect(Collectors.toList());
    }

    /**
     * Direct query without cursor etc.
     * <p>
     * Doesn't matter if class or method document is retrieved
     *
     * @param id
     * @return
     * @throws IOException
     */
    public ClassUnderTest queryForClass(String id) throws IOException {
        CandidateQueryResult result = mavenCentralIndex.query("*:*", new SearchOptions(), new String[]{String.format("id:\"%s\"", id)}, 0, 1, Collections.emptyList());
        List<ClassUnderTest> impls = result.getCandidates().stream().map(c -> {
            CodeUnit implementation = MavenCodeUnitUtils.toImplementation(((SolrCandidateDocument) c).getSolrDocument());
            implementation.setDataSource(solrInstance.getName());

            return implementation;
        }).map(System::new).map(ClassUnderTest::new).collect(Collectors.toList());

        return impls.get(0);
    }

    public List<InterfaceSpecification> fromLQL(String lql) throws IOException {
        // LQL
        LQLParseResult lqlParseResult = parseLQL(lql);

        List<InterfaceSpecification> parseResults = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> mSignatures = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> cSignatures = new LinkedList<>();

        Interface system = lqlParseResult.getInterfaceSpecification();
        String className = system.getName();

        // construct ordered signatures for searching
        List<MethodSignature> methods = system.getMethods();
        if (methods != null && methods.size() > 0) {
            for (int i = 0; i < methods.size(); i++) {
                MethodSignature method = methods.get(i);
                if (!method.isConstructor()) {
                    mSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", method.getName(),
                            method.getInputs(), method.getOutputs().get(0)));
                } else {
                    cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                            method.getInputs(), "void"));
                }
            }
        }

        if (CollectionUtils.isEmpty(cSignatures)) {
            // add default one
            cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                    Collections.emptyList(), "void"));
        }

        InterfaceSpecification parseResult = new InterfaceSpecification();
        parseResult.setClassName(className);
        parseResult.setConstructors(cSignatures.stream()
                .map(c -> {
                    LQLMethodSignature lm = new LQLMethodSignature(parseResult, c);
                    //lm.setParent(parseResult);
                    return lm;
                })
                .collect(Collectors.toList()));
        parseResult.setMethods(mSignatures.stream()
                .map(c -> {
                    LQLMethodSignature lm = new LQLMethodSignature(parseResult, c);
                    //lm.setParent(parseResult);
                    return lm;
                })
                .collect(Collectors.toList()));

        parseResults.add(parseResult);
        //}

        return parseResults;
    }

    public LQLParseResult parseLQL(String lql) throws IOException {
        //

        LQLParseResult parseResult = LQL.parse(lql);
        if (parseResult != null) {
            return parseResult;
        }

        throw new IOException("unsupported LQL query " + lql);
    }
}
