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

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 *
 */
public abstract class FilteredMavenEventListener extends MavenEventListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(FilteredMavenEventListener.class);

    private List<String> allowedEvents;
    private List<String> allowedMojos;

    private Map<String, String> times = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, Long> durations = Collections.synchronizedMap(new LinkedHashMap<>());


    /**
     * <pre>
     *     myMavenBuilds_469665f1-a527-43aa-ae4e-e561f7cecd32 is not an implementation module
     * </pre>
     * @param id
     * @return
     */
    protected boolean isImplementationModule(String id) {
        return StringUtils.indexOf(id, '_') < 0;
    }

    @Override
    public void onExecutionEvent(String time, String eventType, String id, String mojo, String cause) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("SPY EVENT => '{}'", Arrays.toString(new String[]{time, eventType, id, mojo, cause}));
        }

        if(!isImplementationModule(id)) {
            return;
        }

        // event allowed?
        if(allowedEvents != null && !allowedEvents.contains(eventType)) {
            return;
        }

        // mojo allowed?
        if(StringUtils.isNotEmpty(mojo) && allowedMojos != null) {
            String stripped = StringUtils.substringBefore(mojo, "/");
            if(!allowedMojos.contains(stripped)) {
                return;
            }
        }

        // FIXME failure handling

        // delegate
        /**
         *         ProjectSkipped,
         *         ProjectStarted,
         *         ProjectSucceeded,
         *         ProjectFailed,
         *         MojoSkipped,
         *         MojoStarted,
         *         MojoSucceeded,
         *         MojoFailed,
         */
        try {
            switch(eventType) {
                case "ProjectSkipped":
                    stop(id, time);
                    onProjectSkipped(time, id);

                    break;
                case "ProjectStarted":
                    times.put(id, time);

                    onProjectStarted(time, id);

                    break;
                case "ProjectSucceeded":
                    stop(id, time);
                    onProjectSucceeded(time, id);

                    break;
                case "ProjectFailed":
                    stop(id, time);
                    onProjectFailed(time, id);

                    break;
                case "MojoSkipped":
                    onMojoSkipped(time, id, mojo, cause);

                    break;
                case "MojoStarted":
                    onMojoStarted(time, id, mojo, cause);

                    break;
                case "MojoSucceeded":
                    onMojoSucceeded(time, id, mojo, cause);

                    break;
                case "MojoFailed":
                    onMojoFailed(time, id, mojo, cause);

                    break;
                default:
                    // handle unknown
                    onUnknownEvent(time, eventType, id, mojo, cause);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }



    public void onProjectSkipped(String time, String executableId) {}
    public void onProjectStarted(String time, String executableId) {}
    public void onProjectSucceeded(String time, String executableId) {}
    public void onProjectFailed(String time, String executableId) {}

    public void onMojoSkipped(String time, String executableId, String mojo, String cause) {}
    public void onMojoStarted(String time, String executableId, String mojo, String cause) {}
    public void onMojoSucceeded(String time, String executableId, String mojo, String cause) {}
    public void onMojoFailed(String time, String executableId, String mojo, String cause) {}

    public void onUnknownEvent(String time, String eventType, String executableId, String mojo, String cause) {}

    public List<String> getAllowedEvents() {
        return allowedEvents;
    }

    public void setAllowedEvents(List<String> allowedEvents) {
        this.allowedEvents = allowedEvents;
    }

    public List<String> getAllowedMojos() {
        return allowedMojos;
    }

    public void setAllowedMojos(List<String> allowedMojos) {
        this.allowedMojos = allowedMojos;
    }

    protected void stop(String executableId, String endTime) {
        try {
            String startTime = times.get(executableId);
            LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_TIME);
            LocalTime end = LocalTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_TIME);

            long duration = Duration.between(start, end).get(ChronoUnit.SECONDS);
            if(duration < 0) {
                // add one day
                duration = 86400 + duration;
            }

            durations.put(executableId, duration);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public Map<String, Long> getDurations() {
        return durations;
    }
}
