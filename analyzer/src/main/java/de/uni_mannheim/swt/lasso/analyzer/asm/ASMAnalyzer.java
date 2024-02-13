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
package de.uni_mannheim.swt.lasso.analyzer.asm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import de.uni_mannheim.swt.lasso.analyzer.asm.hierarchy.HierarchyAnalyser;
import de.uni_mannheim.swt.lasso.analyzer.analyzer.ProjectAnalyzer;
import de.uni_mannheim.swt.lasso.analyzer.asm.jacoco.JaCoCoSizeMetrics;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.model.Method;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static classes analysis (merely based on byte code) using ASM byte code manipulation framework.
 *
 * @author Marcus Kessel
 */
public class ASMAnalyzer implements ProjectAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ASMAnalyzer.class);

    /**
     * Java root object {@link Object}
     */
    private static String JAVA_ROOT_OBJ = "java.lang.Object";

    /**
     * Shall we exclude 'java.lang.Object'?
     */
    private boolean excludeJavaLangRootObject;

    /**
     * Shall anonymous classes be ignored (saves memory and space)?
     */
    private boolean ignoreAnonymousClasses;

    private JavaParserAnalyzer javaParserAnalyzer = new JavaParserAnalyzer();

    /**
     * @param excludeJavaLangRootObject Should exclude 'java.lang.Object' pkg (e.g. exclude
     *                                  java.lang.Object)
     * @param ignoreAnonymousClasses    Shall anonymous classes be ignored (saves memory and space)?
     */
    public ASMAnalyzer(boolean excludeJavaLangRootObject, boolean ignoreAnonymousClasses) {
        //
        this.excludeJavaLangRootObject = excludeJavaLangRootObject;
        this.ignoreAnonymousClasses = ignoreAnonymousClasses;
    }

    /**
     * Run static class analysis
     */
    @Override
    public List<CompilationUnit> analyze(MavenArtifact mavenArtifact) throws IOException {
        // analyze

        // AClass es
        List<CompilationUnit> classes = new LinkedList<CompilationUnit>();

        //
        JarInputStream jarFile = null;
        try {
            jarFile = new JarInputStream(new FileInputStream(mavenArtifact.getBinaryJar()));
            JarEntry jarEntry;

            while (true) {
                jarEntry = jarFile.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName().replaceAll("/", "\\.");
                    className = className.replace(".class", "");

                    // check if anonymous class
                    boolean isAnonymousClass = NumberUtils.isNumber(StringUtils.substringAfterLast(className, "$"));

                    // check if we ignore anonymous classes
                    if (ignoreAnonymousClasses && isAnonymousClass) {
                        continue;
                    }

                    // Visitor for all 'compile' deps of a given class
                    DependencyVisitor depVisitor = new DependencyVisitor();
                    CandidateVisitor visitor = new CandidateVisitor(depVisitor);
                    ClassReader cr = new ClassReader(
                            jarFile /* read inputstream instead of bytes */);
                    cr.accept(visitor, ClassReader.EXPAND_FRAMES);

                    CompilationUnit unit = visitor.getUnit();
                    //
                    List<String> dependencies = new LinkedList<>();
                    for (String dep : depVisitor.getPackages()) {
                        dependencies.add(dep);
                        // if (!visitedClasses.contains(dep)
                        // && FilterUtils.checkNonJdk(dep)
                        // && FilterUtils.checkNonAdapter(dep)) {
                        //
                        // // add dep IF NOT already resolved/unresolved
                        // if (!dependencies.getResolved().contains(dep)
                        // && !dependencies.getUnresolved().contains(dep)) {
                        // // recursive visit
                        // visit(dep, visitedClasses, dependencies);
                        // }
                        // }
                    }
                    unit.setDependencies(dependencies);
                    // inner class?
                    unit.setInnerClass(StringUtils.contains(className, "$"));

                    // set superclass and interfaces
                    unit.setSuperClassName(depVisitor.getSuperClass());
                    unit.setInterfaceNames(depVisitor.getInterfaces());
                    // has generic signature?
                    unit.setGeneric(depVisitor.isGeneric());

                    // override name
                    //
                    String packageName = StringUtils.substringBeforeLast(className, ".");
                    if (StringUtils.isBlank(packageName)) {
                        packageName = "";
                    }

                    unit.setPackageName(packageName);

                    String name = StringUtils.substringAfterLast(className, ".");
                    if (StringUtils.isBlank(className)) {
                        // keep innerclass $
                        // name = className.replaceAll("$", ".");
                        name = className;
                    }

                    unit.setName(name);

                    classes.add(unit);

                    // measures
                    try {
                        JaCoCoSizeMetrics jaCoCo = new JaCoCoSizeMetrics(cr, className);

                        Map<String, SummaryStatistics> summaries = new HashMap<>();

                        Map<String, Map<String, Double>> metricsMap = jaCoCo.getMethodBodyWeights();

                        if (metricsMap != null && unit.getMethods() != null) {
                            metricsMap.forEach((k, v) -> {
                                Optional<Method> method = unit.getMethods().stream()
                                        .filter(p -> StringUtils.equals(p.getByteCodeName(), k)).findFirst();
                                if (method.isPresent()) {
                                    //
                                    if (v != null) {
                                        v.forEach((metricName, metricValue) -> {
                                            String m = "static_" + metricName;
                                            method.get().getMeasures().put(m, metricValue);

                                            if (!summaries.containsKey(m)) {
                                                summaries.put(m, new SummaryStatistics());
                                            }

                                            summaries.get(m).addValue(metricValue);
                                        });
                                    }
                                }
                            });
                        }

                        // set summaries to class
                        summaries.forEach((k, v) -> {
                            unit.getMeasures().put(k, v.getSum());
                        });
                    } catch (Throwable e) {
                        //
                        e.printStackTrace();
                    }

                    // collect method deps
                    try {
                        MethodBodyDependencyVisitor methodDeps = new MethodBodyDependencyVisitor();
                        cr.accept(methodDeps, ClassReader.EXPAND_FRAMES);

                        Map<String, Set<String>> methodDependencies = methodDeps.getDependencies();
                        if (methodDependencies != null && unit.getMethods() != null) {
                            methodDependencies.forEach((k, v) -> {
                                Optional<Method> method = unit.getMethods().stream()
                                        .filter(p -> StringUtils.equals(p.getByteCodeName(), k)).findFirst();
                                if (method.isPresent()) {
                                    method.get().setDependencies(v);
                                }
                            });
                        }

                        Map<String, Set<String>> calls = methodDeps.getCalls();
                        if (calls != null && unit.getMethods() != null) {
                            calls.forEach((k, v) -> {
                                Optional<Method> method = unit.getMethods().stream()
                                        .filter(p -> StringUtils.equals(p.getByteCodeName(), k)).findFirst();
                                if (method.isPresent()) {
                                    method.get().setCalls(methodDeps.getCalls().get(k));

                                    method.get().getMeasures().put("static_mcalls",
                                            new Integer(methodDeps.getCallsCount().get(k).get()).doubleValue());
                                }
                            });
                        }
                    } catch (Throwable e) {
                        //
                    }
                }
//                else if(jarEntry.getName().endsWith(".pom")) {
//                    LOG.info("Found pom");
//
//                    // analyze pom
//                    try {
//                        //
//                        MavenPomParser.parsePom(jarFile);
//                    } catch (Throwable e) {
//                        //
//                    }
//                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read binary jar " + mavenArtifact.getBinaryJar().getAbsolutePath());
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Jar file cannot be closed", e);
                    }
                }
            }
        }

        // analyse super hierarchy
        try {
            HierarchyAnalyser hierarchyAnalyser = new HierarchyAnalyser();
            hierarchyAnalyser.analyse(classes);

            // set measures
            classes.stream().forEach(c -> {
                // set java.lang.Object by default
                if (c.getSuperClassNames() != null) {
                    c.getMeasures().put("static_idit", new Integer(c.getSuperClassNames().size()).doubleValue());
                }

                // compute no. of accessible methods
                double accessibleMethods = c.getMethods() != null ? c.getMethods().size() : 0d;
                accessibleMethods += c.getInheritedMethods() != null ? c.getInheritedMethods().size() : 0d;
                c.getMeasures().put("static_methodsacc", accessibleMethods);
            });
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Hierarchy analysis failed for " + mavenArtifact.getBinaryJar().getAbsolutePath(), e);
            }
        }

        // find and set source code + hash
        findAndSetSourceCode(classes, mavenArtifact.getSourceJar());

        try {
            //
            javaParserAnalyzer.analyze(classes, mavenArtifact);
        } catch (Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Failed to analyze source code of " + mavenArtifact.toUri(), e);
            }
        }

        // System.out.println("BLABLA" + classes.size() +
        // classes.get(20).getSourceCode());

        // classes.stream().forEach(cl -> System.out.println(cl.getName()))

        // return ..
        return classes;
    }

    /**
     * Find source code for given AClasses. For inner and anonymous, the source
     * code hash of the 'physical' declaring .java file is returned, and no
     * source code is set.
     *
     * @param classes List of AClasses
     * @param src     Source code directory location
     * @throws IOException Couldn't read source code directory or source code
     */
    private static void findAndSetSourceCode(List<CompilationUnit> classes, File src) throws IOException {
        // generate cache for filenames
        MultiValuedMap<String, CompilationUnit> cacheMap = new ArrayListValuedHashMap<>();
        for (CompilationUnit aClass : classes) {
            // construct a la 'foo.bar.Hello.java'
            cacheMap.put(getJavaFilename(aClass), aClass);
        }

        // find and get source
        if (src.isDirectory()) {
            for (File file : FileUtils.listFiles(src, new String[]{"java"}, true)) {
                // search classes
                for (Object key : cacheMap.keySet()) {
                    String bFqName = (String) key;

                    if (StringUtils.endsWith(file.getAbsolutePath(), bFqName)) {
                        // source
                        String sourceCode = FileUtils.readFileToString(file);

                        // compute hash
                        String hash = DigestUtils.md5Hex(sourceCode);

                        // lookup classes
                        for (CompilationUnit aClass : cacheMap.asMap().get(bFqName)) {
                            // set hash
                            aClass.setHash(hash);

                            // only set for non-inner classes
                            if (!aClass.isInnerClass()) {
                                // set source code
                                aClass.setSourceCode(sourceCode);
                            }
                        }
                    }
                }
            }
        } else if (FilenameUtils.isExtension(src.getName(), "jar")) { // jar
            // read jar file tree
            JarFile jar = new JarFile(src);

            try {
                for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = (JarEntry) e.nextElement();

                    if (entry.isDirectory()) {
                        continue;
                    }

                    // search classes
                    for (Object key : cacheMap.keySet()) {
                        String bFqName = (String) key;
                        if (StringUtils.endsWith(entry.getName(), bFqName)) {
                            // source
                            String sourceCode = IOUtils.toString(jar.getInputStream(entry));

                            // compute hash
                            String hash = DigestUtils.md5Hex(sourceCode);

                            // lookup classes
                            for (CompilationUnit aClass : cacheMap.asMap().get(bFqName)) {
                                // set hash
                                aClass.setHash(hash);

                                // only set for non-inner classes
                                if (!aClass.isInnerClass()) {
                                    // set source code
                                    aClass.setSourceCode(sourceCode);
                                }
                            }
                        }
                    }
                }
            } finally {
                // close
                IOUtils.closeQuietly(jar);
            }
        } else {
            throw new IOException("Reading source only supported for directory or jar file: " + src.getAbsolutePath());
        }

        // clear map
        cacheMap.clear();
    }

    /**
     * Construct java file name
     *
     * @param aClass AClass
     * @return Java file name from package and class name
     */
    private static String getJavaFilename(CompilationUnit aClass) {
        // construct file system path
        String pkg = aClass.getPackageName();
        boolean defaultPkg = StringUtils.isBlank(pkg);

        String javaClassName = null;
        if (aClass.isInnerClass()) {
            // get class name part before first $ (e.g.
            // Clazz.Lala.Hoho)
            javaClassName = StringUtils.substringBefore(aClass.getName(), ".");
        } else {
            javaClassName = aClass.getName();
        }

        // construct a la 'foo.bar.Hello.java'
        String bFqName = defaultPkg ? "" : StringUtils.replaceChars(pkg, ".", "/") + "/" + javaClassName + ".java";

        return bFqName;
    }
}
