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
package de.uni_mannheim.swt.lasso.engine.action.classpath;

import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utilities to resolve (Maven) artifacts locally.
 *
 * @author Marcus Kessel
 */
public class JarUtils {

    /**
     * Resolve artifact jar
     *
     * @param executable
     * @return
     * @throws IOException
     */
    public static File resolveArtifact(System executable) throws IOException {
        File cp = new File(executable.getProject().getBaseDir(), "jarlist");

        Map<String, String> map = getEntries(cp, executable.getProject().getArtifactRepository());

        String resolvedJar = map.get(executable.getCode().toUri());

        return new File(resolvedJar);
    }

    /**
     *
     * <pre>
     *     The following files have been resolved:
     *    com.sun:tools:jar:1.8.0:system:/usr/lib/jvm/java-8-oracle-1.8.0.202/jre/../lib/tools.jar
     *    de.uni-mannheim.swt.lasso:testrunner:jar:1.0.0-SNAPSHOT:provided:/home/marcus/.m2/repository/de/uni-mannheim/swt/lasso/testrunner/1.0.0-SNAPSHOT/testrunner-1.0.0-SNAPSHOT.jar
     *    org.javassist:javassist:jar:3.26.0-GA:provided:/home/marcus/.m2/repository/org/javassist/javassist/3.26.0-GA/javassist-3.26.0-GA.jar
     *    io.github.classgraph:classgraph:jar:4.1.7:provided:/home/marcus/.m2/repository/io/github/classgraph/classgraph/4.1.7/classgraph-4.1.7.jar
     * </pre>
     *
     * @param classpath
     * @param artifactRepository
     * @return
     * @throws IOException
     */
    public static Map<String, String> getEntries(File classpath, File artifactRepository) throws IOException {
        try(LineIterator lineIterator = FileUtils.lineIterator(classpath, StandardCharsets.UTF_8.displayName());) {
            Map<String, String> list = new LinkedHashMap<>();
            while(lineIterator.hasNext()) {
                String line = lineIterator.nextLine();

                if(StringUtils.isBlank(line) || StringUtils.startsWith(line.trim(), "The following")) {
                    continue;
                }

                line = StringUtils.trimToEmpty(line);

                String[] parts = StringUtils.split(line, ':');

                String uri = String.format("%s:%s:%s", parts[0], parts[1], parts[3]);
                String jarPath = parts[5];

                if(!StringUtils.contains(jarPath, "repository/")) {
                    // system scope, ignore
                    continue;
                }

                jarPath = artifactRepository.getAbsolutePath() + "/" + StringUtils.substringAfter(jarPath, "repository/");

                list.put(uri, jarPath);
            }

            return list;
        }
    }

    public static List<String> getClasses(File jarFile) throws IOException {
        List<String> classNames = new LinkedList<>();
        try(ZipInputStream zip = new ZipInputStream(new FileInputStream(jarFile))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    classNames.add(StringUtils.substringBeforeLast(entry.getName(), "."));
                }
            }
        }

        return classNames;
    }
}
