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
import de.uni_mannheim.swt.lasso.cluster.data.repository.ExecKey;

import de.uni_mannheim.swt.lasso.engine.data.LassoOperations;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.client.ClientCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Client-side implementation of {@link de.uni_mannheim.swt.lasso.engine.data.LassoOperations}.
 *
 * @author Marcus Kessel
 */
public class ClientLassoRepository implements LassoOperations {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClientLassoRepository.class);

    private final LassoClusterClient clusterClient;

    private final ClientCache<ExecKey, System> executablesCache;

    public ClientLassoRepository(LassoClusterClient clusterClient) {
        this.clusterClient = clusterClient;

        this.executablesCache = this.clusterClient.getClient().cache("executables");
    }

    @Override
    public Cache<ExecKey, System> getExecutableCache() {
        return null;
    }

    @Override
    public Cache.Entry<ExecKey, System> getExecutableFromAction(String executionId, String actionName, String id) {
        return null;
    }

    @Override
    public Systems getExecutables(String executionId, String abstractionName, String actionName) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}' '{}'", executionId, abstractionName, actionName);
        }

        SqlQuery<ExecKey, System> sql = new SqlQuery<>(System.class, "executionId = ? AND actionName = ? AND abstractionName = ?");
        sql.setArgs(executionId, actionName, abstractionName);

        QueryCursor<Cache.Entry<ExecKey, System>> cursor = executablesCache.query(sql);

        List<System> executables = StreamSupport
                .stream(cursor.spliterator(), false)
                .map(Cache.Entry::getValue)
                .collect(Collectors.toList());

        Systems container = new Systems();
        container.setAbstractionName(abstractionName);
        container.setExecutables(executables);

        return container;
    }

    @Override
    public Map<String, Systems> getAbstractions(String executionId, String actionName) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}'", executionId, actionName);
        }

        SqlQuery<ExecKey, System> sql = new SqlQuery<>(System.class, "executionId = ? AND actionName = ?");
        sql.setArgs(executionId, actionName);

        QueryCursor<Cache.Entry<ExecKey, System>> cursor = executablesCache.query(sql);

        Iterator<Cache.Entry<ExecKey, System>> it = cursor.iterator();

        Map<String, Systems> abstractions = new LinkedHashMap<>();
        while(it.hasNext()) {
            Cache.Entry<ExecKey, System> entry = it.next();
            String abstractionName = entry.getKey().getAbstractionName();

            if(!abstractions.containsKey(abstractionName)) {
                Systems execs = new Systems();
                execs.setAbstractionName(abstractionName);
                execs.setExecutables(new LinkedList<>());
                execs.setActionInstanceId(actionName);

                abstractions.put(abstractionName, execs);
            }

            abstractions.get(abstractionName).getExecutables().add(entry.getValue());
        }

        return abstractions;
    }

    @Override
    public void putExecutables(String executionId, String actionName, Systems executables) {

    }

    @Override
    public void putExecutables(String executionId, String actionName, Systems executables, boolean removeExisting) {

    }

    public LassoClusterClient getClusterClient() {
        return clusterClient;
    }
}
