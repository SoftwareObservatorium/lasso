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
package de.uni_mannheim.swt.lasso.arena.sequence.compile;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.compile.InMemoryJavaObject;

import javax.tools.*;
import java.io.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Based on Randoop's SequenceCompiler.
 *
 * @author Marcus Kessel
 */
public class JavaCompiler {

    private static final Logger LOG = LoggerFactory
            .getLogger(JavaCompiler.class);

    public List<Long> compileAndObtainErrors(ClassUnderTest classUnderTest, CompilationUnit unit, DependencyResolver resolver) throws IOException {
        List<String> compilerOptions = new ArrayList<>();
        // These are javac compilerOptions
        compilerOptions.add("-Xmaxerrs");
        compilerOptions.add("1000");

        // write class to target
        compilerOptions.add("-d");
        File target = new File(classUnderTest.getLocalProject().getTarget(), "arena-compile");
        if(!target.exists()) {
            target.mkdirs();
        }
        compilerOptions.add(target.getAbsolutePath());

        Container container = classUnderTest.getProject().getContainer();

        // get test class path
        String testClassPath;
        try {
            testClassPath = TestSupport.getTestClassPath(resolver);
        } catch (Throwable e) {
            throw new IOException("Could not create test classpath", e);
        }

        String classPath = testClassPath + ":" + container.toClassPath();

        LOG.info("Going to compile '{}' for '{}' using classpath '{}'", unit.getFQName(), classUnderTest.getId(), classPath);

        compilerOptions.add("-cp");
        compilerOptions.add(classPath);

        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        //boolean isCompilable = sequenceCompiler.isCompilable(unit.getPkg(), unit.getName(), unit.getSourceCode());
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        String classFileName = unit.getName() + ".java";
        List<JavaFileObject> sources = new ArrayList<>();
        JavaFileObject source = new InMemoryJavaObject(classFileName, unit.getSourceCode());
        sources.add(source);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Source\n{}", unit.getSourceCode());
        }

        javax.tools.StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        javax.tools.JavaCompiler.CompilationTask task =
                ToolProvider.getSystemJavaCompiler().getTask(
                        null, fileManager, diagnostics, compilerOptions, null, sources);
        Boolean succeeded = task.call();

        if(succeeded != null && succeeded) {
            LOG.info("Compilation succeeded for '{}' / '{}'", unit.getFQName(), classUnderTest.getId());

            // nice we were able to compile
            return new LinkedList<>();
        } else {
            List<Long> lineNos = new LinkedList<>();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                LOG.debug("ERROR => '{}'", diagnostic);
                if (diagnostic != null) {
                    if (diagnostic.getSource() != null) {
                        if (diagnostic.getLineNumber() >= 0) {
                            lineNos.add(diagnostic.getLineNumber());
                        }
                    }
                }
            }

            LOG.info("Compilation failed with '{}' errors '{}' / '{}'", lineNos.size(), unit.getFQName(), classUnderTest.getId());

            return lineNos;
        }
    }
}
