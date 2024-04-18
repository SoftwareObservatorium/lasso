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
package de.uni_mannheim.swt.lasso.llm.problem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.Project;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.classloader.Containers;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MultiPL-E benchmarks (Java only).
 *
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://huggingface.co/datasets/nuprl/MultiPL-E-raw-data/">MultiPL-E-raw-data</a>
 */
public class MultiPLE {

    private static final Logger LOG = LoggerFactory
            .getLogger(MultiPLE.class);

    // FIXME change and make configurable
    String mavenRepoUrl = "https://swtweb.informatik.uni-mannheim.de/nexus/repository/maven-public/";
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());

    public List<Problem> getProblems(String benchmark) throws IOException {
        return getProblems(MultiPLE.class.getResourceAsStream(benchmark));
    }

    public List<Problem> getProblems(InputStream in) throws IOException {
        String json = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        return getProblemsFromJson(json);
    }

    public List<Problem> getProblems(File path) throws IOException {
        String json = FileUtils.readFileToString(path, StandardCharsets.UTF_8);

        return getProblemsFromJson(json);
    }

    public List<Problem> getProblemsFromJson(String json) {
        Gson gson = new Gson();
        List<Problem> problems = gson.fromJson(json, new TypeToken<List<Problem>>(){}.getType());

        return problems;
    }

    public static Map<String, Problem> toMap(List<Problem> problemList) {
        return problemList.stream().collect(Collectors.toMap(p -> p.getName(), p -> p));
    }

    public Container createContainer() throws IOException {
        return createContainer(resolver);
    }

    public Container createContainer(DependencyResolver resolver) throws IOException {
        /*
            <dependency>
                <groupId>org.javatuples</groupId>
                <artifactId>javatuples</artifactId>
                <version>1.2</version>
            </dependency>
         */
        Project project = new Project("org.javatuples:javatuples:1.2");

        DefaultArtifact artifact = new DefaultArtifact(
                "org.javatuples",
                "javatuples",
                null,
                "jar",
                "1.2");
        try {
            LOG.info("Downloading artifact {}", artifact);
            DependencyResult dependencyResult = resolver.resolveTransitiveDependencies(artifact, null);
            project.setDependencyResult(dependencyResult);

            //
            CodeUnit codeUnit = new CodeUnit();
            codeUnit.setName("Exist");
            codeUnit.setPackagename("does.not");
            codeUnit.setGroupId("org.javatuples");
            codeUnit.setArtifactId("javatuples");
            codeUnit.setVersion("1.2");
            de.uni_mannheim.swt.lasso.core.model.System system = new de.uni_mannheim.swt.lasso.core.model.System(codeUnit);

            ClassUnderTest classUnderTest = new ClassUnderTest(system);
            classUnderTest.setProject(project);

            Containers containers = new Containers();
            Container container = containers.createUnsafe(classUnderTest, ContainerFactory.DEFAULT_FACTORY);
            return container;
        } catch (Throwable e) {
            LOG.warn("Downloading artifact {} FAILED", artifact);

            throw new IOException(e);
        }
    }
}
