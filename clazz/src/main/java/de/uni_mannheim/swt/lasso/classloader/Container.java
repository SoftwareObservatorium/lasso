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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sandbox container (isolated {@link ClassLoader)} for loading artifacts on the fly (based on classworlds).
 *
 * @author Marcus Kessel
 */
public abstract class Container extends ClassRealm {

    private static final Logger LOG = LoggerFactory
            .getLogger(Container.class);

    public static String[] getNastyPackages() {
        return new String[]{"java.", "javax.", "sun.", "org.xml", "org.w3c.",
                "apple.", "com.apple.", "com.sun.", "org.junit.", "junit.framework.",
                "org.evosuite.", "randoop.", "org.jacoco.", "de.uni_mannheim.swt.lasso."
        };
    }

    protected final Map<String, Class<?>> classes = new LinkedHashMap<>();

    //private final Strategy strategy;

    /**
     * Creates a new class realm.
     *
     * @param containers      The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     */
    public Container(Containers containers, String id, ClassLoader baseClassLoader) {
        super(containers, id, baseClassLoader);

        //this.strategy = new SelfFirstStrategy(this);
    }

    /**
     * getURLs to classpath
     *
     * @return
     */
    public String toClassPath() {
        return Arrays.stream(getURLs())
                .filter(Objects::nonNull)
                .filter(u -> Objects.nonNull(u.getFile()))
                .map(URL::getFile)
                .collect(Collectors.joining(":"));
    }

    /**
     * Intercepts loading of classes to (optionally) instrument classes.
     *
     * @param name
     * @param resolve
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        LOG.debug("loading class '{}'", name);

        // do not load
        if (!isSupportedClass(name)) {
            return super.loadClass(name, resolve);
        }

        if (classes.containsKey(name)) {
            LOG.debug("returning cached class '{}'", name);

            return classes.get(name);
        }

        LOG.debug("self loading class '{}'", name);

        if (instrumentClass(name)) {
            LOG.debug("class marked for instrumentation '{}'", name);

            try {
                byte[] bytes = loadClassBytes(name);

                byte[] bytesInstrumented = instrumentClassBytes(name, bytes);
                Class<?> clazz = defineClass(name, bytesInstrumented, 0, bytesInstrumented.length);

                if (clazz == null) {
                    return null;
                }

                if (clazz.getPackage() == null) {
                    int lastDotIndex = name.lastIndexOf('.');
                    String packageName = (lastDotIndex >= 0) ? name.substring(0, lastDotIndex) : "";
                    definePackage(packageName, null, null, null, null, null, null, null);
                }

                if (resolve) {
                    resolveClass(clazz);
                }

                LOG.debug("Loaded instrumented class '{}'", clazz.getName());

                classes.put(name, clazz);

                return clazz;
            } catch (Throwable e) {
                e.printStackTrace();

                // FIXME instrumentation failed
            }
        }

        Class<?> clazz = super.loadClass(name, resolve);

        classes.put(name, clazz);

        return clazz;
    }

    /**
     * Determine scope for class
     *
     * @param name
     * @return
     */
    public String determineScopeForClass(String name) {
        // determine scope
        String path = name.replace('.', '/').concat(".class");

        // for whatever reason, we get "jar:file:"
        URL url = getResource(path);

        if(url == null) {
            return "system";
        }

        if(StringUtils.startsWith(url.toString(), "jar:")) {
            try {
                url = new URL(StringUtils.substringAfter(url.toString(), "jar:"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        URL[] urls = getURLs();
        for(int i = 0; i < urls.length; i++) {
            URL oURL = urls[i];

            if(StringUtils.startsWith(url.toString(), oURL.toString())) {
                if(i == 0) {
                    return "project"; // first URL is always project
                } else {
                    return "thirdparty";
                }
            }
        }

        return "system";
    }

    /**
     * Can only be invoked once
     *
     * @param name
     * @param bytes
     * @param resolve
     * @return
     * @throws ClassNotFoundException
     */
    protected Class<?> defineAndLoadCustomClass(String name, byte[] bytes, boolean resolve) throws ClassNotFoundException {
        LOG.debug("loading custom class '{}'", name);

        try {
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);

            if (clazz == null) {
                return null;
            }

            if (clazz.getPackage() == null) {
                int lastDotIndex = name.lastIndexOf('.');
                String packageName = (lastDotIndex >= 0) ? name.substring(0, lastDotIndex) : "";
                definePackage(packageName, null, null, null, null, null, null, null);
            }

            if (resolve) {
                resolveClass(clazz);
            }

            LOG.debug("Loaded instrumented class '{}'", clazz.getName());

            classes.put(name, clazz);

            return clazz;
        } catch (Throwable e) {
            throw e;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        LOG.debug("find class '{}'", name);

        return super.findClass(name);
    }

    /**
     * Change permissions for this sandboxed classloader.
     *
     * @param codesource
     * @return
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return super.getPermissions(codesource);
    }

    /**
     * Load class bytes from resources (assuming same order of resolution as class resolution!).
     *
     * @param name
     * @return
     * @throws IOException
     */
    protected byte[] loadClassBytes(String name) throws IOException {
        String path = name.replace('.', '/').concat(".class");

        try (InputStream in = getResourceAsStream(path)) {
            byte[] bytes = IOUtils.toByteArray(in);

            return bytes;
        }
    }

    /**
     * Class loading supported
     *
     * @param name
     * @return
     */
    protected boolean isSupportedClass(String name) {
        // do not instrument
        if (StringUtils.startsWithAny(name, getNastyPackages())) {
            return false;
        }

        return true;
    }

    /**
     * Signal if given class should be instrumented.
     *
     * @param name
     * @return
     */
    protected abstract boolean instrumentClass(String name);

    /**
     * Override to instrument class bytes.
     *
     * @param name
     * @param bytes
     * @return
     */
    protected abstract byte[] instrumentClassBytes(String name, byte[] bytes);

    public Map<String, Class<?>> getClasses() {
        return Collections.unmodifiableMap(classes);
    }
}
