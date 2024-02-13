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
package de.uni_mannheim.swt.lasso.maven;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.Properties;

/**
 * @author Marcus Kessel
 * @see <a href="https://maven.apache.org/examples/maven-3-lifecycle-extensions.html">Maven Extensions Lifecycle</a>
 * @see <a href="https://maven.apache.org/guides/mini/guide-using-extensions.html">Maven Extensions Guide</a>
 */
@Component(role = EventSpy.class)
public class LassoEventSpy extends AbstractEventSpy {

    @Requirement
    private Logger logger;

    private EventStream eventStream;

    @Override
    public void init(Context context) throws Exception {
        super.init(context);
        logger.info("Lasso EventSpy is registered.");
    }

    @Override
    public void close() throws Exception {
        super.close();

        // close stream
        if(eventStream != null) {
            eventStream.close();
        }
    }

    /**
     * <pre>
     *     mvn -DlassoSpy.output=/tmp/mystream.csv install
     * </pre>
     *
     * @param event
     * @throws Exception
     */
    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;

            MavenProject mavenProject = executionEvent.getProject();
            MavenSession mavenSession = executionEvent.getSession();

            if(eventStream == null) {
                Properties properties = mavenSession.getSystemProperties();
                String outputFile = properties.getProperty("lassoSpy.output");

                // do nothing
                if(outputFile == null) {
                    logger.warn("No output set for Lasso Spy. Doing nothing.");

                    return;
                }

                eventStream = new EventStream(outputFile);
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

            // MavenProject is null for these types
            if (executionEvent.getType() == ExecutionEvent.Type.SessionStarted || executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                logger.info(String.format("Lasso Spy: Session started/ended with output stream to %s",
                        eventStream.getFilename()));

                // write to stream
                eventStream.addEvent(new String[]{executionEvent.getType().toString(), "",
                        "", ""});
            } else {
                String mojoName = "";
                String mojoCause = "";
                // Mojo* related descriptor
                if(executionEvent.getMojoExecution() != null) {
                    //
                    MojoExecution mojoExecution = executionEvent.getMojoExecution();

                    mojoName = String.format("%s:%s/%s",
                            mojoExecution.getArtifactId(),
                            mojoExecution.getGoal(),
                            mojoExecution.getExecutionId());

                    logger.debug(String.format("Lasso Spy Mojo execution: %s for project %s with props %s => mojo %s",
                            executionEvent.getType(), mavenProject.getArtifactId(), eventStream.getFilename(), mojoName));

                    if(executionEvent instanceof MojoExecutionEvent) {
                        MojoExecutionEvent mojoExecutionEvent = (MojoExecutionEvent) executionEvent;
                        if(mojoExecutionEvent.getCause() != null) {
                            mojoCause = mojoExecutionEvent.getCause().getMessage();
                        }
                    }
                }

                // write to stream
                eventStream.addEvent(new String[]{executionEvent.getType().toString(), mavenProject.getArtifactId(),
                        mojoName, mojoCause});

                logger.debug(String.format("Lasso Spy: %s for project %s with props %s",
                        executionEvent.getType(), mavenProject.getArtifactId(), eventStream.getFilename()));
            }
        }
    }
}
