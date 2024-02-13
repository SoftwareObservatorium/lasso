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
package de.uni_mannheim.swt.lasso.analyzer.maven;

import de.uni_mannheim.swt.lasso.analyzer.model.MetaData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maven POM parser.
 *
 * @author Marcus Kessel
 */
public class MavenPomParser {

    private static final Logger LOG = LoggerFactory.getLogger(MavenPomParser.class);

    /**
     * Parse given pom.
     *
     * @param in
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    public static MetaData parsePom(Reader in) throws IOException, XmlPullParserException {

        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(in);

            MetaData meta = createMetaData(model);

            return meta;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to parse pom", e);
            }

            return null;
        }
    }

    // FIXME added for MultiPL-E benchmarks
    public static Map<String, String> parseBenchmarkPom(File pom) throws IOException, XmlPullParserException {

        try(FileReader fileReader = new FileReader(pom)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(fileReader);

            Map<String, Plugin> plugs = model.getBuild().getPluginManagement().getPluginsAsMap();
            Plugin plug = plugs.get("de.uni-mannheim.swt.lasso:indexer-maven-plugin");

            org.codehaus.plexus.util.xml.Xpp3Dom dom = (Xpp3Dom) plug.getConfiguration();

            String metadata = dom.getChild("metadata").getValue();

            // metadata=key1,value1|key2,value2 etc.
            final Map<String, String> meta;
            if(StringUtils.isNotBlank(metadata)) {
                String[] pairs = StringUtils.split(metadata, '|');
                meta = Arrays.stream(pairs).map(p -> StringUtils.split(p, ','))
                        .peek(p -> System.out.println(Arrays.toString(p)))
                        .collect(Collectors.toMap(p -> p[0], p -> p[1]));
            } else {
                meta = Collections.emptyMap();
            }

            return meta;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to parse benchmark pom", e);
            }

            return null;
        }
    }

    /**
     * Parse given pom.
     *
     * @param pom
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    public static MetaData parsePom(File pom) throws IOException, XmlPullParserException {

        try (FileReader fileReader = new FileReader(pom)) {
            return parsePom(fileReader);
        }
    }

    private static MetaData createMetaData(Model model) {
        MetaData meta = new MetaData();


        if (model.getScm() != null && StringUtils.isNotBlank(model.getScm().getConnection())) {
            meta.getValues().put("scm", model.getScm().getConnection());
        }

        if (StringUtils.isNotBlank(model.getDescription())) {
            meta.getValues().put("description", model.getDescription());
        }

        if (StringUtils.isNotBlank(model.getName())) {
            meta.getValues().put("name", model.getName());
        }

        if (StringUtils.isNotBlank(model.getUrl())) {
            meta.getValues().put("url", model.getUrl());
        }

        if (CollectionUtils.isNotEmpty(model.getDependencies())) {
            meta.getValues().put("dependency", getDependencies(model.getDependencies(), model));
        }

        if (CollectionUtils.isNotEmpty(model.getDependencies())) {
            meta.getValues().put("license", getLicenses(model.getLicenses()));
        }

        // modules
        if (CollectionUtils.isNotEmpty(model.getModules())) {
            meta.getValues().put("module", model.getModules());
        }

        if(model.getIssueManagement() != null && StringUtils.isNotBlank(model.getIssueManagement().getUrl())) {
            meta.getValues().put("issueUrl", model.getIssueManagement().getUrl());
        }

        if(model.getOrganization() != null && StringUtils.isNotBlank(model.getOrganization().getName())) {
            meta.getValues().put("orgaName", model.getOrganization().getName());
            if(StringUtils.isNotBlank(model.getOrganization().getUrl())) {
                meta.getValues().put("orgaUrl", model.getOrganization().getName());
            }
        }

        return meta;
    }

    private static List<String> getDependencies(List<Dependency> dependencyList, Model model) {
        // TODO add scope
        return dependencyList.stream().map(d -> {
            // try to resolve version tags
            try {
                String version = d.getVersion();
                if(StringUtils.startsWith(version,"${") && model.getProperties() != null) {
                    String sub = StringUtils.substringBetween(d.getVersion(), "${", "}");
                    if(StringUtils.isNotEmpty(sub)) {
                        String rVersion = model.getProperties().getProperty(sub);

                        if(StringUtils.isNotEmpty(rVersion)) {
                            version = rVersion;
                        }
                    }
                }

                return d.getGroupId() + ":" + d.getArtifactId() + ":" + version;
            } catch (Throwable e) {
                return d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion();
            }
        }).collect(Collectors.toList());
    }

    private static List<String> getLicenses(List<License> licenseList) {
        return licenseList.stream().map(l -> l.getName()).collect(Collectors.toList());
    }

    public static MetaData parsePom(InputStream is) throws IOException, XmlPullParserException {
        return parsePom(new InputStreamReader(is));
    }
}
