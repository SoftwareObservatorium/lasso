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
package de.uni_mannheim.swt.lasso.engine;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.action.ActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.dag.ActionEdge;
import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import de.uni_mannheim.swt.lasso.engine.dag.DAG;
import de.uni_mannheim.swt.lasso.engine.dag.ExecutionPlan;
import de.uni_mannheim.swt.lasso.engine.data.LassoOperations;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.engine.data.fs.LassoFileSystem;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.event.EventListener;
import de.uni_mannheim.swt.lasso.engine.event.EventManager;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.matcher.AbstractionSpecMatcher;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.lsl.LSLDelegatingScript;
import de.uni_mannheim.swt.lasso.lsl.LSLLogger;
import de.uni_mannheim.swt.lasso.lsl.LSLRunner;

import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.spec.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Local, embedded LASSO engine (single node mode).
 *
 * @author Marcus Kessel
 */
public class LassoEngine {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoEngine.class);

    protected final LassoConfiguration configuration;

    protected final ActionManager actionManager;
    protected final WorkspaceManager workspaceManager;
    protected final ExecutionEnvironmentManager executionEnvironmentManager;

    protected static final AbstractionSpecMatcher abstractionMatcher = new AbstractionSpecMatcher();

    private ReportOperations reportOperations;
    private LassoOperations lassoOperations;

    private LassoFileSystem lassoFileSystem;

    private BenchmarkManager benchmarkManager;

    private DefaultAction currentAction;

    public LassoEngine(LassoConfiguration configuration, ActionManager actionManager,
                       WorkspaceManager workspaceManager,
                       ExecutionEnvironmentManager executionEnvironmentManager, LassoFileSystem lassoFileSystem,
                       BenchmarkManager benchmarkManager) {
        this.configuration = configuration;
        this.actionManager = actionManager;
        this.workspaceManager = workspaceManager;
        this.executionEnvironmentManager = executionEnvironmentManager;
        this.lassoFileSystem = lassoFileSystem;
        this.benchmarkManager = benchmarkManager;
    }

    public LassoEngine(LassoConfiguration configuration, ActionManager actionManager,
                       WorkspaceManager workspaceManager,
                       BenchmarkManager benchmarkManager,
                       ExecutionEnvironmentManager executionEnvironmentManager) {
        this(configuration, actionManager, workspaceManager, executionEnvironmentManager, null, benchmarkManager);
    }

    /**
     * Execute script.
     *
     * @param script
     */
    public LSLExecutionResult execute(LSLScript script) throws DataSourceNotFoundException, IOException {
        try {
            // workspace
            Workspace workspace = workspaceManager.create(script);

            // setup logger
            LSLLogger lslLogger;
            if(script.getLogger() == null) {
                lslLogger = new LSLFileLogger(workspace.createFile(Workspace.SCRIPT_EXECUTION_LOG));
                script.setLogger(lslLogger);
            } else {
                lslLogger = script.getLogger();
            }

            // new LSL runner
            LSLRunner lslRunner = new LSLRunner();

            // delegating script
            LSLDelegatingScript delegatingScript = lslRunner.runScript(script.getContent(), lslLogger);

            LassoContext lassoContext = delegatingScript.getLasso();
            lassoContext.setExecutionId(script.getExecutionId());
            lassoContext.setWorkspaceRoot(workspace.getRoot());

            // apply data sources
            Map<String, DataSource> dataSourceMap = applyDataSources(lassoContext);

            // create LSL execution context
            LSLExecutionContext lslExecutionContext = new LSLExecutionContext();
            lslExecutionContext.setLassoContext(lassoContext);
            // set self
            lassoContext.setExecutionContext(lslExecutionContext);

            // set benchmark manager
            lslExecutionContext.setBenchmarkManager(benchmarkManager);

            //

            lslExecutionContext.setConfiguration(configuration);

            lslExecutionContext.setWorkspace(workspace);
            lslExecutionContext.setExecutionEnvironmentManager(executionEnvironmentManager);
            lslExecutionContext.setDataSourceMap(dataSourceMap);

            lslExecutionContext.setReportOperations(reportOperations);
            lslExecutionContext.setLassoOperations(lassoOperations);
            lslExecutionContext.setLassoFileSystem(lassoFileSystem);

            lslExecutionContext.setActionManager(actionManager);

            // events
            lslExecutionContext.setEventManager(new EventManager());
            lslExecutionContext.getEventManager().addListener(new EventListener() {
                @Override
                public void onKillAction(String executionId, String name) {
                    // stop now
                    if(currentAction != null && currentAction.getName().equals(name) && !currentAction.isFinished()) {
                        //
                        currentAction.stopNow();
                    }
                }
            });

            // process
            ExecutionPlan executionPlan = createActionExecutionPlan(lassoContext);

            lslExecutionContext.setExecutionPlan(executionPlan);

            // build environments
            //buildEnvironments(lassoContext);
            // execute actions
            runExecutionPlan(executionPlan, lslExecutionContext);

            LSLExecutionResult lslExecutionResult = new LSLExecutionResult();
            lslExecutionResult.setScript(script);

            return lslExecutionResult;
        } finally {
            // close logger and set
            if (script.getLogger() != null && script.getLogger() instanceof LSLFileLogger) {
                LSLFileLogger fileLogger = (LSLFileLogger) script.getLogger();
                try {
                    fileLogger.close();
                } catch (Throwable e) {
                }
            }
        }
    }

    /**
     * Execution plan.
     *
     * Deprecated- do not use anymore
     *
     * @param actionsDag
     * @param lslExecutionContext
     */
    @Deprecated
    protected void runExecutionPlan_deprecated(ExecutionPlan actionsDag, LSLExecutionContext lslExecutionContext) throws IOException {
        // XXX DEBUG create image for debugging purposes
        try {
            DAG.writeGraph(actionsDag, lslExecutionContext.getWorkspace());
        } catch (Throwable e) {
            e.printStackTrace();
        }

        //
        Iterator<ActionNode> actionSpecIterator = actionsDag.iterator();

        // execute actions in order
        while (actionSpecIterator.hasNext()) {
            ActionSpec actionSpec = actionSpecIterator.next().getActionSpec();

            // -- init
            // applies to specific abstraction?
            String abstractionPattern = actionSpec.getIncludeAbstractions();

            LassoContext lassoContext = lslExecutionContext.getLassoContext();

            // match abstractions
            Map<String, AbstractionSpec> matchedAbstractionSpecs = lassoContext.getAbstractionContainerSpec().getAbstractions().values()
                    .stream().filter(s -> abstractionMatcher.match(abstractionPattern, s)).collect(Collectors.toMap(a -> a.getName(), a -> a));

            // set matched abstractions
            actionSpec.getAbstractionContainerSpec().addAll(matchedAbstractionSpecs);

            // apply configure (if defined)
            actionSpec.applyConfigure();

            // apply to action is done as part of DAG creation
            //ActionConfiguration actionConfiguration = actionSpec.apply();
            ActionConfiguration actionConfiguration = actionSpec.getActionConfiguration();

            if (LOG.isInfoEnabled()) {
                LOG.info(lassoContext.getAbstractionContainerSpec().getAbstractions().keySet().toString());

                LOG.info("Action-specific Configuration");

                actionConfiguration.getConfiguration().forEach((k, v) -> {
                    LOG.info(k + " => " + v);
                });
            }

            // -- run execute
            actionSpec.applyExecute();

            // defines any abstractions? (i.e Select-like action)
            List<AbstractionSpec> definesAbstractionSpecs = actionSpec.getAbstractionSpecs();

            //-- LOCAL ACTIONS ONLY!!! including "SELECTION" actions
            DefaultAction producerAction = null;

            // create local instance of action
            try {
                producerAction = actionManager.createLocalAction(actionSpec.getName(), actionSpec.getType(), actionConfiguration);
            } catch (Throwable e) {
                throw new RuntimeException(String.format("Could not create producer action instance for '%s' for '%s'", actionSpec.getName(), actionSpec.getType()));
            }

            if (CollectionUtils.isNotEmpty(definesAbstractionSpecs)) {
                for (AbstractionSpec abstractionSpec : definesAbstractionSpecs) {
                    Abstraction abstraction = producerAction.createAbstraction(lslExecutionContext, actionConfiguration, abstractionSpec);
                    // set
                    abstractionSpec.setAbstraction(abstraction);
                }
            }

            // TODO do other phases inside the action

            // do work on those abstractions
            AbstractionContainerSpec abstractionContainerSpec = actionSpec.getAbstractionContainerSpec();

            // check if producer action defines multiple abstractions on its own (outside LSL script)
            if(producerAction != null) {
                List<Abstraction> internalAbstractions = producerAction.createAbstractions(lslExecutionContext, actionConfiguration);
                if(CollectionUtils.isNotEmpty(internalAbstractions)) {
                    internalAbstractions.forEach(abstraction -> {
                        AbstractionSpec abstractionSpec = abstractionContainerSpec.getAbstractions().get(abstraction.getName());
                        abstractionSpec.setAbstraction(abstraction);
                    });
                }
                // XXX no reason for this condition
//                else {
//                    // set to null if neither defines external nor internal abstractions
//                    if (CollectionUtils.isEmpty(definesAbstractionSpecs)) {
//                        producerAction = null;
//                    }
//                }
            }

            Map<String, AbstractionSpec> abstractionMap = abstractionContainerSpec.getAbstractions();

            if(abstractionMap.isEmpty()) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Abstractions are empty.");
                }
            }

            for (String abstractionName : abstractionMap.keySet()) {
                AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);

                // set abstraction
                actionConfiguration.setAbstraction(abstractionSpec.getAbstraction());

                // profile
                Profile profile = null;
                if (actionSpec.getProfileSpec() != null) {
                    // run in environment X
                    EnvironmentSpec environmentSpec = actionSpec.getProfileSpec().getEnvironmentSpec();

                    profile = new Profile();

                    Environment environment = new Environment();
                    environment.setImage(environmentSpec.getImage());

                    profile.setEnvironment(environment);

                    actionConfiguration.setProfile(profile);

                    // set scope
                    ScopeSpec scopeSpec = actionSpec.getProfileSpec().getScopeSpec();
                    if(scopeSpec != null) {
                        Scope scope = new Scope();
                        scope.setType(scopeSpec.getType());
                        actionConfiguration.setScope(scope);
                    }

                    // TODO commands?
                }

                //
                currentAction = null;
                if (producerAction != null) {
                    currentAction = producerAction;
                } else {
                    try {
                        currentAction = actionManager.createLocalAction(actionSpec.getName(), actionSpec.getType(), actionConfiguration);
                    } catch (Throwable e) {
                        throw new RuntimeException(String.format("Could not create local action instance for '%s' for '%s'", actionSpec.getName(), actionSpec.getType()));
                    }
                }

                // add listener
                currentAction.addListener(new ActionExecutionListener() {
//                    @Override
//                    public void onFailedExecution(DefaultAction action, String executableId) {
//                        if(StringUtils.equals(action.getInstanceId(), executableId)) {
//                            if(LOG.isWarnEnabled()) {
//                                LOG.warn("Skipping id (i.e. parent pom action id) '{}'", executableId);
//                            }
//
//                            return;
//                        }
//
//                        if (LOG.isDebugEnabled()) {
//                            LOG.debug("Implementation {} failed in action {}", executableId, action.getName());
//                        }
//
//                        // FIXME what to set? some flag for failed
//                        try {
//                            action.postExecute(lslExecutionContext, actionConfiguration, executableId, false);
//                        } catch (Throwable e) {
//                            e.printStackTrace();
//                        }
//                    }

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
                            e.printStackTrace();
                        }

                        try {
                            action.postExecute(lslExecutionContext, actionConfiguration, executableId, result == Result.SUCCESS);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });

                // execute action
                try {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Starting execution for action '{}'", currentAction.getName());
                    }

                    currentAction.execute(lslExecutionContext, actionConfiguration);

                    // wait while busy with collecting
                    //if(!currentAction.isFinished()) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Initiating shutdown action '{}'", currentAction.getName());
                    }

                    currentAction.close();

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Shutdown completed for action '{}'", currentAction.getName());
                    }
                    //}

                    // store
                    lslExecutionContext.getLassoOperations().putExecutables(
                            lslExecutionContext.getExecutionId(), currentAction.getName(), currentAction.getExecutables());

                    // update abstraction model (removals, additions etc.)
                    Systems executables = lslExecutionContext.getLassoOperations().getExecutables(lslExecutionContext.getExecutionId(), abstractionName, actionSpec.getName());
                    Abstraction updatedAbstraction = executables.toAbstraction();
                    abstractionSpec.setAbstraction(updatedAbstraction);
                } catch (Throwable e) {
                    LOG.warn("Action " + currentAction.getName() + " failed", e);
                }
            }

            // postProcess
            actionSpec.applyWhenAbstractionsReady();
        }
    }

    /**
     * Execution plan.
     *
     * Deprecated- do not use anymore
     *
     * @param actionsDag
     * @param lslExecutionContext
     */
    protected void runExecutionPlan(ExecutionPlan actionsDag, LSLExecutionContext lslExecutionContext) throws IOException {
        runExecutionPlan_deprecated(actionsDag, lslExecutionContext);
    }

    protected Map<String, DataSource> applyDataSources(LassoContext lassoContext) throws DataSourceNotFoundException {
        List<String> rawDataSourceIds = lassoContext.getDataSources();

        // remove redundant
        List<String> dataSourceIds = rawDataSourceIds.stream().distinct().collect(Collectors.toList());

        Map<String, DataSource> dataSourceMap = new HashMap<>();
        for (String dataSourceId : dataSourceIds) {
            try {
                DataSource dataSource = configuration.getDataSource(dataSourceId);

                if (dataSource == null) {
                    throw new DataSourceNotFoundException(String.format("Could not find data source '%s'",
                            dataSourceId));
                }

                dataSourceMap.put(dataSourceId, dataSource);
            } catch (Throwable e) {
                throw new DataSourceNotFoundException(String.format("Could not find data source '%s'",
                        dataSourceId), e);
            }
        }

        return dataSourceMap;
    }

    /**
     * Create {@link ExecutionPlan} for instant execution
     *
     * @param lassoContext
     * @return
     */
    public static ExecutionPlan createActionExecutionPlan(LassoContext lassoContext) {
        ExecutionPlan executionPlan = DAG.createExecutionPlan();

        // get all studies
        StudyContainerSpec studyContainerSpec = lassoContext.getStudyContainerSpec();

        // FIXME only one study supported right now

        // map is sorted (see Groovy's default Map)
        for (String studyName : studyContainerSpec.getStudies().keySet()) {
            StudySpec studySpec = studyContainerSpec.getStudies().get(studyName);
            // apply
            studySpec.apply();

            executionPlan.setStudy(studyName);

            // now collect actions
            for (String actionName : lassoContext.getActionContainerSpec().getActions().keySet()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Current action => '{}'", actionName);
                }

                ActionSpec actionSpec = lassoContext.getActionContainerSpec().getActions().get(actionName);

                // add node
                ActionNode actionNode = ActionNode.from(actionSpec, lassoContext);

                if (!executionPlan.containsVertex(actionNode)) {
                    executionPlan.addVertex(actionNode);

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Adding new action => '{}'", actionName);
                    }
                }

                if (CollectionUtils.isNotEmpty(actionSpec.getDependsOn())) {
                    // supports multiple actions
                    for(String otherAction : actionSpec.getDependsOn()) {
                        boolean externalStudyRef = StringUtils.contains(otherAction, ":");

                        ActionNode otherActionNode;
                        // support actions from other study scripts
                        if(externalStudyRef) {
                            String[] parts = StringUtils.split(otherAction, ":");
                            String executionIdPart = parts[0];
                            String dependsOnActionPart = parts[1];

                            // create external action node
                            otherActionNode = new ActionNode();
                            otherActionNode.setName(dependsOnActionPart);
                            otherActionNode.setDependsOnStudy(executionIdPart);
                        } else {
                            // action defined in study script
                            ActionSpec otherActionSpec = lassoContext.getActionContainerSpec().getActions().get(otherAction);
                            if (otherActionSpec == null) {
                                throw new RuntimeException(String.format("Could not find match for dependsOn '%s' for '%s'", actionSpec.getDependsOn(), actionSpec.getName()));
                            }

                            otherActionNode = ActionNode.from(otherActionSpec, lassoContext);
                        }

                        // add node
                        if (!executionPlan.containsVertex(otherActionNode)) {
                            executionPlan.addVertex(otherActionNode);

                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Adding dependsOn action => '{}' for current action '{}'", otherActionNode.getName(), actionSpec.getName());
                            }
                        }

                        // add edge
                        ActionEdge edge = executionPlan.addEdge(otherActionNode, actionNode);
                        // add abstractions on edges
                        String abstractionPattern = actionSpec.getIncludeAbstractions();

//                            // match abstractions
//                            // FIXME unused
//                            lassoContext.getAbstractionContainerSpec().getAbstractions().values()
//                                    .stream().filter(s -> abstractionMatcher.match(abstractionPattern, s)).forEach(abstractionSpec -> {
//
//                                        // add abstraction name
//                                        edge.addAbstraction(abstractionSpec.getName());
//                                    });
                    }
                }
            }
        }

        return executionPlan;
    }

//    // TODO
//    private void buildEnvironments(LassoContext lassoContext) {
//        // get from root study
//        for (String environmentName : lassoContext.getEnvironmentContainerSpec().getEnvironments().keySet()) {
//            EnvironmentSpec environmentSpec = lassoContext.getEnvironmentContainerSpec().getEnvironments().get(environmentName);
//
//            // FIXME apply to some model
//            environmentSpec.apply();
//        }
//
//        // TODO get from actions
//    }

    public void shutdown() {
        //
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public void setReportOperations(ReportOperations reportOperations) {
        this.reportOperations = reportOperations;
    }

    public void setLassoOperations(LassoOperations lassoOperations) {
        this.lassoOperations = lassoOperations;
    }

    public void setLassoFileSystem(LassoFileSystem lassoFileSystem) {
        this.lassoFileSystem = lassoFileSystem;
    }
}
