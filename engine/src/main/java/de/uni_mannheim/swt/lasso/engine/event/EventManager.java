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
package de.uni_mannheim.swt.lasso.engine.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author mkessel
 */
public class EventManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(EventManager.class);

    private List<EventListener> listeners = Collections.synchronizedList(new LinkedList<>());

    public void fireKillAction(String executionId, String name) {
        listeners.forEach(l -> {
            try {
                l.onKillAction(executionId, name);
            } catch (Throwable e) {
                LOG.warn("onKillAction failed for listener", e);
            }
        });
    }

    public void addListener(EventListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        this.listeners.remove(listener);
    }
}
