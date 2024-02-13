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
package de.uni_mannheim.swt.lasso.core.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven-specific project (build)
 *
 * @author Marcus Kessel
 */
public class MavenProject implements Serializable {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenProject.class);

    public static final String PERM_REPORTS = "perm-reports";

    public static final String LASSO_REPORTS_PATH = ".lasso";

    private final File baseDir;
    private File srcMain;
    private File resMain;
    private File srcTest;
    private File resTest;

    private File lassoBaseDir;

    /**
     * The project's own artifact repository.
     */
    private File artifactRepository;

    public MavenProject(File baseDir) {
        this(baseDir, true);
    }

    public MavenProject(File baseDir, boolean create) {
        this.baseDir = baseDir;

        // maven folder structure
        srcMain = new File(baseDir, "src/main/java/");
        resMain = new File(baseDir, "src/main/resources/");
        srcTest = new File(baseDir, "src/test/java/");
        resTest = new File(baseDir, "src/test/resources/");
        artifactRepository = new File(baseDir, "repository/.m2/");

        lassoBaseDir = new File(baseDir, LASSO_REPORTS_PATH);

        if (create) {
            baseDir.mkdirs();
            // maven folder structure
            srcMain.mkdirs();
            resMain.mkdirs();
            srcTest.mkdirs();
            resTest.mkdirs();

            lassoBaseDir.mkdirs();
        }
    }

    /**
     * @return the baseDir
     */
    public File getBaseDir() {
        return baseDir;
    }

    public File getClasses() {
        return new File(getTarget(), "classes/");
    }

    /**
     * @return the srcMain
     */
    public File getSrcMain() {
        return srcMain;
    }

    /**
     * @return the resMain
     */
    public File getResMain() {
        return resMain;
    }

    /**
     * @return the srcTest
     */
    public File getSrcTest() {
        return srcTest;
    }

    /**
     * @return the resTest
     */
    public File getResTest() {
        return resTest;
    }

    public File getTarget() {
        return new File(baseDir, "target/");
    }

    public File getPermReports() {
        return new File(getTarget(), PERM_REPORTS);
    }

    /**
     * Get permutation record for given id.
     *
     * @param permId
     * @return
     */
    public File getPermReport(int permId) {
        // find record for given permId
        File permReports = getPermReports();

        // exists and has at least one file?
        if (!permReports.exists() || permReports.listFiles().length == 0) {
            return null;
        }

        // FIXME issue with "0_pass.through.Test". but should be no problem

        Optional<File> permFileOptional = Arrays.stream(permReports.listFiles())
                .filter(f -> f.isDirectory() && StringUtils.startsWith(f.getName(), permId + "_")).findFirst();

        if (permFileOptional.isPresent()) {
            return permFileOptional.get();
        }

        return null;
    }

    public void copyFile(MavenProject mavenProject, String filename) throws IOException {
        copyFile(new File(mavenProject.getBaseDir(), filename), new File(baseDir, filename));
    }

    public void copyFileToDirectory(File file, File dir) throws IOException {
        copyFile(file, new File(dir, file.getName()));
    }

    public void copyFile(File from, File to) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Copying " + from.getAbsolutePath() + " to " + to.getAbsolutePath());
        }

        FileUtils.copyFile(from, to);
    }

    public void copyDirectory(File from, File to) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Copying " + from.getAbsolutePath() + " to " + to.getAbsolutePath());
        }

        FileUtils.copyDirectory(from, to);
    }

    /**
     * Copy sources from given project
     *
     * @param other
     * @param tests also copy 'src/test'?
     * @throws IOException
     */
    public void copySrcFrom(MavenProject other, boolean tests) throws IOException {
        try {
            FileUtils.copyDirectory(other.getSrcMain(), getSrcMain());
        } catch (Throwable e) {
            LOG.warn("Copying src/main from '{}' to '{}' failed", other.getSrcMain(), getSrcMain());
        }
        if(tests) {
            try {
                FileUtils.copyDirectory(other.getSrcTest(), getSrcTest());
            } catch (Throwable e) {
                LOG.warn("Copying src/test from '{}' to '{}' failed", other.getSrcMain(), getSrcTest());
            }
        }
    }

    /**
     * Remove all source files (but not base directory).
     *
     * @param test
     * @throws IOException
     */
    public void cleanSourceFiles(boolean test) throws IOException {
        File dir = test ? getSrcTest() : getSrcMain();

        FileUtils.cleanDirectory(dir);
    }

    /**
     * Write {@link CompilationUnit} to source code directory
     *
     * @param unit {@link CompilationUnit} instance
     * @throws IOException I/O error
     */
    public void writeCompilationUnit(CompilationUnit unit, boolean test) throws IOException {
        // write
        FileUtils.writeStringToFile(new File(test ? getSrcTest() : getSrcMain(), unit.getJavaClassPath()),
                unit.getSourceCode());
    }

    /**
     * Write {@link CodeUnit} to source code directory
     *
     * @param unit {@link CodeUnit} instance
     * @throws IOException I/O error
     */
    public void writeCompilationUnit(CodeUnit unit, boolean test) throws IOException {
        // write
        FileUtils.writeStringToFile(new File(test ? getSrcTest() : getSrcMain(),
                        CompilationUnit.toJavaPkgPath(unit.getPackagename() + "." + unit.getName())),
                unit.getContent());
    }

    /**
     * @return list of {@link File}s having suffix
     */
    public List<File> getFiles(File directory, String suffix) {
        return getFilesAsList(directory, suffix);
    }

    /**
     * List all files with given suffix (recursively!)
     *
     * @param directory
     *            {@link File} denoting directory
     * @param suffix
     *            File suffix
     * @return list of {@link File}s having suffix
     */
    public static List<File> getFilesAsList(File directory, String suffix) {
        return new ArrayList<>(FileUtils.listFiles(directory, new String[] { suffix }, true));
    }

    public File getArtifactRepository() {
        return artifactRepository;
    }

    public void setArtifactRepository(File artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    public File getLassoBaseDir() {
        return lassoBaseDir;
    }


}
