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
package de.uni_mannheim.swt.lasso.engine.action;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Default extension point for new LASSO actions.
 *
 * @author Marcus Kessel
 */
public abstract class DefaultAction extends Action {

    private final Logger LOG = LoggerFactory
            .getLogger(getClass());

    private static final int TIMEOUT_MINUTES  = 2 * 60;

    /**
     * Default executor for events
     */
    private ThreadPoolExecutor eventsExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(getNoOfThreads(),
            new ThreadFactoryBuilder().setNameFormat("collector-thread-%d").build());

    private Systems executables;

    private List<ActionExecutionListener> listeners = Collections.synchronizedList(new LinkedList<>());

    private int getNoOfThreads() {
        int threads = getNoOfThreadsForCollection();

        if(LOG.isInfoEnabled()) {
            LOG.info("Using '{}' threads for collection", threads);
        }

        return threads;
    }

    /**
     * Assign no of threads for collecting data points.
     *
     * Override if collection is computational expensive.
     *
     * @return Runtime.getRuntime().availableProcessors() - 1
     */
    protected int getNoOfThreadsForCollection() {
        int processors = Runtime.getRuntime().availableProcessors();

        int threads = processors - 1;
        if(threads < 1) {
            return processors;
        } else {
            return threads;
        }
    }

    public void addListener(ActionExecutionListener actionListener) {
        this.listeners.add(actionListener);
    }

    public void removeListener(ActionExecutionListener actionListener) {
        this.listeners.remove(actionListener);
    }

    public void fireSuccessfulExecution(String executableId) {
        eventsExecutorService.submit(() -> {
            listeners.forEach(l -> {
                try {
                    l.onSuccessfulExecution(this, executableId);
                } catch (Throwable e) {
                    LOG.warn("failed to fire for " + executableId, e);
                }
            });
        });
    }

    /**
     * Cannot be called remotely!
     *
     * @param context
     * @param actionConfiguration
     * @param abstractionSpec
     * @return
     * @throws IOException
     */
    public Abstraction createAbstraction(LSLExecutionContext context, ActionConfiguration actionConfiguration, AbstractionSpec abstractionSpec) throws IOException {
        return null;
    }

    /**
     * Cannot be called remotely!
     *
     * @param context
     * @param actionConfiguration
     * @return
     * @throws IOException
     */
    public List<Abstraction> createAbstractions(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        return null;
    }

    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {

    }

    public void postExecute(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, boolean success) {
        //
    }

    public List<RecordCollector> createCollectors() {
        return new LinkedList<>();
    }

    public Systems getExecutables() {
        return executables;
    }

    public void setExecutables(Systems executables) {
        // make sure we are thread-safe
        if(executables != null && executables.hasExecutables()) {
            executables.setExecutables(Collections.synchronizedList(executables.getExecutables()));
        }

        this.executables = executables;
    }

    public void setEventsExecutorService(ThreadPoolExecutor eventsExecutorService) {
        this.eventsExecutorService = eventsExecutorService;
    }

    /**
     * Halt action immediately.
     */
    public void stopNow() {
        //
    }

    /**
     *
     * @return
     */
    public boolean isFinished() {
        return this.eventsExecutorService.getActiveCount() < 1;
    }

    @Deprecated
    public void waitForCompletion() {
        while(this.eventsExecutorService.getActiveCount() > 0) {
            try {
                Thread.sleep(1 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes {@link ExecutorService} for the action. Note that no more tasks can be scheduled!
     * Not desirable in the case when actions are reused.
     */
    public void close() {
        try {
            this.eventsExecutorService.shutdown();
            this.eventsExecutorService.awaitTermination(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("eventsExecutorService shutdown failed", e);
        }

        try {
            onClose();
        } catch (Throwable e) {
            LOG.warn("onclose failed", e);
        }
    }

    /**
     * Override to do cleaning etc.
     */
    protected void onClose() {
    }

    protected void removeExecutableConditionally(ActionConfiguration actionConfiguration, String executableId, String reason) {
        // remove
        if(actionConfiguration.isDropFailed()) {
            boolean removed = getExecutables().remove(executableId);

            if(removed) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Removed executable '{}' from list. Reason = {}", executableId, reason);
                }
            }
        }
    }

    protected void removeExecutableConditionally(ActionConfiguration actionConfiguration, System executable, String reason) {
        removeExecutableConditionally(actionConfiguration, executable.getId(), reason);
    }
}
