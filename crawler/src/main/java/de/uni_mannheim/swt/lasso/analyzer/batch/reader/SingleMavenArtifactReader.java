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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link ItemReader} for {@link MavenArtifact}s based on Aether/Maven
 * framework.
 *
 * @author Marcus Kessel
 */
public class SingleMavenArtifactReader implements ItemReader<MavenArtifact> {

    private static final Logger LOG = LoggerFactory.getLogger(SingleMavenArtifactReader.class);

    private final Iterator<MavenArtifact> iterator;

    private List<MavenArtifact> artifactList = Collections.synchronizedList(new LinkedList<>());

    /**
     * Read artifacts from string.
     *
     * <pre>
     *  org.apache.commons:commons-lang3:3.14.0:sources
     * </pre>
     *
     * Separated by '|'.
     *
     * @param artifacts
     */
    public SingleMavenArtifactReader(String artifacts) {
        if(StringUtils.contains(artifacts, '|')) {
            String[] aArr = StringUtils.split(artifacts, '|');
            for(String artifactStr : aArr) {
                String[] parts = StringUtils.split(artifactStr, ':');
                artifactList.add(new MavenArtifact(parts[0], parts[1], parts[2], parts[3]));
            }
        } else {
            String[] parts = StringUtils.split(artifacts, ':');
            artifactList.add(new MavenArtifact(parts[0], parts[1], parts[2], parts[3]));
        }

        this.iterator = artifactList.iterator();
    }

    /**
     * Get next {@link MavenArtifact}.
     */
    @Override
    public MavenArtifact read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }
}
