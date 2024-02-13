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
import de.uni_mannheim.swt.lasso.arena.Project;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.classloader.Containers;

import de.uni_mannheim.swt.lasso.arena.classloader.sandbox.Sandbox;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Support Maven's resolution mechanism.
 *
 * @author Marcus kessel
 */
public class MavenRepository implements Repository {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenRepository.class);

    private final DependencyResolver resolver;

    /**
     * Blacklist of known artefacts that may cause trouble
     */
    private static final Set<String> BLACKLIST_ARTEFACTS = new HashSet<String>() {
        {
            add("commons-logging:commons-logging");
        }
    };

    private final Containers containers;

    public MavenRepository(DependencyResolver resolver) {
        this(resolver, new Containers());
    }

    public MavenRepository(DependencyResolver resolver, Containers containers) {
        this.resolver = resolver;
        this.containers = containers;

        // initialize sandbox
        if(!Sandbox.isInitialized()) {
            Sandbox.initialize(getRepositoryPath().toPath());
        }
    }

    public DependencyResolver getResolver() {
        return resolver;
    }

    public File getRepositoryPath() {
        return resolver.getSession().getLocalRepository().getBasedir();
    }

    /**
     *
     * @param classUnderTest
     * @param factory
     */
    @Override
    public void resolve(ClassUnderTest classUnderTest, ContainerFactory factory) {
        Project project = classUnderTest.getProject();

        if(!project.isResolved()) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Resolving artifacts for '{}', '{}'", classUnderTest.getClassName(), classUnderTest.getId());
            }

            try {
                DependencyResult dependencyResult = resolver.resolveTransitiveDependencies(classUnderTest);
                project.setDependencyResult(dependencyResult);

            } catch (Throwable e) {
                throw new IllegalArgumentException("Dependency resolution failed", e);
            }
        }

        // set container
        if(project.getContainer() == null) {
            try {
                Container container = containers.create(classUnderTest, factory);
                project.setContainer(container);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Creating container failed", e);
            }
        }
    }

    @Override
    public Containers getContainers() {
        return containers;
    }

    @Override
    public void close() {
        containers.closeAll();
    }
}
