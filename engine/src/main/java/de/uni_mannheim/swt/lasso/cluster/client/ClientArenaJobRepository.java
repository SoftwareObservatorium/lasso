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
import org.apache.commons.lang3.EnumUtils;
import org.apache.ignite.client.ClientCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client access to LASSO Platform (based on Ignite).
 *
 * @author Marcus Kessel
 */
public class ClientArenaJobRepository implements ArenaJobRepository {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClientArenaJobRepository.class);

    private final LassoClusterClient clusterClient;

    private ClientCache<String, ArenaJob> jobsCache;
    private ClientCache<String, String> jobsCacheStatus;

    public ClientArenaJobRepository(LassoClusterClient clusterClient) {
        this.clusterClient = clusterClient;

        jobsCache = clusterClient.getClient().cache(ARENAJOBS);
        jobsCacheStatus = clusterClient.getClient().cache(ARENAJOBS_STATUS);
    }

    @Override
    public void put(String id, ArenaJob job) {
        this.jobsCacheStatus.put(id, job.getStatus().name());

        jobsCache.put(id, job);
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

    public ClientCache<String, ArenaJob> getJobsCache() {
        return jobsCache;
    }

    public LassoClusterClient getClusterClient() {
        return clusterClient;
    }
}
