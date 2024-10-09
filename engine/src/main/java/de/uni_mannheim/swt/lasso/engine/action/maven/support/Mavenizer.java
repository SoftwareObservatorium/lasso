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
package de.uni_mannheim.swt.lasso.engine.action.maven.support;

import de.uni_mannheim.swt.lasso.corpus.ArtifactRepository;
import de.uni_mannheim.swt.lasso.datasource.maven.build.Candidate;
import de.uni_mannheim.swt.lasso.core.model.MavenArtifact;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.dependency.DependencyAnalyzer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Setup a maven project
 * 
 * @author Marcus Kessel
 *
 */
public class Mavenizer {

    public static final String MAVENIZER_POM_TEMPLATE_XML = "/mavenizer/pom.template";
    public static final String POM_TEMPLATE = getPomTemplate(MAVENIZER_POM_TEMPLATE_XML);

    public static final String MAVENIZER_AGGREGATED_POM_TEMPLATE_XML = "/mavenizer/pom_aggregated.template";
    public static final String TEMPLATE_AGGREGATED = getPomTemplate(MAVENIZER_AGGREGATED_POM_TEMPLATE_XML);
    public static final String DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS = "de.uni_mannheim.swt.lasso.systems";

    private final File baseDir;

    private final Map<String, String> mvnOptions;

    public Mavenizer(File baseDir, Map<String, String> mvnOptions) {
        this.baseDir = baseDir;
        this.mvnOptions = mvnOptions;
    }

    public MavenProject createAggregatedMavenProject(String actionId, ExecGroup execGroup) throws IOException {
        MavenProject mavenProject = new MavenProject(new File(baseDir, actionId), false);

        // create pom source
        String pomSource = createAggregatedPom(actionId, execGroup);

        // write pom.xml
        FileUtils.write(new File(mavenProject.getBaseDir(), "pom.xml"), pomSource, Charset.forName("UTF-8"));

        return mavenProject;
    }

    public MavenProject createMavenProject(LSLExecutionContext context, String actionId, Candidate candidate, boolean create, String pomTemplate, Map<String, Object> valueMap) throws IOException {
        MavenProject mavenProject = new MavenProject(new File(baseDir, actionId + "/" + candidate.getId()), create);

        // add artifact repository
        ArtifactRepository artifactRepository = context.getConfiguration().getExecutableCorpus().getArtifactRepository();
        valueMap.put("repoId", artifactRepository.getId());
        valueMap.put("repoUrl", artifactRepository.getUrl());

        // create pom source
        String pomSource = createPom(candidate, pomTemplate, valueMap);

        // write pom.xml
        FileUtils.write(new File(mavenProject.getBaseDir(), "pom.xml"), pomSource, Charset.forName("UTF-8"));

        return mavenProject;
    }

    private String createPom(Candidate candidate, String pomTemplate, Map<String, Object> valueMap) {
        MavenArtifact mavenArtifact = candidate.getArtifact().asType(MavenArtifact.class);

        String groupId = mavenArtifact.getGroupId();
        if(StringUtils.isBlank(groupId)) {
            groupId = DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS;
        }

        valueMap.put("groupId", groupId);
        valueMap.put("artifactId", candidate.getId());
        if(!valueMap.containsKey("version")) {
            valueMap.put("version", mavenArtifact.getVersion());
        }

//        //  ${extraArgLine}
//        String extraArgLine = "";
//        if(mvnOptions.containsKey("extraArgLine")) {
//            extraArgLine = mvnOptions.get("extraArgLine");
//        }
//        valueMap.put("extraArgLine", extraArgLine);

        if(MapUtils.isNotEmpty(mvnOptions)) {
            // add keys
            valueMap.putAll(mvnOptions);
        }

        // candidate's dependency
        List<String> dependencies = new LinkedList<>();
        dependencies.add(toDependencyDeclaration(mavenArtifact));

        // add missing dependencies
        if(CollectionUtils.isNotEmpty(candidate.getResolvedDependencies())) {
            //
            List<String> missingDeps = candidate.getResolvedDependencies().stream()
                    .map(a -> toDependencyDeclaration(a.asType(MavenArtifact.class)))
                    .collect(Collectors.toList());
            dependencies.addAll(missingDeps);
        }

        // write all dependencies to pom
        valueMap.put("dependencies", dependencies.stream().collect(Collectors.joining("\n")));

        String pomSource = StrSubstitutor.replace(pomTemplate, valueMap);

        return pomSource;
    }


    private String createAggregatedPom(String actionId, ExecGroup execGroup) {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("groupId", DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS);
        valueMap.put("artifactId", actionId);
        valueMap.put("version", "1.0");

        //

        // write all modules to pom
        valueMap.put("modules", execGroup.getExecutables().stream().map(c -> "<module>" + c.getId() + "</module>").collect(Collectors.joining("\n")));

        String pomSource = StrSubstitutor.replace(TEMPLATE_AGGREGATED, valueMap);

        return pomSource;
    }

    private String toDependencyDeclaration(MavenArtifact mavenArtifact) {
        String exclusions = DependencyAnalyzer.EXCLUDE_GROUPIDS_EXACT.stream()
                .map(this::createExclusion).collect(Collectors.joining());

        return "<dependency><groupId>" + mavenArtifact.getGroupId() + "</groupId><artifactId>"
                + mavenArtifact.getArtifactId() + "</artifactId><version>" + mavenArtifact.getVersion()
                + "</version>" +
                // make sure to exclude our testing dependencies to avoid dependency hell
                "<exclusions>" +
                exclusions +
                "</exclusions>" +
                "</dependency>";
    }

    private String createExclusion(String groupId) {
        return "<exclusion>" +
                "<groupId>"+groupId+"</groupId>" +
                "<artifactId>*</artifactId>" +
                "</exclusion>";
    }

    /**
     * Load pom.xml template
     *
     * @param resourcePath
     * @return
     * @throws RuntimeException
     */
    public synchronized static String getPomTemplate(String resourcePath) {
        try {
            return IOUtils.toString(Mavenizer.class.getResourceAsStream(resourcePath));
        } catch (IOException e) {
            throw new RuntimeException("Cannot find pom template at " + resourcePath, e);
        }
    }
}
