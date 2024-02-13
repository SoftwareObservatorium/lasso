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
package de.uni_mannheim.swt.lasso.arena.sequence.unit;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.sequence.compile.TestSupport;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ParserUtils;
import de.uni_mannheim.swt.lasso.core.model.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.execution.ExecutionHelper;
import randoop.execution.RunCommand;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static randoop.reflection.SignatureParser.DOT_DELIMITED_IDS;
import static randoop.reflection.SignatureParser.ID_STRING;

/**
 * Simple JUnit test runner which detects failing tests and removes them.
 *
 * Inspired by randoop's runner.
 *
 * @author Marcus Kessel
 */
public class JUnitRunner {

    private static final Logger LOG = LoggerFactory
            .getLogger(JUnitRunner.class);

    // taken from randoop
    private static final Pattern FAILURE_MESSAGE_PATTERN =
            Pattern.compile("There\\s+(?:was|were)\\s+(\\d+)\\s+failure(?:s|):");
    // taken from randoop
    private static final Pattern FAILURE_HEADER_PATTERN =
            Pattern.compile("\\d+\\)\\s+(" + ID_STRING + ")\\(" + DOT_DELIMITED_IDS + "\\)");

    private long timeout = 2 * 60 * 1000;

    private boolean removeFailingTests = true;

    /**
     * Run JUnit classes
     *
     * @param classUnderTest
     * @param unit
     * @param resolver
     * @throws IOException
     * @return
     */
    public JUnitReport run(ClassUnderTest classUnderTest, CompilationUnit unit, DependencyResolver resolver) throws IOException {
        File targetTestClasses = new File(classUnderTest.getLocalProject().getTarget(), "arena-compile");

        Container container = classUnderTest.getProject().getContainer();

        // get test class path
        String testClassPath;
        try {
            testClassPath = TestSupport.getTestClassPath(resolver);
        } catch (Throwable e) {
            throw new IOException("Could not create test classpath", e);
        }

        String classPath = testClassPath + ":" + container.toClassPath();

        LOG.info("Going to execute JUnit tests for '{}' using classpath '{}'", classUnderTest.getId(), classPath);

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-ea");
        command.add("-Xmx" + "2G");
        //command.add("-XX:+HeapDumpOnOutOfMemoryError");
        // FIXME still issues
        // java.lang.SecurityException: class "XXX"'s signer information does not match signer information of other classes in the same package
        command.add("-Dsun.misc.URLClassPath.disableJarChecking=true"); // prevents classloading problems with java11
        command.add("-noverify");

        command.add("-classpath");
        command.add(classPath + java.io.File.pathSeparator + targetTestClasses.getAbsolutePath());
        command.add("org.junit.runner.JUnitCore");

        // FIXME do we expect more than one class? (e.g., amplify)
//        String testClasses = classUnderTest.getLocalProject().getFiles(targetTestClasses, "class").stream()
//                .map(f -> classUnderTest.getImplementation().getCode().getPackagename() + "." + FilenameUtils.getBaseName(f.getName()))
//                .collect(Collectors.joining(" "));

        command.add(unit.getFQName());

        LOG.debug("Running '{}'", unit.getFQName());

        try {
            RunCommand.Status status = ExecutionHelper.run(command, targetTestClasses.toPath(), timeout);

            if(LOG.isDebugEnabled()) {
                LOG.debug("status " + status);

                LOG.debug("STDOUT =>\n{}", status.standardOutputLines.stream().collect(Collectors.joining("\n")));
                LOG.debug("STDERR =>\n{}", status.errorOutputLines.stream().collect(Collectors.joining("\n")));
            }

            JUnitReport report = new JUnitReport();
            report.setUnit(unit);

            //
            if(status.exitStatus == 0) {
                // we are happy
                return report;
            } else if(status.timedOut) {
                throw new IOException("timed out");
            } else if (status.exitStatus == 137) {
                // out of memory issues
                throw new IOException("out of memory issues");
            } else {
                Set<String> testMethods = new HashSet<>();
                for(String line : status.standardOutputLines) {
                    Matcher matcher = FAILURE_HEADER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        LOG.debug("Found line '{}' with match '{}'", line, matcher.group(1));

                        testMethods.add(matcher.group(1));
                    }
                }

                LOG.debug("Following test methods contain failing assertions => '{}'", testMethods);

                report.setFailingTestMethods(testMethods);

                if(isRemoveFailingTests() && !testMethods.isEmpty()) {
                    LOG.warn("Removing failing test methods => '{}'", testMethods);

                    unit.setSourceCode(ParserUtils.removeTestMethods(unit.getSourceCode(), testMethods));
                }
            }

            return report;
        } catch (RunCommand.CommandException e) {
            LOG.warn("stack trace", e);

            throw new IOException(e);
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isRemoveFailingTests() {
        return removeFailingTests;
    }

    public void setRemoveFailingTests(boolean removeFailingTests) {
        this.removeFailingTests = removeFailingTests;
    }
}
