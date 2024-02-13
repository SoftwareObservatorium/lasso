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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * Analyze source code with {@link JavaParser}.
 * 
 * @author Marcus Kessel
 *
 */
public class JavaParserAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(JavaParserAnalyzer.class);

    public void analyze(List<CompilationUnit> units, MavenArtifact mavenArtifact) {
        if (CollectionUtils.isEmpty(units)) {
            return;
        }

        // make access to JavaParser thread safe
        units.stream().forEach(unit -> analyze(unit, mavenArtifact));
    }

    public void analyze(CompilationUnit unit, MavenArtifact mavenArtifact) {
        if (!unit.isSource()) {
            unit.getMeasures().put("static_ctlratio", -1d);
            unit.getMeasures().put("static_jdmratio", -1d);
            unit.getMeasures().put("static_loc", -1d);

            return;
        }

        // add no. of methods declared
        double methods = 0d;
        if(CollectionUtils.isNotEmpty(unit.getMethods())) {
            methods = (double) unit.getMethods().size();
        }

        unit.getMeasures().put("static_methods", methods);

        ParseResult<com.github.javaparser.ast.CompilationUnit> parseResult = null;
        try {
            // parse the file
            parseResult = new JavaParser().parse(unit.getSourceCode());

            com.github.javaparser.ast.CompilationUnit cu = parseResult.getResult().get();

            //
            List<Comment> comments = new LinkedList<>(cu.getAllContainedComments());
            if (cu.getComment().isPresent() && !comments.contains(cu.getComment().get())) {
                comments.add(cu.getComment().get());
            }

            double commentToLineRatio = commentToLineRatio(
                    cu.toString()/*
                                  * compare apples by apples, so use
                                  * javaparser formatting
                                  */, comments, unit.getMeasures());
            unit.getMeasures().put("static_ctlratio", commentToLineRatio);

            // javadoc method ratio etc.
            JavaDocMethodVisitor methodVisitor = new JavaDocMethodVisitor();
            methodVisitor.visit(cu, null);

            double jdmratio = methodVisitor.getRatio();
            unit.getMeasures().put("static_jdmratio", jdmratio);

            // match methods
            matchMethods(methodVisitor, unit);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("JavaParser failed for thread " + Thread.currentThread(), e);

                if(parseResult != null) {
                    LOG.warn("JavaParser problems for "+ mavenArtifact.toUri() + " => " + parseResult.getProblems());
                }
            }
        } finally {
            //
        }
    }

    private static void matchMethods(JavaDocMethodVisitor methodVisitor, CompilationUnit unit) {
        try {
            //
            if (CollectionUtils.isNotEmpty(unit.getMethods())) {
                // methids
                unit.getMethods().stream().filter(m -> !m.isConstructor() && !m.isStaticInit()).forEach(m -> {
                    Optional<MethodDesc> mDesc = methodVisitor.methodDescs.stream().filter(mdesc -> mdesc.isSimilar(m))
                            .findFirst();

                    if (mDesc.isPresent() && StringUtils.isNotBlank(mDesc.get().getContent())) {
                        m.setContent(mDesc.get().getContent());

                        // compute hash
                        String hash = DigestUtils.md5Hex(mDesc.get().getContent());
                        m.setHash(hash);

                        List<Comment> comments = new LinkedList<>(
                                mDesc.get().getDeclaration().getAllContainedComments());

                        Optional<JavadocComment> jd = mDesc.get().getDeclaration().getJavadocComment();

                        if (jd.isPresent()
                                && !comments.contains(jd.get())) {
                            comments.add(jd.get());
                        }

                        double commentToLineRatio = commentToLineRatio(mDesc.get().getContent(), comments,
                                m.getMeasures());
                        m.getMeasures().put("static_ctlratio", commentToLineRatio);
                    }
                });

                // inits
                unit.getMethods().stream().filter(m -> m.isConstructor()).forEach(m -> {
                    Optional<ConstructorDesc> mDesc = methodVisitor.initDescs.stream()
                            .filter(mdesc -> mdesc.isSimilar(m)).findFirst();

                    if (mDesc.isPresent() && StringUtils.isNotBlank(mDesc.get().getContent())) {
                        m.setContent(mDesc.get().getContent());
                        // loc
                        m.getMeasures().put("static_loc", new Integer(mDesc.get().getLoc()).doubleValue());

                        // compute hash
                        String hash = DigestUtils.md5Hex(mDesc.get().getContent());
                        m.setHash(hash);

                        List<Comment> comments = new LinkedList<>(
                                mDesc.get().getDeclaration().getAllContainedComments());

                        Optional<JavadocComment> jd = mDesc.get().getDeclaration().getJavadocComment();

                        if (jd.isPresent()
                                && !comments.contains(jd.get())) {
                            comments.add(jd.get());
                        }

                        double commentToLineRatio = commentToLineRatio(mDesc.get().getContent(), comments,
                                m.getMeasures());
                        m.getMeasures().put("static_ctlratio", commentToLineRatio);
                    }
                });
            }
        } catch (Throwable e) {
            //
        }
    }

    public static int loc(String content) {
        if (StringUtils.isBlank(content)) {
            return -1;
        }
        int loc = content.split("\r\n|\r|\n").length;

        return loc;
    }

    private static double commentToLineRatio(String sourceCode, List<Comment> comments, Map<String, Double> measures) {
        //
        try {
            //
            int loc = loc(sourceCode);
            measures.put("static_loc", new Integer(loc).doubleValue());

            // comments
            int clocs = 0;
            if (CollectionUtils.isNotEmpty(comments)) {
                clocs = comments.stream()
                        .collect(Collectors.summingInt(c -> c.getContent().split("\r\n|\r|\n").length));
            }

            measures.put("static_cloc", new Integer(clocs).doubleValue());

            if (clocs == 0) {
                return 0d;
            }

            return new Integer(clocs).doubleValue() / new Integer(loc).doubleValue();
        } catch (Throwable e) {
            return -1d;
        }
    }

    /**
     * Collect and compute javadoc-method ratio
     * 
     * @author Marcus Kessel
     *
     */
    private static class JavaDocMethodVisitor extends VoidVisitorAdapter<Void> {

        private boolean includeInit = false;

        private AtomicInteger methods = new AtomicInteger(0);
        private AtomicInteger javaDocs = new AtomicInteger(0);

        private final List<MethodDesc> methodDescs = new LinkedList<>();
        private final List<ConstructorDesc> initDescs = new LinkedList<>();

        double getRatio() {
            if (methods.get() == 0) {
                return 1d;
            }

            if (javaDocs.get() == 0) {
                return 0d;
            }

            return new Integer(javaDocs.get()).doubleValue() / new Integer(methods.get()).doubleValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.github.javaparser.ast.visitor.VoidVisitorAdapter#visit(com.github
         * .javaparser.ast.body.ConstructorDeclaration, java.lang.Object)
         */
        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            //
            initDescs.add(new ConstructorDesc(n));

            if (!includeInit) {
                return;
            }

            methods.incrementAndGet();

            process(n.getJavadocComment());

            //
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            methods.incrementAndGet();

            methodDescs.add(new MethodDesc(n));

            //
            process(n.getJavadocComment());

            //
            super.visit(n, arg);
        }

        private void process(Optional<JavadocComment> javaDoc) {
            if (!javaDoc.isPresent()) {
                return;
            }

            javaDocs.incrementAndGet();
        }
    }
}
