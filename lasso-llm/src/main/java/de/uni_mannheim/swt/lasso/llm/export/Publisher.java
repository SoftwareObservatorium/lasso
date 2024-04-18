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
package de.uni_mannheim.swt.lasso.llm.export;

import de.uni_mannheim.swt.lasso.core.model.Environment;
import de.uni_mannheim.swt.lasso.corpus.ArtifactRepository;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.llm.problem.Problem;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Simple publisher
 *
 * @author Marcus Kessel
 */
public class Publisher {

    private static final Logger LOG = LoggerFactory
            .getLogger(Publisher.class);

    private static final String SETTINGS_TEMPLATE_XML = "/poms/settings.xml";

    private ExecutableCorpus executableCorpus;

    private String mavenDefaultImage = "maven:3.6.3-openjdk-17";

    private boolean deploy;

    private ExecutionEnvironmentManager executionEnvironmentManager;

    private File m2Home;

    private String mavenThreads = "1";

    public Publisher(ExecutableCorpus executableCorpus, File m2Home) {
        this.m2Home = m2Home;
        this.executableCorpus = executableCorpus;
        initRepo();
        this.executionEnvironmentManager = executionEnvironmentManager();
    }

    private void initRepo() {
        // copy settings.xml new File(this.m2Repository, "settings.xml")
        File settingsXml = new File(this.m2Home, "settings.xml");
        if (!settingsXml.exists()) {
            try {
                // replace placeholders
                ArtifactRepository artifactRepository = executableCorpus.getArtifactRepository();

                Map<String, Object> valueMap = new HashMap<>();
                valueMap.put("repoId", artifactRepository.getId());
                valueMap.put("repoUser", artifactRepository.getUser());
                valueMap.put("repoPass", artifactRepository.getPass());
                valueMap.put("repoUrl", artifactRepository.getUrl());

                String settingsSource = StrSubstitutor.replace(getSettingsTemplate(), valueMap);

                FileUtils.writeStringToFile(new File(this.m2Home, "settings.xml"),
                        settingsSource, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized static String getSettingsTemplate() {
        try {
            return IOUtils.toString(Publisher.class.getResourceAsStream(SETTINGS_TEMPLATE_XML));
        } catch (IOException e) {
            throw new RuntimeException("Cannot find settings template at " + SETTINGS_TEMPLATE_XML, e);
        }
    }

    public List<String> mvnCommand(String logFileName) {
        List<String> mavenDefaultCommand = new LinkedList<>(Arrays.asList("mvn", "-B",
                // logging
                "-l", logFileName, "-fn",
                "-Dmaven.test.failure.ignore=true"));
        mavenDefaultCommand.addAll(Arrays.asList("-T", mavenThreads));

        return mavenDefaultCommand;
    }

    public void doPackage(File projectRoot, Problem problem, String generatorId) {

        // args passed
        List<String> args = new ArrayList<>(mvnCommand("maven_package.log")); // FIXME log is overridden if called multiple times
        // we need to change the deployment server to ours (foreign POMs may specify their own or none)
        // see https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html
        String mojo = deploy ? "deploy" : "package";

        String repoUrl = executableCorpus.getArtifactRepository().getDeploymentUrl();
        String repoId = executableCorpus.getArtifactRepository().getId();

        args.addAll(
                Arrays.asList(
                        "-DskipTests",
                        "-DaltDeploymentRepository=" + repoId + "::default::" + repoUrl,
                        "-DaltReleaseDeploymentRepository=" + repoId + "::default::" + repoUrl,
                        "-DaltSnapshotDeploymentRepository=" + repoId + "::default::" + repoUrl,
                        "clean",
                        // also make sure to deploy a source file! (see maven-source-plugin https://maven.apache.org/plugins/maven-source-plugin/usage.html)
                        "source:jar",
                        mojo // also compiles everything
                ));

        if(LOG.isInfoEnabled()) {
            LOG.info("Packaging '{}/{}' with args '{}'", problem.getName(), generatorId, args);
        }

        //
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);
    }

    public void doAnalyzeAndStore(File projectRoot, Problem problem, String generatorId) {
        // mvn indexer-maven-plugin:index

        // args passed
        List<String> args = new ArrayList<>(mvnCommand("maven_analyze.log"));

        // FIXME select default ds
        Datasource ds = executableCorpus.getDatasources().get(0);

        String core = StringUtils.substringAfterLast(ds.getHost(), "/");
        String url = StringUtils.substringBeforeLast(ds.getHost(), "/");

        //File settingsXml = new File(this.m2Home, "settings.xml");

//        args.addAll(
//                Arrays.asList(
//                        //"-X",
//                        "-DskipTests",
//                        "-Downer=" + problem.getName() + "/" + generatorId,
//                        "-DsolrCore=" + core,
//                        "de.uni-mannheim.swt.lasso:indexer-maven-plugin:1.0.0-SNAPSHOT:index"
//                ));
        args.addAll(
                Arrays.asList(
                        "-X",
//                        "-s", settingsXml.getName(),
//                        "-gs", settingsXml.getName(),
                        "-DskipTests",
                        "-Dindex.url=" + url,
                        "-Dindex.user=" + ds.getUser(),
                        "-Dindex.pass=" + ds.getPass(),
                        "-Dindex.core=" + core,
                        "-Dindex.owner=" + problem.getName() + "/" + generatorId,
                        //"-Dindex.metadata=" + metadata,
                        "de.uni-mannheim.swt.lasso:indexer-maven-plugin:1.0.0-SNAPSHOT:index"
                ));

        if(LOG.isInfoEnabled()) {
            LOG.info("Indexing '{}/{}' with args '{}'", problem.getName(), generatorId, args);
        }

        //
        ExecutionEnvironmentManager executionEnvironmentManager = executionEnvironmentManager();
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);
    }

    MavenExecutionEnvironment createMavenEnvironment(File projectRoot, List<String> args) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = executionEnvironmentManager();

        // set default commands
        Environment environment = new Environment();
        if(CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        MavenExecutionEnvironment mavenExecutionEnvironment =
                (MavenExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment("maven");

        // set image
        mavenExecutionEnvironment.setImage(environment.getImage());

        Workspace workspace = new Workspace();
        // FIXME set roots
        //workspace.setLassoRoot();
        //workspace.setRoot();

        mavenExecutionEnvironment.setProjectRoot(workspace, projectRoot);
        mavenExecutionEnvironment.setM2Home(workspace, m2Home);

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

    protected ExecutionEnvironmentManager executionEnvironmentManager() {
        // populate system properties for docker
        if(System.getProperty("thirdparty.docker.uid") == null) {
            System.setProperty("thirdparty.docker.uid", "1000");
        }

        if(System.getProperty("thirdparty.docker.gid") == null) {
            System.setProperty("thirdparty.docker.gid", "1000");
        }

        String proxyRegistry = "docker.io";
        Integer pullTimeout = 600;

        ExecutionEnvironmentManager executionEnvironmentManager = new ExecutionEnvironmentManager(proxyRegistry, pullTimeout, mavenDefaultImage);

        return executionEnvironmentManager;
    }

    public boolean isDeploy() {
        return deploy;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
    }

    public String getMavenDefaultImage() {
        return mavenDefaultImage;
    }

    public void setMavenDefaultImage(String mavenDefaultImage) {
        this.mavenDefaultImage = mavenDefaultImage;
    }

    public void setMavenThreads(String mavenThreads) {
        this.mavenThreads = mavenThreads;
    }
}
