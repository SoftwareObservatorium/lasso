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
package de.uni_mannheim.swt.lasso.arena.sequence.generator.unit;

import randoop.com.github.javaparser.JavaParser;
import randoop.com.github.javaparser.ParseResult;
import randoop.com.github.javaparser.ast.body.MethodDeclaration;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.JUnit4Creator;

import randoop.com.github.javaparser.ast.CompilationUnit;
import randoop.com.github.javaparser.ast.stmt.BlockStmt;
import randoop.output.JUnitCreator;
import randoop.output.NameGenerator;
import randoop.sequence.ExecutableSequence;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Based on randoop's JUnit generator.
 *
 * @author Marcus Kessel
 */
public class JUnit4Generator {

    private static final Logger LOG = LoggerFactory
            .getLogger(JUnit4Generator.class);

    /**
     * Export {@link ExecutableSequence} to JUnit.
     *
     * @param es
     * @param packageName
     * @param className
     * @return
     */
    @Deprecated
    public CompilationUnit exportJUnit(ExecutableSequence es, String packageName, String className) {
        List<ExecutableSequence> sequenceList = Arrays.asList(es);

        BlockStmt afterAllFixtureBody = new BlockStmt();
        BlockStmt afterEachFixtureBody = new BlockStmt();
        BlockStmt beforeAllFixtureBody = new BlockStmt();
        BlockStmt beforeEachFixtureBody = new BlockStmt();
        JUnitCreator junitCreator =
                JUnitCreator.getTestCreator(
                        packageName,
                        beforeAllFixtureBody,
                        afterAllFixtureBody,
                        beforeEachFixtureBody,
                        afterEachFixtureBody);
        NameGenerator methodNameGenerator = new NameGenerator("test", 1, sequenceList.size());

        CompilationUnit classAST =
                junitCreator.createTestClass(className, methodNameGenerator, sequenceList);

        return classAST;
    }

    /**
     * Export JUnit tests.
     *
     * @param classUnderTest
     * @param executionResults
     * @param suffix
     * @throws IOException
     * @return
     */
    public de.uni_mannheim.swt.lasso.core.model.CompilationUnit exportJUnit(ClassUnderTest classUnderTest, List<SequenceExecutionRecord> executionResults, String suffix) throws IOException {
        return exportJUnit(classUnderTest, executionResults, suffix, new LinkedList<>());
    }

    public de.uni_mannheim.swt.lasso.core.model.CompilationUnit exportJUnit(ClassUnderTest classUnderTest, List<SequenceExecutionRecord> executionResults, String suffix, List<Long> compileErrors) throws IOException {
        BlockStmt afterAllFixtureBody = new BlockStmt();
        BlockStmt afterEachFixtureBody = new BlockStmt();
        BlockStmt beforeAllFixtureBody = new BlockStmt();
        BlockStmt beforeEachFixtureBody = new BlockStmt();
        JUnit4Creator junitCreator =
                JUnit4Creator.getTestCreator(
                        classUnderTest.getImplementation().getCode().getPackagename(),
                        beforeAllFixtureBody,
                        afterAllFixtureBody,
                        beforeEachFixtureBody,
                        afterEachFixtureBody);

        List<SequenceExecutionRecord> filtered = executionResults.stream()
                .filter(Objects::nonNull) // non null
                .filter(record -> Objects.nonNull(record.getExecutableSequence())) // ignore if null
//                .filter(record -> {
//                    // we don't want them
//                    return !record.getExecutableSequence().hasNonExecutedStatements();
//                }) // FIXME for amplify, we want them .. if not compilable .. will be removed anyways
                .filter(record -> {
                    try {
                        String code = record.getExecutableSequence().toCodeString(); // sometimes NPE happens

                        //LOG.info(code);

                        if(StringUtils.contains(code, "???")) {
                            LOG.warn("Ignoring malformed code which is not compilable =>\n{}", code);

                            return false;
                        }
                    } catch (Throwable e) {
                        return false;
                    }

                    return true;
                }).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(filtered)) {
            throw new IOException("No executable sequences available for " + classUnderTest.getId());
        }

        CompilationUnit classAST =
                junitCreator.createTestClass(classUnderTest.getImplementation().getCode().getName() + suffix, filtered);

        if(CollectionUtils.isNotEmpty(compileErrors)) {
            // remove sourrounding tests
            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> parseResult = javaParser.parse(classAST.toString());

            CompilationUnit testClassUnit = parseResult.getResult().get();

            List<MethodDeclaration> testMethods = testClassUnit.findAll(MethodDeclaration.class, m -> {
                if(m.getRange().isPresent()) {
                    int begin = m.getRange().get().begin.line;
                    int end = m.getRange().get().end.line;

                    // happens in method?
                    return compileErrors.stream().anyMatch(line -> line >= begin && line <= end);
                }

                return false;
            });

            if(CollectionUtils.isNotEmpty(testMethods)) {
                for(MethodDeclaration methodDeclaration : testMethods) {
                    LOG.warn("Removing non-compilable method '{}'", methodDeclaration.getNameAsString());
                    //BlockComment comment = new BlockComment(methodDeclaration.toString());
                    //Node parent = methodDeclaration.getParentNode().get();
                    methodDeclaration.remove();
                }

                classAST = testClassUnit; //
            }
        }

        MavenProject mavenProject = classUnderTest.getLocalProject();
        de.uni_mannheim.swt.lasso.core.model.CompilationUnit unit = new de.uni_mannheim.swt.lasso.core.model.CompilationUnit();
        unit.setPkg(classUnderTest.getImplementation().getCode().getPackagename());
        unit.setName(classUnderTest.getImplementation().getCode().getName() + suffix);
        unit.setSourceCode(classAST.toString());
        mavenProject.writeCompilationUnit(unit, true);

        return unit;
    }
}
