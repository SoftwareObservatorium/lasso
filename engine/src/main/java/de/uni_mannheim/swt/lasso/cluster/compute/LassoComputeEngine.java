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
package de.uni_mannheim.swt.lasso.cluster.compute;

import de.uni_mannheim.swt.lasso.benchmark.BenchmarkManager;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.data.repository.StepReport;
import de.uni_mannheim.swt.lasso.cluster.event.SessionEvent;
import de.uni_mannheim.swt.lasso.engine.action.ActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.engine.dag.*;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.engine.LassoEngine;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.data.LassoOperations;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.spec.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.ignite.cluster.ClusterGroup;

import org.apache.ignite.cluster.ClusterState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Distributed LASSO engine based on ClusterManager.
 *
 * By default it uses a round-robin strategy to distribute tasks.
 *
 * @author Marcus Kessel
 */
public class LassoComputeEngine extends LassoEngine {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoComputeEngine.class);

    private final ClusterEngine clusterEngine;

    private final PartitioningStrategy partitioningStrategy;

    private ExecutorService singleExecutor = Executors.newFixedThreadPool(1);

    private final ExecutorService localExecutorService;
    private final int taskTimeout;

    /**
     * Workspace populated?
     */
    private boolean populated = false;

    public LassoComputeEngine(LassoConfiguration configuration, ActionManager actionManager,
                              WorkspaceManager workspaceManager,
                              ExecutionEnvironmentManager executionEnvironmentManager,
                              BenchmarkManager benchmarkManager,
                              ClusterEngine clusterEngine) {
        super(configuration, actionManager, workspaceManager, benchmarkManager, executionEnvironmentManager);

        this.clusterEngine = clusterEngine;

        // parallelism for processing local abstractions
        int threadsPerAbstraction = configuration.getProperty("master.threadsPerAbstraction", Integer.class);
        
        this.taskTimeout = configuration.getProperty("master.jobs.taskTimeout", Integer.class);

        this.localExecutorService = createExecutor("abstraction-worker", threadsPerAbstraction);

        //
        this.partitioningStrategy = new PrioritizePowerfulNodes(configuration);
    }

    protected ExecutorService createExecutor(String prefix, int threads) {
        // create threadpool
        return Executors.newFixedThreadPool(threads,
                new ThreadFactory() {

                    private AtomicInteger inc = new AtomicInteger();

                    /**
                     * Assign a specific name to the TDS job threads
                     */
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                String.format("%s-%s", prefix, inc.getAndIncrement()));
                    }

                });
    }

    /**
     * Execution plan.
     *
     * @param actionsDag
     * @param lslExecutionContext
     */
    protected void runExecutionPlan(ExecutionPlan actionsDag, LSLExecutionContext lslExecutionContext) throws IOException {
        // activate cluster
        if(!clusterEngine.getIgnite().cluster().state().active()) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Cluster is currently inactive. Activating cluster now");
            }

            // activate
            clusterEngine.getIgnite().cluster().state(ClusterState.ACTIVE);
        } else {
            if(LOG.isInfoEnabled()) {
                LOG.info("Cluster is currently active.");
            }
        }

        // setup DataRepository
        lslExecutionContext.setReportOperations(clusterEngine.getReportRepository());
        lslExecutionContext.setLassoOperations(clusterEngine.getLassoRepository());
        lslExecutionContext.setLassoFileSystem(clusterEngine.getFileSystem());

        // populate workspaces
        if(!populated) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Populating workspaces");
            }

            populateWorkspaces();
        }

        // XXX DEBUG create image for debugging purposes
        try {
            DAG.writeGraph(actionsDag, lslExecutionContext.getWorkspace());
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("Action nodes '{}'", actionsDag.vertexSet().size());
        }

        //
        Iterator<ActionNode> actionSpecIterator = actionsDag.iterator();

        // execute actions in order
        while (actionSpecIterator.hasNext()) {
            ActionNode actionNode = actionSpecIterator.next();

            if(!actionNode.isSameStudy(lslExecutionContext.getLassoContext())) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Ignoring external ACTION '{}'", actionNode.getName());
                }

                continue;
            }

            ActionSpec actionSpec = actionNode.getActionSpec();

            if(LOG.isInfoEnabled()) {
                LOG.info("Executing ACTION '{}'", actionSpec.getName());
            }

            // -- init
            // applies to specific abstraction?
            String abstractionPattern = actionSpec.getIncludeAbstractions();

            LassoContext lassoContext = lslExecutionContext.getLassoContext();

            // depends on actions?

            // ------ new
            Map<String, ActionNode> actionNodeMap = new LinkedHashMap<>();
            if (CollectionUtils.isNotEmpty(actionSpec.getDependsOn())) {

                for(String dependsOn : actionSpec.getDependsOn()) {
                    // action defined in other study
                    boolean externalStudyRef = StringUtils.contains(dependsOn, ":");
                    if(externalStudyRef) {
                        String[] parts = StringUtils.split(dependsOn, ":");
                        String dependsOnActionPart = parts[1];

                        ActionNode externalStudyNode = actionsDag.getAction(dependsOnActionPart);

                        // retrieve external study data
                        LOG.info("Retrieving executables from external study '{}' of action '{}'",
                                externalStudyNode.getDependsOnStudy(), externalStudyNode.getName());

                        Map<String, Systems> externalExecutablesMap = lslExecutionContext.getLassoOperations()
                                .getAbstractions(externalStudyNode.getDependsOnStudy(), dependsOnActionPart);

                        // retrieve external study data
                        LOG.info("Found '{}' abstractions", externalExecutablesMap.keySet().size());

                        for(String abstractionName : externalExecutablesMap.keySet()) {
                            if(abstractionMatcher.match(abstractionPattern, abstractionName)) {
                                // retrieve external study data
                                LOG.info("Matched abstraction '{}' from past study '{}' of action '{}'",
                                        abstractionName, externalStudyNode.getDependsOnStudy(), externalStudyNode.getName());

                                Systems executables = externalExecutablesMap.get(abstractionName);

                                // create artificial one
                                AbstractionSpec abstractionSpec = new AbstractionSpec();
                                Map<String, Object> map = new LinkedHashMap<>();
                                map.put("name", abstractionName);
                                abstractionSpec.setMap(map);

                                Abstraction abstraction = new Abstraction();
                                abstraction.setName(abstractionName);
                                abstraction.setImplementations(executables.getExecutables());
                                // TODO abstraction.setSpecification();
                                abstractionSpec.setAbstraction(abstraction);

                                lassoContext.registerAbstraction(abstractionSpec);

                                actionSpec.getAbstractionContainerSpec().getAbstractions().put(abstractionName, abstractionSpec);

                                // update external action node
                                externalStudyNode.setType(executables.getActionInstanceId());
                                actionNodeMap.put(abstractionName, externalStudyNode);
                            }
                        }
                    } else {
                        // action defined in current study
                        ActionNode dependsOnAction = actionsDag.getAction(dependsOn);

                        // retrieve external study data
                        LOG.info("Retrieving executables from current study of action '{}'",
                                dependsOn);

                        // same study
                        Map<String, AbstractionSpec> matchedAbstractionSpecs = dependsOnAction.getActionSpec().getAbstractionContainerSpec().getAbstractions().values()
                                .stream().filter(s -> abstractionMatcher.match(abstractionPattern, s)).collect(Collectors.toMap(AbstractionSpec::getName, a -> a));

                        // fetch dependent action
                        for(String abstractionName : matchedAbstractionSpecs.keySet()) {
                            // depends on
                            AbstractionSpec originalSpec = matchedAbstractionSpecs.get(abstractionName);

                            // create artificial one
                            AbstractionSpec abstractionSpec = new AbstractionSpec();
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("name", abstractionName);
                            abstractionSpec.setMap(map);

                            Abstraction abstraction = new Abstraction();
                            abstraction.setName(abstractionName);
                            abstraction.setSpecification(originalSpec.getAbstraction().getSpecification());
                            if(CollectionUtils.isNotEmpty(originalSpec.getAbstraction().getImplementations())) {
                                abstraction.setImplementations(new ArrayList<>(originalSpec.getAbstraction().getImplementations()));
                            } else {
                                abstraction.setImplementations(new LinkedList<>());
                            }
                            abstractionSpec.setAbstraction(abstraction);

                            lassoContext.registerAbstraction(abstractionSpec);

                            actionSpec.getAbstractionContainerSpec().getAbstractions().put(abstractionName, abstractionSpec);

                            // update node to abstraction map
                            actionNodeMap.put(abstractionName, dependsOnAction);
                        }
                    }
                }
            }

            // ----- new

            // check if we need to reject implementations
            if(actionSpec.getIncludeImplementationsClosure() != null) {
                if(LOG.isInfoEnabled()) {
                    LOG.info("Filtering implementations based on user closure");
                }

                for(String abstractionName : actionSpec.getAbstractionContainerSpec().getAbstractions().keySet()) {
                    AbstractionSpec abstractionSpec = actionSpec.getAbstractionContainerSpec().getAbstractions().get(abstractionName);

                    if(CollectionUtils.isNotEmpty(abstractionSpec.getAbstraction().getImplementations())) {
                        List<System> filteredList = actionSpec.applyIncludeImplementations(abstractionName);
                        List<System> implementations;
                        if(filteredList == null) {
                            implementations = new LinkedList<>();
                        } else {
                            implementations = new ArrayList<>(filteredList);
                        }

                        LOG.warn("Setting '{}' new implementations for abstraction '{}' due to user demand. Original size '{}'",
                                implementations.size(), abstractionName, abstractionSpec.getAbstraction().getImplementations().size());

                        abstractionSpec.getAbstraction().setImplementations(implementations);

//                        Iterator<Implementation> it = abstractionSpec.getAbstraction().getImplementations().iterator();
//                        while(it.hasNext()) {
//                            Implementation implementation = it.next();
//                            if(!actionSpec.getIncludeImplementationsClosure().call(abstractionName, implementation)) {
//                                it.remove();
//
//                                LOG.warn("Removing implementation '{}' from abstraction '{}' due to user demand", implementation.getId(), abstractionName);
//                            }
//                        }
                    }
                }
            }

            // apply configure (if defined)
            actionSpec.applyConfigure();

            // apply to action is done as part of DAG creation
            ActionConfiguration actionConfiguration = actionSpec.getActionConfiguration();

            if (LOG.isInfoEnabled()) {
                LOG.info(actionSpec.getAbstractionContainerSpec().getAbstractions().keySet().toString());

                LOG.info("Action-specific Configuration");

                actionConfiguration.getConfiguration().forEach((k, v) -> {
                    LOG.info(k + " => " + v);
                });
            }

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
                    if(CollectionUtils.isNotEmpty(scopeSpec.getPkgWhitelist())) {
                        scope.addConfiguration("pkgWhitelist", (Serializable) scopeSpec.getPkgWhitelist());
                    }
                    if(CollectionUtils.isNotEmpty(scopeSpec.getPkgBlacklist())) {
                        scope.addConfiguration("pkgBlacklist", (Serializable) scopeSpec.getPkgBlacklist());
                    }
                    if(CollectionUtils.isNotEmpty(scopeSpec.getMethodWhitelist())) {
                        scope.addConfiguration("methodWhitelist", (Serializable) scopeSpec.getMethodWhitelist());
                    }
                    if(CollectionUtils.isNotEmpty(scopeSpec.getMethodBlacklist())) {
                        scope.addConfiguration("methodBlacklist", (Serializable) scopeSpec.getMethodBlacklist());
                    }
                    actionConfiguration.setScope(scope);
                }

                // TODO commands?
            }

            // -- run execute
            if(LOG.isInfoEnabled()) {
                LOG.info("Calling execute block");
            }
            try {
                actionSpec.applyExecute();
            } catch (Throwable e) {
                LOG.warn("Execute block in action failed", e);
            }

            // is distributable?
            boolean distributable = !actionManager.isLocalAction(actionSpec.getType());

            if(LOG.isInfoEnabled()) {
                LOG.info("Action '{}' is '{}'", actionSpec.getType(), distributable ? "distributable" : "local");
            }

            // disable partitioning of implementations?
            boolean disablePartitioning = actionManager.isDisablePartitioning(actionSpec.getType()) || actionConfiguration.isDisablePartitioning();
            int maxSizePartition = actionManager.getPartitioningMax(actionSpec.getType());

            // defines any abstractions? (i.e Select-like action)
            List<AbstractionSpec> definesAbstractionSpecs = actionSpec.getAbstractionSpecs();

            //-- LOCAL ACTIONS ONLY!!! including "SELECTION" actions
            DefaultAction producerAction = null;

            if(!distributable) {
                // create local instance of action
                try {
                    producerAction = actionManager.createLocalAction(actionSpec.getName(), actionSpec.getType(), actionConfiguration);
                } catch (Throwable e) {
                    throw new RuntimeException(String.format("Could not create producer action instance for '%s' for '%s'", actionSpec.getName(), actionSpec.getType()), e);
                }
            }

            if (CollectionUtils.isNotEmpty(definesAbstractionSpecs) && producerAction != null) {
                // run in parallel (producers are always local!)
                DefaultAction finalProducerAction = producerAction;
                Future<?> result = localExecutorService.submit(() -> definesAbstractionSpecs.parallelStream().forEach(abstractionSpec -> {
                    try {
                        Abstraction abstraction;
                        if(abstractionSpec.getAbstraction() != null) {
                            LOG.info("Abstraction is defined by LSL");

                            abstraction = abstractionSpec.getAbstraction();
                        } else {
                            LOG.info("Abstraction must be computed in action");

                            abstraction = finalProducerAction.createAbstraction(lslExecutionContext, actionConfiguration, abstractionSpec);
                            // set
                            abstractionSpec.setAbstraction(abstraction);
                        }

                        // add to current action
                        actionSpec.getAbstractionContainerSpec().getAbstractions().put(abstraction.getName(), abstractionSpec);

                        actionNodeMap.put(abstraction.getName(), actionNode); // self
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }));

                // wait and get
                try {
                    result.get();
                } catch (Throwable e) {
                    LOG.warn("Local executor failed for action producers", e);
                }
            }

            // do work on those abstractions
            AbstractionContainerSpec abstractionContainerSpec = actionSpec.getAbstractionContainerSpec();

            // check if producer action defines multiple abstractions on its own (outside LSL script)
            if(producerAction != null) {
                List<Abstraction> internalAbstractions = producerAction.createAbstractions(lslExecutionContext, actionConfiguration);
                if(CollectionUtils.isNotEmpty(internalAbstractions)) {
                    internalAbstractions.forEach(abstraction -> {
                        AbstractionSpec abstractionSpec;
                        if(!abstractionContainerSpec.getAbstractions().containsKey(abstraction.getName())) {
                            // create artificial one
                            abstractionSpec = new AbstractionSpec();
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("name", abstraction.getName());
                            abstractionSpec.setMap(map);

                            lassoContext.registerAbstraction(abstractionSpec);

                            abstractionContainerSpec.getAbstractions().put(abstraction.getName(), abstractionSpec);

                            actionNodeMap.put(abstraction.getName(), actionNode); // self
                        } else {
                            // FIXME never be called
                            if(true) {
                                throw new UnsupportedOperationException();
                            }

                            abstractionSpec = abstractionContainerSpec.getAbstractions().get(abstraction.getName());
                        }

                        abstractionSpec.setAbstraction(abstraction);
                    });
                }
            }

            Map<String, AbstractionSpec> abstractionMap = abstractionContainerSpec.getAbstractions();

            // keep track of implementations
            Map<String, Map<ReportKey, StepReport>> stepTracker = new LinkedHashMap<>();
            abstractionMap.keySet().forEach(abstractionName -> {
                AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);
                // abstraction specific configuration
                //ActionConfiguration cfg = actionConfiguration.fromAbstraction(abstractionSpec.getAbstraction());

                // add step reports
                //abstractionSpec.getAbstraction().getImplementations().forEach(s -> java.lang.System.out.println(s.getId()));
                Map<ReportKey, StepReport> stepReports = abstractionSpec.getAbstraction().getImplementations().stream()
                        .collect(Collectors.toMap(k -> ReportKey.of(actionSpec.getName(), abstractionName, k),
                                v -> new StepReport()));
                stepTracker.put(abstractionName, stepReports);
            });

            // --- local action?
            if(producerAction != null) {
                // create local tasks
                List<Callable<Void>> localTasks = abstractionMap.keySet().stream().map(abstractionName -> {
                    Callable<Void> task = () -> {
                        // IMPORTANT: we have to create a new instance for each FA (reset state)
                        DefaultAction abProducerAction;

                        // create local instance of action
                        try {
                            abProducerAction = actionManager.createLocalAction(actionSpec.getName(), actionSpec.getType(), actionConfiguration);
                        } catch (Throwable e) {
                            throw new RuntimeException(String.format("Could not create producer action instance for '%s' for '%s'", actionSpec.getName(), actionSpec.getType()), e);
                        }

                        AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);
                        // abstraction specific configuration
                        ActionConfiguration cfg = actionConfiguration.fromAbstraction(abstractionSpec.getAbstraction());

                        if(actionNodeMap.containsKey(abstractionName)) {
                            ActionNode dependsOnAction = actionNodeMap.get(abstractionName);
                            cfg.setDependsOn(dependsOnAction.getName());
                        } else {
                            cfg.setDependsOn("");
                        }

                        if(LOG.isInfoEnabled()) {
                            LOG.info("=========== Local PARTITION =============");
                            LOG.info("Action Name: '{}'", actionSpec.getName());
                            LOG.info("Action Type: '{}'", actionSpec.getType());
                            //LOG.info("DependsOnStudy Origin: '{}' ({})", actionNode.getDependsOnStudy(), sameStudy ? "current" : "external"); // same study or not
                            LOG.info("Abstraction Name: '{}'", abstractionName);
                            LOG.info("Depends on action name: '{}'", actionNodeMap.containsKey(abstractionName) ? actionNodeMap.get(abstractionName) : "unknown");
                            LOG.info("Include Tests Pattern: '{}'", cfg.getIncludeTestsPattern());
                            LOG.info("Total implementations: {}", cfg.getAbstraction().getImplementations().size());
                            LOG.info("Specification: {}", ToStringBuilder.reflectionToString(cfg.getAbstraction().getSpecification()));
                            LOG.info("=========== PARTITION =============");
                        }

                        // execute local action
                        // add listener
                        abProducerAction.addListener(new LocalActionListener(lslExecutionContext, cfg));

                        try {
                            abProducerAction.execute(lslExecutionContext, cfg);

                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Initiating shutdown action '{}'", abProducerAction.getName());
                            }

                            abProducerAction.close();

                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Shutdown completed for action '{}'", abProducerAction.getName());
                            }

                            // UPDATE executables
                            abstractionSpec.setAbstraction(updateAbstraction(lslExecutionContext, abProducerAction));
                            if(cfg.getAbstraction().getSpecification() != null) {
                                abstractionSpec.getAbstraction().setSpecification(cfg.getAbstraction().getSpecification());
                            }
                        } catch(Throwable e) {
                            LOG.warn("Local action '{}' failed", abProducerAction.getName());
                            LOG.warn("Stack trace", e);

                            throw new RuntimeException(String.format("Could not execute producer action instance for '%s' for '%s'", actionSpec.getName(), actionSpec.getType()), e);
                        }

                        return null;
                    };

                    return task;
                }).collect(Collectors.toList());

                if(LOG.isInfoEnabled()) {
                    LOG.info("Running parallel (local)");
                }

                // wait and get
                List<Future<Void>> results = null;
                try {
                    results = localExecutorService.invokeAll(localTasks);
                } catch (InterruptedException e) {
                    LOG.warn("N-thread executor failed for action executions", e);
                }

                // --- end local action
            } else {
                // --- remote action

                // cluster group
                ClusterGroup clusterGroup = clusterEngine.getWorkerNodes();

                if (LOG.isInfoEnabled()) {
                    LOG.info("Worker nodes registered => {}",
                            clusterGroup.nodes().stream().map(n -> n.id().toString()).collect(Collectors.joining(",")));
                }

                // COMPUTE PARTITIONS (here ActionJobs)
                // constraint: per machine: run one FA at a time

                List<ActionRequest> requests;
                if (disablePartitioning) {
                    // return jobs
                    requests = abstractionMap.keySet().stream().map(abstractionName -> {
                        AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);
                        // abstraction specific configuration
                        ActionConfiguration cfg = actionConfiguration.fromAbstraction(abstractionSpec.getAbstraction());

                        if(actionNodeMap.containsKey(abstractionName)) {
                            ActionNode dependsOnAction = actionNodeMap.get(abstractionName);
                            cfg.setDependsOn(dependsOnAction.getName());
                        } else {
                            cfg.setDependsOn("");
                        }

                        if (LOG.isInfoEnabled()) {
                            LOG.info("Partitioning disabled for action of type '{}'. Distributing implementations to random worker node", actionSpec.getType());
                        }

                        // random worker node gets ALL implementations
                        List<System> implementations = new ArrayList<>(cfg.getAbstraction().getImplementations());

                        if (LOG.isInfoEnabled()) {
                            LOG.info("=========== PARTITION (partitioning disabled) =============");
                            LOG.info("Action Name: '{}'", actionSpec.getName());
                            LOG.info("Action Type: '{}'", actionSpec.getType());
                            LOG.info("Abstraction Name: '{}'", abstractionName);
                            LOG.info("Depends on action name: '{}'", actionNodeMap.containsKey(abstractionName) ? actionNodeMap.get(abstractionName) : "unknown");
                            LOG.info("Include Tests Pattern: '{}'", cfg.getIncludeTestsPattern());
                            LOG.info("Total implementations: {}", cfg.getAbstraction().getImplementations().size());
                            LOG.info("Specification: {}", ToStringBuilder.reflectionToString(cfg.getAbstraction().getSpecification()));
                            LOG.info("=========== PARTITION =============");
                        }

                        // create requests
                        // node specific configuration
                        ActionConfiguration nodeConfiguration = new ActionConfiguration();
                        nodeConfiguration.resetConfiguration(cfg.getConfiguration());
                        nodeConfiguration.setDependsOn(cfg.getDependsOn());
                        nodeConfiguration.setProfile(cfg.getProfile());
                        nodeConfiguration.setScope(cfg.getScope());
                        nodeConfiguration.setIncludeTestsPattern(cfg.getIncludeTestsPattern());
                        nodeConfiguration.setDependsOnActions(cfg.getDependsOnActions());

                        Abstraction abstraction = new Abstraction();
                        abstraction.setName(cfg.getAbstraction().getName());
                        abstraction.setSpecification(cfg.getAbstraction().getSpecification());

                        // set block of implementations
                        abstraction.setImplementations(implementations);

                        nodeConfiguration.setAbstraction(abstraction);

                        ActionRequest actionRequest = new ActionRequest();
                        actionRequest.setExecutionId(lslExecutionContext.getExecutionId());

                        // BETTER: let actions specify multiple DEPENDS on to do a filtering selection on those
                        actionRequest.setExecutionPlan(actionsDag);

                        actionRequest.setName(actionSpec.getName());
                        actionRequest.setType(actionSpec.getType());
                        actionRequest.setConfiguration(nodeConfiguration);

                        return actionRequest;
                    }).collect(Collectors.toList());
                } else {
                    // do partitioning
                    requests = abstractionMap.keySet().stream().map(abstractionName -> {
                        AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);
                        // abstraction specific configuration
                        ActionConfiguration cfg = actionConfiguration.fromAbstraction(abstractionSpec.getAbstraction());

                        if(actionNodeMap.containsKey(abstractionName)) {
                            ActionNode dependsOnAction = actionNodeMap.get(abstractionName);
                            cfg.setDependsOn(dependsOnAction.getName());
                        } else {
                            cfg.setDependsOn("");
                        }

                        if (LOG.isInfoEnabled()) {
                            LOG.info("=========== Local PARTITION =============");
                            LOG.info("Action Name: '{}'", actionSpec.getName());
                            LOG.info("Action Type: '{}'", actionSpec.getType());
                            //LOG.info("DependsOnStudy Origin: '{}' ({})", actionNode.getDependsOnStudy(), sameStudy ? "current" : "external"); // same study or not
                            LOG.info("Abstraction Name: '{}'", abstractionName);
                            LOG.info("Depends on action name: '{}'", actionNodeMap.containsKey(abstractionName) ? actionNodeMap.get(abstractionName) : "unknown");
                            LOG.info("Include Tests Pattern: '{}'", cfg.getIncludeTestsPattern());
                            LOG.info("Total implementations: {}", cfg.getAbstraction().getImplementations().size());
                            LOG.info("Specification: {}", ToStringBuilder.reflectionToString(cfg.getAbstraction().getSpecification()));
                            LOG.info("=========== PARTITION =============");
                        }

                        // size of cluster
                        int clusterSize = clusterGroup.nodes().size();

                        //boolean alreadyPartitioned = cfg.getAbstraction().getImplementations().stream().anyMatch(impl -> impl.getWorkerNodeId() != null);

                        // figure out partitioning
                        Map<String, List<System>> partition = new HashMap<>();

//                        if(alreadyPartitioned) {
//                            cfg.getAbstraction().getImplementations().forEach(impl -> {
//                                partition.computeIfAbsent(impl.getWorkerNodeId(), k -> partition.put(k, new ArrayList<>()));
//                                partition.get(impl.getWorkerNodeId()).add(impl);
//                            });
//                        } else {
                        // create a partition remember for other actions .. maintain this information
                        List<System> impls = cfg.getAbstraction().getImplementations();

                        // do partition
                        LOG.info("Partition block size is set to '{}' for '{}' in action '{}'", maxSizePartition, abstractionName, actionSpec.getName());
                        if(maxSizePartition > 0) {
                            // action sets partition size
                            Map<String, List<System>> computedPartition = impls.stream()
                                    .collect(Collectors.toMap(k -> UUID.randomUUID().toString(), v -> new ArrayList<>(Collections.singleton(v))));
                            partition.putAll(computedPartition);
                        } else {
                            Map<String, List<System>> computedPartition = partitioningStrategy.partition(impls, clusterGroup.nodes());
                            partition.putAll(computedPartition);
                        }

                        if (LOG.isInfoEnabled()) {
                            LOG.info("=========== Remote PARTITION =============");
                            LOG.info("Action Name: '{}'", actionSpec.getName());
                            LOG.info("Action Type: '{}'", actionSpec.getType());
                            LOG.info("Abstraction Name: '{}'", abstractionName);
                            LOG.info("Depends on action name: '{}'", actionNodeMap.containsKey(abstractionName) ? actionNodeMap.get(abstractionName) : "unknown");
                            LOG.info("Include Tests Pattern: '{}'", cfg.getIncludeTestsPattern());
                            LOG.info("Total implementations: {}", cfg.getAbstraction().getImplementations().size());
                            LOG.info("Specification: {}", ToStringBuilder.reflectionToString(cfg.getAbstraction().getSpecification()));

                            partition.entrySet().forEach(e -> {
                                LOG.info("{} => {}", e.getKey(), e.getValue().size());
                            });
                            LOG.info("=========== PARTITION =============");
                        }

                        // create requests
                        // generate session id
                        String sessionId = UUID.randomUUID().toString();
                        return partition.entrySet().stream().map(entry -> {
                            // node specific configuration
                            ActionConfiguration nodeConfiguration = new ActionConfiguration();
                            nodeConfiguration.resetConfiguration(cfg.getConfiguration());
                            nodeConfiguration.setDependsOn(cfg.getDependsOn());
                            nodeConfiguration.setProfile(cfg.getProfile());
                            nodeConfiguration.setScope(cfg.getScope());
                            nodeConfiguration.setIncludeTestsPattern(cfg.getIncludeTestsPattern());
                            nodeConfiguration.setDependsOnActions(cfg.getDependsOnActions());

                            Abstraction abstraction = new Abstraction();
                            abstraction.setName(cfg.getAbstraction().getName());
                            abstraction.setSpecification(cfg.getAbstraction().getSpecification());

                            // set block of implementations
                            List<System> implementations = entry.getValue();
                            abstraction.setImplementations(implementations);

                            nodeConfiguration.setAbstraction(abstraction);

                            ActionRequest actionRequest = new ActionRequest();
                            actionRequest.setSessionId(sessionId);
                            actionRequest.setExecutionId(lslExecutionContext.getExecutionId());
                            actionRequest.setExecutionPlan(actionsDag);

                            actionRequest.setName(actionSpec.getName());
                            actionRequest.setType(actionSpec.getType());
                            actionRequest.setConfiguration(nodeConfiguration);

                            return actionRequest;
                        }).collect(Collectors.toList());
                    }).flatMap(Collection::stream).collect(Collectors.toList());
                }

                // submit jobs to cluster
                if (CollectionUtils.isNotEmpty(requests)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Sending '{}' Action Requests to cluster. Using timeout '{}' seconds", requests.size(), taskTimeout);
                    }

                    try {
                        List<ActionResponse> actionResponses = clusterEngine.compute(requests, taskTimeout * 1000L);

                        // update abstraction model for LSL script (removals, additions etc.)
                        LassoOperations lassoOperations = lslExecutionContext.getLassoOperations();

                        abstractionMap.keySet().forEach(abstractionName -> {
                            try {
                                Systems executables = lassoOperations.getExecutables(lslExecutionContext.getExecutionId(), abstractionName, actionSpec.getName());

                                // set implementations in same order as retrieved (maintains relevance in case the list is sublisted)
                                if (executables.hasExecutables()) {
                                    List<System> executableList = executables.getExecutables().stream()
                                            // descending
                                            .sorted(Collections.reverseOrder(Comparator.comparingDouble(a -> a.getCode().getScore())))
                                            .collect(Collectors.toList());
                                    executables.setExecutables(executableList);
                                }

                                Abstraction updatedAbstraction = executables.toAbstraction();
                                AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);
                                Abstraction oldAbstraction = abstractionSpec.getAbstraction();
                                if(oldAbstraction.getSpecification() != null) {
                                    updatedAbstraction.setSpecification(oldAbstraction.getSpecification());
                                }
                                abstractionSpec.setAbstraction(updatedAbstraction);
                            } catch (Throwable e) {
                                LOG.warn("Updating executables failed", e);
                            }
                        });
                    } catch (Throwable e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Action requests failed in cluster", e);
                        }
                    }
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("No action requests. Doing nothing.");
                    }
                }
            }

            // step tracker update
            for(String abstractionName : abstractionMap.keySet()) {
                AbstractionSpec abstractionSpec = abstractionMap.get(abstractionName);

                // update step tracker
                try {
                    for(Map.Entry<ReportKey, StepReport> entry : stepTracker.get(abstractionName).entrySet()) {
                        StepReport report = entry.getValue();
                        report.setEnd(new Date());

                        if(abstractionSpec.getAbstraction().getImplementations() != null &&
                                abstractionSpec.getAbstraction().getImplementations().stream().anyMatch(i -> StringUtils.equals(i.getId(), entry.getKey().getSystem()))) {
                            report.setPassed(true);
                        } else {
                            report.setPassed(false);
                            report.setRejectReason("action");
                        }
                    }
                } catch (Throwable e) {
                    LOG.warn("Step tracker failed", e);
                }
            }

            // postProcess
            try {
                actionSpec.applyWhenAbstractionsReady();
            } catch (Throwable e) {
                LOG.warn("WhenAbstractionsReady failed", e);
            }

            // now we need to check if script has filtered implementations
            Map<String, AbstractionSpec> abstractionSpecMap = abstractionContainerSpec.getAbstractions();

            // update abstraction model for LSL script (removals, additions etc.)
            LassoOperations lassoOperations = lslExecutionContext.getLassoOperations();

            //if(actionSpec.getWhenAbstractionsReadyClosure() != null) {
                for(String abstractionName : abstractionSpecMap.keySet()) {
                    AbstractionSpec abstractionSpec = abstractionSpecMap.get(abstractionName);

                    if(abstractionSpec.getAbstraction().getImplementations() != null) {
                        Systems executables = lassoOperations.getExecutables(lslExecutionContext.getExecutionId(), abstractionName, actionSpec.getName());

                        if(executables.getExecutables() != null) {
                            if(LOG.isInfoEnabled()) {
                                LOG.info("WhenAbstractionsReady before modified implementations '{}', '{}'",
                                        abstractionName, executables.getExecutables().size());
                            }

                            // removals
                            boolean removed = executables.getExecutables().removeIf(executable -> {
                                boolean remove = abstractionSpec.getAbstraction().getImplementations().stream()
                                        .noneMatch(impl -> StringUtils.equals(executable.getId(), impl.getId()));

                                if(remove) {
                                    try {
                                        // update step tracker
                                        StepReport stepReport = stepTracker.get(abstractionName).get(ReportKey.of(actionSpec.getName(), abstractionName, executable));
                                        stepReport.setPassed(false);
                                        stepReport.setRejectReason("lsl");
                                    } catch (Throwable e) {
                                        LOG.warn("Step report failed", e);
                                    }
                                }

                                return remove;
                            });

                            if(removed) {
                                if(LOG.isInfoEnabled()) {
                                    LOG.info("WhenAbstractionsReady modified implementations. Reflecting change in database for '{}'. New size '{}'",
                                            abstractionName, executables.getExecutables().size());
                                }
                            }

                            // FIXME additions ? some bug if executables == null .. 50 refs?
//                            if(actionSpec.getWhenAbstractionsReadyClosure() != null) {
//
//                            }
                            List<System> added = abstractionSpec.getAbstraction().getImplementations().stream().filter(impl -> {
                                try {
                                    System exist = executables.getExecutable(impl.getId());
                                    return exist == null;
                                } catch (Throwable e) {
                                    return true;
                                }
                            }).collect(Collectors.toList());

                            // add step reports
                            if(CollectionUtils.isNotEmpty(added)) {
                                LOG.info("WhenAbstractionsReady added new implementations '{}' for abstraction '{}'", added.size(), abstractionName);

                                // add
                                executables.getExecutables().addAll(added);

                                Map<ReportKey, StepReport> stepReports = added.stream()
                                        .collect(Collectors.toMap(k -> ReportKey.of(actionSpec.getName(), abstractionName, k),
                                                v -> {
                                                    StepReport stepReport = new StepReport();
                                                    stepReport.setPassed(true);
                                                    return stepReport;
                                                }));
                                stepTracker.put(abstractionName, stepReports);
                            }

                            if(removed || CollectionUtils.isNotEmpty(added)) {
                                // update
                                // master node
                                lassoOperations.putExecutables(lslExecutionContext.getExecutionId(),
                                        actionSpec.getName(),
                                        executables, true /*remove existing*/);
                            }
                        }
                    }

                    // TODO also report if other actions and their abstractions were modified (actions[].abstractions[]...)
                }
            //}

            // store step tracker
            try {
                for(String abName : stepTracker.keySet()) {
                    Map<ReportKey, StepReport> stepReportMap = stepTracker.get(abName);
                    stepReportMap.forEach((key, value) -> lslExecutionContext.getReportOperations().put(lslExecutionContext.getExecutionId(), key, value));
                }
            } catch (Throwable e) {
                LOG.warn("Step tracker store failed", e);
            }

            // postProcess: export CSVs
            try {
                lslExecutionContext.getReportOperations().export(lslExecutionContext.getExecutionId(),
                        actionSpec.getName(),
                        lslExecutionContext.getWorkspace().getRoot());
            } catch (Throwable e) {
                LOG.warn("export CSVs failed", e);
            }
        }

        // close all open LSLSessions in worker nodes
        SessionEvent sessionEvent = new SessionEvent();
                    sessionEvent.setType(SessionEvent.CLOSE);
            sessionEvent.setExecutionId(lslExecutionContext.getExecutionId());
        clusterEngine.sendSessionEvent(sessionEvent);

        //-- release all shared things
        //cleanUp(lslExecutionContext);

        // save workspace script to IGFS
        populateWorkspace(lslExecutionContext.getWorkspace().getRoot());
    }

    /**
     * Update abstraction with new {@link Systems}.
     *
     * @param lslExecutionContext
     * @param action
     * @return
     */
    private Abstraction updateAbstraction(LSLExecutionContext lslExecutionContext, DefaultAction action) {
        // UPDATE executables
        try {
            // update abstraction model for LSL script (removals, additions etc.)
            LassoOperations lassoOperations = lslExecutionContext.getLassoOperations();

            // store executables
            Systems executables = action.getExecutables();

            if(LOG.isDebugEnabled()) {
                LOG.debug("Storing executables for '{}' '{}' '{}'", lslExecutionContext.getExecutionId(), action.getName(), executables.getAbstractionName());
            }

            // master node
            lassoOperations.putExecutables(lslExecutionContext.getExecutionId(),
                    action.getName(), executables, true /*remove existing*/);
//                    ,
//                    clusterEngine.getMasterNode().node().id().toString());

            Abstraction updatedAbstraction = executables.toAbstraction();
            return updatedAbstraction;
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Updating abstraction failed for " + action.getName(), e);
            }

            throw e;
        }
    }

//    private void cleanUp(/*LSLExecutionContext lslExecutionContext*/) {
//        // shared action key-values
//
//        // FIXME keep reports for SQL
//        // reports
//        //clusterEngine.removeReportCaches(lslExecutionContext.getExecutionId());
//
//        // executables
//        // remove cache entries for current executionId --> LassoRepository
//
//        if(LOG.isInfoEnabled()) {
//            LOG.warn("Clearing shared file system");
//        }
//
//        // clear shared filesystem
//        try {
//            clusterEngine.getFileSystem().clear();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//
//        if(LOG.isInfoEnabled()) {
//            LOG.warn("Clearing LASSO repository");
//        }
//
//        clusterEngine.getLassoRepository().clear();
//    }

    @Override
    public void shutdown() {
        //-- release all shared things
        //cleanUp();

        //
        if(localExecutorService != null) {
            localExecutorService.shutdown();
        }

        if(singleExecutor != null) {
            singleExecutor.shutdown();
        }
    }

    /**
     *
     * @author Marcus Kessel
     */
    private class LocalActionListener implements ActionExecutionListener {

        private LSLExecutionContext context;
        private ActionConfiguration actionConfiguration;

        private LocalActionListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) {
            this.context = context;
            this.actionConfiguration = actionConfiguration;
        }

//        @Override
//        public void onFailedExecution(DefaultAction action, String executableId) {
//            if(StringUtils.equals(action.getInstanceId(), executableId)) {
//                if(LOG.isWarnEnabled()) {
//                    LOG.warn("Skipping id (i.e. parent pom action id) '{}'", executableId);
//                }
//
//                return;
//            }
//
//            if (LOG.isDebugEnabled()) {
//                LOG.debug("Implementation {} failed in action {}", executableId, action.getName());
//            }
//
//            // FIXME what to set? some flag for failed
//            try {
//                action.postExecute(context, actionConfiguration, executableId, false);
//            } catch (Throwable e) {
//                e.printStackTrace();
//            }
//        }

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
                result = actionManager.collect(action, context, executableId);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            try {
                action.postExecute(context, actionConfiguration, executableId, result == Result.SUCCESS);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void populateWorkspaces() {
        File[] workspaces = workspaceManager.listWorkspaces();

        for(File workspaceRoot : workspaces) {
            populateWorkspace(workspaceRoot);
        }

        populated = true;
    }

    public void populateWorkspace(File workspaceRoot) {
        File[] workspaces = workspaceManager.listWorkspaces();

        try {
            Workspace workspace = workspaceManager.load(workspaceRoot.getName());

            String path = String.format("/workspace/%s/script", workspaceRoot.getName());

            if(clusterEngine.getFileSystem().exists(path)) {
                if(LOG.isInfoEnabled()) {
                    LOG.info("IGFS Already populated workspace '{}' to '{}'", workspaceRoot.getName(), path);
                }

                return;
            }

            File tmpFile = Files.createTempFile("workspace", "lsl").toFile();
            FileUtils.writeStringToFile(tmpFile, workspace.getScript().getContent(), StandardCharsets.UTF_8);

            clusterEngine.getFileSystem().write(path, tmpFile);
            tmpFile.delete();

            if(LOG.isInfoEnabled()) {
                LOG.info("IGFS Populated workspace '{}' to '{}'", workspaceRoot.getName(), path);
            }
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Failed to populate workspace '{}'", workspaceRoot.getAbsolutePath());
            }
        }

        populated = true;
    }

    public ClusterEngine getClusterEngine() {
        return clusterEngine;
    }
}
