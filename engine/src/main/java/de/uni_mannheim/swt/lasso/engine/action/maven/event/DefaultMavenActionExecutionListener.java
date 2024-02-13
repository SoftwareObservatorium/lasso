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
package de.uni_mannheim.swt.lasso.engine.action.maven.event;

import com.google.common.cache.*;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Marcus Kessel
 */
public class DefaultMavenActionExecutionListener extends FilteredMavenEventListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultMavenActionExecutionListener.class);

    public static long DEFAULT_PROJECT_TIMEOUT_IN_MILLIS = 3 * 60 * 1000L;

    private final DefaultAction defaultAction;
    private final long projectTimeoutInMillis;

    private final LoadingCache<String, String> projectCache;

    // scheduler to clean cache (i.e. evict entries)
    private ScheduledExecutorService evictionScheduler;

    private ScheduledFuture<?> evictionHandle;

    public DefaultMavenActionExecutionListener(DefaultAction defaultAction) {
        this(defaultAction, DEFAULT_PROJECT_TIMEOUT_IN_MILLIS);
    }

    public DefaultMavenActionExecutionListener(DefaultAction defaultAction, long projectTimeoutInMillis) {
        this.defaultAction = defaultAction;
        this.projectTimeoutInMillis = projectTimeoutInMillis;

        this.projectCache = createCache(projectTimeoutInMillis);
    }

    protected LoadingCache<String, String> createCache(long projectTimeoutInMillis) {
        CacheLoader<String, String> loader;
        loader = new CacheLoader<String, String>() {
            @Override
            public String load(String key) {
                return key;
            }
        };

        LoadingCache<String, String> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(projectTimeoutInMillis, TimeUnit.MILLISECONDS)
                .removalListener(new RemovalListener<String, String>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, String> removalNotification) {
                        if (removalNotification.wasEvicted()) {
                            String cause = removalNotification.getCause().name();

                            String executableId = removalNotification.getKey();

                            if(LOG.isWarnEnabled()) {
                                LOG.warn(String.format("Project timeout reached for '%s' = '%s'. Calling 'onTimeoutReached'",
                                        executableId,
                                        cause));
                            }

                            // KILL process
                            try {
                                onTimeoutReached(executableId);
                            } catch (Throwable e) {
                                if(LOG.isWarnEnabled()) {
                                    LOG.warn(String.format("onTimeoutReached failed for '%s'",
                                            executableId), e);
                                }
                            }
                        }
                    }
                })
                .build(loader);

        return cache;
    }

    /**
     * Override for custom timeout.
     *
     * @param executableId
     */
    protected void onTimeoutReached(String executableId) {
        // KILL process
        try {
            ProcessKiller.kill9(executableId);

            if(LOG.isInfoEnabled()) {
                LOG.info(String.format("Killed process for '%s'",
                        executableId));
            }
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("Was not able to kill process for '%s'",
                        executableId), e);
            }
        }
    }

    @Override
    public void onProjectSucceeded(String time, String executableId) {
        // invalidate cache
        projectCache.invalidate(executableId);

        //
        defaultAction.fireSuccessfulExecution(executableId);
    }

    @Override
    public void onProjectFailed(String time, String executableId) {
        // invalidate cache
        projectCache.invalidate(executableId);

        defaultAction.fireSuccessfulExecution(executableId);
    }

    @Override
    public void onProjectSkipped(String time, String executableId) {
        // invalidate cache
        projectCache.invalidate(executableId);
    }

    @Override
    public void onProjectStarted(String time, String executableId) {
        // add to cache
        projectCache.getUnchecked(executableId);
    }

    @Override
    public void onMavenStart() {
        super.onMavenStart();

        // start scheduler to evict entries
        evictionScheduler = Executors.newScheduledThreadPool(1);

        Runnable beeper = () -> {
            if(projectCache != null) {
//                if(LOG.isDebugEnabled()) {
//                    LOG.debug("Cleaning cache up");
//                }

                projectCache.cleanUp();
            }
        };

        evictionHandle =
                evictionScheduler.scheduleAtFixedRate(beeper, 10, 10, TimeUnit.SECONDS);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Scheduled cache eviction task");
        }
    }

    @Override
    public void onMavenEnd() {
        if(isFinished()) {
            return;
        }

        super.onMavenEnd();

        if(evictionHandle != null) {
            evictionHandle.cancel(true);
        }

        // end scheduler
        if(evictionScheduler != null) {
            evictionScheduler.shutdown();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Shut down cache eviction scheduler");
        }
    }
}
