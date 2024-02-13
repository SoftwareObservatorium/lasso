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
package de.uni_mannheim.swt.lasso.analyzer.systemtests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import de.uni_mannheim.swt.lasso.analyzer.asm.ASMAnalyzer;
import de.uni_mannheim.swt.lasso.analyzer.asm.CandidateVisitor;
import de.uni_mannheim.swt.lasso.analyzer.asm.DependencyVisitor;
import de.uni_mannheim.swt.lasso.analyzer.asm.JavaParserAnalyzer;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;

import org.junit.Test;

import org.objectweb.asm.ClassReader;

/**
 * Integration tests for debugging purposes.
 *
 * @author Marcus Kessel
 */
public class ASMAnalyzerTest {

    File src = new File(
            "testdata/commons-codec/commons-codec/1.11/commons-codec-1.11-sources.jar");
    File bin = new File("testdata/commons-codec/commons-codec/1.11/commons-codec-1.11.jar");

    File testSrc = new File(
            "testdata/commons-codec/commons-codec/1.11/commons-codec-1.11-test-sources.jar");
    File testBin = new File("testdata/commons-codec/commons-codec/1.11/commons-codec-1.11-tests.jar");

    File pom = new File(
            "testdata/commons-codec/commons-codec/1.11/commons-codec-1.11.pom");

    @Test
    public void test_persist() throws IOException {
        ASMAnalyzer analyzer = new ASMAnalyzer(true, true);

        MavenArtifact mavenArtifact = new MavenArtifact("commons-codec", "commons-codec", "1.11");
        mavenArtifact.setBinaryJar(bin);
        mavenArtifact.setSourceJar(src);

        List<CompilationUnit> units = analyzer.analyze(mavenArtifact);

        units.stream().forEach(cu -> System.out.println(cu.getName()));

        units.get(0).getDependencies().forEach(System.out::println);
    }

    @Test
    public void testAnalyze() throws IOException {
        ASMAnalyzer analyzer = new ASMAnalyzer(true, true);

        MavenArtifact mavenArtifact = new MavenArtifact("commons-codec", "commons-codec", "1.11");
        mavenArtifact.setBinaryJar(bin);
        mavenArtifact.setSourceJar(src);

        List<CompilationUnit> units = analyzer.analyze(mavenArtifact);

        JavaParserAnalyzer jp = new JavaParserAnalyzer();
        jp.analyze(units.get(0), mavenArtifact);

        units.get(0).getMethods().forEach(m -> {
            String measures = m.getMeasures().entrySet().stream().map(e -> e.getKey() + " = " + e.getValue()).collect(Collectors.joining(","));
            System.out.println(m.getName() + " = " + m.getContent() + " " + measures);
        });
    }

    @Test
    public void testAnalyze_large() throws IOException {
        ASMAnalyzer analyzer = new ASMAnalyzer(true, true);

        MavenArtifact mavenArtifact = new MavenArtifact("commons-codec", "commons-codec", "1.11");
        mavenArtifact.setBinaryJar(bin);
        mavenArtifact.setSourceJar(src);

        List<CompilationUnit> units = analyzer.analyze(mavenArtifact);

        JavaParserAnalyzer jp = new JavaParserAnalyzer();

        for(CompilationUnit cu : units) {
            jp.analyze(cu, mavenArtifact);

            cu.getMethods().forEach(m -> {
                String measures = m.getMeasures().entrySet().stream().map(e -> e.getKey() + " = " + e.getValue()).collect(Collectors.joining(","));
                System.out.println(m.getName() + " = " + m.getContent() + " " + measures);
            });
        }
    }

    @Test
    public void testAnalyze_classic() throws IOException {
        // Visitor for all 'compile' deps of a given class
        DependencyVisitor depVisitor = new DependencyVisitor();
        CandidateVisitor visitor = new CandidateVisitor(depVisitor);

        String resName = "/" + ClassicClass.class.getName().replaceAll("\\.", "/") + ".class";
        InputStream is = ClassicClass.class.getResourceAsStream(resName);

        ClassReader cr = new ClassReader(
                is /* read inputstream instead of bytes */);
        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        CompilationUnit unit = visitor.getUnit();
        //
        for (String dep : depVisitor.getPackages()) {


            System.out.println("DEP " + dep);
        }

        System.out.println("SUPER " + depVisitor.getSuperClass());

        for (String dep : depVisitor.getInterfaces()) {
            System.out.println("INTERFACE " + dep);
        }
    }

    @Test
    public void testAnalyze_signature() throws IOException {
        // Visitor for all 'compile' deps of a given class
        DependencyVisitor depVisitor = new DependencyVisitor();
        CandidateVisitor visitor = new CandidateVisitor(depVisitor);

        String resName = "/" + SignatureClass.class.getName().replaceAll("\\.", "/") + ".class";
        InputStream is = SignatureClass.class.getResourceAsStream(resName);

        ClassReader cr = new ClassReader(
                is /* read inputstream instead of bytes */);
        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        CompilationUnit unit = visitor.getUnit();
        //
        for (String dep : depVisitor.getPackages()) {


            System.out.println("DEP " + dep);
        }

        System.out.println("SUPER " + depVisitor.getSuperClass());

        for (String dep : depVisitor.getInterfaces()) {
            System.out.println("INTERFACE " + dep);
        }
    }
}
