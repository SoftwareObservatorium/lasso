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
package de.uni_mannheim.swt.lasso.sheets.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PreDestroy;

/**
 * Shutdown manager.
 *
 * @author Marcus Kessel
 *
 */
public class ShutdownManager {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownManager.class);

    @Autowired
    ApplicationContext applicationContext;

    @PreDestroy
    public void onDestroy() throws Exception {
        if(LOG.isInfoEnabled()) {
            LOG.info("Friendly shutdown of services initiated");
        }

        // shutdown existing jobs
        // FIXME
    }
}
