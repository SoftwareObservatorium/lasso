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
package de.uni_mannheim.swt.lasso.engine.action.maven;

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.Environment;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.MavenEventListener;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.MavenSpyMonitor;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.DefaultMavenActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * An abstract Maven action. Extension point for everything Maven.
 *
 * @author Marcus Kessel
 */
// TODO add generic maven goal action
public abstract class MavenAction extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenAction.class);

    protected static final String MAVEN_LOG_TXT = "maven_log.txt";

    public static List<String> MAVEN_DEFAULT_COMMAND = Arrays.asList("mvn", "-B",
            // logging
            "-l", MavenProjectManager.MAVEN_LOG, "-fn",
            "-Dmaven.test.failure.ignore=true");

    /**
     * FIXME still duplication issue if more than one abstraction is executed by this action (?)
     */
    protected List<String> mavenDefaultCommand = new LinkedList<>(MAVEN_DEFAULT_COMMAND);

    // runtime environment
    protected MavenExecutionEnvironment mavenExecutionEnvironment;
    protected DefaultMavenActionExecutionListener testListener;

    protected MavenSpyMonitor mavenSpyMonitor = null;

    /**
     * Overrides default number of parallel threads for Maven batch.
     *
     * @param context
     * @return
     */
    protected String getMavenParallelThreads(LSLExecutionContext context) {
        return context.getConfiguration().getProperty("batch.maven.threads", String.class);
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        try {
            // configure maven
            String mavenThreads = getMavenParallelThreads(context);
            mavenDefaultCommand.addAll(Arrays.asList("-T", mavenThreads));
            // max memory per Maven instance
            String opts = context.getConfiguration().getProperty("batch.maven.opts", String.class);
            if(StringUtils.isNotBlank(opts)) {
                // e.g. export MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=128m'
                mavenDefaultCommand.addAll(0, Arrays.asList(String.format("export MAVEN_OPTS='%s'", opts), "&&"));
            }

            // also fires action listener ..
            testListener = createListener(context, actionConfiguration);

            mavenSpyMonitor = setupMavenSpy(actionConfiguration, context.getWorkspace(), testListener);

            // commands to run
            List<String> commands = createMavenCommand(newMavenCommand(mavenSpyMonitor));

            // set default commands
            Environment environment = actionConfiguration.getProfile().getEnvironment();
            if(CollectionUtils.isEmpty(environment.getCommandArgsList())) {
                environment.setCommandArgsList(new LinkedList<>());
            }

            environment.getCommandArgsList().add(commands);

            // create manager
            MavenProjectManager manager = createManager(context, actionConfiguration);

            //-- sanity checks

            // no candidates?
            if(getExecutables() == null || !getExecutables().hasExecutables()) {
                throw new IOException(
                        String.format("No executables for execution. Skipping execution for abstraction '%s'",
                                actionConfiguration.getAbstraction().getName()));
            }

            // start Maven spy monitor to get Maven events
            mavenSpyMonitor.start();

            // prepare run / commands
            mavenExecutionEnvironment = manager.runArgs(getInstanceId(), actionConfiguration.getAbstraction(), environment);

            // run (blocks) until container finishes (or dies)
            ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
            executionEnvironmentManager.run(mavenExecutionEnvironment);

            // block until listener has finished
            long timeoutMillis = 30 * 1000L; // grant 30 secs
            long waitingTimeStart = 0L;
            while(!testListener.isStarted()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Waiting for Maven to start. '{}' millis elapsed", waitingTimeStart);
                }

                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Start Waiter thread failed", e);
                }

                waitingTimeStart += 500L;

                if(waitingTimeStart > timeoutMillis) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Maven start took to long. Returning.");
                    }

                    throw new IOException("Maven start took to long. Returning.");
                }
            }

            // wait until tailer process catches up + collector has finished (needs a bit of time after container has finished)
            long waitingTimeEnd = 0L;
            while(!testListener.isFinished()) {
                // wait 5mins (since expensive collection processes may be going on)
                if(waitingTimeEnd > (5 * 60 * 1000L)) {
                    if(!testListener.isFinished()) {
                        testListener.onMavenEnd();
                    }

                    LOG.warn("Waited 5 mins. Container process already finished, but Maven process did not succeed.");

                    break;
                }

                if(LOG.isDebugEnabled()) {
                    LOG.debug("waiting Maven to end. '{}' millis elapsed", waitingTimeEnd);
                }

                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    throw new RuntimeException("End Waiter thread failed", e);
                }

                waitingTimeEnd += 500L;
            }
        } finally {
            // make sure that everything is set to finished
            doEndMaven();
        }
    }

    /**
     * Stop execution environment NOW.
     */
    public void stopNow() {
        if(mavenExecutionEnvironment != null) {
            try {
                mavenExecutionEnvironment.kill();
            } catch (Throwable e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Killing maven execution environment failed", e);
                }
            }
        }

        // end
        doEndMaven();
    }

    /**
     * Set flag to end maven execution manually.
     */
    protected void doEndMaven() {
        if(testListener != null && !testListener.isFinished()) {
            try {
                // signal end to listener
                testListener.onMavenEnd();
            } catch (Throwable e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Ending maven execution environment failed", e);
                }
            }
        }

        if(mavenSpyMonitor != null) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Stopping tailer process");
            }

            try {
                mavenSpyMonitor.stop();
            } catch (Throwable e) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Stopping tailer process failed", e);
                }
            }
        }
    }

    protected abstract MavenProjectManager createManager(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException;

    protected abstract DefaultMavenActionExecutionListener createListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException;

    protected List<String> newMavenCommand(MavenSpyMonitor mavenSpyMonitor) {
        List<String> args = new LinkedList<>(mavenDefaultCommand);

        // add spy
        if(mavenSpyMonitor != null) {
            File mavenSpyCsv = mavenSpyMonitor.getCsvFile();
            String mavenSpyArg = String.format("-DlassoSpy.output=%s", mavenSpyCsv.getName());
            args.add(mavenSpyArg);
        }

        return args;
    }

    /**
     * E.g mavenDefaultCommand.addAll(Arrays.asList("dependency:tree", "-DoutputFile=deps.txt", "-DoutputType=tgf"));
     *
     * @param mavenDefaultCommand
     * @return
     */
    protected abstract List<String> createMavenCommand(List<String> mavenDefaultCommand);

    protected MavenSpyMonitor setupMavenSpy(ActionConfiguration configuration, Workspace workspace, MavenEventListener listener) {
        File root = workspace.getRoot(getInstanceId(), configuration.getAbstraction());

        MavenSpyMonitor mavenSpy = new MavenSpyMonitor();
        mavenSpy.init(root, listener);

        return mavenSpy;
    }

    /**
     * Get underlying log file
     *
     * @param configuration
     * @param workspace
     * @return
     */
    protected File getLogFile(ActionConfiguration configuration, Workspace workspace) {
        File root = workspace.getRoot(getInstanceId(), configuration.getAbstraction());
        File logFile = new File(root, MAVEN_LOG_TXT);

        return logFile;
    }
}
