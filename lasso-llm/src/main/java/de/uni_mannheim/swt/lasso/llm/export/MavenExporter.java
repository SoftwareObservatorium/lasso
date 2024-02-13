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
package de.uni_mannheim.swt.lasso.llm.export;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.MavenArtifact;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.llm.eval.ExecutedSolution;
import de.uni_mannheim.swt.lasso.llm.problem.Problem;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create maven project for each problem and solution (idea can be indexed later)
 *
 * @author Marcus Kessel
 */
public class MavenExporter {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenExporter.class);

    private static final String POM_TEMPLATE =
            getPomTemplate("/poms/pom_package.template");

    private File baseDir;
    private Map<String, String> mvnOptions;

    public MavenExporter(File baseDir, Map<String, String> mvnOptions) {
        this.baseDir = baseDir;
        this.mvnOptions = mvnOptions;
    }

    public void export(Problem problem, ExecutedSolution solution, Map<String, Object> metaData) throws IOException {
        int k = (int) metaData.get("k");
        String generator = (String) metaData.get("generator");

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("groupId", problem.getName());
        valueMap.put("artifactId", generator);
        valueMap.put("version", "" + k);
        valueMap.put("jdk.version", 11);
        valueMap.put("metaData", metaData.get("metaData"));

        MavenProject project = createMavenProject(POM_TEMPLATE, k, valueMap);

        CodeUnit codeUnit = codeUnit(problem, solution);

        project.writeCompilationUnit(codeUnit, false);
    }

    private CodeUnit codeUnit(Problem problem, ExecutedSolution solution) {
        CodeUnit unit = new CodeUnit();
        unit.setId(UUID.randomUUID().toString());
        //unit.setDataSource(getId());
        unit.setName("Problem");
        unit.setPackagename(problem.getName());

        // alter content to contain package name
        // FIXME also remove main method?
        unit.setContent("package " + problem.getName() + ";\n\n" + solution.getProgram());
        unit.setUnitType(CodeUnit.CodeUnitType.CLASS);

        return unit;
    }

    public MavenProject createMavenProject(String pomTemplate, int k, Map<String, Object> valueMap) throws IOException {
        MavenProject mavenProject = new MavenProject(new File(baseDir, String.valueOf(k)), true);

        // create pom source
        String pomSource = createPom(pomTemplate, valueMap);

        // write pom.xml
        FileUtils.write(new File(mavenProject.getBaseDir(), "pom.xml"), pomSource, Charset.forName("UTF-8"));

        return mavenProject;
    }

    public String createPom(String pomTemplate, Map<String, Object> valueMap) {
        MavenArtifact mavenArtifact = new MavenArtifact();
        mavenArtifact.setGroupId("org.javatuples");
        mavenArtifact.setArtifactId("javatuples");
        mavenArtifact.setVersion("1.2");

        if(MapUtils.isNotEmpty(mvnOptions)) {
            // add keys
            valueMap.putAll(mvnOptions);
        }

        // candidate's dependency
        List<String> dependencies = new LinkedList<>();
        dependencies.add(toDependencyDeclaration(mavenArtifact));

        // write all dependencies to pom
        valueMap.put("dependencies", dependencies.stream().collect(Collectors.joining("\n")));

        String pomSource = StrSubstitutor.replace(pomTemplate, valueMap);

        return pomSource;
    }

    private String toDependencyDeclaration(MavenArtifact mavenArtifact) {
        return "<dependency><groupId>" + mavenArtifact.getGroupId() + "</groupId><artifactId>"
                + mavenArtifact.getArtifactId() + "</artifactId><version>" + mavenArtifact.getVersion()
                + "</version>" +
                "</dependency>";
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
            return IOUtils.toString(MavenExporter.class.getResourceAsStream(resourcePath));
        } catch (IOException e) {
            throw new RuntimeException("Cannot find pom template at " + resourcePath, e);
        }
    }

    public void createAggregatedPom(File baseDir, Problem problem, String generator) throws IOException {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("groupId", problem.getName());
        valueMap.put("artifactId", generator);
        valueMap.put("version", "1.0");

        String[] dirs = baseDir.list(DirectoryFileFilter.DIRECTORY);

        // write all modules to pom
        valueMap.put("modules", Arrays.stream(dirs).map(d -> "<module>" + d + "</module>").collect(Collectors.joining("\n")));

        String pomSource = StrSubstitutor.replace(MavenExporter.getPomTemplate("/poms/pom_aggregated.template"), valueMap);

        // write pom.xml
        FileUtils.write(new File(baseDir, "pom.xml"), pomSource, Charset.forName("UTF-8"));
    }
}
