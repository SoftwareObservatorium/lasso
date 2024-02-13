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
package de.uni_mannheim.swt.lasso.engine.action.task;

import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.event.DefaultMavenActionExecutionListener;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Action for packaging (compiling) source code.
 *
 * Assumption: Source is provided by {@link CodeUnit#getContent()}
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Package (Compile) Source Code")
@Stable
public class Package extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Package.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_package.template");

    @LassoInput(desc = "deploy package to LASSO Nexus", optional = true)
    public boolean deploy = false;

    @Override
    protected MavenProjectManager createManager(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        TestAdaptationManager testAdaptationManager = new TestAdaptationManager(context);

        Systems executables = testAdaptationManager.initNew(this,
                getInstanceId(),
                actionConfiguration.getAbstraction(),
                POM_TEMPLATE,
                (implementation, candidate, valueMap) -> {
                    // FIXME create dep info for deploy
                    valueMap.put("version", implementation.getVersion() + "-SNAPSHOT");
                },
                executable -> {
                    // save source
                    MavenProject targetProject = executable.getProject();

                    LOG.info("Writing source code to target '{}'", targetProject.getBaseDir());
                    try {
                        targetProject.writeCompilationUnit(executable.getCode(), false);
                    } catch (IOException e) {
                        LOG.warn("Writing source code failed", e);

                        return false;
                    }

                    return true;
                });

        Validate.notNull(executables, "Executables are null");

        // set
        setExecutables(executables);

        return testAdaptationManager;
    }

    @Override
    protected DefaultMavenActionExecutionListener createListener(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        // also fires action listener ..
        DefaultMavenActionExecutionListener testListener = new DefaultMavenActionExecutionListener(this);
        testListener.setAllowedEvents(Arrays.asList(
                "ProjectSkipped",
                "ProjectStarted",
                "ProjectSucceeded",
                "ProjectFailed"
        ));

        // FIXME changeme
        //testListener.setAllowedMojos(Arrays.asList("maven-compiler-plugin:compile"));
        //
        testListener.setAllowedMojos(Arrays.asList(" maven-jar-plugin:jar"));

        return testListener;
    }

    @Override
    public Abstraction createAbstraction(LSLExecutionContext context, ActionConfiguration actionConfiguration, AbstractionSpec abstractionSpec) throws IOException {
        return null;
    }

    @Override
    public void postExecute(LSLExecutionContext context, ActionConfiguration actionConfiguration, String executableId, boolean success) {
        System executable = getExecutables().getExecutable(executableId);

        MavenProject project = executable.getProject();

        List<File> classes = project.getFiles(project.getClasses(), "class");
        List<File> sources = project.getFiles(project.getSrcMain(), "java");
        List<File> jars = project.getFiles(project.getTarget(), "jar");

        if(LOG.isInfoEnabled()) {
            LOG.info("Found '{}' classes for '{}'", classes.size(), executable.getId());
            LOG.info("Found '{}' sources for '{}'", sources.size(), executable.getId());
            LOG.info("Found '{}' jars for '{}'", jars.size(), executable.getId());
        }

        // FIXME set maven coordinates
        executable.getCode().setGroupId(Mavenizer.DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS);
        executable.getCode().setArtifactId(executableId);
        executable.getCode().setVersion(executable.getCode().getVersion());
    }

    @Override
    public List<RecordCollector> createCollectors() {
        return new LinkedList<>();
    }

    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        String mojo = deploy ? "deploy" : "package";

        mavenDefaultCommand.addAll(
                Arrays.asList(
                        "-DskipTests",
                        "clean",
                        mojo // also compiles everything
                ));

        return mavenDefaultCommand;
    }
}
