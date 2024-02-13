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
package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.MethodAmbiguityException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionConstructorDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.filter.SpecFilter;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JUnit parser utilities
 *
 * @author Marcus Kessel
 */
public class ParserUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(ParserUtils.class);

    /**
     * Remove methods from class.
     *
     * @param source
     * @param testMethods
     * @return
     * @throws IOException
     */
    public static String removeTestMethods(String source, Set<String> testMethods) throws IOException {
        JavaParser javaParser = new JavaParser();

        ParseResult<com.github.javaparser.ast.CompilationUnit> compilationUnitParseResult = javaParser.parse(source);

        com.github.javaparser.ast.CompilationUnit cu = compilationUnitParseResult.getResult().orElseThrow(() -> new IOException("Cannot parse source"));

        List<MethodDeclaration> methodDeclarationList = cu.findAll(MethodDeclaration.class, m -> {
            if(testMethods.contains(m.getNameAsString())) {
                return true;
            }

            return false;
        });

        // remove methods entirely
        if(CollectionUtils.isNotEmpty(methodDeclarationList)) {
            methodDeclarationList.forEach(Node::remove);
        }

        return cu.toString();
    }

    public static String filterTestsBySpec(ClassUnderTest classUnderTest, String source, SpecFilter specFilter) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ClassLoaderTypeSolver(classUnderTest.getProject().getContainer()));
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(source).getResult().get();

        // inject
        try {
            solver.inject(cu);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

        Map<String, InterfaceSpecification> specificationMap = new LinkedHashMap<>();

        TypeDeclaration<?> testClass = cu.getType(0);
        String testClassName = testClass.getNameAsString();

        List<MethodDeclaration> tbr = Collections.synchronizedList(new LinkedList<>());

        // iterate over test methods
        List<MethodDeclaration> testMethods = testClass.getMethods().stream().filter(m -> {
            return m.getAnnotationByName("Test").isPresent()
                    || m.getAnnotationByName("org.junit.Test").isPresent();
        })
                .filter(m -> {
                    // do pre-selection! remove any test method which do not contain CUT/MUT
                    try {
                        if(classUnderTest.getImplementation().getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                            String name = classUnderTest.getImplementation().getCode().getMethodNames().get(0);

                            boolean match = m.findAll(MethodCallExpr.class).stream().anyMatch(me -> StringUtils.equals(me.getNameAsString(), name));
                            if(!match) {
                                LOG.warn("Removing test method '{}'", m.getNameAsString());

                                tbr.add(m);
                            }

                            return match;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                    return true;
                })
                .collect(Collectors.toList());

        testMethods.parallelStream().forEach(testMethod ->

        //for(MethodDeclaration testMethod : testMethods)
        {
            // find all methods
            Set<Method> methods = testMethod.findAll(MethodCallExpr.class).stream().map(me -> {
                String methodName = me.getNameAsString();

                // speed up things (member resolution is expensive!)
                if (StringUtils.startsWithAny(methodName, "assert", "fail", "verifyException", "mock")) {
                    return null;
                }

                try {
                    ResolvedMethodDeclaration resolvedMethod = me.resolve();
                    Method method = resolveMethod((ReflectionMethodDeclaration) resolvedMethod);

                    if (method.getDeclaringClass().getName().equals(classUnderTest.getClassName())) {
                        ReflectionMethodSignature sig = new ReflectionMethodSignature(method);

                        return sig.getMethod();
                    }
                } catch (Throwable e) {
                    LOG.warn("resolution of method failed", e);

                    /*
                    com.github.javaparser.resolution.MethodAmbiguityException: Ambiguous method call: cannot find a most applicable method: ReflectionMethodDeclaration{method=public static double[] com.sibvisions.util.ArrayUtil.addAll(double[],int,double[])}, ReflectionMethodDeclaration{method=public static long[] com.sibvisions.util.ArrayUtil.addAll(long[],int,long[])}
                     */
                    if(e instanceof MethodAmbiguityException) {
                        MethodAmbiguityException mae = (MethodAmbiguityException) e;

                        // FIXME remove this method?
                    }
                }

                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());

            boolean removeMethod = methods.stream().anyMatch(m -> !specFilter.accept(classUnderTest, m));

            if(removeMethod) {
                LOG.debug("Removing test method '{}' because of non-accepted methods", testMethod.getNameAsString());

                // synchronize?
                //testMethod.remove(); // remove it

                tbr.add(testMethod);

                return;
            }

            // check for default constructor (any other constructors are forbidden)
            Set<Constructor> constructors = testMethod.findAll(ObjectCreationExpr.class).stream().map(oc -> {
                try {
                    ResolvedConstructorDeclaration resolvedConstructorDeclaration = oc.resolve();
                    Constructor constructor = ParserUtils.resolveConstructor((ReflectionConstructorDeclaration) resolvedConstructorDeclaration);

                    if (constructor.getDeclaringClass().getName().equals(classUnderTest.getClassName())) {
                        ReflectionConstructorSignature sig = new ReflectionConstructorSignature(constructor);

                        return sig.getConstructor();
                    }
                } catch (Throwable e) {
                    LOG.warn("Could not resolve constructor", e);
                }

                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());

            boolean removeConstructor = constructors.stream().anyMatch(c -> c.getParameterCount() > 0);

            if(removeConstructor) {
                LOG.debug("Removing test method '{}' because of non-default constructor", testMethod.getNameAsString());

                // synchronize?
                //testMethod.remove(); // remove it

                tbr.add(testMethod);
            }
        });

        // remove test methods
        tbr.forEach(Node::remove);

        return cu.toString();
    }

    public static Method resolveMethod(ReflectionMethodDeclaration decl) throws IllegalAccessException {
        Field field = FieldUtils.getDeclaredField(ReflectionMethodDeclaration.class, "method", true);
        return (Method) field.get(decl);
    }

    public static Constructor resolveConstructor(ReflectionConstructorDeclaration decl) throws IllegalAccessException {
        Field field = FieldUtils.getDeclaredField(ReflectionConstructorDeclaration.class, "constructor", true);
        return (Constructor) field.get(decl);
    }

    public static Field resolveField(ReflectionFieldDeclaration decl) throws IllegalAccessException {
        Field field = FieldUtils.getDeclaredField(ReflectionFieldDeclaration.class, "field", true);
        return (Field) field.get(decl);
    }
}
