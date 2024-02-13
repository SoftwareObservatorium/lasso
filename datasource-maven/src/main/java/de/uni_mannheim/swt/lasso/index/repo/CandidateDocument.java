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
package de.uni_mannheim.swt.lasso.index.repo;

import de.uni_mannheim.swt.lasso.index.match.SignatureMatch;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Candidate document.
 * 
 * @author Marcus Kessel
 *
 */
public interface CandidateDocument {

    /**
     * @return Artifact identifier
     */
    default String getUri() {
        return StringUtils.join(Arrays.asList(getGroupId(), getArtifactId(), getVersion()), ':');
    }

    /**
     * @return Fully-qualified class name of entry class of this candidate in
     *         Java-like format
     */
    default String getFQName() {
        return StringUtils.join(Arrays.asList(getPackagename(), getName()), '.');
    }

    /**
     * @param other
     *            {@link CandidateDocument} instance
     * @return true if they are similar based on groupId, artifactId, name and
     *         packagename
     */
    default boolean isSimilar(CandidateDocument other) {
        return other != null && StringUtils.equals(getGroupId(), other.getGroupId())
                && StringUtils.equals(getArtifactId(), other.getArtifactId())
                && StringUtils.equals(getName(), other.getName())
                && StringUtils.equals(getPackagename(), other.getPackagename());
    }

    Map<String, Object> getValues();

    String getId();

    String getParentId();

    String getName();

    String getPackagename();

    String getBytecodeName();

    String getGroupId();

    String getArtifactId();

    String getVersion();

    double getScore();

    String getContent();

    String getHash();

    String getDocType();

    String getType();

    String getMethodSignature();

    List<String> getMethods();

    List<String> getSuperClasses();

    List<String> getInterfaces();

    List<String> getDependencies();

    Map<String, Double> getMetrics();

    Map<String, Object> getMetaData();

    String getInheritedMethodSignature();

    List<String> getInheritedMethods();

    List<SignatureMatch> getMethodSignatureMatches();

    List<SignatureMatch> getConstructorSignatureMatches();

    /**
     * Alternatives to current document (e.g., from same project)
     *
     * @return
     */
    List<CandidateDocument> getAlternatives();

    /**
     * Get clone documents (e.g., hash clones)
     *
     * @return
     */
    List<CandidateDocument> getClones();

    /**
     * Get similar documents (e.g., likely clones)
     *
     * @return
     */
    List<CandidateDocument> getSimilar();
}
