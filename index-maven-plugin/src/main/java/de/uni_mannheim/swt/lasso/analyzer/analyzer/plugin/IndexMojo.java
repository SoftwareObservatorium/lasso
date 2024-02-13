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
package de.uni_mannheim.swt.lasso.analyzer.analyzer.plugin;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Index mojo
 *
 * @author Marcus Kessel
 *
 */
@Mojo(name = "index", requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.NONE)
public class IndexMojo extends AbstractMojo {

    @Parameter(defaultValue = "${plugin.artifacts}", required = true, readonly = true)
    private List<Artifact> artifacts;

    @Parameter(required = true, property = "index.url")
    private String solrUrl;
    @Parameter(required = true, defaultValue = "", property = "index.user")
    private String solrUser;
    @Parameter(required = true, defaultValue = "", property = "index.pass")
    private String solrPass;
    @Parameter(required = true, property = "index.core")
    private String solrCore;

    @Parameter(required = true, defaultValue = "unknown", property = "index.owner")
    private String owner;

    @Parameter(required = true, defaultValue = "", property = "index.metadata")
    private String metadata;

    @Parameter(required = true, defaultValue = "86400")
    private int timeoutInSeconds;

    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
//        // read owner
//        owner = System.getProperty("owner");

        // plugin jars for class path
        List<URL> urls = loadPluginArtifacts();

        getLog().info("Running in normal mode");

        // run
        run(urls);
    }

    private void run(List<URL> urls) throws MojoExecutionException {
        getLog().info("Running in normal mode");

        final List<String> args = buildArgs(urls);

        final String commandLine = args.stream().collect(Collectors.joining(" "));

        getLog().info("Call outside Maven: " + commandLine);

        runAnalyzer(args);
    }

    private List<String> buildArgs(final List<URL> urls) throws MojoExecutionException {
        String classPath = urls.stream()
                .map(u -> u.toString())
                .collect(Collectors.joining(File.pathSeparator));

        //
        // detect jars from target directory
        File target = new File(this.project.getBasedir(), "target");

        // args passed to analyzer
        List<String> args = new ArrayList<>(Arrays.asList(
                "java",
                "-Xmx1024m", // memory
                "-Dsun.misc.URLClassPath.disableJarChecking=true", // prevents classloading problems with java11
                "-cp",
                classPath,
                "de.uni_mannheim.swt.lasso.analyzer.LocalApplication",
                // options passed
                "--path", target.getAbsolutePath(),
                "--solrurl", solrUrl,
                "--solruser", solrUser,
                "--solrpass", solrPass,
                "--solrcore", solrCore,
                "--groupId", project.getGroupId(),
                "--artifactId", project.getArtifactId(),
                "--version", project.getVersion(),
                "--owner", owner
        ));

        if(StringUtils.isNotBlank(metadata)) {
            args.add("--metadata");
            args.add(metadata);
        }

        return args;
    }

    private void runAnalyzer(final List<String> args) throws MojoExecutionException {
        // redirect to stdout
        ProcessBuilder processBuilder = new ProcessBuilder(args).inheritIO().directory(project.getBasedir());

        try {
            Process process = processBuilder.start();
            getLog().info("Analyzer started with time limit of " + timeoutInSeconds + " seconds.");
            // be patient
            int waitForSecsBuffer = 30;
            process.waitFor(timeoutInSeconds + waitForSecsBuffer, TimeUnit.SECONDS);

            getLog().info("Analyzer finished with status " + process.exitValue());

            if (process.exitValue() != 0) {
                throw new MojoFailureException(this, "Analyzer encountered an error!", "Failed to index " +
                        "project, exit value is " + process.exitValue());
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private List<URL> loadPluginArtifacts() throws MojoExecutionException {
        final List<URL> urls = new LinkedList<>();
        try {
            for (Artifact artifact : artifacts) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Could not add artifact!", e);
        }
        return urls;
    }
}
