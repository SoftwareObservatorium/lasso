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
package de.uni_mannheim.swt.lasso.cluster.fs;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.engine.data.fs.LassoFileSystem;

import org.apache.commons.io.IOUtils;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Distributed file system based on simple key-value cache (might be inefficient, but IGFS was deprecated in Ignite 2.9).
 *
 * @author Marcus Kessel
 */
public class ClusterFileSystem implements LassoFileSystem {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClusterFileSystem.class);

    private final ClusterEngine clusterEngine;

    private IgniteCache<String, LassoFile> cache;

    public ClusterFileSystem(ClusterEngine clusterEngine) {
        this.clusterEngine = clusterEngine;

        initCaches();
    }

    protected void initCaches() {
        CacheConfiguration<String, LassoFile> implCacheConfig =
                new CacheConfiguration<>(CACHE_NAME);
        implCacheConfig.setIndexedTypes(String.class, LassoFile.class);
        //implCacheConfig.setGroupName("lassoModel");

        this.cache = this.clusterEngine.getIgnite().getOrCreateCache(implCacheConfig);

    }

    @Override
    public void write(String path, File file) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("write file '{}, {}'", path, file);
        }

        LassoFile lassoFile = new LassoFile();
        lassoFile.setPath(path);
        lassoFile.setBytes(IOUtils.toByteArray(new FileInputStream(file)));

        cache.put(path, lassoFile);
    }

    @Override
    public OutputStream writeToOutputStream(String path) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("writeToOutputStream file '{}'", path);
        }

        return new ByteArrayOutputStream() {

            @Override
            public void close() throws IOException {
                super.close();

                // put
                LassoFile lassoFile = new LassoFile();
                lassoFile.setPath(path);
                lassoFile.setBytes(toByteArray());

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Putting {} bytes for {}", lassoFile.getBytes().length, path);
                }

                cache.put(path, lassoFile);
            }
        };
    }

    @Override
    public InputStream read(String path) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("read file '{}'", path);
        }

        LassoFile lassoFile = cache.get(path);
        if(lassoFile == null) {
            throw new IOException("path does not exist " + path);
        }

        if(lassoFile.getBytes() != null) {
            return new ByteArrayInputStream(lassoFile.getBytes());
        } else {
            throw new IOException("no bytes for path " + path);
        }
    }

    @Override
    public boolean exists(String path) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("exists file '{}'", path);
        }

        return cache.containsKey(path);
    }

    @Override
    public boolean delete(String path) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("remove file '{}'", path);
        }

        if(!exists(path)) {
            throw new IOException("path does not exist " + path);
        }

        return cache.remove(path);
    }

    @Override
    public long length(String path) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("length file '{}'", path);
        }

        LassoFile lassoFile = cache.get(path);
        if(lassoFile == null) {
            throw new IOException("path does not exist " + path);
        }

        return lassoFile.getBytes() != null ? lassoFile.getBytes().length : 0L;
    }

    public List<String> listFiles(String path) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("list files '{}'", path);
        }

        SqlFieldsQuery sql = new SqlFieldsQuery(
                "select path from LassoFile where path like ?");
        sql.setArgs(String.format("%s%%", path));

        try (QueryCursor<List<?>> cursor = cache.query(sql)) {
            return StreamSupport
                    .stream(cursor.spliterator(), false)
                    .map(r -> (String) r.get(0))
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<String> listFiles(String path, int limit) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("list files '{}'", path);
        }

        SqlFieldsQuery sql = new SqlFieldsQuery(
                "select path from LassoFile where path like ? limit ?");
        sql.setArgs(String.format("%s%%", path), limit);

        try (QueryCursor<List<?>> cursor = cache.query(sql)) {
            return StreamSupport
                    .stream(cursor.spliterator(), false)
                    .map(r -> (String) r.get(0))
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    // TODO better to create caches by LSL executionId and then destroy them!
    // issue is that by now sequential processing of LSL scripts is assumed
    public void clear() throws IOException {
        if(cache != null) {
            cache.clear();
        }
    }
}
