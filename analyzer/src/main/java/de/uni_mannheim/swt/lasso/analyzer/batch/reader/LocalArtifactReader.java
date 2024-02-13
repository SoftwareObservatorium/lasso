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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.iterators.PushbackIterator;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Read artifacts from local directory.
 *
 * @author Marcus Kessel
 */
public class LocalArtifactReader implements ItemReader<MavenArtifact> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalArtifactReader.class);

    private final File repositoryDir;

    private PushbackIterator<MavenArtifact> iterator;

    private final Lock readLock = new ReentrantLock();

    public LocalArtifactReader(File repositoryDir) {
        this.repositoryDir = repositoryDir;

        // list all info files
        listFolders();
    }

    private void listFolders() {
        List<File> infoFiles = (List<File>) FileUtils.listFiles(repositoryDir, FileFilterUtils.nameFileFilter("lasso_info"), FileFilterUtils.trueFileFilter());

        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Found '%s' info files", infoFiles.size()));
        }

        // shuffle files to avoid sequences of heavy "jars" (improve memory consumption)
        Collections.shuffle(infoFiles);

        Iterator<File> infoFilesIt = infoFiles.iterator();

        TransformIterator<File, MavenArtifact> t = new TransformIterator<>(infoFilesIt);
        t.setTransformer((infoFile) -> toMavenArtifact(repositoryDir, infoFile));

        // enables adding new elements during iteration ..
        iterator = new PushbackIterator<>(t);
    }

    @Override
    public MavenArtifact read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        try {
            // lock
            readLock.lock();

            MavenArtifact mavenArtifact = this.iterator.hasNext() ? this.iterator.next() : null;

            // end
            if (mavenArtifact == null) {
                return null;
            }

            if (mavenArtifact.getTestArtifact() != null && mavenArtifact.getTestArtifact().getBinaryJar() != null) {
                iterator.pushback(mavenArtifact.getTestArtifact());
            }

            return mavenArtifact;
        } finally {
            readLock.unlock();
        }
    }

    public static MavenArtifact toMavenArtifact(File repositoryDir, File infoFile) {
        File parent = infoFile.getParentFile();

        String repoPath = repositoryDir.getAbsolutePath() + File.separator;
        String parentPath = parent.getAbsolutePath();
        String uri = StringUtils.substringAfter(parentPath, repoPath);

        String[] parts = StringUtils.split(uri, File.separator);
        String version = parts[parts.length - 1];
        String artifactId = parts[parts.length - 2];
        String groupId = StringUtils.join(ArrayUtils.subarray(parts, 0, parts.length - 2), ".");

        MavenArtifact mavenArtifact = new MavenArtifact(groupId, artifactId, version);
        MavenArtifact testArtifact = new MavenArtifact(groupId, artifactId, version);
        mavenArtifact.setTestArtifact(testArtifact);

        Collection<File> jars = FileUtils.listFiles(parent, new String[]{"jar"}, false);
        for (File jar : jars) {
            if (StringUtils.endsWith(jar.getName(), "-test-sources.jar")) {
                testArtifact.setSourceJar(jar);
            } else if (StringUtils.endsWith(jar.getName(), "-sources.jar")) {
                mavenArtifact.setSourceJar(jar);
            } else if (StringUtils.endsWith(jar.getName(), "-tests.jar")) {
                testArtifact.setBinaryJar(jar);
            } else {
                mavenArtifact.setBinaryJar(jar);
            }
        }

        Collection<File> poms = FileUtils.listFiles(parent, new String[]{"pom"}, false);
        if (CollectionUtils.isNotEmpty(poms)) {
            Optional<File> pom = poms.stream().findFirst();
            if (pom.isPresent()) {
                mavenArtifact.setPomFile(pom.get());
                mavenArtifact.getTestArtifact().setPomFile(pom.get());
            }
        }

        // read most recent version from lasso_info
        try {
            String content = FileUtils.readFileToString(infoFile, "UTF-8");

            // version_head:%s
            int head = NumberUtils.toInt(StringUtils.substringAfter(content, ":"), -1);
            mavenArtifact.setVersionHead(head);
            if(mavenArtifact.getTestArtifact() != null) {
                mavenArtifact.getTestArtifact().setLatestVersion(true);
            }
        } catch (Throwable e) {
            LOG.warn(String.format("Failed to read info file %s", infoFile), e);
        }

        return mavenArtifact;
    }
}
