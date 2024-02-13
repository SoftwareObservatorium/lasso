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

import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Tests for debugging purposes.
 *
 *
 * @author Marcus Kessel
 */
public class MavenAetherResolverIntegrationTest {

    @Test
    public void testResolveJar_failed() throws IOException {
        String repoUrl = "https://repo1.maven.org/maven2";
        String tmp = "/tmp/myrepotest_" + System.currentTimeMillis();

        MavenAetherResolver resolver = new MavenAetherResolver(tmp, repoUrl);

        MavenArtifact mavenArtifact = new MavenArtifact("com.helger", "ph-jaxb", "9.1.8", "test-sources");

        File jarFile = resolver.getJar(mavenArtifact, true);

        assertTrue(jarFile.exists());
    }

    @Test
    public void testResolveJar() throws IOException {
        String repoUrl = "http://swt100.informatik.uni-mannheim.de:8081/repository/maven-central/";
        String tmp = "/tmp/myrepotest_" + System.currentTimeMillis();

        MavenAetherResolver resolver = new MavenAetherResolver(tmp, repoUrl);

        MavenArtifact mavenArtifact = new MavenArtifact("commons-codec", "commons-codec", "1.9", "sources");

        resolver.getJar(mavenArtifact, true);
    }

    @Test
    public void testResolvePom() throws IOException, XmlPullParserException {
        String repoUrl = "http://swt100.informatik.uni-mannheim.de:8081/repository/maven-central/";
        String tmp = "/tmp/myrepotest_" + System.currentTimeMillis();

        MavenAetherResolver resolver = new MavenAetherResolver(tmp, repoUrl);

        MavenArtifact mavenArtifact = new MavenArtifact("commons-codec", "commons-codec", "1.9", "sources");

        File pom = resolver.getPom(mavenArtifact);

        assertTrue(pom.exists());
    }
}
