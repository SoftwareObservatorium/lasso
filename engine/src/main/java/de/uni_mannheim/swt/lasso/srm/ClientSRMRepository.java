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
package de.uni_mannheim.swt.lasso.srm;

import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.ignite.client.ClientCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class ClientSRMRepository implements SRMRepository {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClientSRMRepository.class);

    /**
     * MegaByte = 1024 x 1024
     */
    public static long MB = 1024*1024;

    private final LassoClusterClient clusterClient;

    private ClientCache<CellId, CellValue> cache;

    public ClientSRMRepository(LassoClusterClient clusterClient) {
        this.clusterClient = clusterClient;

        cache = clusterClient.getClient().cache(CACHE_NAME);
    }

    @Override
    public void putAll(Map<CellId, CellValue> cells) {
        // filter out huge values, see issue #393 (record too long, Btree+ corrupted
        if(MapUtils.isNotEmpty(cells)) {
            cells.forEach((key, value) -> {
                // both together
                if (value.getRawValue() != null) {
                    // rough estimate of bytes (in UTF-16, each char is 2 bytes) > 1MB
                    if (value.getRawValue().length() * 2L > MB) {
                        value.setRawValue("_TOOLONG_");
                    }

                    if (LOG.isWarnEnabled()) {
                        LOG.warn("shortened SRM record 'raw value' for '{}'",
                                ToStringBuilder.reflectionToString(key));
                    }
                }
                // both together
                if (value.getValue() != null) {
                    // rough estimate of bytes (in UTF-16, each char is 2 bytes) > 1MB
                    if (value.getValue().length() * 2L > MB) {
                        value.setValue("_TOOLONG_");
                    }

                    if (LOG.isWarnEnabled()) {
                        LOG.warn("shortened SRM record 'value' for '{}'",
                                ToStringBuilder.reflectionToString(key));
                    }
                }
            });
        }

        cache.putAll(cells);
    }

    @Override
    public void put(CellId id, CellValue value) {
        cache.put(id, value);
    }

    @Override
    public CellValue get(CellId id) {
        return cache.get(id);
    }

    @Override
    public void remove(CellId id) {
        cache.remove(id);
    }

    @Override
    public void clear() {
        if(cache != null) {
            cache.clear();
        }
    }

    public ClientCache<CellId, CellValue> getCache() {
        return cache;
    }

    public LassoClusterClient getClusterClient() {
        return clusterClient;
    }
}
