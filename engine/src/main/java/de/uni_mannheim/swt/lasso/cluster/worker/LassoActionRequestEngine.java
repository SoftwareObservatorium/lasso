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
package de.uni_mannheim.swt.lasso.cluster.worker;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.event.SessionEvent;
import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.dag.ActionRequest;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.engine.action.ActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.dag.ExecutionPlan;
import de.uni_mannheim.swt.lasso.engine.data.LassoOperations;
import de.uni_mannheim.swt.lasso.engine.data.fs.LassoFileSystem;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.event.EventManager;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;

import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages action requests on worker nodes.
 *
 * @author Marcus Kessel
 */
public class LassoActionRequestEngine {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoActionRequestEngine.class);

    protected final LassoConfiguration configuration;

    /**
     * TODO evict them after a certain time (by event?)
     */
    protected final Map<String, LSLSession> sessions = new LinkedHashMap<>();

    protected final WorkspaceManager workspaceManager;
    protected final ExecutionEnvironmentManager executionEnvironmentManager;

    private BenchmarkManager benchmarkManager;

    private final ClusterEngine clusterEngine;

    private final EventManager eventManager;

    public LassoActionRequestEngine(LassoConfiguration configuration,
                                    WorkspaceManager workspaceManager,
                                    ExecutionEnvironmentManager executionEnvironmentManager,
                                    BenchmarkManager benchmarkManager,
                                    ClusterEngine clusterEngine) {
        this.configuration = configuration;
        this.workspaceManager = workspaceManager;
        this.executionEnvironmentManager = executionEnvironmentManager;

        this.benchmarkManager = benchmarkManager;

        this.clusterEngine = clusterEngine;

        // add session event listener
        RemoteSessionEventListener remoteSessionEventListener = new RemoteSessionEventListener(this);
        clusterEngine.addLocalSessionListener(remoteSessionEventListener);

        // events
        this.eventManager = new EventManager() {

            @Override
            public void fireKillAction(String executionId, String name) {
                // send session event to all the other worker nodes (including this one)
                SessionEvent sessionEvent = new SessionEvent();
                sessionEvent.setType(SessionEvent.KILL_ACTION);
                sessionEvent.setPayload(name);
                sessionEvent.setExecutionId(executionId);
                clusterEngine.sendSessionEvent(sessionEvent);
            }
        };

        eventManager.addListener(remoteSessionEventListener);
    }

    /**
     * Execute action.
     *
     * @param actionRequest
     * @return
     */
    public LSLExecutionContext execute(ActionRequest actionRequest) throws IOException {
        try {
            // lookup existing session
            LSLSession session = getSession(actionRequest.getExecutionId());
            if(session == null) {
                session = createSession(actionRequest);

                // add
                sessions.put(actionRequest.getExecutionId(), session);
            } else {
                session = sessions.get(actionRequest.getExecutionId());

                // existing session --> we need to update DAG (!)
                if((LOG.isInfoEnabled())) {
                    LOG.info("Updating existing session with current execution plan (DAG)");
                }

                ExecutionPlan executionPlan = actionRequest.getExecutionPlan();

                session.getLslExecutionContext().setExecutionPlan(executionPlan);
            }

            // process
            session.setCurrentAction(null);

            DefaultAction currentAction = createAction(actionRequest, session.getLslExecutionContext());
            session.setCurrentAction(currentAction);

            // execute action
            runAction(actionRequest, currentAction, session.getLslExecutionContext());

            // store executables
            Systems executables = currentAction.getExecutables();

            if(LOG.isDebugEnabled()) {
                LOG.debug("Storing executables for '{}' '{}'", session.getLslExecutionContext().getExecutionId(), currentAction.getName());
            }

            LassoOperations lassoOperations = session.getLslExecutionContext().getLassoOperations();
            lassoOperations.putExecutables(session.getExecutionId(), currentAction.getName(), executables/*, session.getLslExecutionContext().getWorkerNodeId()*/);

            return session.getLslExecutionContext();
        } finally {
            //
        }
    }

    protected DefaultAction createAction(ActionRequest actionRequest, LSLExecutionContext lslExecutionContext) {
        ActionManager actionManager = lslExecutionContext.getActionManager();

        // create remote action
        DefaultAction action;
//        if (actionManager.containsLocalAction(actionRequest.getName())) {
//            action = actionManager.getLocalAction(actionRequest.getName());
//        } else {
//            try {
//                action = actionManager.createLocalAction(actionRequest.getName(), actionRequest.getType(), actionRequest.getConfiguration());
//            } catch (Throwable e) {
//                throw new RuntimeException(String.format("Could not create local action instance for '%s' for '%s'", actionRequest.getName(), actionRequest.getType()));
//            }
//        }
        try {
            action = actionManager.createLocalAction(actionRequest.getName(), actionRequest.getType(), actionRequest.getConfiguration());
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Could not create local action instance for '%s' for '%s'", actionRequest.getName(), actionRequest.getType()), e);
        }

        return action;
    }

    /**
     *
     * @param actionRequest
     * @param action
     * @param lslExecutionContext
     * @throws IOException
     */
    protected void runAction(ActionRequest actionRequest, DefaultAction action, LSLExecutionContext lslExecutionContext) throws IOException {
        ActionManager actionManager = lslExecutionContext.getActionManager();

        // add listener
        action.addListener(new ActionExecutionListener() {
//            @Override
//            public void onFailedExecution(DefaultAction action, String executableId) {
//                if(StringUtils.equals(action.getInstanceId(), executableId)) {
//                    if(LOG.isWarnEnabled()) {
//                        LOG.warn("Skipping id (i.e. parent pom action id) '{}'", executableId);
//                    }
//
//                    return;
//                }
//
//                if (LOG.isDebugEnabled()) {
//                    LOG.debug("Implementation {} failed in action {}", executableId, action.getName());
//                }
//
//                // FIXME what to set? some flag for failed
//                try {
//                    action.postExecute(lslExecutionContext, actionRequest.getConfiguration(), executableId, false);
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            }

            @Override
            public void onSuccessfulExecution(DefaultAction action, String executableId) {
                if(StringUtils.equals(action.getInstanceId(), executableId)) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Skipping id (i.e. parent pom action id) '{}'", executableId);
                    }

                    return;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Implementation {} succeeded in action {}", executableId, action.getName());
                }

                Result result = Result.ERROR;

                try {
                    result = actionManager.collect(action, lslExecutionContext, executableId);
                } catch (Throwable e) {
                    LOG.warn("collect failed", e);
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Collector for implementation {} returned status {}", executableId, result);
                }

                try {
                    action.postExecute(lslExecutionContext, actionRequest.getConfiguration(), executableId, result == Result.SUCCESS);
                } catch (Throwable e) {
                    LOG.warn("postExecute failed", e);
                }
            }
        });

        // execute action

//        // set implementations from cache
//        Abstraction abstraction = actionRequest.getConfiguration().getAbstraction();
//
//        LassoOperations lassoOperations = lslExecutionContext.getLassoOperations();
//        List<Implementation> implementations = lassoOperations.getImplementations(lslExecutionContext.getExecutionId(), abstraction.getName());
//        abstraction.setImplementations(implementations);
//
//        if(LOG.isDebugEnabled()) {
//            LOG.debug("Setting implementations for '{}'. Size '{}'", abstraction.getName(), implementations.size());
//        }

        try {
            action.execute(lslExecutionContext, actionRequest.getConfiguration());

            // wait while busy with collecting
            //if(!action.isFinished()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Initiating shutdown action '{}'", action.getName());
                }

                action.close();

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Shutdown completed for action '{}'", action.getName());
                }
            //}
        } catch (Throwable e) {
            //e.printStackTrace();
            LOG.warn("Action " + action.getName() + " failed", e);
        }
    }

    public LSLSession getSession(String executionId) {
        return sessions.get(executionId);
    }

    public boolean sessionExists(String executionId) {
        return sessions.containsKey(executionId);
    }

    public void closeSession(String executionId) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Closing session '{}'", executionId);
        }

        sessions.remove(executionId);
    }

    public void killAction(String executionId, String name) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Killing action '{}' for session '{}'", name, executionId);
        }

        LSLSession session = sessions.get(executionId);

        if(LOG.isDebugEnabled()) {
            LOG.debug("" + (session.getCurrentAction() != null && session.getCurrentAction().getName().equals(name) && !session.getCurrentAction().isFinished()));
            LOG.debug(session.getCurrentAction() + "");
        }

        if(session == null) {
            return;
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("" + (session.getCurrentAction() != null && session.getCurrentAction().getName().equals(name) && !session.getCurrentAction().isFinished()));
            LOG.debug(session.getCurrentAction() + "");
        }

        // stop now
        if(session.getCurrentAction() != null && session.getCurrentAction().getName().equals(name) && !session.getCurrentAction().isFinished()) {
            //
            session.getCurrentAction().stopNow();
        }
    }

    public LSLSession createSession(ActionRequest actionRequest) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Creating new LSLSession for script '{}' and session id '{}'",
                    actionRequest.getExecutionId(), actionRequest.getSessionId());
        }

        // workspace
        Workspace workspace = workspaceManager.create(actionRequest);

        // create LSL execution context
        LSLExecutionContext lslExecutionContext = new LSLExecutionContext();
        //lslExecutionContext.setLassoContext(lassoContext);

        lslExecutionContext.setConfiguration(configuration);

        lslExecutionContext.setWorkspace(workspace);
        lslExecutionContext.setExecutionEnvironmentManager(executionEnvironmentManager);

        // on demand map
        Map<String, DataSource> dataSourceMap = LazyMap.lazyMap(
                new HashMap<>(), configuration::getDataSource);
        lslExecutionContext.setDataSourceMap(dataSourceMap);

        lslExecutionContext.setActionManager(new ActionManager());

        // events
        lslExecutionContext.setEventManager(eventManager);

        /////
        // data repository
        lslExecutionContext.setReportOperations(clusterEngine.getReportRepository());
        lslExecutionContext.setLassoOperations(clusterEngine.getLassoRepository());
        lslExecutionContext.setLassoFileSystem(clusterEngine.getFileSystem());
        /////

        lslExecutionContext.setBenchmarkManager(benchmarkManager);

        // initialize new one
        ExecutionPlan executionPlan = actionRequest.getExecutionPlan();
        lslExecutionContext.setExecutionPlan(executionPlan);

        lslExecutionContext.setWorkerNodeId(actionRequest.getWorkerNodeId());

        LSLSession session = new LSLSession();
        session.setExecutionId(actionRequest.getExecutionId());
        session.setSessionId(actionRequest.getSessionId());
        session.setLslExecutionContext(lslExecutionContext);

        return session;
    }

    public LassoFileSystem getLassoFileSystem() {
        return clusterEngine.getFileSystem();
    }

    public ClusterEngine getClusterEngine() {
        return clusterEngine;
    }
}
