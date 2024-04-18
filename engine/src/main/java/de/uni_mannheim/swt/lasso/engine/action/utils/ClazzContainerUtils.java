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
package de.uni_mannheim.swt.lasso.engine.action.utils;

import de.uni_mannheim.swt.lasso.classloader.Container;
import de.uni_mannheim.swt.lasso.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.classloader.Containers;
import de.uni_mannheim.swt.lasso.classloader.resolver.MavenDependencyResolver;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.arena.ArenaProjectManager;
import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ClazzContainerUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClazzContainerUtils.class);

    /**
     * Create class container (i.e., classloader) to load custom types (i.e., classes from artifacts).
     *
     * @param context
     * @return
     */
    public static Container createClazzContainer(LSLExecutionContext context, List<String> dependencies) {
        try {
            Containers containers = new Containers();

            Container container;
            if(CollectionUtils.isNotEmpty(dependencies)) {
                // set default executable corpus
                ExecutableCorpus corpus = context.getConfiguration().getExecutableCorpus();
                String mavenRepoUrl = corpus.getArtifactRepository().getUrl();

                ArenaProjectManager manager = new ArenaProjectManager(context);
                MavenDependencyResolver dependencyResolver = new MavenDependencyResolver(mavenRepoUrl,
                        manager.getRepository().getAbsolutePath());

                List<DependencyResult> dependencyResults = new ArrayList<>(dependencies.size());

                for(String coords : dependencies) {
                    DefaultArtifact artifact = new DefaultArtifact(
                            coords);

                    dependencyResults.add(dependencyResolver.resolveTransitiveDependencies(artifact, null));
                }

                container = containers.createUnsafe(dependencyResults, ContainerFactory.DEFAULT_FACTORY);
            } else {
                container = containers.createUnsafe(new ArrayList<>(0), ContainerFactory.DEFAULT_FACTORY);
            }

            return container;
        } catch (Throwable e) {
            LOG.warn("createClazzContainer failed for {}", context.getExecutionId());

            throw new RuntimeException(e);
        }
    }

    public static void dispose(Container container) {
        //
        if(LOG.isDebugEnabled()) {
            LOG.debug("Disposing clazz container {}", container.getId());
        }
        try {
            container.getWorld().disposeRealm(container.getId());
        } catch (NoSuchRealmException e) {
            throw new RuntimeException(e);
        }
    }
}
