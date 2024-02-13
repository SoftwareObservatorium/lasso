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
import de.uni_mannheim.swt.lasso.engine.data.LassoOperations;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 * @author Marcus Kessel
 */
public class LassoRepository implements LassoOperations {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoRepository.class);

    private final ClusterEngine clusterEngine;

    private IgniteCache<ExecKey, System> executionCache;

    public LassoRepository(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;

        initCaches();
    }

    protected void initCaches() {
        CacheConfiguration<ExecKey, System> execCacheConfig =
                new CacheConfiguration<>("executables");
        execCacheConfig.setIndexedTypes(ExecKey.class, System.class);
        //execCacheConfig.setGroupName("lassoModel");

        this.executionCache = this.clusterEngine.getIgnite().getOrCreateCache(execCacheConfig);

    }

    @Override
    public Cache<ExecKey, System> getExecutableCache() {
        return executionCache;
    }

    @Override
    public Cache.Entry<ExecKey, System> getExecutableFromAction(String executionId, String actionName, String id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}' '{}'", executionId, actionName, id);
        }

        SqlQuery<ExecKey, System> sql = new SqlQuery<>(System.class, "executionId = ? AND actionName = ? AND id = ?");
        sql.setArgs(executionId, actionName, id);

        QueryCursor<Cache.Entry<ExecKey, System>> cursor = executionCache.query(sql);

        return cursor.iterator().next();
    }

    @Override
    public Systems getExecutables(String executionId, String abstractionName, String actionName) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("GET '{}' '{}' '{}'", executionId, abstractionName, actionName);
        }

        SqlQuery<ExecKey, System> sql = new SqlQuery<>(System.class, "executionId = ? AND actionName = ? AND abstractionName = ?");
        sql.setArgs(executionId, actionName, abstractionName);

        QueryCursor<Cache.Entry<ExecKey, System>> cursor = executionCache.query(sql);

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

        QueryCursor<Cache.Entry<ExecKey, System>> cursor = executionCache.query(sql);

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

//    @Override
//    public Executables getExecutables(String executionId, String abstractionName, String actionName, String workerNodeId) {
//        if(LOG.isDebugEnabled()) {
//            LOG.debug("GET '{}' '{}' '{}' '{}'", executionId, abstractionName, actionName, workerNodeId);
//        }
//
//        SqlQuery<ExecKey, Executable> sql = new SqlQuery<>(Executable.class, "executionId = ? AND actionName = ? AND abstractionName = ? AND workerNodeId = ?");
//        sql.setArgs(executionId, actionName, abstractionName, workerNodeId);
//
//        QueryCursor<Cache.Entry<ExecKey, Executable>> cursor = executionCache.query(sql);
//
//        List<Executable> executables = StreamSupport
//                .stream(cursor.spliterator(), false)
//                .map(Cache.Entry::getValue)
//                .collect(Collectors.toList());
//
//        Executables container = new Executables();
//        container.setAbstractionName(abstractionName);
//        container.setExecutables(executables);
//
//        return container;
//    }

    @Override
    public void putExecutables(String executionId, String actionName, Systems executables) {
        putExecutables(executionId, actionName, executables, false);
    }

    @Override
    public void putExecutables(String executionId, String actionName, Systems executables, boolean removeExisting) {
        if(LOG.isInfoEnabled()) {
            LOG.info("PUT '{}' '{}' '{}' '{}' removeExisting '{}'", executionId, actionName, executables.getAbstractionName(), executables, removeExisting);
        }

        if(executables == null || CollectionUtils.isEmpty(executables.getExecutables())) {
            return;
        }

        List<System> executableList = executables.getExecutables();

        Set<String> ids = executableList.stream().map(System::getId).collect(Collectors.toSet());

        if(removeExisting) {
            // remove existing ones
            Systems existingExecutables = getExecutables(executionId, executables.getAbstractionName(), actionName);
            if(existingExecutables.hasExecutables()) {
                //
                Set<ExecKey> keysToRemove = existingExecutables.getExecutables().stream()
                        .filter(e -> !ids.contains(e.getId()))
                        .map(e -> {
                            ExecKey key = new ExecKey();
                            key.setExecutionId(executionId);
                            key.setAbstractionName(executables.getAbstractionName());
                            key.setActionName(actionName);
                            key.setId(e.getId());
                            key.setWorkerNodeId("");

                            return key;
                        }).collect(Collectors.toSet());

                if(LOG.isWarnEnabled()) {
                    LOG.warn("Removing '{}' executables", keysToRemove.size());
                }

                if(CollectionUtils.isNotEmpty(keysToRemove)) {
                    executionCache.removeAll(keysToRemove);
                }
            }
        }

        // add / update
        executableList.forEach(i -> {
            ExecKey key = new ExecKey();
            key.setExecutionId(executionId);
            key.setAbstractionName(executables.getAbstractionName());
            key.setActionName(actionName);
            key.setId(i.getId());
            key.setWorkerNodeId("");

            executionCache.put(key, i);
        });
    }

    public IgniteCache<ExecKey, System> getExecutableIgniteCache() {
        return executionCache;
    }

    // TODO better to create caches by LSL executionId and then destroy them!
    // issue is that by now sequential processing of LSL scripts is assumed
    @Deprecated
    public void clear() {
        if(executionCache != null) {
            executionCache.clear();
        }
    }
}
