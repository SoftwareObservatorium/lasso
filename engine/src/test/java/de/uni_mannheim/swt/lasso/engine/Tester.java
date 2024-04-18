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
package de.uni_mannheim.swt.lasso.engine;

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.corpus.ArtifactRepository;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.action.utils.SequenceUtils;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Tester providing some utilities for convenience
 * 
 * @author Marcus Kessel
 *
 */
public class Tester {
    
    /**
     * Load resources from classpath
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String getResource(String fileName) throws IOException {
        return IOUtils.toString(Tester.class
                .getResourceAsStream(fileName));
    }

    public static File getResourceFile(String fileName) {
        return FileUtils.toFile(Tester.class
                .getResource(fileName));
    }

    public static LSLExecutionContext ctx(String mavenRepoUrl) {
        LSLExecutionContext context = new LSLExecutionContext();
        Workspace workspace = new Workspace();
        workspace.setLassoRoot(new File("/tmp/lasso_rnd_" + System.currentTimeMillis()));
        context.setWorkspace(workspace);

        ExecutableCorpus executableCorpus = new ExecutableCorpus();
        ArtifactRepository artifactRepository = new ArtifactRepository();
        artifactRepository.setUrl(mavenRepoUrl);
        executableCorpus.setArtifactRepository(artifactRepository);
        context.setConfiguration(new LassoConfiguration() {
            @Override
            public DataSource getDataSource(String id) {
                return null;
            }

            @Override
            public ExecutableCorpus getExecutableCorpus() {
                return executableCorpus;
            }

            @Override
            public <T> T getProperty(String name, Class<T> tClass) {
                return null;
            }

            @Override
            public <T> T getService(Class<T> type) {
                return null;
            }
        });

        return context;
    }

    public static de.uni_mannheim.swt.lasso.core.model.System system(String id, String name, String pkg) {
        CodeUnit codeUnit = new CodeUnit();
        codeUnit.setId(id);
        codeUnit.setName(name);
        codeUnit.setPackagename(pkg);
        MavenProject mavenProject = new MavenProject(new File("/tmp/project_" + id + "_" + System.currentTimeMillis()), true);
        de.uni_mannheim.swt.lasso.core.model.System system = new de.uni_mannheim.swt.lasso.core.model.System(codeUnit, mavenProject);

        return system;
    }

    public static LQLParseResult parse(String lql) {
        return SequenceUtils.parseLQL(lql);
    }
}
