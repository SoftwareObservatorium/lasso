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
package de.uni_mannheim.swt.lasso.arena.repository;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Dependency resolver for Maven repositories.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://stackoverflow.com/questions/40813062/maven-get-all-dependencies-programmatically">Source Code</a>
 */
public class DependencyResolver {

    private static final Logger LOG = LoggerFactory
            .getLogger(DependencyResolver.class);

    private final DefaultRepositorySystemSession session;
    private final RepositorySystem system;
    private final RemoteRepository central;

    /**
     * Set configuration options.
     *
     * @param session
     * @param repoUrl
     */
    public void onConfigure(DefaultRepositorySystemSession session, String repoUrl) {
        // mirror any (third-party) repositories
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        mirrorSelector.add("swt100nexus-public", repoUrl, "default", true, "*", "*");

        session.setMirrorSelector(mirrorSelector);

        //
        //ConfigurationProperties.DEFAULT_REQUEST_TIMEOUT = 10 * 1000;

        // set timeout to 10secs
        session.setConfigProperty(ConfigurationProperties.CONNECT_TIMEOUT, 10 * 1000);
        session.setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, 2 * 60 * 1000);

        // set update policy to NEVER
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
    }

    public DependencyResolver(String repoUrl, String localRepoPath) {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        this.system = newRepositorySystem(locator);
        this.session = newSession(system, localRepoPath);
        // configure
        onConfigure(session, repoUrl);

        this.central = new RemoteRepository.Builder("central", "default", repoUrl).build();
    }

    public DependencyResult resolveTransitiveDependencies(ClassUnderTest classUnderTest) throws DependencyResolutionException {
        return resolveTransitiveDependencies(classUnderTest, null);
    }

    public DependencyResult resolveTransitiveDependencies(ClassUnderTest classUnderTest, DependencyFilter dependencyFilter) throws DependencyResolutionException {
        CodeUnit implementation = classUnderTest.getImplementation().getCode();

        DefaultArtifact artifact = new DefaultArtifact(
                implementation.getGroupId(),
                implementation.getArtifactId(),
                implementation.getClassifier(),
                "jar",
                implementation.getVersion());

        return resolveTransitiveDependencies(artifact, dependencyFilter);
    }

    public DependencyResult resolveTransitiveDependencies(DefaultArtifact artifact, DependencyFilter dependencyFilter) throws DependencyResolutionException {
        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), Arrays.asList(central));
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE/*, JavaScopes.PROVIDED, JavaScopes.TEST*/, JavaScopes.RUNTIME);

        if(dependencyFilter != null) {
            filter = DependencyFilterUtils.andFilter(filter, dependencyFilter);
        }

        DependencyRequest request = new DependencyRequest(collectRequest, filter);

//        try {
//            system.collectDependencies(session, collectRequest);
//        } catch (DependencyCollectionException e) {
//            LOG.warn("Could not collect all dependencies => '{}'", ToStringBuilder.reflectionToString(e));
//        }

        DependencyResult result = null;
        try {
            result = system.resolveDependencies(session, request);
        } catch (DependencyResolutionException e) {
            LOG.warn("Dependency resolution failed, proceeding with partial resolution for '{}'", artifact);
            LOG.warn("Stack trace", e);

            if(e.getCause() instanceof ArtifactResolutionException) {
                ArtifactResolutionException are = (ArtifactResolutionException) e.getCause();
                result = new DependencyResult(request);
                result.setArtifactResults(are.getResults().stream().filter(Objects::nonNull).collect(Collectors.toList()));
            } else {
                throw e;
            }

//            Throwable r = ExceptionUtils.getRootCause(e);
//            if(r instanceof ArtifactNotFoundException) {
//                ArtifactNotFoundException anf = (ArtifactNotFoundException) r;
//                Artifact notFound = anf.getArtifact();
//
//                LOG.warn("Not found '{}'", notFound);
//
//                // retry attempt without missing artifact
//                ExclusionsDependencyFilter exclude = new ExclusionsDependencyFilter(Collections.singleton(String.format("%s:%s", notFound.getGroupId(), notFound.getArtifactId())));
//                return resolveTransitiveDependencies(classUnderTest, exclude);
//            }

            // does not work (contrary to what JavaDoc is telling us)!
            //result = e.getResult();
            //throw e;
        }

        return result;
    }

    private static RepositorySystem newRepositorySystem(DefaultServiceLocator locator) {
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession newSession(RepositorySystem system, String localRepoPath) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoPath);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    public RepositorySystemSession getSession() {
        return session;
    }

    public RepositorySystem getSystem() {
        return system;
    }

    public RemoteRepository getCentral() {
        return central;
    }

    public void close() {
        //
    }
}
