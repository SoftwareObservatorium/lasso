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
package de.uni_mannheim.swt.lasso.classloader;

import org.apache.commons.lang3.RandomStringUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.ClassWorldListener;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Custom version of {@link ClassWorld}.
 *
 * @author original authors (see license)
 * @author Marcus Kessel
 */
public class Containers extends ClassWorld {

    private static final Logger LOG = LoggerFactory
            .getLogger(Containers.class);

    private Map<String, ClassRealm> realms;

    private final List<ClassWorldListener> listeners = new ArrayList<ClassWorldListener>();

    public Containers(String realmId, ClassLoader classLoader) {
        this();

        try {
            newRealm(realmId, classLoader);
        } catch (DuplicateRealmException e) {
            // Will never happen as we are just creating the world.
        }
    }

    public Containers() {
        this.realms = new LinkedHashMap<>();
    }

    public ClassRealm newRealm(String id)
            throws DuplicateRealmException {
        return newRealm(id, getClass().getClassLoader());
    }

    public Container newContainer(String id) throws DuplicateRealmException {
        return (Container) newRealm(id, getClass().getClassLoader(), ContainerFactory.DEFAULT_FACTORY);
    }

    public synchronized ClassRealm newRealm(String id, ClassLoader classLoader, ContainerFactory factory)
            throws DuplicateRealmException {
        if (realms.containsKey(id)) {
            throw new DuplicateRealmException(this, id);
        }

        Container realm;

        realm = factory.create(this, id, classLoader);

        realms.put(id, realm);

        for (ClassWorldListener listener : listeners) {
            listener.realmCreated(realm);
        }

        return realm;
    }

    public synchronized void disposeRealm(String id)
            throws NoSuchRealmException {
        ClassRealm realm = (ClassRealm) realms.remove(id);

        if (realm != null) {
            closeIfJava7(realm);
            for (ClassWorldListener listener : listeners) {
                listener.realmDisposed(realm);
            }
        }
    }

    private void closeIfJava7(ClassRealm realm) {
        try {
            //noinspection ConstantConditions
            if (realm instanceof Closeable) {
                //noinspection RedundantCast
                ((Closeable) realm).close();
            }
        } catch (IOException ignore) {
        }
    }

    public synchronized ClassRealm getRealm(String id)
            throws NoSuchRealmException {
        if (realms.containsKey(id)) {
            return (ClassRealm) realms.get(id);
        }

        throw new NoSuchRealmException(this, id);
    }

    public synchronized Collection<ClassRealm> getRealms() {
        return Collections.unmodifiableList(new ArrayList<ClassRealm>(realms.values()));
    }

    // from exports branch
    public synchronized ClassRealm getClassRealm(String id) {
        if (realms.containsKey(id)) {
            return realms.get(id);
        }

        return null;
    }

    public synchronized void addListener(ClassWorldListener listener) {
        // TODO ideally, use object identity, not equals
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(ClassWorldListener listener) {
        listeners.remove(listener);
    }

    /**
     * Blacklist of known artefacts that may cause trouble
     */
    private static final Set<String> BLACKLIST_ARTEFACTS = new HashSet<String>() {
        {
            //add("commons-logging:commons-logging");
        }
    };

    public synchronized Container create(DependencyResult dependencyResult, ContainerFactory factory) throws IllegalAccessException {
        String containerId = RandomStringUtils.random(10);

        // avoid duplicates
        int i = 0;
        while(realms.containsKey(containerId)) {
            containerId += "_" + i++;
        }

        Container classRealm = null;
        try {
            classRealm = (Container) newRealm(containerId, getClass().getClassLoader(), factory);
        } catch (DuplicateRealmException e) {
            throw new RuntimeException(e);
        }

        //
        for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
            Artifact artifact = artifactResult.getArtifact();

            if(artifact == null) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Artifact was null. Ignoring");
                }

                continue;
            }

            String uri = artifact.getGroupId() + ":" + artifact.getArtifactId();
            if (BLACKLIST_ARTEFACTS.contains(uri)) {
                // skip
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping Artifact ==> " + artifactResult.toString());
                }

                continue;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Artifact ==> " + artifactResult.toString());
            }

            try {
                classRealm.addURL(artifact.getFile().toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        // set parent classloader?
        //classRealm.setParentClassLoader(getClass().getClassLoader());

        return classRealm;
    }

    public synchronized Container createUnsafe(List<DependencyResult> dependencyResults, ContainerFactory factory) throws IllegalAccessException {
        String containerId = RandomStringUtils.random(10);

        // avoid duplicates
        int i = 0;
        while(realms.containsKey(containerId)) {
            containerId += "_" + i++;
        }

        Container classRealm = null;
        try {
            classRealm = (Container) newRealm(containerId, getClass().getClassLoader(), factory);
        } catch (DuplicateRealmException e) {
            throw new RuntimeException(e);
        }

        //
        for(DependencyResult dependencyResult : dependencyResults) {
            for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
                Artifact artifact = artifactResult.getArtifact();

                if(artifact == null) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Artifact was null. Ignoring");
                    }

                    continue;
                }

                String uri = artifact.getGroupId() + ":" + artifact.getArtifactId();
//            if (BLACKLIST_ARTEFACTS.contains(uri)) {
//                // skip
//                if (LOG.isDebugEnabled()) {
//                    LOG.debug("Skipping Artifact ==> " + artifactResult.toString());
//                }
//
//                continue;
//            }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Artifact ==> " + artifactResult.toString());
                }

                try {
                    classRealm.addURL(artifact.getFile().toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        // set parent classloader?
        //classRealm.setParentClassLoader(getClass().getClassLoader());

        return classRealm;
    }

    public synchronized Container createUnsafe(DependencyResult dependencyResult, ContainerFactory factory) throws IllegalAccessException {
        return createUnsafe(Arrays.asList(dependencyResult), factory);
    }

    public void closeAll() {
        getRealms().forEach(c -> {
            try {
                disposeRealm(c.getId());
            } catch (NoSuchRealmException e) {
                e.printStackTrace();
            }
        });
    }
}
