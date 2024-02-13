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
package de.uni_mannheim.swt.lasso.engine.action.task;

import de.uni_mannheim.swt.lasso.cluster.data.repository.ExecKey;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.*;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.lsl.spec.ActionSpec;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Merge an implementation into the current abstraction")
@Stable
@Local
@DisablePartitioning // redundant in this case since this action is local
@Tester // handles tests
public class Merge extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Merge.class);

    @LassoInput(desc = "Defines mapping of (key,value) mapping of functional abstraction and its action", optional = false)
    public Map<String, String> from;

    @LassoInput(desc = "Create new functional abstraction", optional = false)
    public String abstractionName;

    @LassoInput(desc = "Reference implementation only", optional = true)
    public boolean referenceImplementationOnly = true;

    @Override
    public List<Abstraction> createAbstractions(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(StringUtils.isBlank(abstractionName)) {
            return null;
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("Creating new abstraction '{}'", abstractionName);
        }

        Abstraction abstraction = new Abstraction();
        abstraction.setName(abstractionName);
        abstraction.setImplementations(new LinkedList<>());

        return new ArrayList<>(Collections.singleton(abstraction));
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass() + " => " + actionConfiguration.getAbstraction().getName());
        }

        if(StringUtils.equals(abstractionName, actionConfiguration.getAbstraction().getName())) {
            LOG.warn("New functional abstraction required '{}'", abstractionName);

            Systems executables = new Systems();
            executables.setExecutables(new LinkedList<>());

            // determine existing FAs from LSL context
            ActionSpec actionSpec = context.getLassoContext().getActionContainerSpec().getActions().get(getName());
            for(String abName : actionSpec.getAbstractionContainerSpec().getAbstractions().keySet()) {
                //
                Systems existingExecutables = context.getLassoOperations()
                        .getExecutables(context.getExecutionId(), abName, actionConfiguration.getDependsOn());
                if(existingExecutables.hasExecutables()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Merging '{}' implementations of abstraction '{}' into abstraction '{}'",
                                existingExecutables.getExecutables().size(), abName, actionConfiguration.getAbstraction().getName());
                    }

                    executables.getExecutables().addAll(existingExecutables.getExecutables());
                }
            }

            for(String otherAbstraction : from.keySet()) {
                String actionName = from.get(otherAbstraction);

                if (referenceImplementationOnly) {
                    // obtain reference implementation
                    String refImplId = actionConfiguration.getAbstraction().getName();
                    Cache.Entry<ExecKey, System> refExecutableEntry = context.getLassoOperations()
                            .getExecutableFromAction(context.getExecutionId(), actionName, refImplId);

                    // that's it
                    System refExecutable = refExecutableEntry.getValue();

                    // FIXME routing: what about workerIds for partitioning stored in implementation?
                    //refExecutable.getImplementation().getWorkerNodeId();

                    if (LOG.isInfoEnabled()) {
                        LOG.info("Merging reference implementation '{}' into abstraction '{}'", refImplId, otherAbstraction);
                    }

                    // add it
                    executables.getExecutables().add(refExecutable);
                } else {
                    Systems fromExecutables = context.getLassoOperations()
                            .getExecutables(context.getExecutionId(), otherAbstraction, actionName);

                    if (fromExecutables.hasExecutables()) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Merging '{}' implementations of abstraction '{}' into abstraction '{}'",
                                    fromExecutables.getExecutables().size(), otherAbstraction, actionConfiguration.getAbstraction().getName());
                        }

                        executables.getExecutables().addAll(fromExecutables.getExecutables());
                    }
                }
            }

            setExecutables(executables);

            return;
        }

        // no new abstraction required
        Systems existingExecutables = context.getLassoOperations()
                .getExecutables(context.getExecutionId(), actionConfiguration.getAbstraction().getName(), actionConfiguration.getDependsOn());

        // only if no abstraction is created
        if(StringUtils.isBlank(abstractionName)) {
            // now fetch implementation from other abstraction(s)

            for(String otherAbstraction : from.keySet()) {
                String actionName = from.get(otherAbstraction);

                if (referenceImplementationOnly) {
                    // obtain reference implementation
                    String refImplId = actionConfiguration.getAbstraction().getName();
                    Cache.Entry<ExecKey, System> refExecutableEntry = context.getLassoOperations()
                            .getExecutableFromAction(context.getExecutionId(), actionName, refImplId);

                    // that's it
                    System refExecutable = refExecutableEntry.getValue();

                    // FIXME routing: what about workerIds for partitioning stored in implementation?
                    //refExecutable.getImplementation().getWorkerNodeId();

                    if (LOG.isInfoEnabled()) {
                        LOG.info("Merging reference implementation '{}' into abstraction '{}'", refImplId, refImplId);
                    }

                    // add it
                    existingExecutables.getExecutables().add(refExecutable);
                } else {
                    Systems fromExecutables = context.getLassoOperations()
                            .getExecutables(context.getExecutionId(), otherAbstraction, actionName);

                    if (fromExecutables.hasExecutables()) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Merging '{}' implementations of abstraction '{}' into abstraction '{}'",
                                    fromExecutables.getExecutables().size(), otherAbstraction, existingExecutables.getAbstractionName());
                        }

                        existingExecutables.getExecutables().addAll(fromExecutables.getExecutables());
                    }
                }
            }
        }

        setExecutables(existingExecutables);
    }
}
