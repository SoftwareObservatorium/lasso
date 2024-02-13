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
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import org.apache.commons.collections4.bag.TreeBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

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
 * This class analyzes all classes inside a Maven artifact containing JUnit 4 test sequences, guesses the CUT (class under test)
 * and derives the required interface to execute each individual test sequence.
 */
public class HeuristicGuesser implements CutGuesser {
    private static final Logger LOG = LoggerFactory.getLogger(HeuristicGuesser.class);

    private TestClassAnalysisContext testClassAnalysisContext = new TestClassAnalysisContext();

    public static final double COEFFICIENT_OFFSET = 0.001;
    public static final double MAX_ADDITIONAL_SCOPE_WEIGHT = 4.0;
    public static final double WEIGHT_INIT_ASSIGN = 0.5;
    public static final double WEIGHT_METHOD_CALL = 1.0;
    public static final double WEIGHT_ASSERTION = 2.0;

    public HeuristicGuesser() { }

    public TestClassAnalysisContext getTestClassAnalysisContext() {
        return testClassAnalysisContext;
    }

    public void analyze(ClassUnderTest testClass) {
        LOG.info(String.format("Analyzing tests inside '%s' (heuristic)", testClass.getClassName()));

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

        List<String> dependencies = testClass.getImplementation().getCode().getDependencies().stream().map(d -> d.replace(".", "/")).collect(Collectors.toList());
        testClassAnalysisContext.setDependencies(dependencies);

        analyzeTestClass(parsedTestClass);
    }

    /**
     * analyze a single test class and guess the CUT of all its test methods
     * @param parsedTestClass an instance of com.github.javaparser.ast.CompilationUnit, a parsed representation of a Java source file
     */
    public void analyzeTestClass(com.github.javaparser.ast.CompilationUnit parsedTestClass) {
        if (parsedTestClass != null) {
            TypeDeclaration<?> testClassType = parsedTestClass.getType(0);

            testClassType.getFields().forEach(fieldDeclaration -> {
                fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                    String variableName = variableDeclarator.getNameAsString();

                    String returnedType = resolveReturnedType(variableDeclarator.getInitializer().orElse(null));
                    if (returnedType == null) {
                        try {
                            returnedType = variableDeclarator.getType().resolve().describe();
                        } catch (Throwable ignored) { }
                    }

                    if (returnedType == null) return;

                    variableName = "this." + variableName;

                    testClassAnalysisContext.getVariableTypes().put(variableName, returnedType);
                });
            });


            /*
                retrieve method declarations annotated with @Before, as those are used to setup the test environment and therefore required before any tests are executed
             */
            List<MethodDeclaration> beforeTestMethods = testClassType.getMethods().stream().filter(method -> method.getAnnotationByName("Before").isPresent()
                    || method.getAnnotationByName("org.junit.Before").isPresent()).collect(Collectors.toList());

            beforeTestMethods.forEach(beforeMethod -> testClassAnalysisContext.getBeforeCalls().add(beforeMethod.getSignature().asString()));

            // find constructor of test class
            MethodDeclaration constructor = testClassType.getMethods().stream().filter(BodyDeclaration::isConstructorDeclaration).findFirst().orElse(null);

            /*
                iterate through all assign expressions in methods annotated with @Before and in the constructor (=> setupMethods)
                e.g. a test class might define an InputStream "inputStream" reference in its body, but fill it with a FileInputStream in its @Before methods or constructor
                then, we want to store a mapping variable -> referenced type, so inputStream -> FileInputStream
             */

            if (constructor != null) {
                beforeTestMethods.add(constructor);
            }

            beforeTestMethods.forEach(setupMethod -> {
                setupMethod.findAll(AssignExpr.class).forEach(assignExpr -> {
                    if (!assignExpr.getTarget().isNameExpr()) return;
                    String variableName = assignExpr.getTarget().asNameExpr().getNameAsString();

                    String returnedType = resolveReturnedType(assignExpr.getValue());
                    if (returnedType != null) {
                        variableName = "this." + variableName;

                        testClassAnalysisContext.getVariableTypes().put(variableName, returnedType);
                    }
                });
            });

            /*
                retrieve also method declarations annotated with @After, as those are used to "teardown" the test environment: e.g. delete files, shutdown server, etc.
             */
            List<MethodDeclaration> afterTestMethods = testClassType.getMethods().stream().filter(method -> method.getAnnotationByName("After").isPresent()
                    || method.getAnnotationByName("org.junit.After").isPresent()).collect(Collectors.toList());

            afterTestMethods.forEach(afterMethod -> testClassAnalysisContext.getAfterCalls().add(afterMethod.getSignature().asString()));

            // snippet from JUnitSequenceSpecificationParser in arena module, select methods with @Test (org.junit.Test) annotation
            List<MethodDeclaration> junit4TestMethods = testClassType.getMethods().stream().filter(method -> method.getAnnotationByName("Test").isPresent()
                    || method.getAnnotationByName("org.junit.Test").isPresent()).collect(Collectors.toList());

            junit4TestMethods.forEach(junit4TestMethod -> {
                try {
                    TestMethod testMethod = analyzeTestMethod(junit4TestMethod);
                    testMethod.setBeforeCalls(testClassAnalysisContext.getBeforeCalls());
                    testMethod.setAfterCalls(testClassAnalysisContext.getAfterCalls());

                    testClassAnalysisContext.getTestMethods().add(testMethod);
                } catch (Throwable ignored) { }
            });
        }
    }

    /**
     * Analyze methods annotated with org.junit.Test
     * @param methodDeclaration
     * @return a TestMethod, containing information on the methods class under test
     */
    public TestMethod analyzeTestMethod(MethodDeclaration methodDeclaration) {

        // What methods to we want to evaluate here? The test method itself, and other methods that are called inside, from the same class

        TestMethodVisitor testMethodVisitor = new TestMethodVisitor(testClassAnalysisContext);
        testMethodVisitor.visitPreOrder(methodDeclaration);

        TestMethodAnalysisContext testMethodAnalysisContext = testMethodVisitor.currentContext();

        CUTCandidate cutCandidate = testMethodAnalysisContext.getHighestProbableCUT();
        LinkedHashSet<String> requiredInterface = testMethodAnalysisContext.getMethodSignatures(cutCandidate.getType());

        TestMethod testMethod = new TestMethod();
        testMethod.setName(methodDeclaration.getNameAsString());
        testMethod.setSignature(getFullSignature(methodDeclaration));
        testMethod.setClassUnderTest(testMethodAnalysisContext.getHighestProbableCUT().getType());
        testMethod.setRequiredSignatures(requiredInterface);

        return testMethod;
    }

    public String resolveReturnedType(Expression expression) {
        if (expression == null) return null;
        String type = null;

        try {
            if (expression.isObjectCreationExpr()) {
                type = expression.asObjectCreationExpr().getType().resolve().describe();
            } else if (expression.isMethodCallExpr()) {
                type = expression.asMethodCallExpr().resolve().getReturnType().describe();
            } else if (expression.isNameExpr()) {
                type = expression.asNameExpr().calculateResolvedType().describe();
            } else if (expression.isFieldAccessExpr()) {
                type = expression.asFieldAccessExpr().calculateResolvedType().describe();
            } else if (expression.isArrayAccessExpr()) {
                type = resolveReturnedType(expression.asArrayAccessExpr().getName());
            }
        } catch (Throwable ignored) { }

        return type;
    }

    /*
        Return "full" signature to be used for method matching during Solr document creation
     */
    public String getFullSignature(MethodDeclaration methodDeclaration) {
        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();

            String cleanedSignature = stripPackageFromMethodName(resolvedMethodDeclaration.getQualifiedSignature());
            String returnType = resolvedMethodDeclaration.getReturnType().describe();

            return returnType + " " + cleanedSignature;
        } catch (Exception ignored) { return null; }
    }

    @Override
    public String guess(ClassUnderTest testClass) throws IOException {
        analyze(testClass);

        final TreeBag<String> bag = new TreeBag<>();

        testClassAnalysisContext.getTestMethods().forEach(t -> {
            //System.out.println(t.getName() + " " + t.getClassUnderTest());
            if(t != null && t.getClassUnderTest() != null) {
                bag.add(t.getClassUnderTest());
            }
        });

        return bag.stream().max(Comparator.comparingInt(bag::getCount)).get();
    }

    static class TestMethodVisitor extends TreeVisitor {
        private final TestClassAnalysisContext testClassAnalysisContext;

        // Stack used to store the "analysis context" - necessary when recursing into methods called from the "main"/"starting" test method
        private final Stack<TestMethodAnalysisContext> testMethodAnalysisContextStack;

        String currentMethodClass = "";
        String currentMethodPackage = "";
        String currentMethodName = "";

        private Node parent = null;

        public TestMethodVisitor(TestClassAnalysisContext testClassAnalysisContext) {
            this.testClassAnalysisContext = testClassAnalysisContext;
            this.testMethodAnalysisContextStack = new Stack<>();
        }

        @Override
        public void process(Node node) {
            // do preorder processing of a test method, build a HashMap containing a mapping of variableName <-> type of reference stored inside, access this hashmap during assert statement evaluation
            // in case of methods called from the same test class, recurse into those and retrieve the likely CUT

            if (checkIfHasParent(node)) {
                return;
            }

            if (node instanceof MethodDeclaration) {
                MethodDeclaration methodDeclaration = (MethodDeclaration) node;

                String signature = methodDeclaration.getSignature().asString();

                // if method stack is empty or method is "new", push a new method analysis context on the stack
                if (testMethodAnalysisContextStack.size() == 0 || testMethodAnalysisContextStack.peek() == null || !testMethodAnalysisContextStack.peek().getSignature().equals(signature)) {
                    TestMethodAnalysisContext testMethodAnalysisContext = new TestMethodAnalysisContext();
                    testMethodAnalysisContext.setName(methodDeclaration.getNameAsString());
                    testMethodAnalysisContext.setSignature(signature);

                    try {
                        ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();
                        currentMethodClass = resolvedMethodDeclaration.getClassName();
                        currentMethodPackage = resolvedMethodDeclaration.getPackageName();
                        currentMethodName = resolvedMethodDeclaration.getName();
                    } catch (Throwable ignored) { }

                    testMethodAnalysisContextStack.push(testMethodAnalysisContext);
                }

            } else if (node instanceof VariableDeclarationExpr) {
                VariableDeclarator variableDeclarator = ((VariableDeclarationExpr) node).getVariable(0);

                String variableName = variableDeclarator.getNameAsString();

                String returnedType = resolveReturnedType(variableDeclarator.getInitializer().orElse(null));
                if (returnedType == null) {
                    try {
                        returnedType = resolveType(variableDeclarator.getType());
                    } catch (Throwable ignored) { }
                }

                if (returnedType != null) {
                    currentContext().getVariableTypes().put(variableName, returnedType);
                    increaseCUTMapping(returnedType, WEIGHT_INIT_ASSIGN);
                }
            } else if (node instanceof AssignExpr) {
                AssignExpr assignExpr = ((AssignExpr) node);

                String variableName = "";

                if (assignExpr.getTarget().isNameExpr()) {
                    variableName = assignExpr.getTarget().asNameExpr().toString();
                } else if(assignExpr.getTarget().isArrayAccessExpr()) {
                    variableName = assignExpr.getTarget().asArrayAccessExpr().getName().asNameExpr().toString();
                }

                String returnedType = resolveReturnedType(((AssignExpr) node).getValue());
                if (returnedType == null) {
                    try {
                        returnedType = ((AssignExpr) node).getTarget().calculateResolvedType().describe();
                    } catch (Throwable ignored) { }
                }

                if (returnedType != null) {
                    currentContext().getVariableTypes().put(variableName, returnedType);
                    increaseCUTMapping(returnedType, WEIGHT_INIT_ASSIGN);
                }
            } else if (node instanceof MethodCallExpr) {
                MethodCallExpr method = ((MethodCallExpr) node);

                parent = method;

                try {
                    ResolvedMethodDeclaration resolvedMethodDeclaration = method.resolve();

                    boolean isInSameClass = currentMethodPackage.equals(resolvedMethodDeclaration.getPackageName()) && currentMethodClass.equals(resolvedMethodDeclaration.getClassName());

                    if (isInSameClass && resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                        MethodDeclaration childMethodDeclaration = ((JavaParserMethodDeclaration) resolvedMethodDeclaration).getWrappedNode();

                        String signature = childMethodDeclaration.getSignature().asString();
                        TestMethodAnalysisContext result;

                        if (testClassAnalysisContext.getTestMethodAnalysisContextCache().containsKey(signature)) {
                            result = testClassAnalysisContext.getTestMethodAnalysisContextCache().get(signature);
                        } else {
                            this.visitPreOrder(childMethodDeclaration);

                            // retrieve results from the recursive analysis
                            result = this.testMethodAnalysisContextStack.pop();
                            testClassAnalysisContext.getTestMethodAnalysisContextCache().put(signature, result);
                        }

                        mergeCUTMapping(result.getCutWeightMapping());
                        mergeMethodSignatures(result.getMethodSignatures());
                    }
                } catch (Throwable ignored) { }

                String methodName = method.getNameAsString();

                if (methodName.matches("assert.*")) {
                    int lookupIndex = -1;

                    switch(methodName) {
                        case "assertNull":
                        case "assertNotNull":
                        case "assertTrue":
                        case "assertFalse":
                            lookupIndex = method.getArguments().size() == 2 ? 1 : 0;
                            break;
                        case "assertSame":
                        case "assertNotSame":
                        case "assertEquals":
                            lookupIndex = method.getArguments().size() == 3 ? 2 : 1;
                            break;
                        case "assertThat":
                            lookupIndex = method.getArguments().size() == 3 ? 1 : 0;
                            break;
                    }

                    try {
                        if (lookupIndex != -1) {
                            Expression argument = method.getArgument(lookupIndex);
                            traverseExpressionAndIncreaseScore(argument, WEIGHT_ASSERTION);
                        } else {
                            traverseExpressionAndIncreaseScore(method, WEIGHT_METHOD_CALL);
                        }
                    } catch(Exception ignored) { }
                } else {
                    traverseExpressionAndIncreaseScore(method, WEIGHT_METHOD_CALL);
                }

            } else if (node instanceof ObjectCreationExpr) {
                ObjectCreationExpr objectCreationExpr = ((ObjectCreationExpr) node);

                String type = resolveType(objectCreationExpr.getType());

                increaseCUTMapping(type, WEIGHT_INIT_ASSIGN);
            }
        }

        private boolean checkIfHasParent(Node nodeToEvaluate) {
            if (parent == null) return false;

            Node directParent = nodeToEvaluate.getParentNode().orElse(null);

            if (directParent == null) return false;

            if (directParent == parent || nodeToEvaluate == parent) {
                return true;
            } else {
                return checkIfHasParent(directParent);
            }
        }

        private TestMethodAnalysisContext currentContext() {
            return testMethodAnalysisContextStack.peek();
        }

        public String resolveReturnedType(Expression expression) {
            if (expression == null) return null;
            String type = null;

            if (expression.isObjectCreationExpr() || expression.isMethodCallExpr() || expression.isNameExpr() || expression.isFieldAccessExpr() || expression.isArrayAccessExpr()) {
                type = calculateResolvedType(expression);
                if (expression.isObjectCreationExpr()) {
                    String signature = getSignature(expression.asObjectCreationExpr());
                    currentContext().addMethodSignature(type, signature);
                }
            }

            return type;
        }

        private String traverseExpressionAndIncreaseScore(Expression expression, double weight) {
            if (expression == null) return null;

            String typeName = null;

            if (expression.isMethodCallExpr()) {
                MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

                Expression scope = methodCallExpr.getScope().orElse(null);

                typeName = calculateResolvedType(methodCallExpr);

                double scopeAdditionalWeight = calculateAdditionalScopeWeight(methodCallExpr.getNameAsString());
                double scopeWeight = scopeAdditionalWeight + weight;

                String scopeType = traverseExpressionAndIncreaseScore(scope, scopeWeight);
                String signature = getSignature(methodCallExpr);

                if (signature != null) currentContext().addMethodSignature(scopeType, signature);

                for (Expression parameter : methodCallExpr.getArguments()) {
                    traverseExpressionAndIncreaseScore(parameter, weight);
                }
            } else if (expression.isObjectCreationExpr()) {
                ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();

                typeName = calculateResolvedType(objectCreationExpr);

                String signature = getSignature(objectCreationExpr);

                if (signature != null) currentContext().addMethodSignature(typeName, signature);

                for (Expression argument : objectCreationExpr.getArguments()) {
                    traverseExpressionAndIncreaseScore(argument, weight);
                }
            } else if (expression.isFieldAccessExpr()) {
                FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();

                String fieldName = fieldAccessExpr.getScope().toString() + "." + fieldAccessExpr.asFieldAccessExpr().getNameAsString();

                typeName = retrieveTypeFromMapping(fieldName);

                if (typeName == null) typeName = calculateResolvedType(fieldAccessExpr);

                traverseExpressionAndIncreaseScore(fieldAccessExpr.getScope(), weight);
            } else if (expression.isNameExpr()) {
                NameExpr nameExpr = expression.asNameExpr();

                String variableName = nameExpr.getNameAsString();

                typeName = retrieveTypeFromMapping(variableName);

                if (typeName == null) typeName = calculateResolvedType(nameExpr);
            } else if (expression.isClassExpr()) {
                typeName = resolveType(expression.asClassExpr().getType());
            }

            if (typeName != null) increaseCUTMapping(typeName, weight);

            return typeName;
        }

        /**
         * utility method to resolve an expression into the corresponding type while catching any throwables
         * @param expression
         * @return
         */
        @Nullable
        private String calculateResolvedType(Expression expression) {
            try {
                if (expression.isMethodCallExpr()) {
                    return expression.asMethodCallExpr().resolve().getReturnType().describe();
                } else if (expression.isArrayAccessExpr()) {
                    return expression.asArrayAccessExpr().getName().calculateResolvedType().describe();
                } else {
                    return expression.calculateResolvedType().describe();
                }
            } catch (Throwable ignored) {
                if (expression.isNameExpr()) {
                    return resolveUsingDependencies(expression.asNameExpr().getNameAsString());
                }
            }
            return null;
        }

        @Nullable
        private String resolveType(Type type) {
            try {
                return type.resolve().describe();
            } catch (Throwable ignored) { }
            return resolveUsingDependencies(type.asString());
        }

        private String resolveUsingDependencies(String typeName) {
            String dep = testClassAnalysisContext.getDependencies().stream().filter(depCandidate -> depCandidate.endsWith("/" + typeName)).findFirst().orElse(null);
            if (dep != null) {
                dep = dep.replace("/", ".").trim();
            }
            return dep;
        }

        public String getSignature(ObjectCreationExpr objectCreationExpr) {
            try {
                ResolvedConstructorDeclaration resolvedConstructor = objectCreationExpr.resolve();

                StringBuilder signature = new StringBuilder();

                signature.append("<init>").append("(");

                int numParams = resolvedConstructor.getNumberOfParams();

                for (int i = 0; i < numParams; i++) {
                    String parameterType = cleanType(resolvedConstructor.getParam(i).getType().describe());

                    signature.append(parameterType);
                    if (i < (numParams - 1)) signature.append(",");
                }

                signature.append(")").append(":void");

                return signature.toString();

            } catch (Throwable ignored) { }
            return null;
        }

        public String getSignature(MethodCallExpr methodCallExpr) {
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = methodCallExpr.resolve();

                StringBuilder signature = new StringBuilder();

                signature.append(resolvedMethodDeclaration.getName()).append("(");

                int numParams = resolvedMethodDeclaration.getNumberOfParams();

                for (int i = 0; i < numParams; i++) {
                    String parameterType = cleanType(resolvedMethodDeclaration.getParam(i).getType().describe());

                    signature.append(parameterType);
                    if (i < (numParams - 1)) signature.append(",");
                }

                signature.append(")");

                String returnType = cleanType(resolvedMethodDeclaration.getReturnType().describe());

                signature.append(":").append(returnType);

                return signature.toString();

            } catch (Throwable ignored) { }
            return null;
        }

        private String cleanType(String qualifiedType) {

            // is generic type?
            if (qualifiedType.matches("^[ A-Z]$")) {
                return "java.lang.Object";
            }

            qualifiedType = qualifiedType.replaceAll("<.*>", "");

            return qualifiedType;
        }

        /**
         *
         * @param fieldName
         * @return the type of that mapping
         */
        @Nullable
        private String retrieveTypeFromMapping(String fieldName) {
            String typeName = currentContext().getVariableTypes().get(fieldName);

            if (typeName == null) typeName = testClassAnalysisContext.getVariableTypes().get(fieldName);

            if (typeName == null) typeName = testClassAnalysisContext.getVariableTypes().get("this." + fieldName);

            return typeName;
        }

        /**
         * check whether a fullyQualifiedName lies in the java.* namespace
         * @param fullyQualifiedName the fq to check
         * @return whether the fq matches java.*
         */
        private boolean isNonJavaType(String fullyQualifiedName) {
            return !fullyQualifiedName.matches("java\\..*|(?:byte|char|int|short|long|float|double|boolean|void)[\\[\\]]*");
        }

        private void increaseCUTMapping(String type, double weight) {
            if (!isNonJavaType(type)) return;
            type = type.replaceAll("<.*>", "");
            String cleanedType = stripPackageFromType(type);
            double jaccardClassName = NGramUtils.calculateJaccardDistance(cleanedType, currentMethodClass);

            // calculate the score by multiplying WEIGHT and JACCARD, with a small customizable OFFSET to the JACCARD similarity
            // otherwise, a Jaccard similarity of ZERO would prohibit this class from being a CUT at all
            double score = (jaccardClassName + COEFFICIENT_OFFSET) * weight;

            currentContext().increaseCutMapping(type, score);
        }

        private double calculateAdditionalScopeWeight(String methodName) {
            double jaccardMethodName = NGramUtils.calculateJaccardDistance(methodName, currentMethodName);

            return MAX_ADDITIONAL_SCOPE_WEIGHT * jaccardMethodName;
        }

        private void mergeCUTMapping(Map<String, Double> cutMapping) {
            currentContext().mergeCutMapping(cutMapping);
        }

        private void mergeMethodSignatures(Map<String, LinkedHashSet<String>> methodSignatures) {
            currentContext().mergeMethodSignatures(methodSignatures);
        }

        private String stripPackageFromType(String type) {
            int classPart = type.lastIndexOf(".");
            return type.substring(classPart + 1);
        }
    }

    protected static String stripPackageFromMethodName(String fullyQualified) {
        Pattern stripPackagePattern = Pattern.compile(".*\\.(.*\\(.*\\))");
        Matcher matcher = stripPackagePattern.matcher(fullyQualified);

        return matcher.matches() ? matcher.group(1) : null;
    }
}
