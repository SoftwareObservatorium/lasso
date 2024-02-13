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

import de.uni_mannheim.swt.lasso.cluster.event.SessionEvent;
import de.uni_mannheim.swt.lasso.engine.event.EventListener;
import org.apache.ignite.lang.IgniteBiPredicate;

import java.util.UUID;

/**
 * Session event listener. Lambdas seem not to work in Ignite (serialization issues).
 *
 * @author Marcus Kessel
 */
public class RemoteSessionEventListener implements IgniteBiPredicate<UUID, SessionEvent>, EventListener {

    private final LassoActionRequestEngine lassoActionRequestEngine;

    public RemoteSessionEventListener(LassoActionRequestEngine lassoActionRequestEngine) {
        this.lassoActionRequestEngine = lassoActionRequestEngine;
    }

    @Override
    public boolean apply(UUID uuid, SessionEvent sessionEvent) {
        //
        try {
            if(sessionEvent.isClose()) {
                lassoActionRequestEngine.closeSession(sessionEvent.getExecutionId());
            }

            if(sessionEvent.isKillAction()) {
                onKillAction(sessionEvent.getExecutionId(), sessionEvent.getPayload());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onKillAction(String executionId, String name) {
        lassoActionRequestEngine.killAction(executionId, name);
    }
}
