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

import de.uni_mannheim.swt.lasso.engine.Tester;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Marcus Kessel
 */
public class MavenEventListenerIntegrationTest {

    @Test
    public void test_line() throws IOException {
        File file = Tester.getResourceFile("/testdata/mystream.csv");

        MavenEventListener cut = new MyListener();

        LineIterator lineIterator = FileUtils.lineIterator(file);
        while(lineIterator.hasNext()) {
            cut.handle(lineIterator.nextLine());
        }
    }

    @Test
    public void test_tail() throws InterruptedException, IOException {
        File file = Tester.getResourceFile("/testdata/mystream.csv");

        MavenSpyMonitor mavenSpyMonitor = new MavenSpyMonitor();

        MavenEventListener cut = new MyListener();

        File root = new File("/tmp/");

        mavenSpyMonitor.init(root, cut);

        File csvFile = mavenSpyMonitor.getCsvFile();

        FileUtils.copyFile(file, csvFile);

        mavenSpyMonitor.start();

        while(!cut.isFinished()) {
            Thread.sleep(1 * 1000L);
        }
    }

    @Test
    public void test_evosuite() throws IOException {
        File file = Tester.getResourceFile("/testdata/maven_log/evosuite.csv");

        Map<String, StopWatch> timeKeeper = Collections.synchronizedMap(new LinkedHashMap<>());

        Map<String, String> times = Collections.synchronizedMap(new LinkedHashMap<>());

        DefaultMavenActionExecutionListener cut = new DefaultMavenActionExecutionListener(new DefaultAction() {
        }) {

            void stopTimer(String executableId, String endTime) {
                if(timeKeeper.containsKey(executableId)) {
                    timeKeeper.get(executableId).stop();
                }

                String startTime = times.get(executableId);
                LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_TIME);
                LocalTime end = LocalTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_TIME);

                long duration = Duration.between(start, end).get(ChronoUnit.SECONDS);
                if(duration < 0) {
                    // add one day
                    duration = 86400 + duration;
                }

                System.out.println(executableId + " => " + duration);
            }

            @Override
            public void onProjectSucceeded(String time, String executableId) {
                stopTimer(executableId, time);

                super.onProjectSucceeded(time, executableId);
            }

            @Override
            public void onProjectFailed(String time, String executableId) {
                stopTimer(executableId, time);

                super.onProjectFailed(time, executableId);
            }

            @Override
            public void onProjectSkipped(String time, String executableId) {
                stopTimer(executableId, time);

                super.onProjectSkipped(time, executableId);
            }

            @Override
            public void onProjectStarted(String time, String executableId) {
                times.put(executableId, time);

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                timeKeeper.put(executableId, stopWatch);

//                try {
//                    Thread.sleep(5 * 1000L);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                super.onProjectStarted(time, executableId);
            }
        };

        cut.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));
        cut.setAllowedMojos(Arrays.asList("evosuite-maven-plugin:generate"));

        LineIterator lineIterator = FileUtils.lineIterator(file);
        while(lineIterator.hasNext()) {
            cut.handle(lineIterator.nextLine());
        }

        System.out.println(timeKeeper.size());

        for(String executableId : timeKeeper.keySet())  {
            StopWatch stopWatch = timeKeeper.get(executableId);
            System.out.println(executableId + " => " + stopWatch.getTime(TimeUnit.SECONDS));
        }

        LocalTime start = LocalTime.parse("23:59:00", DateTimeFormatter.ISO_LOCAL_TIME);
        LocalTime end = LocalTime.parse("00:30:00", DateTimeFormatter.ISO_LOCAL_TIME);

        long duration = Duration.between(start, end).get(ChronoUnit.SECONDS);
        System.out.println("testing => " + duration);
        if(duration < 0) {
            // add one day
            duration = 86400 + duration;
        }

        System.out.println("testing => " + duration);
    }

    private static class MyListener extends MavenEventListener {
        @Override
        public void onExecutionEvent(String time, String eventType, String id, String mojo, String cause) {
            System.out.println(String.format("%s %s %s %s %s", time, eventType, id, mojo, cause));
        }
    }
}
