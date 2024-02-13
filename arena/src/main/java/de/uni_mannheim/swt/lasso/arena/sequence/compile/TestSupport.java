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
package de.uni_mannheim.swt.lasso.arena.sequence.compile;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utilities for JUnit (classpath etc.)
 *
 * @author Marcus Kessel
 */
public class TestSupport {

    /**
     * Gets test artifact paths (arena-support + testrunner + junit).
     *
     * @param resolver
     * @return
     * @throws DependencyResolutionException
     */
    public static String getTestClassPath(DependencyResolver resolver) throws DependencyResolutionException {
        DefaultArtifact artifact = new DefaultArtifact(
                "de.uni-mannheim.swt.lasso",
                "arena-support",
                null,
                "jar",
                "1.0.0-SNAPSHOT");
        DependencyResult result = resolver.resolveTransitiveDependencies(artifact, null);

        return result.getArtifactResults().stream()
                .map(a -> a.getArtifact().getFile().getAbsolutePath())
                .collect(Collectors.joining(":"));
    }

    public static ClassUnderTest createPseudoImplementation(String name) {
        CodeUnit implementation = new CodeUnit();
        implementation.setId(UUID.randomUUID().toString());
        implementation.setName(name);
        implementation.setPackagename("de.uni_mannheim.swt.lasso.arena");
        implementation.setGroupId("de.uni-mannheim.swt.lasso");
        implementation.setArtifactId("arena-support");
        implementation.setVersion("1.0.0-SNAPSHOT");
        ClassUnderTest classUnderTest = new ClassUnderTest(new System(implementation));
        classUnderTest.setPseudo(true);

        return classUnderTest;
    }

    /**
     * <pre>
     *                 <dependency>
     *                   <groupId>org.evosuite</groupId>
     *                   <artifactId>evosuite-standalone-runtime</artifactId>
     *                   <version>1.1.1.LASSO.9</version>
     *                   <scope>test</scope>
     *                 </dependency>
     * </pre>
     *
     * @param resolver
     * @return
     * @throws DependencyResolutionException
     */
    public static String getEvoSuiteRunnerTestClassPath(DependencyResolver resolver) throws DependencyResolutionException {
        DefaultArtifact artifact = new DefaultArtifact(
                "org.evosuite",
                "evosuite-standalone-runtime",
                null,
                "jar",
                "1.1.1.LASSO.9");
        DependencyResult result = resolver.resolveTransitiveDependencies(artifact, null);

        return result.getArtifactResults().stream()
                .map(a -> a.getArtifact().getFile().getAbsolutePath())
                .collect(Collectors.joining(":"));
    }
}
