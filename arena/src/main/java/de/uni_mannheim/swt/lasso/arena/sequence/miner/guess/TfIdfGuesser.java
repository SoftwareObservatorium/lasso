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
package de.uni_mannheim.swt.lasso.arena.sequence.miner.guess;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



/*
Copyright (c) 2022, Chair of Software Technology
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of the University Mannheim nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Malte Brockmeier
 * Analyzer to determine the CUT of a test method using tf-idf score of types contained within
 */
public class TfIdfGuesser implements CutGuesser {
    private static final Logger LOG = LoggerFactory.getLogger(TfIdfGuesser.class);

    protected static HashMap<String, HashMap<String, Integer>> termDocumentMatrix;

    protected static HashMap<String, LinkedHashSet<String>> methodTerms;

    protected static String testClassName = null;

    protected static String testMethodSignature = null;

    public TfIdfGuesser() { }

    static class TfIdfCUT implements Comparable<TfIdfCUT> {

        private final String cut;
        private final double tfIdf;

        public TfIdfCUT(String cut, double tfIdf) {
            this.cut = cut;
            this.tfIdf = tfIdf;
        }

        @Override
        public int compareTo(TfIdfCUT o) {
            return Double.compare(this.tfIdf, o.getTfIdf());
        }

        public String getCut() {
            return cut;
        }

        public double getTfIdf() {
            return tfIdf;
        }
    }

    public HashMap<String, List<TfIdfCUT>> analyze(ClassUnderTest testClass) {
        LOG.info(String.format("Analyzing tests inside '%s' (tf-idf)", testClass.getClassName()));

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ClassLoaderTypeSolver(testClass.getProject().getContainer()));
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit parsedTestClass = javaParser.parse(testClass.getImplementation().getCode().getContent()).getResult().get();

        // inject
        try {
            solver.inject(parsedTestClass);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

        termDocumentMatrix = new HashMap<>();
        methodTerms = new HashMap<>();

        HashMap<String, List<TfIdfCUT>> tfIdfCUTMap = new HashMap<>();

        testClassName = testClass.getImplementation().getCode().getBytecodeName();

        termDocumentMatrix.put(testClassName, new HashMap<>());

        List<String> dependencies = testClass.getImplementation().getCode().getDependencies().stream().map(d -> d.replace(".", "/")).collect(Collectors.toList());
        analyzeTestClass(parsedTestClass, dependencies);

        HashMap<String, HashMap<String, Double>> termFrequencies = new HashMap<>();
        int documentCount = termDocumentMatrix.size();

        for (Map.Entry<String, HashMap<String, Integer>> document : termDocumentMatrix.entrySet()) {
            String testClassDocument = document.getKey();

            int sumOfTermOccurences = document.getValue().values().stream().mapToInt(Integer::intValue).sum();

            for (Map.Entry<String, Integer> termOccurence : document.getValue().entrySet()) {
                String cutCandidate = termOccurence.getKey();

                double termFrequency = ((double) termOccurence.getValue()) / sumOfTermOccurences;

                if (!termFrequencies.containsKey(document.getKey())) termFrequencies.put(document.getKey(), new HashMap<>());
                termFrequencies.get(document.getKey()).put(cutCandidate, termFrequency);

                // calculate idf, find out how many docs contain this word
                int docCount = 0;
                for(String iteratedDocument : termDocumentMatrix.keySet()) {
                    if (termDocumentMatrix.get(iteratedDocument).containsKey(cutCandidate) && termDocumentMatrix.get(iteratedDocument).get(cutCandidate) > 0) {
                        docCount++;
                    }
                }

                double idf = Math.log(((double) documentCount) / docCount);

                double tfIdf = termFrequency * idf;

                if (!tfIdfCUTMap.containsKey(testClassDocument)) tfIdfCUTMap.put(testClassDocument, new LinkedList<>());
                tfIdfCUTMap.get(testClassDocument).add(new TfIdfCUT(cutCandidate, tfIdf));
            }
        }

//        for (CompilationUnit unit : units) {
//            unit.setTestMethods(new LinkedList<>());
//
//            if (unit.getDependencies().contains("org/junit/Test")) {
//                String docName = unit.getByteCodeName();
//
//                if (tfIdfCUTMap.containsKey(docName)) {
//                    List<TfIdfCUT> tfIdfCUTs = tfIdfCUTMap.get(docName);
//                    Collections.sort(tfIdfCUTs);
//
//                    List<Method> methods = unit.getMethods();
//                    if (methods == null) continue;
//
//                    for (Method method : methods) {
//                        if (method == null) continue;
//                        if (method.getDependencies() == null || !method.getDependencies().contains("org/junit/Test")) continue;
//
//                        String methodSignature = getFullSignature(method);
//                        String fullSignature = docName + "/" + methodSignature;
//
//                        Set<String> currentMethodTerms = methodTerms.get(fullSignature);
//
//                        if (currentMethodTerms == null) continue;
//
//                        TfIdfCUT matchingCUT = tfIdfCUTs.stream().filter(tfIdfCUT -> currentMethodTerms.contains(tfIdfCUT.getCut())).findFirst().orElse(null);
//
//                        if (matchingCUT != null) {
//                            String matchingCUTName = matchingCUT.getCut();
//
//                            TestMethod testMethod = new TestMethod();
//                            testMethod.setName(method.getName());
//                            testMethod.setSignature(getFullSignature(method));
//                            testMethod.setClassUnderTest(matchingCUTName);
//                            unit.getTestMethods().add(testMethod);
//                        }
//                    }
//                }
//            }
//        }

        return tfIdfCUTMap;
    }

    @Override
    public String guess(ClassUnderTest testClass) throws IOException {
        HashMap<String, List<TfIdfGuesser.TfIdfCUT>> tfIdfCUTMap = analyze(testClass);

//        tfIdfCUTMap.get("org/apache/commons/net/util/Base64Test").stream().forEach(c -> {
//            System.out.println(c.getTfIdf() + " " + c.getCut());
//        });

        return tfIdfCUTMap.get(testClass.getClassName().replace(".", "/")).get(0).getCut();
    }

//    private String getFullSignature(Method method) {
//        String returnType = method.getReturnParameter() != null ? method.getReturnParameter().getType() : "java.lang.Object";
//        ArrayList<String> parameters = new ArrayList<>();
//
//        if (method.getParameters() != null) {
//            for (Parameter parameter : method.getParameters()) {
//                String parameterName = StringUtils.isEmpty(parameter.getType()) ? "java.lang.Object" : parameter.getType();
//                parameters.add(parameterName);
//            }
//        }
//
//        Signature signature = new Signature("", method.getName(), parameters, returnType);
//        return signature.toStringOrigSignature(true);
//    }

    public String getFullSignature(MethodDeclaration methodDeclaration) {
        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();

            String cleanedSignature = stripPackageFromMethodName(resolvedMethodDeclaration.getQualifiedSignature());
            String returnType = resolvedMethodDeclaration.getReturnType().describe();

            return returnType + " " + cleanedSignature;
        } catch (Exception ignored) { return null; }
    }

    public void analyzeTestClass(com.github.javaparser.ast.CompilationUnit parsedTestClass, List<String> dependencies) {
        if (parsedTestClass != null) {
            TypeDeclaration<?> testClassType = parsedTestClass.getType(0);

            try {
                testClassType.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                    String methodSignature = getFullSignature(methodDeclaration);

                    if (methodSignature == null) return;

                    testMethodSignature = testClassName + "/" + methodSignature;

                    methodTerms.put(testMethodSignature, new LinkedHashSet<>());

                    TypeCountVisitor typeCountVisitor = new TypeCountVisitor(dependencies);

                    typeCountVisitor.visitPreOrder(methodDeclaration);
                });
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        }
    }

    static class TypeCountVisitor extends TreeVisitor {
        List<String> dependencies;

        public TypeCountVisitor(List<String> dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public void process(Node node) {
            String type = null;
            if (node instanceof NameExpr) {
                try {
                    type = ((NameExpr) node).calculateResolvedType().describe();
                } catch (Throwable ignored) { }
            } else if (node instanceof ClassOrInterfaceType) {
                try {
                    type = ((ClassOrInterfaceType) node).resolve().describe();
                } catch (Throwable ignored) { }
                if (type == null) type = resolveUsingDependencies(((ClassOrInterfaceType) node).getNameAsString());
            }

            if (type != null) {
                type = type.replace("/", ".").replaceAll("<.*>", "");

                if (isNonJavaType(type)) {
                    termDocumentMatrix.get(testClassName).merge(type, 1, Integer::sum);
                    methodTerms.get(testMethodSignature).add(type);
                }
            }
        }

        private String resolveUsingDependencies(String typeName) {
            if (dependencies == null) return null;
            return dependencies.stream().filter(depCandidate -> depCandidate.endsWith("/" + typeName)).findFirst().orElse(null);
        }

        private boolean isNonJavaType(String fullyQualifiedName) {
            return !fullyQualifiedName.matches("java\\..*|(?:byte|char|int|short|long|float|void|double|boolean)[\\[\\]]*");
        }
    }

    protected static String stripPackageFromMethodName(String fullyQualified) {
        Pattern stripPackagePattern = Pattern.compile(".*\\.(.*\\(.*\\))");
        Matcher matcher = stripPackagePattern.matcher(fullyQualified);

        return matcher.matches() ? matcher.group(1) : null;
    }
}
