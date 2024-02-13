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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.Grouping;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Artifact Reader that only considers the latest version of each artifact
 * (optionally allow for more recent version by taking a certain head size for
 * each artifact).
 * 
 * @author Marcus Kessel
 *
 */
public class LatestVersionArtifactReader extends MavenArtifactReader {

    private static final Logger LOG = LoggerFactory.getLogger(LatestVersionArtifactReader.class);

    // Head size of latest versions to take for each artifact (> 0)
    private int latestVersionHeadSize;

    /**
     * {@link GroupedSearchResponse} instance
     */
    private GroupedSearchResponse searchResponse;

    // latest version head hits
    private int latestVersionHeadHits;

    // current iterators
    private Iterator<Entry<String, ArtifactInfoGroup>> resultEntryIt;
    private Iterator<ArtifactInfo> artifactInfoIt;
    // artifact counter
    private int artifactCounter = 0;

    // current head count for artifact
    private int headCount = 0;

    /**
     * Is resume enabled?
     */
    private boolean resumeEnabled;

    private boolean testArtifactMode;

    private String archiveExtension;

    /**
     * Lock to ensure consistent Maven repo
     */
    private final Lock readLock = new ReentrantLock();

    /**
     * Constructor
     * 
     * @param mavenIndexDirectory
     *            Maven Index directory
     * @param mavenRepoUrl
     *            Maven repo url (e.g. central)
     * @param latestVersionHeadSize
     *            Head size of latest versions to take for each artifact (> 0)
     * @param allowIndexUpdate
     *            Enable index updates?
     * @throws PlexusContainerException
     *             Plexus error
     * @throws ComponentLookupException
     *             Plexus error
     * @throws IOException
     *             I/O error
     */
    public LatestVersionArtifactReader(File mavenIndexDirectory, String mavenRepoUrl, int latestVersionHeadSize,
            boolean allowIndexUpdate, boolean testArtifactMode)
            throws IOException, PlexusContainerException, ComponentLookupException {
        super(mavenIndexDirectory, mavenRepoUrl, null, allowIndexUpdate);

        this.testArtifactMode = testArtifactMode;

        Validate.isTrue(latestVersionHeadSize > 0, "latestVersionHeadSize must be greater 0");
        this.latestVersionHeadSize = latestVersionHeadSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MavenArtifact read()
            throws Exception {
        try {
            // lock
            readLock.lock();

            // first run?
            if (searchResponse == null) {
                if (testArtifactMode) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Test Maven artifact mode");
                    }

                    // query for "test-sources"
                    searchResponse = queryForTestSources();
                } else {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Maven artifact mode");
                    }

                    // query for "sources"
                    searchResponse = queryForSources();
                }
            }

            // get next artifact
            MavenArtifact mavenArtifact = getNext();

            if (mavenArtifact != null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Accepted maven artifact no. " + (artifactCounter++) + ": "
                            + ToStringBuilder.reflectionToString(mavenArtifact));
                }

                //
                return mavenArtifact;
            }
        } finally {
            // unlock
            readLock.unlock();
        }

        // null signals end
        return null;
    }

    /**
     * Resolve next {@link ArtifactInfo} instance based on the following
     * criteria:
     * 
     * <ol>
     * <li>Get next Result Group</li>
     * <li>Get next Artifact Info of current Result Group</li>
     * <li>Limit Artifact Info of current Result Group to
     * latestVersionHeadSize</li>
     * </ol>
     * 
     * @return {@link MavenArtifact} instance
     */
    private MavenArtifact getNext() {
        if (resultEntryIt == null) {
            resultEntryIt = searchResponse.getResults().entrySet().iterator();
        }

        boolean groupEntryAvailable = artifactInfoIt != null && artifactInfoIt.hasNext();

        // next artifact available?
        if (!groupEntryAvailable && resultEntryIt.hasNext()) {
            artifactInfoIt = resultEntryIt.next().getValue().getArtifactInfos().iterator();
            // has next?
            groupEntryAvailable = artifactInfoIt.hasNext();

            // head count reset
            headCount = 0;
        }

        // entries available for specific artifact?
        if (groupEntryAvailable) {
            ArtifactInfo artifactInfo = artifactInfoIt.next();

            // accepted?
            if (accept(artifactInfo) && headCount < latestVersionHeadSize) {
                // resume support, move on to next artifact if already indexed
                // by us and if not processed yet (runtime, headCount < 1). If
                // headCount > 0, process artifact
                if (headCount == 0 && isAvailable(artifactInfo)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Ignoring already indexed artifact " + Arrays.toString(
                                new String[] { artifactInfo.groupId, artifactInfo.artifactId, artifactInfo.version }));
                    }

                    // move on to next artifact if already indexed
                    artifactInfoIt = null;
                    //
                    return getNext();
                }

                // inc head count
                headCount++;
                // entry available
                MavenArtifact mavenArtifact = new MavenArtifact(artifactInfo.groupId, artifactInfo.artifactId,
                        artifactInfo.version, artifactInfo.classifier);
                // is latest version?
                mavenArtifact.setLatestVersion(headCount == 1);

                // set version head
                mavenArtifact.setVersionHead(headCount);

                return mavenArtifact;
            } else {
                // move on to next artifact
                artifactInfoIt = null;
                //
                return getNext();
            }
        } else {
            // no more available
            return null;
        }
    }

    /**
     * Query for artifacts having "sources" classifier
     * 
     * @return {@link GroupedSearchResponse} instance
     * @throws IOException
     *             I/O index error
     */
    protected GroupedSearchResponse queryForSources() throws IOException {
        // only restrict on classifier = sources
        return query(indexer.constructQuery(MAVEN.CLASSIFIER, new SourcedSearchExpression("sources")),
                new GAGrouping());
    }

    /**
     * Query for artifacts having "test-sources" classifier (i.e., test
     * artifacts). Note that binary test artifacts are classified by "tests".
     * 
     * @return {@link GroupedSearchResponse} instance
     * @throws IOException
     *             I/O index error
     */
    protected GroupedSearchResponse queryForTestSources() throws IOException {
        // only restrict on classifier = sources
        return query(indexer.constructQuery(MAVEN.CLASSIFIER, new SourcedSearchExpression("test-sources")),
                new GAGrouping());
    }

    /**
     * Group query Maven Index
     * 
     * @param query
     *            {@link Query} instance
     * @param grouping
     *            {@link Grouping} instance
     * @return {@link GroupedSearchResponse} instance
     * @throws IOException
     */
    private GroupedSearchResponse query(Query query, Grouping grouping) throws IOException {
        GroupedSearchResponse response = indexer
                .searchGrouped(new GroupedSearchRequest(query, grouping, centralContext));

        if (LOG.isInfoEnabled()) {
            LOG.info("Index total hits count: " + response.getTotalHitsCount());
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Index latest version hits count: " + response.getResults().size());
        }

        latestVersionHeadHits = 0;
        for (Map.Entry<String, ArtifactInfoGroup> entry : response.getResults().entrySet()) {
            Iterator<ArtifactInfo> aiIt = entry.getValue().getArtifactInfos().iterator();
            int count = 0;
            while (count < latestVersionHeadSize && aiIt.hasNext()) {
                // inc
                latestVersionHeadHits++;

                // inc
                count++;
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Index latest version head hits count: " + latestVersionHeadHits);
        }

        return response;
    }

    /**
     * Check if {@link ArtifactInfo} is already indexed at our side
     * 
     * @param artifactInfo
     *            {@link ArtifactInfo}
     * @return true if artifact is indexed, false otherwise (and false in case
     *         of query error). In case of NOT {@link #resumeEnabled}, returns
     *         false
     */
    private boolean isAvailable(ArtifactInfo artifactInfo) {
        // don't check (expensive) if resume disabled
        if (!resumeEnabled) {
            return false;
        }

        // TODO checks
        return true;
    }

    /**
     * @return the resumeEnabled
     */
    public boolean isResumeEnabled() {
        return resumeEnabled;
    }

    /**
     * @param resumeEnabled
     *            the resumeEnabled to set
     */
    public void setResumeEnabled(boolean resumeEnabled) {
        this.resumeEnabled = resumeEnabled;
    }

    public String getArchiveExtension() {
        return archiveExtension;
    }

    public void setArchiveExtension(String archiveExtension) {
        this.archiveExtension = archiveExtension;
    }
}
