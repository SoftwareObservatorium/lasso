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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;

import de.uni_mannheim.swt.lasso.analyzer.batch.processor.MavenArtifactProcessorDownloadOnly;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.GroupedSearchResponse;
import org.junit.Test;

/**
 * Integration tests for {@link LatestVersionArtifactReader} and
 * {@link MavenArtifactProcessorDownloadOnly}
 * 
 * @author Marcus Kessel
 *
 */
public class LatestVersionArtifactReaderIntegrationTest {

    private static LatestVersionArtifactReader reader;

    @Test
    public void testIsTestArtifact() {
        // is test artifact? yes
        assertTrue(new MavenArtifact("GroupId", "ArtifactId", "Version", "test-sources").isTestArtifact());

        // is test artifact? no
        assertFalse(new MavenArtifact("GroupId", "ArtifactId", "Version", "sources").isTestArtifact());
    }

    @Test
    public void test_lowlevel() throws IOException {
        assertNotNull(reader);

        //
        GroupedSearchResponse response = reader.queryForTestSources();

        Optional<Entry<String, ArtifactInfoGroup>> firstEntry = response.getResults().entrySet().stream().findFirst();

        ArtifactInfo artifactInfo = firstEntry.get().getValue().getArtifactInfos().stream().findFirst().get();

        System.out.println(firstEntry.get().getKey() + " = " + ToStringBuilder.reflectionToString(artifactInfo));

        MavenArtifact mavenArtifact = new MavenArtifact(artifactInfo.groupId, artifactInfo.artifactId,
                artifactInfo.version, artifactInfo.classifier);

        // is test artifact?
        assertTrue(mavenArtifact.isTestArtifact());
    }

    @Test
    public void test_read() throws Exception {
        assertNotNull(reader);

        //
        MavenArtifact mavenArtifact = reader.read();

        // is test artifact?
        assertTrue(mavenArtifact.isTestArtifact());
    }
}
