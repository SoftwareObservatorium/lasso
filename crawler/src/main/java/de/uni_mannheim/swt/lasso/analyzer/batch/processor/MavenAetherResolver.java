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
package de.uni_mannheim.swt.lasso.analyzer.batch.processor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;

/**
 * Resolve Maven artifacts using AETHER (currently only maven repos are used).
 * 
 * Attention: This impl. is not thread-safe.
 * 
 * @author Marcus Kessel
 * 
 */
public class MavenAetherResolver {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenAetherResolver.class);

    private final RepositorySystem system;
    private final RemoteRepository repo;
    private final RepositorySystemSession session;

    /**
     * Constructor
     * 
     * @param localWorkingRepository
     *            temp repo for local storage of retrieved artifacts
     * @param repoUrl
     *            Repository url (e.g. maven central url)
     */
    public MavenAetherResolver(String localWorkingRepository, String repoUrl) {
        this.system = newRepositorySystem();
        this.session = newRepositorySystemSession(system,
                localWorkingRepository);
        this.repo = newCentralRepository(repoUrl);
    }

    /**
     * Resolve jar
     * 
     * @param mavenArtifact
     *            {@link MavenArtifact} instance
     * @param includeClassifier
     *            Include classifier?
     * @return resolved {@link File}
     * @throws IOException
     *             I/O resolution error
     */
    public File getJar(MavenArtifact mavenArtifact, boolean includeClassifier)
            throws IOException {
        try {

            String classifierStr = "";
            if (includeClassifier) {
                classifierStr = StringUtils.isNotBlank(mavenArtifact
                        .getClassifier()) ? (mavenArtifact.getClassifier() + ":")
                        : "";
            }

            // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
            Artifact artifact = new DefaultArtifact(mavenArtifact.getGroupId()
                    + ":" + mavenArtifact.getArtifactId() + ":jar:"
                    + classifierStr + mavenArtifact.getVersion());

            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.addRepository(repo);

            ArtifactResult artifactResult = system.resolveArtifact(session,
                    artifactRequest);

            if (artifactResult == null) {
                throw new IOException("Artifact is null");
            }

            File jarFile = artifactResult.getArtifact().getFile();

            return jarFile;
        } catch (Throwable e) {
            throw new IOException("Jar retrieval failed for "
                    + ToStringBuilder.reflectionToString(mavenArtifact), e);
        }
    }

    /**
     * Resolve jar
     *
     * @param mavenArtifact
     *            {@link MavenArtifact} instance
     * @param includeClassifier
     *            Include classifier?
     * @return resolved {@link File}
     * @throws IOException
     *             I/O resolution error
     */
    public File getAar(MavenArtifact mavenArtifact, boolean includeClassifier)
            throws IOException {
        try {

            String classifierStr = "";
            if (includeClassifier) {
                classifierStr = StringUtils.isNotBlank(mavenArtifact
                        .getClassifier()) ? (mavenArtifact.getClassifier() + ":")
                        : "";
            }

            // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
            Artifact artifact = new DefaultArtifact(mavenArtifact.getGroupId()
                    + ":" + mavenArtifact.getArtifactId() + ":aar:"
                    + classifierStr + mavenArtifact.getVersion());

            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.addRepository(repo);

            ArtifactResult artifactResult = system.resolveArtifact(session,
                    artifactRequest);

            if (artifactResult == null) {
                throw new IOException("Artifact is null");
            }

            File jarFile = artifactResult.getArtifact().getFile();

            return jarFile;
        } catch (Throwable e) {
            throw new IOException("Jar retrieval failed for "
                    + ToStringBuilder.reflectionToString(mavenArtifact), e);
        }
    }

    /**
     * Get pom.
     *
     * @param mavenArtifact
     *            {@link MavenArtifact} instance
     * @return resolved {@link File}
     * @throws IOException
     *             I/O resolution error
     */
    public File getPom(MavenArtifact mavenArtifact)
            throws IOException {
        try {
            // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
            Artifact artifact = new DefaultArtifact(mavenArtifact.getGroupId()
                    + ":" + mavenArtifact.getArtifactId() + ":pom:" + mavenArtifact.getVersion()
                    );

            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.addRepository(repo);

            ArtifactResult artifactResult = system.resolveArtifact(session,
                    artifactRequest);

            if (artifactResult == null) {
                throw new IOException("POM is null");
            }

            File pomFile = artifactResult.getArtifact().getFile();

            return pomFile;
        } catch (Throwable e) {
            throw new IOException("Pom retrieval failed for "
                    + ToStringBuilder.reflectionToString(mavenArtifact), e);
        }
    }

    /**
     * Create repo system
     * 
     * @return
     */
    protected static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils
                .newServiceLocator();
        // enable slf4j logging
        locator.addService(org.eclipse.aether.spi.log.LoggerFactory.class,
                org.eclipse.aether.internal.impl.Slf4jLoggerFactory.class);
        //
        locator.addService(RepositoryConnectorFactory.class,
                BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class,
                FileTransporterFactory.class);
        locator.addService(TransporterFactory.class,
                HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl,
                    Throwable exception) {
                LOG.warn("RepositorySystem init failed", exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Create repo session
     * 
     * @param system
     * @param localWorkingRepository
     * @return
     */
    protected static DefaultRepositorySystemSession newRepositorySystemSession(
            RepositorySystem system, String localWorkingRepository) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils
                .newSession();

        File localRepoPath = new File(localWorkingRepository);
        if (!localRepoPath.exists()) {
            localRepoPath.mkdirs();
        }

        LocalRepository localRepo = new LocalRepository(localWorkingRepository);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(
                session, localRepo));

        return session;
    }

    /**
     * @return maven central repository
     */
    protected static RemoteRepository newCentralRepository(String repoUrl) {
        return new RemoteRepository.Builder("central", "default", repoUrl)
                .build();
    }
}
