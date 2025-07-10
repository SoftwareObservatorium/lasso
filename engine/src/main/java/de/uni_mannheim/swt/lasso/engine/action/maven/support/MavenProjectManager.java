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
package de.uni_mannheim.swt.lasso.engine.action.maven.support;

import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.corpus.ArtifactRepository;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.datasource.maven.build.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.action.arena.ProjectManager;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import de.uni_mannheim.swt.lasso.engine.action.maven.support.dependency.DependencyAnalyzer;

/**
 * Utilities to manage maven projects.
 *
 * @author Marcus Kessel
 */
public class MavenProjectManager implements ProjectManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenProjectManager.class);

    public static final String MAVEN_LOG = "maven_log.txt";

    private static final String SETTINGS_TEMPLATE_XML = "/mavenizer/settings.xml";
    private static final String EXTENSIONS_TEMPLATE_XML = "/mavenizer/extensions.xml";

    private final LSLExecutionContext lslExecutionContext;
    private final Workspace workspace;

    private final File m2Home;

    public File getM2Repository() {
        return m2Repository;
    }

    private final File m2Repository;

    private boolean useGlobalRepository = true;

    public MavenProjectManager(LSLExecutionContext lslExecutionContext) throws IOException {
        Validate.notNull(lslExecutionContext, "%s cannot be null", LSLExecutionContext.class);

        this.lslExecutionContext = lslExecutionContext;
        this.workspace = lslExecutionContext.getWorkspace();

        // .m2/
        if (isUseGlobalRepository()) {
            // global
            this.m2Home = workspace.createGlobalLassoDirectory("repository");
        } else {
            // inside workspace
            this.m2Home = workspace.createDirectory("repository");
        }

        // .m2/repository
        this.m2Repository = new File(this.m2Home, "repository");

        // copy global Maven settings.xml
        File settingsXml = new File(this.m2Home, "settings.xml");
        if (!settingsXml.exists()) {
            writeMavenSettings(getSettingsTemplate(SETTINGS_TEMPLATE_XML), settingsXml, lslExecutionContext);
        }
    }

    public static void writeMavenSettings(String settingsTemplate, File settingsXml, LSLExecutionContext context) throws IOException {
        // replace placeholders
        ExecutableCorpus executableCorpus = context.getConfiguration().getExecutableCorpus();
        ArtifactRepository artifactRepository = executableCorpus.getArtifactRepository();

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("repoId", artifactRepository.getId());
        valueMap.put("repoUser", artifactRepository.getUser());
        valueMap.put("repoPass", artifactRepository.getPass());
        valueMap.put("repoUrl", artifactRepository.getUrl());

        String settingsSource = StrSubstitutor.replace(settingsTemplate, valueMap);

        FileUtils.writeStringToFile(settingsXml,
                settingsSource, "UTF-8");
    }

    /**
     * Load settings template
     *
     * @param name
     * @return
     * @throws RuntimeException
     */
    public synchronized static String getSettingsTemplate(String name) {
        try {
            return IOUtils.toString(Mavenizer.class.getResourceAsStream(name));
        } catch (IOException e) {
            throw new RuntimeException("Cannot find settings template at " + name, e);
        }
    }

    /**
     * Load extensions template
     *
     * @return
     * @throws RuntimeException
     */
    public synchronized static String getExtensionsTemplate() {
        try {
            return IOUtils.toString(Mavenizer.class.getResourceAsStream(EXTENSIONS_TEMPLATE_XML));
        } catch (IOException e) {
            throw new RuntimeException("Cannot find extensions template at " + EXTENSIONS_TEMPLATE_XML, e);
        }
    }

    public Systems initNew(Action action, String actionInstanceId, Abstraction abstraction, String pomTemplate, ProjectManager.ProjectSettingsHandler mavenProjectPomHandler, ProjectManager.ExecutableFilter executableFilter) throws IOException {
        //
        File abstractionRoot = workspace.createDirectory(abstraction);

        // init other stuff
        Map<String, String> mvnOptions = new HashMap<>();

        Mavenizer mavenizer = new Mavenizer(abstractionRoot, mvnOptions);

        List<CodeUnit> impls = abstraction.getImplementations().stream().map(System::getCode).toList();

        List<System> execs = new ArrayList<>();

        // XXX remove duplicates
        LassoUtils.findDuplicates(abstraction, false);

        for (CodeUnit implementation : impls) {
            Candidate candidate = new Candidate();
            // set id
            candidate.setId(implementation.getId());
            // set candidate class
            CompilationUnit cunit = new CompilationUnit();
            cunit.setName(implementation.getName());
            cunit.setPkg(implementation.getPackagename());
            candidate.setCompilationUnit(cunit);

            // artifact
            MavenArtifact artifact = new MavenArtifact();
            artifact.setGroupId(implementation.getGroupId());
            artifact.setArtifactId(implementation.getArtifactId());
            artifact.setVersion(implementation.getVersion());
            candidate.setArtifact(artifact);

            try {
                // try to resolve missing dependencies
                DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer();
                // sets resolved deps to
                List<Artifact> resolvedDependencies = dependencyAnalyzer.resolveMissingDependencies(implementation);
                if (CollectionUtils.isNotEmpty(resolvedDependencies)) {
                    candidate.setResolvedDependencies(resolvedDependencies);
                }
            } catch (Throwable e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to resolve missing dependencies for " + candidate.getId(), e);
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Initializing build for candidate " + candidate.getId());
            }

            // add bytecodename
            Map<String, Object> valueMap = new HashMap<>();

            // call before creation
            try {
                mavenProjectPomHandler.onFillTemplate(implementation, candidate, valueMap);
            } catch (Throwable e) {
                LOG.warn("onFillTemplate failed for {}", implementation.getId());
                LOG.warn("Exception", e);

                continue;
            }

            // mavenize, setup project
            MavenProject mavenProject = null;
            try {
                mavenProject = mavenizer.createMavenProject(lslExecutionContext, actionInstanceId,
                        candidate, true, pomTemplate, valueMap);
            } catch (IOException e) {
                //throw new IOException(String.format("Could not create maven project for %s", candidate.getId()), e);

                LOG.warn("createMavenProject failed for {}", implementation.getId());
                LOG.warn("Exception", e);

                continue;
            }
            // set repository
            mavenProject.setArtifactRepository(m2Repository);

            System executable = new System(implementation, mavenProject);

            //
            try {
                if (executableFilter.accept(executable)) {
                    execs.add(executable);
                } else {
                    // FIXME remove project
                }
            } catch (Throwable e) {
                LOG.warn("executableFilter failed for {}", implementation.getId());
                LOG.warn("Exception", e);

                continue;
            }
        }

        // create aggregated pom
        MavenProject aggregatedMavenProject = mavenizer
                .createAggregatedMavenProject(actionInstanceId, new ExecGroup(execs));

        // add lasso maven spy extension
        File mvnExtensions = new File(aggregatedMavenProject.getBaseDir(), ".mvn");
        if (!mvnExtensions.exists()) {
            mvnExtensions.mkdirs();

            FileUtils.writeStringToFile(new File(mvnExtensions, "extensions.xml"),
                    getExtensionsTemplate(), "UTF-8");
        }

        Systems executables = new Systems();
        executables.setAbstractionName(abstraction.getName());
        executables.setExecutables(execs);
        executables.setActionInstanceId(actionInstanceId);
        executables.setSpecification(abstraction.getSpecification());

        return executables;
    }

    public MavenExecutionEnvironment runArgs(String actionInstanceId, Abstraction abstraction, Environment environment) {
        //
        File projectsRoot = workspace.getRoot(actionInstanceId, abstraction);

        //
        ExecutionEnvironmentManager executionEnvironmentManager = lslExecutionContext.getExecutionEnvironmentManager();

        MavenExecutionEnvironment mavenExecutionEnvironment =
                (MavenExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment("maven");

        // set image
        mavenExecutionEnvironment.setImage(environment.getImage());

        mavenExecutionEnvironment.setProjectRoot(workspace, projectsRoot);
        mavenExecutionEnvironment.setM2Home(workspace, m2Home);

        List<String> commands = new LinkedList<>();
        List<List<String>> commandArgsList = environment.getCommandArgsList();
        for (List<String> commandArgs : commandArgsList) {
            String command = String.join(" ", commandArgs);
            commands.add(command);
        }

        mavenExecutionEnvironment.setCommands(commands);

        return mavenExecutionEnvironment;
    }

    public LSLExecutionContext getLslExecutionContext() {
        return lslExecutionContext;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public boolean isUseGlobalRepository() {
        return useGlobalRepository;
    }

    public void setUseGlobalRepository(boolean useGlobalRepository) {
        this.useGlobalRepository = useGlobalRepository;
    }

    public File getM2Home() {
        return m2Home;
    }
}
