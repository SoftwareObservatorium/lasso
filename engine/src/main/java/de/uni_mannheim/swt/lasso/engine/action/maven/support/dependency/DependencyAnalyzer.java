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
package de.uni_mannheim.swt.lasso.engine.action.maven.support.dependency;

import de.uni_mannheim.swt.lasso.core.model.Artifact;
import de.uni_mannheim.swt.lasso.core.model.MavenArtifact;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolve missing dependencies
 *
 * @author Marcus Kessel
 */
public class DependencyAnalyzer {

    // FIXME
    public static Map<String/*namespace*/, MavenArtifact> LOOKUP = new HashMap<>();
    static {
        // popular loggers
        LOOKUP.put("org.slf4j", new MavenArtifact("org.slf4j:slf4j-api:1.7.25"));
        LOOKUP.put("org.apache.commons.logging", new MavenArtifact("commons-logging:commons-logging:1.2"));

        // popular container APIs (i.e servlet API etc.)
        LOOKUP.put("javax.servlet", new MavenArtifact("javax.servlet:javax.servlet-api:3.1.0"));
    }

    /**
     * Group IDS which are oftentimes declared as "provided"
     */
    public static Set<String> INCLUDE_GROUPIDS_STARTSWITH = new HashSet<>(Arrays.asList(
            "org.slf4j",
            "commons-logging",
            "log4j",
            "org.apache.logging.log4j",
            "ch.qos.logback",
            // any Java EE specs
            "javax",
            // application/servlet containers
            "org.mortbay.jetty",
            "org.eclipse.jetty",
            "org.glassfish",
            "org.apache.tomcat",
            "org.apache.geronimo",
            // jboss stuff
            "org.jboss",
            "org.wildfly"
    ));

    /**
     * Group IDS which we don't want
     *
     * XXX those are used by Mavenize as well, so make sure they are actually group ids
     */
    public static Set<String> EXCLUDE_GROUPIDS_EXACT = new HashSet<>(Arrays.asList(
            "junit",
            "org.hamcrest",
            "org.powermock",
            "org.mockito",
            "org.easymock",
            "org.assertj"
    ));

    /**
     * Best practices in maven artifact names: e.g. api or javax
     */
    public static Set<String> ARTIFACTIDS_SPEC_KEYWORDS = new HashSet<>(Arrays.asList(
            "-api",
            "api-",
            "spec-",
            "-spec",
            "ee-",
            "-ee",
            "javax"
    ));

    /**
     * Attempt to recognize provided dependencies.
     *
     * @param mavenArtifact
     * @return
     */
    public static boolean match(MavenArtifact mavenArtifact) {
        // match groupId
        Optional<String> groupId = INCLUDE_GROUPIDS_STARTSWITH.stream()
                .filter(g -> StringUtils.startsWith(mavenArtifact.getGroupId(), g)).findAny();

        if(groupId.isPresent()) {
            return true;
        }

        // match artifactId for keywords
        Optional<String> artifactId = ARTIFACTIDS_SPEC_KEYWORDS.stream()
                .filter(a -> StringUtils.contains(mavenArtifact.getArtifactId(), a)).findAny();

        return artifactId.isPresent();
    }

    /**
     * Attempt to solve missing dependencies based on Maven meta-data and class dependency information.
     *
     * @param implementation
     * @return
     */
    public List<Artifact> resolveMissingDependencies(CodeUnit implementation) {
//        // a) analyze content
//        candidateDocument.getContent();
//
//        // b) check class dependencies for knowns, java format
//        candidateDocument.getDependencies();
//
//        // c) check maven deps for likely provided deps
//        // meta_dependency_ss
//        candidateDocument.getMetaData();

        //List<MavenArtifact> resolved = fromImports(candidateDocument);

        List<MavenArtifact> resolved = ensureAllConcretePopularDependencies(implementation);

        if(CollectionUtils.isEmpty(resolved)) {
            return null;
        }

        //
        return resolved.stream().map(a -> (Artifact) a).collect(Collectors.toList());
    }

    // FIXME do something useful with content
    protected List<MavenArtifact> fromContent(CodeUnit implementation) {
        //        candidateDocument.getContent();

        return null;
    }

    /**
     * Ensures that provided scoped dependencies are available at runtime by simply copying them over.
     *
     * Currently limited to 'popular' group ids.
     *
     * @param implementation
     * @return
     */
    protected List<MavenArtifact> ensureAllConcretePopularDependencies(CodeUnit implementation) {
        // now cross-check with maven deps
        List<String> mavenCoordinates = null;
        if(implementation.getMetaData().containsKey("meta_dependency_ss")
                && CollectionUtils.isNotEmpty((mavenCoordinates = (List<String>) implementation.getMetaData().get("meta_dependency_ss")))) {
            List<MavenArtifact> artifacts = mavenCoordinates.stream()
                    .map(c -> new MavenArtifact(c)).collect(Collectors.toList());

            // only pick those having concrete version defined
            return artifacts.stream()
                    // filter by concrete version
                    .filter(DependencyAnalyzer::isNoVersionPlaceholder)
                    // filter by popular group id
                    .filter(DependencyAnalyzer::match)
                    .collect(Collectors.toList());
        }

        return null;
    }

    /**
     * Cross-check class imports and maven dependencies.
     *
     * @param implementation
     * @return
     */
    // FIXME find good heuristics
    protected List<MavenArtifact> fromImports(CodeUnit implementation) {
        if(CollectionUtils.isNotEmpty(implementation.getDependencies())) {
            //
            Map<String /*namespace*/, MavenArtifact> mapping = new HashMap<>();
            for(String importStr : implementation.getDependencies()) {
                Optional<String> key = LOOKUP.keySet().stream().filter(k -> StringUtils.startsWith(importStr, k)).findFirst();
                if(key.isPresent()) {
                    // this makes sure that we create a map of UNIQUE artifacts
                    if(!mapping.containsKey(key.get())) {
                        mapping.put(key.get(), LOOKUP.get(key.get()));
                    }
                }
            }

            if(MapUtils.isEmpty(mapping)) {
                return null;
            }

            // now cross-check with maven deps
            if(implementation.getMetaData().containsKey("meta_dependency_ss")) {
                List<String> mavenCoordinates = (List<String>) implementation.getMetaData().get("meta_dependency_ss");

                if(CollectionUtils.isNotEmpty(mavenCoordinates)) {
                    List<MavenArtifact> artifacts = mavenCoordinates.stream()
                            .map(c -> new MavenArtifact(c)).collect(Collectors.toList());

                    // FIXME maven dep scope provided is unknown here

                    // similar?
                    List<MavenArtifact> resolved = new LinkedList<>();
                    for(String namespace : mapping.keySet()) {
                        MavenArtifact missing = mapping.get(namespace);

                        Optional<MavenArtifact> found = artifacts.stream().filter(a -> a.isSimilar(missing)).findFirst();
                        if(!found.isPresent()) {
                            // FIXME this is tricky, since we do not analyzed the entire dependency hierarchy
                            // we could misconfigure the pom accordingly due to explicit overrides using a wrong version
                            //resolved.add(missing);
                        }

                        // is they are actually declared, simply add them to the list to make sure that they are
                        // required, not only set as scope "provided"
                        if(found.isPresent()) {
                            resolved.add(found.get());
                        }
                    }

                    return resolved;
                }
            }
        }

        return null;
    }

    /**
     * Concrete version available?
     *
     * @param mavenArtifact
     * @return
     */
    private static boolean isNoVersionPlaceholder(MavenArtifact mavenArtifact) {
        // do not use deps having a placeholder version or no version at all (i.e 'null')
        //return !StringUtils.startsWithAny(mavenArtifact.getVersion(), "${", "null" /*index issue*/);

        return !isPlaceholder(mavenArtifact.getGroupId())
                && !isPlaceholder(mavenArtifact.getArtifactId())
                && !isPlaceholder(mavenArtifact.getVersion());

        // FIXME support  "org.apache.openejb:openejb-itests-beans:${project.version}", substitute placeholder

        // FIXME in null case we could guess a right version no based on artifact info?
    }

    /**
     *
     *
     * @param str
     * @return is blank, null or placeholder ?
     */
    private static boolean isPlaceholder(String str) {
        //
        String trimmed = StringUtils.trim(str);
        return StringUtils.isBlank(trimmed) || StringUtils.startsWithAny(trimmed, "${", "null")
                // 7.0-NETBEANS-${netbeans.release} or [${jetty.version}]
                || StringUtils.containsAny(trimmed, "${");
                //&& StringUtils.containsAny(trimmed, '+', '/');
    }
}
