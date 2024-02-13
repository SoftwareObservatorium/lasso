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
package de.uni_mannheim.swt.lasso.cluster.client;

import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import org.apache.commons.lang3.Validate;

import org.apache.ignite.cache.CacheKeyConfiguration;
import org.apache.ignite.client.ClientCache;

import org.apache.ignite.client.ClientCacheConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Client-side implementation of {@link ReportOperations}.
 *
 * @author Marcus Kessel
 */
public class ClientReportRepository implements ReportOperations {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClientReportRepository.class);

    private final LassoClusterClient clusterClient;

    private ClientCache<String, ArenaJob> jobsCache;

    public ClientReportRepository(LassoClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    public LassoClusterClient getClusterClient() {
        return clusterClient;
    }

    public String toCacheName(String executionId, String reportName) {
        return String.format("%s_%s", executionId, reportName);
    }

    public <T extends LassoReport> ClientCache<ReportKey, T> getCache(String executionId, Class<T> reportType) {
        return clusterClient.getClient().cache(toCacheName(executionId, reportType.getName()));
    }

    @Override
    public Class<? extends LassoReport> toLassoReportClass(String executionId, String reportId) {
        return null;
    }

    @Override
    public void put(String executionId, ReportKey key, LassoReport report) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("PUT '{}' '{}' '{}'", executionId, key, report);
        }

        Validate.notNull(report);

        ClientCache cache = clusterClient.getClient().cache(toCacheName(executionId, report.getClass().getName()));
        cache.put(key, report);
    }

//    public ClientCache getOrCreateReportCache(String executionId, Class<? extends LassoReport> type) {
//        ClientCacheConfiguration cacheConfig = new ClientCacheConfiguration();
//        cacheConfig.setName(toCacheName(executionId, type.getName()));
//
//        //cacheConfig.setIndexedTypes(ReportKey.class, type);
//        cacheConfig.setKeyConfiguration(new CacheKeyConfiguration(ReportKey.class), new CacheKeyConfiguration(type));
//        // set schema -- may be ambiguous (since we use only class name not FQ)
//        cacheConfig.setSqlSchema(String.format("%s_%s", executionId, type.getSimpleName()));
//
//        return clusterClient.getClient().getOrCreateCache(cacheConfig);
//    }

    @Override
    public <T extends LassoReport> void remove(String executionId, ReportKey key, Class<T> reportType) {

    }

    @Override
    public <T extends LassoReport> T get(String executionId, ReportKey key, Class<T> reportType) {
        ClientCache<ReportKey, T> cache = getCache(executionId, reportType);

        T report = cache.get(key);

        return report;
    }

    @Override
    public <T extends LassoReport> T getLast(String executionId, ReportKey key, Class<T> reportType) {
        return null;
    }

    @Override
    public <T extends LassoReport> T getFirst(String executionId, ReportKey key, Class<T> reportType) {
        return null;
    }

    @Override
    public Table reportToTable(String executionId, Class<? extends LassoReport> reportType) throws IOException {
        return null;
    }

    @Override
    public Table reportToTable(String executionId, String tableName) throws IOException {
        return null;
    }

    @Override
    public Table reportToTable(String executionId) throws IOException {
        return null;
    }

    @Override
    public List<Integer> getPermutationIds(String executionId, ReportKey key, Class<? extends LassoReport> reportType) throws IOException {
        return null;
    }

    @Override
    public void export(Table table, File file) throws IOException {

    }

    @Override
    public void export(String executionId, String actionName, Class<? extends LassoReport> reportType, File file) throws IOException {

    }

    @Override
    public void export(String executionId, String actionName, File file) throws IOException {

    }

    @Override
    public Table getValues(String executionId, Abstraction abstraction, String qualifiedField) throws IOException {
        return null;
    }

    @Override
    public Table select(String executionId, String sql) throws IOException {
        return null;
    }

    @Override
    public void newValuesReport(String executionId, String reportName, Map<String, String> valueTypes) {

    }

    @Override
    public void putValues(String executionId, ReportKey key, String reportName, Map<String, ?> values) {

    }

    @Override
    public void putValues(String executionId, String action, Abstraction abstraction, System implementation, String reportName, Map<String, ?> values) {

    }
}
