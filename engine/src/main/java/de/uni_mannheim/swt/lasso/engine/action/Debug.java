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

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A LASSO action which prints some debug information.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "A LASSO action which prints some debug information")
@Stable
public class Debug extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Debug.class);

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        LOG.info("Abstraction = {}", abstraction.getName());
        LOG.info("Systems = {}", abstraction.getImplementations().size());
        for(System implementation : abstraction.getImplementations()) {
            LOG.info(">> System = {}, {}", implementation.getId(), implementation.getCode().toFQName());
        }

        setExecutables(Systems.fromAbstraction(abstraction, getName()));
    }
}
