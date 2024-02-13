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
package de.uni_mannheim.swt.lasso.arena;

import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Pool of CUTs
 *
 * @author Marcus Kessel
 */
public class CandidatePool {

    private final MavenRepository mavenRepository;
    private final List<ClassUnderTest> classesUnderTest;

    /**
     * Parallel module init?
     */
    private boolean enableParallel = true;

    private ContainerFactory containerFactory = ContainerFactory.DEFAULT_FACTORY;

    private File workingDirectory;

    public CandidatePool(MavenRepository mavenRepository, List<ClassUnderTest> classesUnderTest) {
        this.mavenRepository = mavenRepository;
        this.classesUnderTest = new ArrayList<>(classesUnderTest);
    }

    public CandidatePool(MavenRepository mavenRepository) {
        this(mavenRepository, new LinkedList<>());
    }

    public List<ClassUnderTest> getClassesUnderTest() {
        return classesUnderTest;
    }

    public void addClasses(List<ClassUnderTest> classUnderTestList) {
        this.classesUnderTest.addAll(classUnderTestList);
    }

    public void addClass(ClassUnderTest classUnderTest) {
        this.classesUnderTest.add(classUnderTest);
    }

    public boolean containsClass(String id) {
        return classesUnderTest.stream().anyMatch(c -> StringUtils.equals(c.getId(), id));
    }

    public Optional<ClassUnderTest> getClassUnderTest(String id) {
        return classesUnderTest.stream().filter(c -> StringUtils.equals(c.getId(), id)).findFirst();
    }

    /**
     * Initialize projects with default container.
     */
    public void initProjects() {
        initProjects(containerFactory);
    }

    /**
     * Initialize projects.
     *
     * @param factory
     */
    private void initProjects(ContainerFactory factory) {
        // init modules
        if(enableParallel) {
            classesUnderTest.parallelStream().forEach(cut -> {
                resolve(cut, factory);
            });
        } else {
            classesUnderTest.stream().forEach(cut -> {
                resolve(cut, factory);
            });
        }
    }

    /**
     * Resolve project.
     *
     * @param classUnderTest
     * @param factory
     */
    protected void resolve(ClassUnderTest classUnderTest, ContainerFactory factory) {
        try {
            // resolve project + dependencies
            mavenRepository.resolve(classUnderTest, factory);

            // setup working directory
            if(workingDirectory != null) {
                File projectRoot = new File(workingDirectory, classUnderTest.getId());
                MavenProject mavenProject = new MavenProject(projectRoot);
                classUnderTest.setLocalProject(mavenProject);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Close all projects
     */
    public void close() {
        mavenRepository.close();
    }

    public boolean isEnableParallel() {
        return enableParallel;
    }

    public void setEnableParallel(boolean enableParallel) {
        this.enableParallel = enableParallel;
    }

    public ContainerFactory getContainerFactory() {
        return containerFactory;
    }

    public void setContainerFactory(ContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public MavenRepository getMavenRepository() {
        return mavenRepository;
    }
}
