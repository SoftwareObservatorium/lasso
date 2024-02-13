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
package de.uni_mannheim.swt.lasso.cluster.data.repository;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.compute.test.TestApplication;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.core.model.Adapter;
import org.apache.commons.io.FileUtils;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestApplication.class})
public class ReportRepositoryIntegrationTest {

    @Autowired
    ClusterEngine clusterEngine;

    String executionId = UUID.randomUUID().toString();
    ReportRepository reportRepository;

    @BeforeEach
    public void beforeEach() {
        if(!clusterEngine.getIgnite().cluster().active()) {
            clusterEngine.getIgnite().cluster().active(true);
        }

        reportRepository = new ReportRepository(clusterEngine);
    }

    @AfterEach
    public void afterEach() {
        // destroy
        clusterEngine.removeReportCaches(executionId);
    }

    @Test
    public void test_binary() throws IOException {
        Map<String, Double> measures = new HashMap<>();
        measures.put("val1", 50d);
        measures.put("val2", 100d);

        CacheConfiguration<String, BinaryObject> cfg = new CacheConfiguration<>("GlobalMeasurements");
        cfg.setQueryEntities(new ArrayList<QueryEntity>() {{
            QueryEntity e = new QueryEntity();
            e.setKeyType("java.lang.String");
            e.setValueType("GlobalMeasurements");

            Map<String, String> fields = measures.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> "java.lang.Double"));
            e.setFields(new LinkedHashMap<>(fields));
            add(e);
        }});

        IgniteCache<String, BinaryObject> measurementCache = clusterEngine.getIgnite().getOrCreateCache(cfg).withKeepBinary();

        BinaryObjectBuilder builder = clusterEngine.getIgnite().binary().builder("GlobalMeasurements");
        builder.setField("val1", 50d);

        measurementCache.put("1", builder.build());

        BinaryObject object = measurementCache.get("1");

        java.lang.System.out.println(object);

        java.lang.System.out.println(measurementCache.getName());

        SqlFieldsQuery query = new SqlFieldsQuery("select val1 from GlobalMeasurements");
        FieldsQueryCursor<List<?>> cursor = measurementCache.query(query);
        Iterator<List<?>> iterator = cursor.iterator();
        while(iterator.hasNext()) {
            List row = iterator.next();

            java.lang.System.out.println(Arrays.toString(row.toArray()));
        }
    }

    @Test
    public void test_merge() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction", abstraction.getName(), implementation, executionSignature);

        AReport aReport = new AReport();
        aReport.setValueA(10d);
        reportRepository.put(executionId, dataKey, aReport);
        BReport bReport = new BReport();
        bReport.setValueB(20d);
        reportRepository.put(executionId, dataKey, bReport);

        //
        Table aReportTable = reportRepository.reportToTable(executionId, AReport.class);
        java.lang.System.out.println(aReportTable.print());
        Table bReportTable = reportRepository.reportToTable(executionId, BReport.class);
        java.lang.System.out.println(bReportTable.print());

        Table bigTable = aReportTable.joinOn("ABSTRACTION", "IMPLEMENTATION", "PERMID", "ACTIONNAME", "DATASOURCE").fullOuter(bReportTable);

        java.lang.System.out.println(bigTable);

        Set<String> caches = clusterEngine.getReportCaches(executionId);

        java.lang.System.out.println(caches);

        Class<? extends LassoReport> reportClass = reportRepository.toLassoReportClass(executionId, "AReport");
        assertThat(reportClass, equalTo(AReport.class));
    }

    @Test
    public void test_inheritance() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction", abstraction.getName(), implementation, executionSignature);

        CReport aReport = new CReport();
        aReport.setValueB(10d);
        aReport.setValueC(20d);
        reportRepository.put(executionId, dataKey, aReport);

        //
        Table aReportTable = reportRepository.reportToTable(executionId, CReport.class);
        java.lang.System.out.println(aReportTable.print());
    }

    @Test
    public void test_getPermutationIds() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction", abstraction.getName(), implementation, executionSignature);

        SimpleReport report = new SimpleReport();
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report);

        // put another
        reportRepository.put(executionId, ReportKey.of("testAction", abstraction.getName(), implementation), report);

        //

        List<Integer> permIds = reportRepository.getPermutationIds(executionId, ReportKey.of("testAction", abstraction.getName(), implementation), report.getClass());

        assertThat(permIds, containsInAnyOrder(-1, 0));
    }

    @Test
    public void test_simplereport() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction", abstraction.getName(), implementation, executionSignature);

        SimpleReport report = new SimpleReport();
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report);

        // put another
        reportRepository.put(executionId, ReportKey.of("testAction", abstraction.getName(), implementation), report);

        //
        Table simpleReportTable = null;
        try {
            simpleReportTable = reportRepository.reportToTable(executionId, SimpleReport.class);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        java.lang.System.out.println(simpleReportTable.print());

        // export
        File csvFile = File.createTempFile("lasso_", ".csv");

        reportRepository.export(executionId, "testAction", SimpleReport.class, csvFile);

        java.lang.System.out.println(FileUtils.readFileToString(csvFile, Charset.defaultCharset()));

        //
        // export
        File allCsvFile = File.createTempFile("lasso_", ".csv");

        reportRepository.export(executionId, "testAction", allCsvFile);

        // fields query
        String query = "select PERMID from " + SimpleReport.class.getSimpleName() + " WHERE IMPLEMENTATION = '" + implementation.getId() + "' AND ABSTRACTION = '" + abstraction.getName() + "'";

        Iterator<List<?>> iterator = reportRepository.query(executionId, query);

        while(iterator.hasNext()) {
            List row = iterator.next();

            java.lang.System.out.println(Arrays.toString(row.toArray()));
        }
    }

    @Test
    public void test_last_modified_order_desc() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction1", abstraction.getName(), implementation, executionSignature);

        SimpleReport report = new SimpleReport();
        report.setLastModified(new Date(0L));
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report);

        ReportKey dataKey2 = ReportKey.of("testAction2", abstraction.getName(), implementation, executionSignature);

        SimpleReport report2 = new SimpleReport();
        report2.setSomeMetric(10d);

        reportRepository.put(executionId, dataKey2, report2);

        ReportKey dataKey3 = ReportKey.of("testAction3", abstraction.getName(), implementation, executionSignature);

        SimpleReport report3 = new SimpleReport();
        report3.setLastModified(new Date(5000));
        report3.setSomeMetric(2d);

        reportRepository.put(executionId, dataKey3, report3);

        SimpleReport latest = reportRepository.getLast(executionId, dataKey, SimpleReport.class);

        assertEquals(report2.getSomeMetric(), latest.getSomeMetric());
    }

    @Test
    public void test_last_modified_order_asc() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction1", abstraction.getName(), implementation, executionSignature);

        SimpleReport report = new SimpleReport();
        report.setLastModified(new Date(0L));
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report);

        ReportKey dataKey2 = ReportKey.of("testAction2", abstraction.getName(), implementation, executionSignature);

        SimpleReport report2 = new SimpleReport();
        report2.setSomeMetric(10d);

        reportRepository.put(executionId, dataKey2, report2);

        ReportKey dataKey3 = ReportKey.of("testAction3", abstraction.getName(), implementation, executionSignature);

        SimpleReport report3 = new SimpleReport();
        report3.setLastModified(new Date(5000));
        report3.setSomeMetric(2d);

        reportRepository.put(executionId, dataKey3, report3);

        SimpleReport latest = reportRepository.getFirst(executionId, dataKey, SimpleReport.class);

        assertEquals(report.getSomeMetric(), latest.getSomeMetric());
    }

    @Test
    public void test_simplereport_JdbcTemplate() throws IOException, SQLException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction", abstraction.getName(), implementation, executionSignature);

        SimpleReport report = new SimpleReport();
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report);

        // put another
        reportRepository.put(executionId, ReportKey.of("testAction", abstraction.getName(), implementation), report);

        //
        Table simpleReportTable = null;
        try {
            simpleReportTable = reportRepository.reportToTable(executionId, SimpleReport.class);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        java.lang.System.out.println(simpleReportTable.print());

        // export
        File csvFile = File.createTempFile("lasso_", ".csv");

        reportRepository.export(executionId,"testAction", SimpleReport.class, csvFile);

        java.lang.System.out.println(FileUtils.readFileToString(csvFile, Charset.defaultCharset()));

        //
        // export
        File allCsvFile = File.createTempFile("lasso_", ".csv");

        reportRepository.export(executionId, "testAction", allCsvFile);

        // SQL query
        String query = "select PERMID from " + SimpleReport.class.getSimpleName() + " WHERE IMPLEMENTATION = '" + implementation.getId() + "' AND ABSTRACTION = '" + abstraction.getName() + "'";

        JdbcTemplate jdbcTemplate = reportRepository.getJdbcTemplate(executionId, SimpleReport.class.getSimpleName());

        jdbcTemplate.query(query, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                java.lang.System.out.println(resultSet);
            }
        });
    }

    @Test
    public void test_simplereport_duplicate_key_index_issue() throws IOException {
        Abstraction abstraction = abstraction("ab1");
        System implementation = implementation("1");
        Adapter executionSignature = executionSignature(0);

        ReportKey dataKey = ReportKey.of("testAction", abstraction.getName(), implementation, executionSignature);

        SimpleReport report = new SimpleReport();
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report);

        SimpleReport2 report2 = new SimpleReport2();
        report.setSomeMetric(5d);

        reportRepository.put(executionId, dataKey, report2);
    }

    private Abstraction abstraction(String name) {
        Abstraction abstraction = new Abstraction();
        abstraction.setName(name);

        return abstraction;
    }

    private System implementation(String id) {
        CodeUnit implementation = new CodeUnit();
        implementation.setId(id);

        return new System(implementation);
    }

    private Adapter executionSignature(int id) {
        Adapter executionSignature = new Adapter(id);

        return executionSignature;
    }
}
