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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.helper;

import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger bridge for JS over slf4j
 * 
 * @author Marcus Kessel
 *
 */
public class Slf4JConsole extends ScriptableObject {

    private static final Logger LOG = LoggerFactory
            .getLogger(Slf4JConsole.class);

    /**
     * Log to {@link Logger}.
     * 
     * @param msg
     *            Message
     */
    public void jsFunction_log(String msg) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassName() {
        return Slf4JConsole.class.getSimpleName();
    }
}
