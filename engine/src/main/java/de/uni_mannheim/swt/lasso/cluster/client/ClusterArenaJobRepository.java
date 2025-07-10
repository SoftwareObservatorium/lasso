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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;

import org.apache.commons.lang3.EnumUtils;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal access to Arena jobs (Ignite).
 *
 * @author Marcus Kessel
 */
public class ClusterArenaJobRepository implements ArenaJobRepository {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClusterArenaJobRepository.class);

    private final ClusterEngine clusterEngine;

    private IgniteCache<String, ArenaJob> jobsCache;
    private IgniteCache<String, String> jobsCacheStatus;
    private IgniteCache<String, String> jobsCacheJson;

    private ObjectMapper objectMapper = new ObjectMapper();

    public ClusterArenaJobRepository(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;

        initCaches();
    }

    protected void initCaches() {
        CacheConfiguration<String, ArenaJob> implCacheConfig =
                new CacheConfiguration<>(ARENAJOBS);
        implCacheConfig.setIndexedTypes(String.class, String.class);
        CacheConfiguration<String, String> implCacheConfigJson =
                new CacheConfiguration<>(ARENAJOBS_JSON);
        implCacheConfig.setIndexedTypes(String.class, String.class);
        CacheConfiguration<String, String> implCacheConfigStatus =
                new CacheConfiguration<>(ARENAJOBS_STATUS);
        implCacheConfig.setIndexedTypes(String.class, String.class);
        //implCacheConfig.setGroupName("lassoModel");

        this.jobsCache = this.clusterEngine.getIgnite().getOrCreateCache(implCacheConfig);
        this.jobsCacheJson = this.clusterEngine.getIgnite().getOrCreateCache(implCacheConfigJson);
        this.jobsCacheStatus = this.clusterEngine.getIgnite().getOrCreateCache(implCacheConfigStatus);
    }

    @Override
    public void put(String id, ArenaJob job) {
        LOG.debug("Putting Job {}", id);

        this.jobsCache.put(id, job);
        this.jobsCacheStatus.put(id, job.getStatus().name());

        // alternative representation (i.e., Python support)
        try {
            String json = objectMapper.writeValueAsString(job);
            jobsCacheJson.put(id, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArenaJob get(String id) {
        String status = this.jobsCacheStatus.get(id);
        JobStatus jobStatus = EnumUtils.getEnum(JobStatus.class, status);

        ArenaJob job = jobsCache.get(id);
        job.setStatus(jobStatus);

        return job;
    }

    @Override
    public void remove(String id) {
        jobsCache.remove(id);
    }

    // TODO better to create caches by LSL executionId and then destroy them!
    // issue is that by now sequential processing of LSL scripts is assumed
    @Override
    public void clear() {
        if(jobsCache != null) {
            jobsCache.clear();
        }
    }

    public IgniteCache<String, ArenaJob> getJobsCache() {
        return jobsCache;
    }

    public ClusterEngine getClusterEngine() {
        return clusterEngine;
    }
}
