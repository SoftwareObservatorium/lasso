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
package de.uni_mannheim.swt.lasso.analyzer.batch.reader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Marcus Kessel
 */
public class SingleMavenArtifactReaderTest {

    @Test
    public void test_read_one_element() throws Exception {
        String artifacts = "org.apache.commons:commons-lang3:3.14.0:sources";
        SingleMavenArtifactReader reader = new SingleMavenArtifactReader(artifacts);

        //
        MavenArtifact mavenArtifact = reader.read();

        assertEquals("org.apache.commons", mavenArtifact.getGroupId());
        assertEquals("commons-lang3", mavenArtifact.getArtifactId());
        assertEquals("3.14.0", mavenArtifact.getVersion());
        assertEquals("sources", mavenArtifact.getClassifier());
        assertFalse(mavenArtifact.isTestArtifact());

        MavenArtifact mavenArtifact2 = reader.read();
        assertNull(mavenArtifact2);
    }

    @Test
    public void test_read_two_elements() throws Exception {
        String artifacts = "org.apache.commons:commons-lang3:3.14.0:sources|bla:blub:v:sources";
        SingleMavenArtifactReader reader = new SingleMavenArtifactReader(artifacts);

        //
        MavenArtifact mavenArtifact = reader.read();

        assertEquals("org.apache.commons", mavenArtifact.getGroupId());
        assertEquals("commons-lang3", mavenArtifact.getArtifactId());
        assertEquals("3.14.0", mavenArtifact.getVersion());
        assertEquals("sources", mavenArtifact.getClassifier());
        assertFalse(mavenArtifact.isTestArtifact());

        MavenArtifact mavenArtifact2 = reader.read();

        assertEquals("bla", mavenArtifact2.getGroupId());
        assertEquals("blub", mavenArtifact2.getArtifactId());
        assertEquals("v", mavenArtifact2.getVersion());
        assertEquals("sources", mavenArtifact2.getClassifier());
        assertFalse(mavenArtifact2.isTestArtifact());

        MavenArtifact mavenArtifact3 = reader.read();
        assertNull(mavenArtifact3);
    }
}
