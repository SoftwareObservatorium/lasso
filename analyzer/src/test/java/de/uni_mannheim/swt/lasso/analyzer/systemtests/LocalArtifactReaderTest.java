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
package de.uni_mannheim.swt.lasso.analyzer.systemtests;

import de.uni_mannheim.swt.lasso.analyzer.batch.reader.LocalArtifactReader;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 *
 * @author Marcus Kessel
 */
public class LocalArtifactReaderTest {

    File projectDir = new File(
            "testdata/commons-codec/commons-codec/1.11/");

    @Test
    public void test() {
        File repositoryDir = new File("testdata");
        File infoFile = new File(projectDir,"lasso_info");

        MavenArtifact mavenArtifact = LocalArtifactReader.toMavenArtifact(repositoryDir, infoFile);

        assertEquals("commons-codec", mavenArtifact.getGroupId());
        assertEquals("commons-codec", mavenArtifact.getArtifactId());
        assertEquals("1.11", mavenArtifact.getVersion());

        assertTrue(mavenArtifact.getBinaryJar().exists());
        assertTrue(mavenArtifact.getSourceJar().exists());
        assertTrue(mavenArtifact.getTestArtifact().getBinaryJar().exists());
        assertTrue(mavenArtifact.getTestArtifact().getSourceJar().exists());
        assertTrue(mavenArtifact.getPomFile().exists());

        assertFalse(mavenArtifact.isTests());
        assertTrue(mavenArtifact.getTestArtifact().isTests());
    }
}
