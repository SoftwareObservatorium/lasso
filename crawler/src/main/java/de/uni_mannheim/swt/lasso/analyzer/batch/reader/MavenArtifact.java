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

import org.apache.commons.lang3.StringUtils;

/**
 * Description of a Maven artifact (see Maven core concepts).
 * 
 * @author Marcus Kessel
 * 
 */
public class MavenArtifact {

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private File sourceJar;
    private File binaryJar;

    private boolean latestVersion;

    /**
     * Version "head" starting at 0 = unknown, 1 = latest Version, 2 = second latest etc.
     */
    private int versionHead = 0;

    public MavenArtifact(String groupId, String artifactId, String version, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    public String toUri() {
        return StringUtils.join(new String[]{groupId, artifactId, version, classifier}, ':');
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the classifier
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Returns if two artifacts are similar wrt to groupid and artifactid
     * (version is ignored)
     * 
     * @param other
     * @return
     */
    public boolean isSimilar(MavenArtifact other) {
        return StringUtils.equals(other.getGroupId(), getGroupId())
                && StringUtils.equals(other.getArtifactId(), getArtifactId());
    }

    /**
     * @return the sourceJar
     */
    public File getSourceJar() {
        return sourceJar;
    }

    /**
     * @param sourceJar
     *            the sourceJar to set
     */
    public void setSourceJar(File sourceJar) {
        this.sourceJar = sourceJar;
    }

    /**
     * @return the binaryJar
     */
    public File getBinaryJar() {
        return binaryJar;
    }

    /**
     * @param binaryJar
     *            the binaryJar to set
     */
    public void setBinaryJar(File binaryJar) {
        this.binaryJar = binaryJar;
    }

    /**
     * @return the latestVersion
     */
    public boolean isLatestVersion() {
        return latestVersion;
    }

    /**
     * @param latestVersion
     *            the latestVersion to set
     */
    public void setLatestVersion(boolean latestVersion) {
        this.latestVersion = latestVersion;
    }

    /**
     * @return test artifact if classifier equals "test-sources"
     */
    public boolean isTestArtifact() {
        return StringUtils.equalsIgnoreCase(classifier, "test-sources");
    }

    public int getVersionHead() {
        return versionHead;
    }

    public void setVersionHead(int versionHead) {
        this.versionHead = versionHead;
    }
}
