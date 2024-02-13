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
package de.uni_mannheim.swt.lasso.engine.action.maven.event;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * see lasso-maven-extension.
 *
 * @author Marcus Kessel
 */
public abstract class MavenEventListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenEventListener.class);

    private AtomicBoolean started = new AtomicBoolean(false);
    private AtomicBoolean finished = new AtomicBoolean(false);

    public void fileNotFound() {
//        if(LOG.isWarnEnabled()) {
//            LOG.warn(String.format("File not found '%s'", tailer.getFile()));
//        }
    }

    public void handle(String line) {
        // no CSV row?
        if(StringUtils.indexOf(line, ';') < 0) {
            // do nothing
            return;
        }

        // known event types
            /*
                    ProjectDiscoveryStarted,
        SessionStarted,
        SessionEnded,
        ProjectSkipped,
        ProjectStarted,
        ProjectSucceeded,
        ProjectFailed,
        MojoSkipped,
        MojoStarted,
        MojoSucceeded,
        MojoFailed,
        ForkStarted,
        ForkSucceeded,
        ForkFailed,
        ForkedProjectStarted,
        ForkedProjectSucceeded,
        ForkedProjectFailed;
             */

        try {
            String[] cols = StringUtils.splitPreserveAllTokens(line, ';');

            switch(cols[1]) {
                case "SessionStarted":
                    onMavenStart();

                    break;
                case "SessionEnded":
                    onMavenEnd();

                    break;

                default:
                    // an execution event
                    try {
                        onExecutionEvent(cols[0],cols[1],cols[2],cols[3],cols.length > 4 ? cols[4] : null);
                    } catch (Throwable e) {
                        if(LOG.isWarnEnabled()) {
                            LOG.warn(String.format("Listener has thrown exception for %s", line), e);
                        }
                    }
            }
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("Cannot read line %s", line), e);
            }
        }
    }

    public void handle(Exception ex) {
//        if(LOG.isWarnEnabled()) {
//            LOG.warn(String.format("Handled exception for '%s'", tailer.getFile()), ex);
//        }
    }

    public abstract void onExecutionEvent(String time, String eventType, String id, String mojo, String cause);

    /**
     * <pre>
     *     10:14:38;SessionStarted;;;
     * </pre>
     */
    public void onMavenStart() {
        //
        if(LOG.isInfoEnabled()) {
            LOG.info("Maven execution started");
        }

        this.started.set(true);
    }

    /**
     * EOF
     *
     * <pre>
     *     10:14:38;SessionEnded;;;
     * </pre>
     */
    public void onMavenEnd() {
        //
        if(LOG.isInfoEnabled()) {
            LOG.info("Maven execution ended");
        }

        this.finished.set(true);
    }

    public boolean isFinished() {
        return finished.get();
    }

    public boolean isStarted() {
        return started.get();
    }
}
