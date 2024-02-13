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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * {@link ItemReader} for {@link MavenArtifact}s based on Aether/Maven
 * framework.
 * 
 * @author Marcus Kessel
 *
 * @see <a href=
 *      "https://github.com/apache/maven-indexer/blob/master/indexer-examples/indexer-examples-basic/src/main/java/org/apache/maven/indexer/examples/BasicUsageExample.java">https://github.com/apache/maven-indexer</a>
 */
public class MavenArtifactReader implements ItemReader<MavenArtifact> {

    private static final Logger LOG = LoggerFactory.getLogger(MavenArtifactReader.class);

    /**
     * Blacklists
     * 
     * Language runtimes of JVM languages, list extracted from
     * <a href="http://mvnrepository.com/open-source/language-runtime"
     * >mvnrepository.com</a>
     */
    protected static final Set<String> groupIdBlackList = new HashSet<>() {
        {
            // // language runtimes
            // add("org.scala-lang");
            // add("org.codehaus.groovy");
            // add("org.clojure");
            // add("org.jruby");
            // add("org.jetbrains.kotlin");
            // add("org.python");
            // add("org.eclipse.xtend");
            // add("groovy");
            // add("jython");
            // add("jruby");
            // add("pl.symentis.lua4j");
            //
            // // from experience, stucks at some point
            // add("org.vert-x");
            // add("org.glassfish.extras");
            // add("org.scala-lang.virtualized");
            // add("org.graylog2");
        }
    };

    protected final PlexusContainer plexusContainer;
    protected final Indexer indexer;
    protected final IndexUpdater indexUpdater;
    protected final Wagon httpWagon;
    protected IndexingContext centralContext;

    // current index reader
    protected IndexReader indexReader;
    // maximum number of docs
    private int maxDoc;
    // current index
    private int docIndex;

    // resume functionality
    private String resumeAtArtifact;
    //
    private boolean resume;

    /**
     * Run update?
     */
    private boolean allowIndexUpdate;

    /**
     * Constructor
     * 
     * @param mavenIndexDirectory
     *            Maven Index directory
     * @param mavenRepoUrl
     *            Maven repo url (e.g. central)
     * @param resumeAtArtifact
     *            Resume at specific Maven Artifact URI
     * @throws PlexusContainerException
     *             Plexus error
     * @throws ComponentLookupException
     *             Plexus error
     * @throws IOException
     *             I/O error
     */
    public MavenArtifactReader(File mavenIndexDirectory, String mavenRepoUrl, String resumeAtArtifact,
            boolean allowIndexUpdate) throws IOException, PlexusContainerException, ComponentLookupException {
        this.allowIndexUpdate = allowIndexUpdate;
        this.plexusContainer = new DefaultPlexusContainer();

        // indexer
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // fetch index over http
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");

        // init index
        initIndex(mavenIndexDirectory, mavenRepoUrl);

        //
        IndexSearcher searcher = centralContext.acquireIndexSearcher();
        this.indexReader = searcher.getIndexReader();
        // maximum number of docs
        this.maxDoc = indexReader.maxDoc();
        // start index at zero
        this.docIndex = 0;

        // resume (optional)
        this.resumeAtArtifact = resumeAtArtifact;
        // resume activated? weak check if correct Maven URI
        String[] parts = StringUtils.split(resumeAtArtifact, ':');
        this.resume = parts != null && parts.length == 3;
    }

    /**
     * Init maven index (fetch remotely if necessary and/or update it).
     * 
     * @param mavenIndexDirectory
     *            Maven Index directory
     * @param mavenRepoUrl
     * @throws IOException
     *             I/O fetching maven index
     */
    private void initIndex(File mavenIndexDirectory, String mavenRepoUrl) throws IOException {
        try {
            // path where index should be stored
            File centralLocalCache = new File(mavenIndexDirectory, "central-cache");
            File centralIndexDir = new File(mavenIndexDirectory, "central-index");

            // indexer
            List<IndexCreator> indexers = new ArrayList<IndexCreator>();
            indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
            indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
            indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));

            // context
            centralContext = indexer.createIndexingContext("central-context", "central", centralLocalCache,
                    centralIndexDir, mavenRepoUrl, null, true, true, indexers);

            ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, new AbstractTransferListener() {
                @Override
                public void transferStarted(TransferEvent transferEvent) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Downloading index " + transferEvent.getResource().getName());
                    }
                }

                @Override
                public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
                }

                @Override
                public void transferCompleted(TransferEvent transferEvent) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Downloading index completed");
                    }
                }
            }, null, null);

            Date lastUpdated = centralContext.getTimestamp();

            // run index update(s)?
            if (allowIndexUpdate) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Index updates ENABLED");
                }

                // run update
                IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
                IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
                if (updateResult.isFullUpdate()) {
                    System.out.println("Full update happened!");
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Created inital index locally");
                    }
                } else if (updateResult.getTimestamp().equals(lastUpdated)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Local index is up-to-date");
                    }
                } else {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Local index was updated since " + lastUpdated);
                    }
                }
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Index updates DISABLED");
                }
            }
        } catch (Throwable e) {
            throw new IOException("Couldn't fetch remove maven index", e);
        }
    }

    /**
     * Get next {@link MavenArtifact}.
     */
    @Override
    public MavenArtifact read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // check if we are done
        if (docIndex >= maxDoc) {
            // signal end
            return null;
        }

        // current doc index (increment afterwards!)
        MavenArtifact mavenArtifact = null;
        for (int i = docIndex; i < maxDoc; i++) {
            // check if it exists
            if (indexReader.isDeleted(i)) {
                // skip non-existent docs
                continue;
            }

            // get document
            Document doc = indexReader.document(i);
            ArtifactInfo artifactInfo = IndexUtils.constructArtifactInfo(doc, centralContext);

            // check if artifact info
            if (artifactInfo == null) {
                // skip non-existent docs
                continue;
            }

            // do we resume from a past run?
            if (resume) {
                // have we reached last known artifact?
                if (StringUtils.equals(resumeAtArtifact,
                        artifactInfo.groupId + ":" + artifactInfo.artifactId + ":" + artifactInfo.version)
                        && StringUtils.equalsIgnoreCase("sources", artifactInfo.classifier)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Resuming after artifact " + resumeAtArtifact + ". Found Document ID " + i);
                    }
                    // set resume to false
                    resume = false;
                } else {
                    // skip existing artifacts
                }

                // continue in both cases
                continue;
            }

            // check if accepted classifier
            if (accept(artifactInfo)) {
                // break loop (inc doc index!)
                docIndex = i + 1;
                // set artifact
                mavenArtifact = new MavenArtifact(artifactInfo.groupId, artifactInfo.artifactId, artifactInfo.version,
                        artifactInfo.classifier);

                break;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Rejected maven artifact " + Arrays.toString(new String[] { artifactInfo.groupId,
                            artifactInfo.artifactId, artifactInfo.version, artifactInfo.classifier }));
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Accept maven artifact " + ToStringBuilder.reflectionToString(mavenArtifact));
        }

        // return (if null, end reached of this reader)
        // return mavenArtifact;
        return null;
    }

    /**
     * @param artifactInfo
     *            {@link ArtifactInfo} instance
     * @return true if {@link ArtifactInfo} accepted (i.e., sources classifier)
     *         and not blacklisted
     */
    protected boolean accept(ArtifactInfo artifactInfo) {
        return (StringUtils.equalsIgnoreCase(artifactInfo.classifier, "sources")
                || StringUtils.equalsIgnoreCase(artifactInfo.classifier, "test-sources"))
                && !groupIdBlackList.contains(artifactInfo.groupId);
    }
}
