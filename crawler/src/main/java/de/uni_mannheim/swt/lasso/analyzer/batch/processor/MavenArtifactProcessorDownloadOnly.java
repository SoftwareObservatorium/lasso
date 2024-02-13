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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.io.File;
import java.io.IOException;

/**
 * Download artifacts.
 *
 * @author Marcus Kessel
 */
public class MavenArtifactProcessorDownloadOnly implements ItemProcessor<MavenArtifact, AnalysisResult> {

    private static final Logger LOG = LoggerFactory.getLogger(MavenArtifactProcessorDownloadOnly.class);

    private final MavenAetherResolver resolver;

    /**
     * Constructor
     *
     * @param resolver        {@link MavenAetherResolver} instance
     */
    public MavenArtifactProcessorDownloadOnly(MavenAetherResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisResult process(MavenArtifact mavenArtifact) throws Exception {
        try {
            // resolve
            resolve(mavenArtifact);

            AnalysisResult result = new AnalysisResult();
            result.setMavenArtifact(mavenArtifact);

            //
            try {
                resolveAndAnalyzePom(mavenArtifact);
            } catch (Throwable e) {
                //
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to resolve/analyze maven pom "
                            + ToStringBuilder.reflectionToString(mavenArtifact), e);
                }
            }

            return result;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to resolve/analyze maven artifact "
                        + ToStringBuilder.reflectionToString(mavenArtifact), e);
            }
        }

        // signal fail
        return null;
    }

    private File resolveAndAnalyzePom(MavenArtifact mavenArtifact) throws IOException, XmlPullParserException {
        File pomFile = resolver.getPom(mavenArtifact);

        return pomFile;
    }

    /**
     * Resolve artifacts
     *
     * @param mavenArtifact
     * @throws IOException
     */
    protected void resolve(MavenArtifact mavenArtifact) throws IOException {
        // try to resolve jars
        File srcFile = resolver.getJar(mavenArtifact, true);

        File binFile = null;
        // has srcFile -> sources-test classifier?
        if (mavenArtifact.isTestArtifact()) {
            // Maven convention: "tests" classifier
            MavenArtifact testArtifact = new MavenArtifact(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(),
                    mavenArtifact.getVersion(), "tests");

            binFile = resolver.getJar(testArtifact,
                    true /* include classifier */);
        } else {
            binFile = resolver.getJar(mavenArtifact, false);
        }

        mavenArtifact.setSourceJar(srcFile);
        mavenArtifact.setBinaryJar(binFile);
    }
}
