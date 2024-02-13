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

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
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
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.Systems;
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
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Unpack artifact to local project (both classes and sources).")
@Stable
public class Unpack extends MavenAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Unpack.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_unpack.template");

    @LassoInput(desc = "Unpack classes to ?", optional = true)
    public String classes = "target/classes";

    @LassoInput(desc = "Unpack sources to ?", optional = true)
    public String sources = "src/main/java";

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
                            // for methods only
                            if(implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                                // limit permutations to method signature only
                                valueMap.put("bytecodename", implementation.getBytecodeName());
                            } else {
                                valueMap.put("bytecodename", "");
                            }

                            // dep info
                            valueMap.put("candidate.groupId", implementation.getGroupId());
                            valueMap.put("candidate.artifactId", implementation.getArtifactId());
                            valueMap.put("candidate.version", implementation.getVersion());

                            // unpack to directories
                            valueMap.put("candidate.classes", classes);
                            valueMap.put("candidate.sources", sources);
                        },
                        executable -> true);

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
        testListener.setAllowedMojos(Arrays.asList("maven-dependency-plugin:unpack"));

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

        if(LOG.isInfoEnabled()) {
            LOG.info("Found '{}' classes for '{}'", classes.size(), executable.getId());
            LOG.info("Found '{}' sources for '{}'", sources.size(), executable.getId());
        }
    }

    @Override
    public List<RecordCollector> createCollectors() {
        return new LinkedList<>();
    }

    protected List<String> createMavenCommand(List<String> mavenDefaultCommand) {
        mavenDefaultCommand.addAll(
                Arrays.asList(
                        "clean",
                        "package" // dependency:unpack is bound to package goal
                        // more goals
                ));

        return mavenDefaultCommand;
    }
}
