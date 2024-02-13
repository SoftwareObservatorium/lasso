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

/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * A file tailing message producer that delegates to the OS tail program.
 * This is likely the most efficient mechanism on platforms that support it.
 * Default options are "-F -n 0" (follow file name, no existing records).
 *
 * @author Gary Russell
 * @author Gavin Gray
 * @author Ali Shahbour
 * @since 3.0
 *
 */
public class TailCmd extends FileTailingMessageProducerSupport
        implements SchedulingAwareRunnable {

    private static final Logger LOG = LoggerFactory
            .getLogger(TailCmd.class);

    private volatile Process nativeTailProcess;

    private volatile String options = "-F -n 0";

    private volatile String command = "ADAPTER_NOT_INITIALIZED";

    private volatile boolean enableStatusReader = true;

    private volatile BufferedReader stdOutReader;

    public void setOptions(String options) {
        if (options == null) {
            this.options = "";
        }
        else {
            this.options = options;
        }
    }

    /**
     * If false, thread for capturing stderr will not be started
     * and stderr output will be ignored
     * @param enableStatusReader true or false
     * @since 4.3.6
     */
    public void setEnableStatusReader(boolean enableStatusReader) {
        this.enableStatusReader = enableStatusReader;
    }

    public String getCommand() {
        return this.command;
    }

    @Override
    public String getComponentType() {
        return super.getComponentType() + " (native)";
    }

    @Override
    public boolean isLongLived() {
        return true;
    }

    @Override
    protected void onInit() {
        Assert.notNull(getFile(), "File cannot be null");
        super.onInit();
    }

    @Override
    protected void doStart() {
        super.doStart();
        destroyProcess();
        this.command = getTailCmd() + " " + this.options + " " + this.getFile().getAbsolutePath();
        this.getTaskExecutor().execute(this::runExec);
    }

    /**
     * Override to manually set tail cmd.
     *
     * @return tail command
     */
    public String getTailCmd() {
        if(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Detected MacOSX. Using 'gtail'");
            }

            return "gtail";
        } else if(SystemUtils.IS_OS_LINUX) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Detected Linux. Using 'tail'");
            }

            return "tail";
        } else {
            throw new IllegalArgumentException("Unsupported operation system");
        }
    }

    @Override
    protected void doStop() {
        super.doStop();
        destroyProcess();
    }

    private void destroyProcess() {
        Process process = this.nativeTailProcess;
        if (process != null) {
            process.destroy();
            this.nativeTailProcess = null;
        }
    }

    /**
     * Exec the native tail process.
     */
    private void runExec() {
        this.destroyProcess();
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting tail process");
        }
        try {
            Process process = Runtime.getRuntime().exec(this.command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.nativeTailProcess = process;
            this.startProcessMonitor();
            if (this.enableStatusReader) {
                startStatusReader();
            }
            this.stdOutReader = reader;
            this.getTaskExecutor().execute(this);
        }
        catch (IOException e) {
            throw new MessagingException("Failed to exec tail command: '" + this.command + "'", e);
        }
    }


    /**
     * Runs a thread that waits for the Process result.
     */
    private void startProcessMonitor() {
        this.getTaskExecutor().execute(() -> {
            Process process = TailCmd.this.nativeTailProcess;
            if (process == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Process destroyed before starting process monitor");
                }
                return;
            }

            int result = Integer.MIN_VALUE;
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Monitoring process " + process);
                }
                result = process.waitFor();
                if (LOG.isInfoEnabled()) {
                    LOG.info("tail process terminated with value " + result);
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Interrupted - stopping adapter", e);
                stop();
            }
            finally {
                destroyProcess();
            }
            if (isRunning()) {
                // we don't want a restart, since a NPE is thrown
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Tailer process terminated abnormally.");
                }

                if(restartRequired()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Restarting tail process in " + getMissingFileDelay() + " milliseconds");
                    }

                    getTaskScheduler()
                        .schedule(this::runExec, new Date(System.currentTimeMillis() + getMissingFileDelay()));
                } else {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("No tailer restart allowed.");
                    }
                }
            }
        });
    }

    /**
     *
     * @return signal restart
     */
    protected boolean restartRequired() {
        return false;
    }

    /**
     * Runs a thread that reads stderr - on some platforms status messages
     * (file not available, rotations etc) are sent to stderr.
     */
    private void startStatusReader() {
        Process process = this.nativeTailProcess;
        if (process == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Process destroyed before starting stderr reader");
            }
            return;
        }
        this.getTaskExecutor().execute(() -> {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String statusMessage;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading stderr");
            }
            try {
                while ((statusMessage = errorReader.readLine()) != null) {
                    publish(statusMessage);
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(statusMessage);
                    }
                }
            }
            catch (IOException e1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exception on tail error reader", e1);
                }
            }
            finally {
                try {
                    errorReader.close();
                }
                catch (IOException e2) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Exception while closing stderr", e2);
                    }
                }
            }
        });
    }

    /**
     * Reads lines from stdout and sends in a message to the output channel.
     */
    @Override
    public void run() {
        String line;
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading stdout");
            }
            while ((line = this.stdOutReader.readLine()) != null) {
                this.send(line);
            }
        }
        catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception on tail reader", e);
            }
            try {
                this.stdOutReader.close();
            }
            catch (IOException e1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exception while closing stdout", e);
                }
            }
            this.destroyProcess();
        }
    }

}

