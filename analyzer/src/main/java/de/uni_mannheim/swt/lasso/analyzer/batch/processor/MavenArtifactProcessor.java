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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.uni_mannheim.swt.lasso.analyzer.analyzer.ProjectAnalyzer;
import de.uni_mannheim.swt.lasso.analyzer.maven.MavenPomParser;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.model.MetaData;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;

/**
 * {@link ItemProcessor} for {@link MavenArtifact}s.
 * <p>
 * <ol>
 * <li>Apply {@link ProjectAnalyzer}</li>
 * <li>return {@link AnalysisResult}</li>
 * </ol>
 *
 * @author Marcus Kessel
 */
public class MavenArtifactProcessor implements ItemProcessor<MavenArtifact, AnalysisResult> {

    private static final Logger LOG = LoggerFactory.getLogger(MavenArtifactProcessor.class);

    private final ProjectAnalyzer projectAnalyzer;

    /**
     * Constructor
     *
     * @param projectAnalyzer {@link ProjectAnalyzer} impl.
     */
    public MavenArtifactProcessor(ProjectAnalyzer projectAnalyzer) {
        this.projectAnalyzer = projectAnalyzer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisResult process(MavenArtifact mavenArtifact) throws Exception {
        LOG.info(String.format("Processing %s %s", mavenArtifact.toUri(), mavenArtifact.isTests()));

        // only process if everything is there
        if(mavenArtifact.getBinaryJar() == null || mavenArtifact.getSourceJar() == null) {
            LOG.warn(String.format("Processing failed %s %s", mavenArtifact.toUri(), mavenArtifact.isTests()));

            return null;
        }

        try {
            // resolve and analyze
            List<CompilationUnit> compilationUnits = resolveAndAnalyze(mavenArtifact);

            AnalysisResult result = new AnalysisResult();
            result.setMavenArtifact(mavenArtifact);
            result.setCompilationUnits(compilationUnits);

            //
            try {
                MetaData metaData = resolveAndAnalyzePom(mavenArtifact);

                if(metaData != null) {
                    result.setMetaData(metaData);
                }
            } catch (Throwable e) {
                //
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to resolve/analyze maven pom "
                            + ToStringBuilder.reflectionToString(mavenArtifact));
                }
            }

            //
            try {
                Map<String, String> meta = resolveAndAnalyzeBenchmarkPom(mavenArtifact);

                if(meta != null) {
                    result.setMeta(meta);
                }
            } catch (Throwable e) {
                //
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to resolve/analyze maven benchmark pom "
                            + ToStringBuilder.reflectionToString(mavenArtifact));
                }
            }

            LOG.info(String.format("Processing success %s %s", mavenArtifact.toUri(), mavenArtifact.isTests()));

            return result;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to resolve/analyze maven artifact "
                        + ToStringBuilder.reflectionToString(mavenArtifact));
            }
        } finally {
            //
        }

        // signal fail
        return null;
    }

    private MetaData resolveAndAnalyzePom(MavenArtifact mavenArtifact) throws IOException, XmlPullParserException {
        File pomFile = mavenArtifact.getPomFile();

        if(!pomFile.exists()) {
            return null;
        }

        return MavenPomParser.parsePom(pomFile);
    }

    private Map<String, String> resolveAndAnalyzeBenchmarkPom(MavenArtifact mavenArtifact) throws IOException, XmlPullParserException {
        File pomFile = mavenArtifact.getPomFile();

        if(!pomFile.exists()) {
            return null;
        }

        return MavenPomParser.parseBenchmarkPom(pomFile);
    }

    /**
     * @param mavenArtifact {@link MavenArtifact} instance
     * @return {@link List} of {@link CompilationUnit}s.
     * @throws IOException Resolution/Analysis error
     */
    protected List<CompilationUnit> resolveAndAnalyze(MavenArtifact mavenArtifact) throws IOException {
        // analyze
        List<CompilationUnit> compilationUnits = projectAnalyzer.analyze(mavenArtifact);

        return compilationUnits;
    }
}
