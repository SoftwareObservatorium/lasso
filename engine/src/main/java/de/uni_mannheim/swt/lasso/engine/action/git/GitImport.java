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
package de.uni_mannheim.swt.lasso.engine.action.git;

import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Git clone
 * mvn package (or deploy)
 * index
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Import Git repositories, build and index them")
@Stable
@Local // currently a local action that is not distributed
public class GitImport extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(GitImport.class);

    private static final String SETTINGS_TEMPLATE_XML = "/mavenizer/settings_git.xml";

    public static List<String> MAVEN_DEFAULT_COMMAND = Arrays.asList("mvn", "-B",
            "-fn",
            "-Dmaven.test.failure.ignore=true");

    @LassoInput(desc = "Repositories", optional = false)
    public Map<String, String> repositories;

    @LassoInput(desc = "deploy package to LASSO Nexus", optional = true)
    public boolean deploy = true;

    @LassoInput(desc = "Maven Repository for Deployment", optional = true)
    public String repoUrl;
    @LassoInput(desc = "Maven Repository Id for Deployment", optional = true)
    public String repoId;

    /**
     * One Abstraction per repositories (holds all classes)
     *
     * @param context
     * @param actionConfiguration
     * @return
     * @throws IOException
     */
    @Override
    public List<Abstraction> createAbstractions(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing createAbstractions in "+  this.getClass());
        }

        // create manager
        MavenProjectManager manager = new MavenProjectManager(context);

        // 1. git clone
        List<Abstraction> abstractions = gitClone(context, actionConfiguration, manager);

        // 2. mvn package
        doPackage(context, actionConfiguration, manager, abstractions);

        // 3. run analyzer and index (store results in database)
        doAnalyzeAndStore(context, actionConfiguration, manager, abstractions);

        return abstractions;
    }

    List<Abstraction> gitClone(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager) {
        //
        File projectRoot = context.getWorkspace().getRoot();

        // args passed
        List<String> args = new ArrayList<>();

        List<Abstraction> abstractions = new ArrayList<>(repositories.size());

        int c = 0;
        for(String k : repositories.keySet()) {
            if(c++ > 0) {
                args.add("&&");
            }
            args.add("git");
            args.add("clone");

            args.add(repositories.get(k));

            // set folder name (= Abstraction folder)
            File abRoot = new File(projectRoot, k);
            abRoot.mkdirs();
            args.add(abRoot.getName()); // just folder name (no absolute path)

            //
            Abstraction abstraction = new Abstraction();
            abstraction.setName(k);
            abstraction.setSystems(new LinkedList<>());
            abstractions.add(abstraction);
        }

        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);

        return abstractions;
    }

    void doPackage(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, List<Abstraction> abstractions) {
        for(Abstraction abstraction : abstractions) { // can be parallelized
            File projectRoot = context.getWorkspace().getRoot(abstraction);

            // write maven settings
            File settingsXml = writeSettings(projectRoot, context);

            // args passed
            List<String> args = defaultArgs("maven_pkg_deploy.txt");
            // we need to change the deployment server to ours (foreign POMs may specify their own or none)
            // see https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html
            String mojo = deploy ? "deploy" : "package";

            if(StringUtils.isBlank(repoUrl)) {
                ExecutableCorpus executableCorpus = context.getConfiguration().getExecutableCorpus();
                repoUrl = executableCorpus.getArtifactRepository().getDeploymentUrl();
                repoId = executableCorpus.getArtifactRepository().getId();
            }

            // FIXME make configurable
            args.addAll(
                    Arrays.asList(
                            "-s", settingsXml.getName(),
                            "-gs", settingsXml.getName(),
                            "-DskipTests",
                            "-Drat.skip=true", // not really necessary, just for this commons-lang example
                            "-DaltDeploymentRepository=" + repoId + "::default::" + repoUrl,
                            "-DaltReleaseDeploymentRepository=" + repoId + "::default::" + repoUrl,
                            "-DaltSnapshotDeploymentRepository=" + repoId + "::default::" + repoUrl,
                            "clean",
                            // TODO also make sure to deploy a source file! (see maven-source-plugin https://maven.apache.org/plugins/maven-source-plugin/usage.html)
                            "source:jar",
                            // "source:test-jar",
                            mojo // also compiles everything
                    ));

            if(LOG.isInfoEnabled()) {
                LOG.info("Packaging '{}' with args '{}'", abstraction.getName(), args);
            }

            //
            ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
            MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
            executionEnvironmentManager.run(mavenExecutionEnvironment);
        }
    }

    private File writeSettings(File projectRoot, LSLExecutionContext context) {
        File settingsXml = new File(projectRoot, "settings_GitImport.xml");
        LOG.info("Writing global settings for Maven to '{}'", settingsXml.getAbsolutePath());

        try {
            MavenProjectManager.writeMavenSettings(
                    MavenProjectManager.getSettingsTemplate(SETTINGS_TEMPLATE_XML), settingsXml, context);

            return settingsXml;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> defaultArgs(String log) {
        List<String> args = new ArrayList<>(MAVEN_DEFAULT_COMMAND);
        args.add("-l");
        args.add(log);

        return args;
    }

    void doAnalyzeAndStore(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, List<Abstraction> abstractions) {
        // mvn indexer-maven-plugin:index

        for(Abstraction abstraction : abstractions) { // can be parallelized
            File projectRoot = context.getWorkspace().getRoot(abstraction);

            // write maven settings
            File settingsXml = writeSettings(projectRoot, context);

            // args passed
            List<String> args = defaultArgs("maven_analyze_store.txt");

            // metadata=key1,value1|key2,value2 etc.
            String metadata = "\"executionId," + context.getExecutionId() + "|" + "action," + getName() + "\"";

            ExecutableCorpus executableCorpus = context.getConfiguration().getExecutableCorpus();
            // FIXME select default ds
            Datasource ds = executableCorpus.getDatasources().get(0);

            String core = StringUtils.substringAfterLast(ds.getHost(), "/");
            String url = StringUtils.substringBeforeLast(ds.getHost(), "/");

            args.addAll(
                    Arrays.asList(
                            "-X",
                            "-s", settingsXml.getName(),
                            "-gs", settingsXml.getName(),
                            "-DskipTests",
                            "-Dindex.url=" + url,
                            "-Dindex.user=" + ds.getUser(),
                            "-Dindex.pass=" + ds.getPass(),
                            "-Dindex.core=" + core,
                            "-Dindex.owner=" + abstraction.getName(),
                            "-Dindex.metadata=" + metadata,
                            "de.uni-mannheim.swt.lasso:indexer-maven-plugin:1.0.0-SNAPSHOT:index"
                    ));

            if(LOG.isInfoEnabled()) {
                LOG.info("Indexing '{}' with args '{}'", abstraction.getName(), args);
            }

            //
            ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
            MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
            executionEnvironmentManager.run(mavenExecutionEnvironment);
        }
    }

    MavenExecutionEnvironment createMavenEnvironment(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, File projectRoot, List<String> args) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();

        // set default commands
        Environment environment = actionConfiguration.getProfile().getEnvironment();
        if(CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        MavenExecutionEnvironment mavenExecutionEnvironment =
                (MavenExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment("maven");

        // set image
        mavenExecutionEnvironment.setImage(environment.getImage());

        mavenExecutionEnvironment.setProjectRoot(context.getWorkspace(), projectRoot);
        mavenExecutionEnvironment.setM2Home(context.getWorkspace(), manager.getM2Home());

        List<String> commands = new LinkedList<>();
        List<List<String>> commandArgsList = new LinkedList<>();
        commandArgsList.add(args);
        for (List<String> commandArgs : commandArgsList) {
            String command = String.join(" ", commandArgs);
            commands.add(command);
        }

        mavenExecutionEnvironment.setCommands(commands);

        return mavenExecutionEnvironment;
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        //
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        // FIXME create reports etc.

        // set setExecutables() to be compliant with other actions
        Systems executables = new Systems();
        executables.setExecutables(new LinkedList<>());
        executables.setAbstractionName(actionConfiguration.getAbstraction().getName());
        executables.setActionInstanceId(getInstanceId());
        setExecutables(executables);
    }
}
