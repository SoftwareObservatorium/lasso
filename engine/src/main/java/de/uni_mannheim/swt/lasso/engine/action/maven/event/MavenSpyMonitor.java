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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport;

import java.io.File;

/**
 * Reads maven logs in realtime using tailing.
 *
 * @author Marcus Kessel
 */
public class MavenSpyMonitor {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenSpyMonitor.class);

    private static final String LASSO_MAVEN_SPY = "lasso_spy_%s.csv";
    private static final long DELAY_MILLIS = 1000L;
    private static final String OPTIONS = "-F -n +1 --retry";

    private MavenEventListener mavenProjectListener;

    private File csvFile;

    private TailCmd tailer;

    public void init(File root, MavenEventListener mavenProjectListener) {
        long millis = System.currentTimeMillis();

        this.csvFile = new File(root, String.format(LASSO_MAVEN_SPY, millis));

        this.mavenProjectListener = mavenProjectListener;

        tailer = new TailCmd() {

            @Override
            protected boolean restartRequired() {
                // we don't want a restart of the tailer command

                // stop tailer
                MavenSpyMonitor.this.stop();

                return false;
            }

            @Override
            protected void send(String line) {
                mavenProjectListener.handle(line);
            }
        };
        tailer.setOptions(OPTIONS);
        tailer.setFile(csvFile);

        // set event handler for publishing events (i.e warnings, errors etc.)
        tailer.setApplicationEventPublisher(event -> {
            FileTailingMessageProducerSupport.FileTailingEvent tailingEvent = (FileTailingMessageProducerSupport.FileTailingEvent) event;

            if(LOG.isWarnEnabled()) {
                LOG.warn("Received Tail event => {}", tailingEvent.toString());
            }

            String msg = tailingEvent.getMessage();
            if(StringUtils.endsWith(msg, "has become inaccessible: No such file or directory")) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Stopping tailer because of undesired event => {}", tailingEvent.toString());
                }

                // stop tailer
                MavenSpyMonitor.this.stop();
            }
        });
    }

    public void start() {
        if(this.tailer != null) {
            this.tailer.start();
        }
    }

    public void stop() {
        if(this.tailer != null) {
            this.tailer.stop();
        }
    }

    public File getCsvFile() {
        return csvFile;
    }
}
