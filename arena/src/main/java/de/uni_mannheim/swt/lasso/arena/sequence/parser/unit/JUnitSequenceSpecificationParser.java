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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.YamlPrinter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionConstructorDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parse JUnit test methods into {@link SequenceSpecification}s.
 *
 * @author Marcus Kessel
 */
public class JUnitSequenceSpecificationParser {

    private static final Logger LOG = LoggerFactory
            .getLogger(JUnitSequenceSpecificationParser.class);

    /**
     * Resolve pseudo operations based on {@link InterfaceSpecification}.
     */
    private boolean resolvePseudoOperations = true;

    /**
     * Guesses CUT specification from test class.
     *
     * @param testClassSource
     * @param classUnderTest
     * @return
     * @throws IOException
     */
    public Map<String, InterfaceSpecification> toSpecification(String testClassSource, ClassUnderTest classUnderTest) throws IOException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ClassLoaderTypeSolver(classUnderTest.getProject().getContainer()));
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(testClassSource).getResult().get();

        // inject
        try {
            solver.inject(cu);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

        Map<String, InterfaceSpecification> specificationMap = new LinkedHashMap<>();

        TypeDeclaration<?> tc = cu.getType(0);
        String testClassName = tc.getNameAsString();

        // iterate over test methods
        List<MethodDeclaration> testMethods = tc.getMethods().stream().filter(m -> {
            return m.getAnnotationByName("Test").isPresent()
                    || m.getAnnotationByName("org.junit.Test").isPresent();
        }).collect(Collectors.toList());

//        testMethods.forEach(testMethod -> {
//            if (LOG.isDebugEnabled()) {
//                LOG.debug(new YamlPrinter(true).output(testMethod));
//            }
//        });

        // FIXME only do search over test methods
        InterfaceSpecification specification = new InterfaceSpecification();
        List<MethodSignature> cSpecs = new LinkedList<>();
        specification.setConstructors(cSpecs);
        List<MethodSignature> mSpecs = new LinkedList<>();
        specification.setMethods(mSpecs);

        Map<String, String> cuts = new LinkedHashMap<>();

        // find all variables
        List<VariableDeclarationExpr> vars = cu.findAll(VariableDeclarationExpr.class).stream().filter(var -> {
            VariableDeclarator variableDeclarator = var.getVariable(0);

            // lhs check
            Type type = variableDeclarator.getType();
            String className = StringUtils.substringBefore(type.asString(), "<");

            try {
                ResolvedType resolvedType = type.resolve();

//                if(resolvedType.isReferenceType()) {
//                    className = resolvedType.asReferenceType().toString();
//                }

                LOG.debug("RESOLVED POTENTIAL CUT TYPE " + resolvedType);
            } catch (Throwable e) {
                LOG.warn("resolution of type failed", e);
            }

//            // fully qualified
//            if (StringUtils.contains(className, ".")) {
//
//            }

//            // ignore generic types
//            if (StringUtils.contains(testClassName, className)) {
//                cuts.put(var.getVariable(0).getNameAsString(), className);
//
//                return true;
//            }

            if (StringUtils.endsWith(classUnderTest.getClassName(), className)) {
                cuts.put(var.getVariable(0).getNameAsString(), className);

                return true;
            }

            // check right-hand-side
            if (variableDeclarator.getInitializer().isPresent()) {
                Expression initExpr = variableDeclarator.getInitializer().get();
                // check if we find static calls
                if (initExpr.isMethodCallExpr()) {
                    MethodCallExpr methodCallExpr = initExpr.asMethodCallExpr();

                    try {
                        ResolvedMethodDeclaration resolvedMethod = methodCallExpr.resolve();

                        Method method = ParserUtils.resolveMethod((ReflectionMethodDeclaration) resolvedMethod);

                        if (method.getDeclaringClass().getName().equals(classUnderTest.getClassName())) {
                            LOG.debug("RESOLVED METHOD " + method);

                            ReflectionMethodSignature sig = new ReflectionMethodSignature(method);
                            if (!mSpecs.contains(sig)) {
                                mSpecs.add(sig);
                            }

                            cuts.put("static", method.getDeclaringClass().getSimpleName());
                        }
                    } catch (Throwable e) {
                        LOG.warn("resolution of method failed", e);
                    }
                }
            }

            return false;
        }).collect(Collectors.toList());

        // find all methods
        List<MethodCallExpr> methods = tc.findAll(MethodCallExpr.class).stream().filter(me -> {
            if (me.getScope().isPresent()) {
                Expression expr = me.getScope().get();

                if (expr.isNameExpr()) {
                    boolean found = vars.stream().anyMatch(v -> v.getVariable(0).getNameAsString().equals(expr.asNameExpr().getNameAsString()));

                    return found;
                }
            }

            return false;
        }).collect(Collectors.toList());

        if (cuts.size() < 1 && mSpecs.size() < 1) {
            throw new IOException("Could not find any CUTs");
        }

//        if (new HashSet<>(cuts.values()).size() > 1) {
//            throw new IOException("Found more than one CUT");
//        }

        String actualCut = new ArrayList<>(cuts.values()).get(0);
        specification.setClassName(actualCut);

        LOG.debug("ACTUAL CUT " + actualCut);

        if (!StringUtils.endsWith(classUnderTest.getClassName(), actualCut)) {
            throw new IOException(String.format("Identified CUT != expected CUT => %s vs %s", actualCut, classUnderTest.getClassName()));
        }

        specificationMap.put(classUnderTest.getClassName(), specification);

        for (VariableDeclarationExpr v : vars) {
            LOG.debug("FOUND SPEC VAR " + v);

            v.getVariable(0).getInitializer().ifPresent(i -> {
                if (i.isObjectCreationExpr()) {
                    ObjectCreationExpr objectCreationExpr = i.asObjectCreationExpr();

                    try {
                        ResolvedConstructorDeclaration resolvedConstructorDeclaration = objectCreationExpr.resolve();
                        LOG.debug("FOUND SPEC RESOLVED CONSTRUCTOR " + resolvedConstructorDeclaration.getQualifiedSignature());

                        LOG.debug("IS REFLECTION CONSTRUCTOR " + (resolvedConstructorDeclaration instanceof ReflectionConstructorDeclaration));

                        Constructor constructor = ParserUtils.resolveConstructor((ReflectionConstructorDeclaration) resolvedConstructorDeclaration);
                        LOG.debug("RESOLVED CONSTRUCTOR " + constructor);

                        if (constructor.getDeclaringClass().getName().equals(classUnderTest.getClassName())) {
                            ReflectionConstructorSignature sig = new ReflectionConstructorSignature(constructor);
                            if (!cSpecs.contains(sig)) {
                                cSpecs.add(sig);
                            }
                        }
                    } catch (Throwable e) {
                        LOG.warn("Could not resolve constructor", e);
                    }
                } else {
                    LOG.debug("FOUND " + i);
                }
            });
        }

        for (MethodCallExpr m : methods) {
            LOG.debug("FOUND SPEC METHOD " + m.getNameAsString());

            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = m.resolve();
                LOG.debug("FOUND SPEC RESOLVED METHOD " + resolvedMethodDeclaration);

                LOG.debug("IS REFLECTION METHOD " + (resolvedMethodDeclaration instanceof ReflectionMethodDeclaration));

                Method method = ParserUtils.resolveMethod((ReflectionMethodDeclaration) resolvedMethodDeclaration);
                LOG.debug("RESOLVED METHOD " + method);

                ReflectionMethodSignature sig = new ReflectionMethodSignature(method);
                if (!mSpecs.contains(sig)) {
                    mSpecs.add(sig);
                }
            } catch (Throwable e) {
                LOG.warn("Could not resolve method", e);
            }
        }

        // add default constructor if none is used (required for adaptation)
        if(CollectionUtils.isEmpty(cSpecs)) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Adding default constructor");
            }

            MethodSignature defaultConstructor = new MethodSignature(specification) {
                @Override
                public String toLQL() {
                    StringBuilder sb = new StringBuilder();
//        sb.append(className);
//        sb.append("(");

                    sb.append(getName());
                    sb.append("(");
                    sb.append(String.join(",", toParameterString()));
                    sb.append(")");
                    //sb.append(toReturnString());

                    return sb.toString();
                }
            };
            try {
                Class<?> cut = classUnderTest.loadClass();
                defaultConstructor.setName(cut.getSimpleName());
                defaultConstructor.setReturnType(cut);
            } catch (ClassNotFoundException e) {
                LOG.warn("default constructor failed", e);
            }
            defaultConstructor.setParameterTypes(new Class[0]);
            cSpecs.add(defaultConstructor);
        }

        return specificationMap;
    }

    public Map<String, SequenceSpecification> toSequenceSpecifications(String testClassSource, InterfaceSpecification specification, ClassUnderTest classUnderTest, String fullyQualifiedCut, String postfix) {
        return toSequenceSpecifications(testClassSource, specification, classUnderTest, fullyQualifiedCut, null, postfix);
    }

    public Map<String, SequenceSpecification> toSequenceSpecifications(String testClassSource, InterfaceSpecification specification, ClassUnderTest classUnderTest, String fullyQualifiedCut, String prefix, String postfix) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        if (classUnderTest != null) {
            combinedTypeSolver.add(new ClassLoaderTypeSolver(classUnderTest.getProject().getContainer()));
        } else {
            combinedTypeSolver.add(new ReflectionTypeSolver());
        }
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        JavaParser javaParser = new JavaParser();

        // expected cut class
        //Type cutClass = javaParser.parseType(fullyQualifiedCut).getResult().get();

        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(testClassSource).getResult().get();

        // inject
        try {
            solver.inject(cu);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

//        String packageName = "";
//        if (cu.getPackageDeclaration().isPresent()) {
//            packageName = cu.getPackageDeclaration().get().getNameAsString();
//        }
//
//        // imports
//        Set<String> imports = new LinkedHashSet<>();
//        cu.getImports().forEach(implDecl -> {
//            String name = implDecl.getNameAsString();
//            if (implDecl.isStatic()) {
//                // pkg.class.method OR pkg.class.*
//                name = StringUtils.substringBeforeLast(name, ".");
//            }
//
//            if (!implDecl.isStatic() && implDecl.isAsterisk()) {
//                // FIXME here we simply add pkg.* to the list for now
//            }
//
//            imports.add(name);
//        });

        // limit
        if (cu.getTypes().size() > 1) {
            throw new UnsupportedOperationException("No more than one class supported right now");
        }

        // expect single class
        TypeDeclaration<?> tc = cu.getType(0);

        String testClassName = tc.getNameAsString();

        // get global cut names
        if (CollectionUtils.isNotEmpty(tc.getFields())) {
            // FIXME
        }

        // iterate over test methods
        List<MethodDeclaration> testMethods = tc.getMethods().stream().filter(m -> {
            return m.getAnnotationByName("Test").isPresent()
                    || m.getAnnotationByName("org.junit.Test").isPresent();
        }).collect(Collectors.toList());

        Map<String, SequenceSpecification> ssMap = new LinkedHashMap<>();
        for (MethodDeclaration testMethod : testMethods) {
            try {
                SequenceSpecification ss = toSequenceSpecification(testMethod, specification, classUnderTest, postfix);

                // sheet might be null if method body empty
                if (ss != null) {
                    if(StringUtils.isNotBlank(prefix)) {
                        ss.setName(prefix + "." + ss.getName());
                    }

                    ssMap.put(ss.getName(), ss);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Test method '{}'", testMethod.getNameAsString());
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Could not parse sequence specification from test method '{}'", testMethod.getNameAsString(), e);
            }
        }

        return ssMap;
    }

    public SequenceSpecification toSequenceSpecification(MethodDeclaration testMethod, InterfaceSpecification specification, ClassUnderTest classUnderTest, String postfix) throws ClassNotFoundException {
        SequenceSpecification ss = new SequenceSpecification();

        JUnitParseContext context = new JUnitParseContext(testMethod);
        context.setSpecification(specification);
        context.setClassUnderTest(classUnderTest);
        context.setSequenceSpecification(ss);

        String name = testMethod.getNameAsString();
        ss.setName(String.format("%s_%s", name, postfix));

        if (LOG.isDebugEnabled()) {
            LOG.debug(new YamlPrinter(true).output(testMethod));
        }


        // empty method body, ignore
        Optional<BlockStmt> blockStmtOptional = testMethod.getBody();
        if (!blockStmtOptional.isPresent()) {
            return null;
        }

        BlockStmt blockStmt = blockStmtOptional.get();
        // XXX can this happen?
        if (blockStmt.getStatements().size() < 1) {
            return null;
        }

        // unnest statements
        List<Statement> statementList = new LinkedList<>();
        for (Statement statement : blockStmt.getStatements()) {
            if (statement.isTryStmt()) {
                LOG.debug("Unnesting try statement");

                statementList.addAll(statement.asTryStmt().getTryBlock().getStatements());
            } else {
                statementList.add(statement);
            }
        }

        for (Statement statement : statementList) {
            // FIXME currently we support expression statements only
            if (statement.isExpressionStmt()) {
                ExpressionStmt expressionStmt = statement.asExpressionStmt();

                Expression expression = expressionStmt.getExpression();

                if (expression.isVariableDeclarationExpr()) {
                    VariableDeclarationExpr vExpr = expression.asVariableDeclarationExpr();
                    VariableDeclarator vDecl = vExpr.getVariable(0); // FIXME add support for more

                    // LHS type arguments
                    if (vDecl.getType().isClassOrInterfaceType()) {
                        Optional<NodeList<Type>> targs = vDecl.getType().asClassOrInterfaceType().getTypeArguments();
                        if (targs.isPresent()) {
                            targs.get().stream().forEach(t -> LOG.debug("LHS GENERIC '{}' for '{}'", t, vDecl));
                        }
                    }

                    // left-hand side assignment
                    String lhs = vDecl.getNameAsString();

                    LOG.debug("LHS '{}'", lhs);

                    context.getLocalFields().add(lhs);

                    // lhs type
                    Type lhsAssignmentType = vDecl.getType(); // if type ClassOrInterface then also check typeargs
                    context.getLocalFieldTypes().put(lhs, lhsAssignmentType);

                    // right hand side
                    Optional<Expression> initializerDeclarationOptional = vDecl.getInitializer();
                    if (initializerDeclarationOptional.isPresent()) {
                        Expression initExpr = initializerDeclarationOptional.get();

                        processAssignment(context, initExpr, lhs);
                    }
                } else if (expression.isMethodCallExpr()) {
                    MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

                    processMethodCallExpr(context, methodCallExpr, null);
                } else if (expression.isAssignExpr()) {
                    // variable assignment (without declaration)
                    AssignExpr assignExpr = expression.asAssignExpr();
                    Expression initExpr = assignExpr.getValue();
                    if (assignExpr.getTarget().isNameExpr()) {
                        String lhs = assignExpr.getTarget().asNameExpr().getNameAsString();

                        processAssignment(context, initExpr, lhs);
                    } else if (assignExpr.getTarget().isArrayAccessExpr()) {
                        ArrayAccessExpr arrayAccessExpr = assignExpr.getTarget().asArrayAccessExpr();
                        LOG.debug("Found array access '{}'", arrayAccessExpr);

                        String arrayName = arrayAccessExpr.getName().asNameExpr().getNameAsString();
                        int index = arrayAccessExpr.getIndex().asIntegerLiteralExpr().asNumber().intValue();
                        Expression value = assignExpr.getValue();

                        int rowNum = context.getLocalFields().lastIndexOf(arrayName);
                        //LOG.debug(context.getLocalFields().stream().collect(Collectors.joining(",")));
                        SpecificationStatement arrStatement = context.getSequenceSpecification().getStatement(rowNum);

                        LOG.debug("arr = {}, index = {}, value = {}, stmt = {}", arrayName, index, value, arrStatement);

                        int size = context.getSequenceSpecification().getLength();
                        processAssignment(context, value, null);

                        // any changes?
                        if (size < context.getSequenceSpecification().getLength()) {
                            SpecificationStatement addedStmt = context.getSequenceSpecification().getLastStatement();

                            ArraySetStatement arraySetStatement = arraySetStatement = new ArraySetStatement(index);
                            arraySetStatement.addInput(arrStatement);
                            arraySetStatement.addInput(addedStmt);

                            context.getSequenceSpecification().addStatement(arraySetStatement, -1);
                        }
                    } else {
                        LOG.warn("Ignoring assign expression type '{}'", assignExpr.getTarget().getClass());
                    }
                } else {
                    // ignore
                    LOG.warn("Ignoring expression type '{}'", expression.getClass());
                }
            } else {
                // ignore
                LOG.warn("Ignoring statement type '{}'", statement.getClass());
            }
        }

        // alternative way of doing it ..
//        statementList.accept(new VoidVisitor<Void>() {
//        });

        // mark CUT operations
        for (SpecificationStatement statement : ss.getStatements()) {
            // set if CUT or NOT
            if (statement instanceof ConstructorCallStatement) {
                ConstructorCallStatement constructorCallStatement = (ConstructorCallStatement) statement;
                if (constructorCallStatement.isResolved()) {
                    Constructor<?> constructor = constructorCallStatement.getResolvedConstructor();
                    constructorCallStatement.setClassUnderTest(constructor.getDeclaringClass().equals(classUnderTest.loadClass()));
                } else {
                    constructorCallStatement.setClassUnderTest(true);
                }
            }

            if (statement instanceof MethodCallStatement) {
                MethodCallStatement methodCallStatement = (MethodCallStatement) statement;
                if (methodCallStatement.isResolved()) {
                    Method method = methodCallStatement.getResolvedMethod();
                    methodCallStatement.setClassUnderTest(method.getDeclaringClass().equals(classUnderTest.loadClass()));
                } else {
                    methodCallStatement.setClassUnderTest(true);
                }
            }
        }


        return ss;
    }

    private void processAssignment(JUnitParseContext context, Expression initExpr, String lhs) {
        int position = -1;
        if (StringUtils.isNotBlank(lhs)) {
            position = context.getLocalFields().lastIndexOf(lhs);
        }

        Type type = null;
        if (initExpr.isCastExpr()) {
            CastExpr castExpr = initExpr.asCastExpr();
            type = castExpr.getType(); // FIXME use type information
            LOG.debug("Found type info in init '{}'", type);

            initExpr = castExpr.getExpression();
        }

        if (initExpr.isEnclosedExpr()) {
            initExpr = initExpr.asEnclosedExpr().getInner();
        }

        if (initExpr.isObjectCreationExpr()) {
            // constructor call
            ObjectCreationExpr objectCreationExpr = initExpr.asObjectCreationExpr();
            String className = objectCreationExpr.getType().getNameAsString();

            // attempt to resolve fully-qualified name
            try {
                className = objectCreationExpr.getType().resolve().getQualifiedName();
            } catch (Throwable e) {
                //
            }

            LOG.debug("Found constructor call '{}'", className);

            ConstructorCallStatement statement;
            try {
                ResolvedConstructorDeclaration resolvedConstructorDeclaration = objectCreationExpr.resolve();
                Constructor<?> constructor = ParserUtils.resolveConstructor((ReflectionConstructorDeclaration) resolvedConstructorDeclaration);

                ReflectionConstructorSignature sig = new ReflectionConstructorSignature(constructor);
                statement = new ConstructorCallStatement(sig);
            } catch (Throwable e) {
                LOG.warn("Could not resolve constructor", e);

                //statement = new ConstructorCallStatement(new MethodSignature());
                // resolve manually
                if(isResolvePseudoOperations()) {
                    try {
                        MethodSignature sig = resolveConstructor(context, objectCreationExpr);
                        statement = new ConstructorCallStatement(sig);
                    } catch (Throwable ex) {
                        LOG.warn("manual resolution of constructor failed", e);

                        statement = new ConstructorCallStatement(new MethodSignature(context.getSpecification()));
                    }
                } else {
                    statement = new ConstructorCallStatement(new MethodSignature(context.getSpecification()));
                }
            }

            if (statement != null) {
                context.getSequenceSpecification().addStatement(statement, position);
                processArguments(context, statement, objectCreationExpr.getArguments());
            }
        } else if (initExpr.isMethodCallExpr()) {
            // method call
            MethodCallExpr methodCallExpr = initExpr.asMethodCallExpr();

            processMethodCallExpr(context, methodCallExpr, lhs);
        } else if (initExpr.isLiteralExpr()) {
            LiteralExpr literalExpr = initExpr.asLiteralExpr();

            LOG.debug("Found literal expr in assignment '{}'", literalExpr);

            // handle null where we cannot determine the type
            if(initExpr.isNullLiteralExpr() && StringUtils.isNotBlank(lhs)) {
                type = context.getLocalFieldTypes().get(lhs);

            }

            ValueStatement valueStatement = processLiteral(context, literalExpr, type);
            context.getSequenceSpecification().addStatement(valueStatement, position);
        } else if (initExpr.isUnaryExpr()) {
            UnaryExpr unaryExpr = initExpr.asUnaryExpr();

            LOG.debug("Found unary expr in assignment '{}'", unaryExpr);

            ValueStatement valueStatement = processUnary(context, unaryExpr, type);
            context.getSequenceSpecification().addStatement(valueStatement, position);
        } else if (initExpr.isBinaryExpr()) {
            BinaryExpr binaryExpr = initExpr.asBinaryExpr();

            LOG.debug("Found binary expr in assignment '{}'", binaryExpr);

            ValueStatement valueStatement = processBinary(context, binaryExpr, type);
            context.getSequenceSpecification().addStatement(valueStatement, position);
        } else if (initExpr.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = initExpr.asFieldAccessExpr();

            LOG.debug("Found FieldAccessExpr '{}'", fieldAccessExpr);

            // constants like bla.Base64.VALUE

            // extract reference
            String reference = fieldAccessExpr.toString();
            try {
                ResolvedValueDeclaration resolvedValueDeclaration = fieldAccessExpr.resolve();
                ReflectionFieldDeclaration reflectionFieldDeclaration = (ReflectionFieldDeclaration) resolvedValueDeclaration;

                // resolve field
                Field field = ParserUtils.resolveField(reflectionFieldDeclaration);

                LOG.debug("Resolved field '{}'", field);

                reference = String.format("%s.%s", field.getDeclaringClass().getName(), field.getName());

                // read in value

                //
                ValueStatement valueStatement = new ValueStatement(field.getType(), context.getEval().expr(reference));
                context.getSequenceSpecification().addStatement(valueStatement, position);
            } catch (Throwable e) {
                LOG.warn("Failed to resolve field reference '{}'", fieldAccessExpr);
                LOG.warn("Stack trace", e);
            }

            // just allow for FQ references
//            row.createCell(1).setCellValue("FIELD");
//            Cell cell = row.createCell(row.getLastCellNum());
//            cell.setCellValue(String.format("^%s", reference));
        } else if (initExpr.isArrayInitializerExpr()) {
            ArrayInitializerExpr arrayInitializerExpr = initExpr.asArrayInitializerExpr();

            LOG.debug("Found ArrayInitializerExpr '{}'", arrayInitializerExpr);

            // FIXME more sophisticated handling especially when field references are in etc.

            Object arr = context.getEval().expr(arrayInitializerExpr.toString());
            ValueStatement valueStatement = new ValueStatement(arr != null ? arr.getClass() : null, arr);
            context.getSequenceSpecification().addStatement(valueStatement, position);
        } else if (initExpr.isArrayCreationExpr()) {
            ArrayCreationExpr arrayCreationExpr = initExpr.asArrayCreationExpr();

            LOG.debug("Found ArrayCreationExpr '{}'", arrayCreationExpr);

            // do we need to resolve element values?
            Object arr;
            if(arrayCreationExpr.getInitializer().isPresent()
                    && CollectionUtils.isNotEmpty(arrayCreationExpr.getInitializer().get().getValues())
                    && arrayCreationExpr.getInitializer().get().getValues().stream().allMatch(Expression::isNameExpr)) {
                // returns local var inits
                List<String> initLines = processArrayInitializerExpr(arrayCreationExpr.getInitializer().get(), context);
                String init = String.join("\n", initLines);

                String arrInit = init + "\n" + arrayCreationExpr.toString();

                arr = context.getEval().exprRaw(arrInit); // raw evaluation of mini script
            } else {
                arr = context.getEval().expr(arrayCreationExpr.toString());
            }

            //Object arr = context.getEval().expr(arrayCreationExpr.toString());
            ValueStatement valueStatement = new ValueStatement(arr != null ? arr.getClass() : null, arr);
            context.getSequenceSpecification().addStatement(valueStatement, position);
        } else if (initExpr.isNameExpr()) {
            LOG.debug("Found var name in initializer '{}' of type '{}'", initExpr, initExpr.getClass());

            NameExpr nameExpr = initExpr.asNameExpr();

            // aliasing

            int pos = context.getLocalFields().lastIndexOf(nameExpr.getNameAsString());
            SpecificationStatement specificationStatement = context.getSequenceSpecification().getStatement(pos);
            ValueStatement alias = new ValueStatement(null, null);
            alias.setAlias(true);
            alias.addInput(specificationStatement);
            context.getSequenceSpecification().addStatement(alias, position);
        } else {
            // FIXME anything missing?
            LOG.debug("Missed initializer '{}' of type '{}'", initExpr, initExpr.getClass());

        }
    }

    protected void processMethodCallExpr(JUnitParseContext context, MethodCallExpr methodCallExpr, String lhs) {
        int position = -1;
        if (StringUtils.isNotBlank(lhs)) {
            position = context.getLocalFields().lastIndexOf(lhs);
        }

        String methodName = methodCallExpr.getNameAsString();

        if (StringUtils.startsWithAny(methodName, "assert", "fail", "verifyException" /*, "mock"*/)) {
            LOG.warn("Found assertion statement " + methodCallExpr);

            // add to expected column or so

            // assertEquals: equivalence check: optional: 1st argument is description
            // assertTrue: condition (expression)
            // ...

            // check if there are a nested CUT calls
            methodCallExpr.findAll(MethodCallExpr.class, n -> !n.equals(methodCallExpr))
                    .forEach(mc -> {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found nested method call in assertion '{}'", mc);
                        }

                        try {
                            processMethodCallExpr(context, mc, null);
                        } catch (Throwable e) {
                            LOG.warn("processing method call failed for '{}'", mc);
                            LOG.warn("Stack trace", e);
                        }
                    });

            return;
        }

        MethodCallStatement statement = null;

        Optional<Expression> scopeOptional = methodCallExpr.getScope();
        if (scopeOptional.isPresent()) {
            // instance call: scope here is the instance (method owner), var name
            if (scopeOptional.get().isNameExpr()) {
                // map var name to row id
                String varName = scopeOptional.get().asNameExpr().getNameAsString();

//                // is CUT call?
//                if (cutInstances.contains(varName)) {
//                    // double check if in specification, otherwise drop
//                    boolean exists = specification.getMethods().stream().anyMatch(m -> StringUtils.equalsIgnoreCase(methodName, m.getName()));
//                    if (!exists) {
//                        throw new IllegalArgumentException(String.format("Ignoring CUT method '%s' since it is not part of the specification", methodName));
//                    }
//                }

                // identify cell id
                int rowNum = -1;

                // FIXME for PSEUDO, this can never happen?
                if (!context.getLocalFields().contains(varName)) {
                    // FIXME at this point a static method call might happen
                    // we have to check whether we need to do an instance NEW
                    // perhaps resolve the reference and find the class owner

                    LOG.debug("Found static method call in NameExpr '{}'", scopeOptional.get().asNameExpr());

                    try {
                        ResolvedMethodDeclaration resolvedMethod = methodCallExpr.resolve();
                        Method method = ParserUtils.resolveMethod((ReflectionMethodDeclaration) resolvedMethod);
                        ReflectionMethodSignature sig = new ReflectionMethodSignature(method);
                        statement = new MethodCallStatement(sig);

                        // no owner
                    } catch (Throwable e) {
                        LOG.warn("resolution of method failed", e);

                        // resolve manually
                        if(isResolvePseudoOperations()) {
                            try {
                                MethodSignature sig = resolveMethod(context, methodCallExpr);
                                statement = new MethodCallStatement(sig);
                            } catch (Throwable ex) {
                                LOG.warn("manual resolution of method failed", e);
                            }
                        }
                    }
                } else {
                    rowNum = context.getLocalFields().lastIndexOf(varName);
                }

                if (rowNum > -1) {
                    try {
                        ResolvedMethodDeclaration resolvedMethod = methodCallExpr.resolve();

                        Method method = ParserUtils.resolveMethod((ReflectionMethodDeclaration) resolvedMethod);

                        ReflectionMethodSignature sig = new ReflectionMethodSignature(method);

                        statement = new MethodCallStatement(sig);

                        // add owner
                        statement.addInput(context.getSequenceSpecification().getStatement(rowNum));
                    } catch (Throwable e) {
                        LOG.warn("resolution of method failed", e);

                        // resolve manually
                        if(isResolvePseudoOperations()) {
                            try {
                                MethodSignature sig = resolveMethod(context, methodCallExpr);
                                statement = new MethodCallStatement(sig);

                                // add owner
                                statement.addInput(context.getSequenceSpecification().getStatement(rowNum));
                            } catch (Throwable ex) {
                                LOG.warn("manual resolution of method failed", e);
                            }
                        }
                    }
                } else {
                    LOG.warn("Cannot map var name '{}'", varName);
                }
            }

            // static call (i.e FieldExpression)
            if (scopeOptional.get().isFieldAccessExpr()) {
                FieldAccessExpr fieldAccessExpr = scopeOptional.get().asFieldAccessExpr();

                // fully qualified (includes pkg)
                String fullyQualifiedName = fieldAccessExpr.toString();

                if (StringUtils.equalsAny(fullyQualifiedName, "Assert", "org.junit.Assert")) {
                    LOG.warn("Found assertion statement " + methodCallExpr);

                    // add to expected column or so

                    // assertEquals: equivalence check: optional: 1st argument is description
                    // assertTrue: condition (expression)
                    // ...

                    return;
                }

                // at this point a static method call might happen with fully qualified name like java.lang.Math.abs(10)
                // we have to check whether we need to do an instance NEW
                LOG.debug("Found static method call in FieldAccessExpr '{}'", fieldAccessExpr);

                String fqName = fieldAccessExpr.toString();

                if(StringUtils.equalsAny(fqName, "System.out")) {
                    LOG.warn("Ignoring method call '{}'", methodCallExpr.toString());

                    return;
                }

                try {
                    ResolvedMethodDeclaration resolvedMethod = methodCallExpr.resolve();
                    Method method = ParserUtils.resolveMethod((ReflectionMethodDeclaration) resolvedMethod);
                    ReflectionMethodSignature sig = new ReflectionMethodSignature(method);
                    statement = new MethodCallStatement(sig);

                    // no owner
                } catch (Throwable e) {
                    LOG.warn("resolution of method failed", e);

                    // resolve manually
                    if(isResolvePseudoOperations()) {
                        try {
                            MethodSignature sig = resolveMethod(context, methodCallExpr);
                            statement = new MethodCallStatement(sig);
                        } catch (Throwable ex) {
                            LOG.warn("manual resolution of method failed", e);
                        }
                    }
                }
            }
        } else {
            // no scope expression
        }

        // arguments passed
        if (statement != null) {
            if(statement.isResolvedMethodStatic()) {
                // need to introduce initializer
            }

            context.getSequenceSpecification().addStatement(statement, position);
            processArguments(context, statement, methodCallExpr.getArguments());
        }
    }

    private void processArguments(JUnitParseContext context, CallStatement statement, NodeList<Expression> arguments) {
        // arguments passed
        int a = 0;
        for (Expression arg : arguments) {
            Type type = null;
            // FIXME check if wrapped
            if (arg.isCastExpr()) {
                CastExpr castExpr = arg.asCastExpr();
                type = castExpr.getType(); // FIXME use type information

                LOG.debug("Found type info in method call '{}'", type);

                arg = castExpr.getExpression();
            }

            // FIXME we may need to "evaluate" enclosed Expressions (like (1+1) etc.)
            if (arg.isEnclosedExpr()) {
                arg = arg.asEnclosedExpr().getInner();
            }

            // identify cell id
            if (context.getLocalFields().lastIndexOf(arg.toString()) > -1) {
                int rowNum = context.getLocalFields().lastIndexOf(arg.toString());

                LOG.debug("last index '{}' for '{}'", statement, rowNum);

                SpecificationStatement input = context.getSequenceSpecification().getStatement(rowNum);
                statement.addInput(input);
            } else {
                //
                ValueStatement valueStatement = null;

                if (arg.isLiteralExpr()) {
                    LiteralExpr literalExpr = arg.asLiteralExpr();

                    Class<?> paramType = null;
                    if(literalExpr.isNullLiteralExpr()) {
                        paramType = statement.getMethodSignature().getParameterTypes()[a];
                    }

                    LOG.debug("Found literal expr in method argument '{}'", literalExpr);

                    valueStatement = processLiteral(context, literalExpr, type);
                    if(valueStatement.getType() == null && paramType != null) {
                        valueStatement = new ValueStatement(paramType, valueStatement.getValue()); // should be null
                    }
                }

                if (arg.isUnaryExpr()) {
                    UnaryExpr unaryExpr = arg.asUnaryExpr();

                    LOG.debug("Found unary expr in method argument '{}'", unaryExpr);

                    valueStatement = processUnary(context, unaryExpr, type);
                }

                if (arg.isBinaryExpr()) {
                    BinaryExpr binaryExpr = arg.asBinaryExpr();

                    LOG.debug("Found unary binary in method argument '{}'", binaryExpr);

                    valueStatement = processBinary(context, binaryExpr, type);
                }

                if (valueStatement != null) {
                    statement.addInput(valueStatement);
                }
            }

            a++;
        }
    }

    private ValueStatement processLiteral(JUnitParseContext context, LiteralExpr literalExpr, Type type) {
        LOG.debug("Found literal expr '{}'", literalExpr);

        Class<?> typeClazz = null;
        Object value = null;

        String resolvedTypeStr = null;
        try {
            if(type != null) {
                ResolvedType resolvedType = type.resolve();
                resolvedTypeStr = resolvedType.describe();

                // use Container!!
                typeClazz = context.resolveClass(resolvedTypeStr);
            } else if(!literalExpr.isNullLiteralExpr()) {
                ResolvedType resolvedType = literalExpr.calculateResolvedType();
                resolvedTypeStr = resolvedType.describe();

                // use Container!!
                typeClazz = context.resolveClass(resolvedTypeStr);
            }
        } catch (Throwable e) {
            LOG.warn("Could not resolve literal type for '{}'", literalExpr);
            LOG.warn("Stack trace", e);
        }

        value = context.getEval().expr(literalExpr.toString());

        return new ValueStatement(typeClazz, value);
    }

    private List<String> processArrayInitializerExpr(ArrayInitializerExpr arrayInitializerExpr, JUnitParseContext context) {
        List<String> init = new LinkedList<>();
        for(Expression exp : arrayInitializerExpr.getValues()) {
            LOG.debug("is arr element var {}", exp.isNameExpr());

            if(exp.isNameExpr()) {
                int index = context.getLocalFields().indexOf(exp.asNameExpr().getNameAsString());
                LOG.debug("index of  '{}' is '{}'", exp.asNameExpr().getNameAsString(), index);

                if(index > -1) {
                    SpecificationStatement statement = context.getSequenceSpecification().getStatement(index);

                    LOG.debug("resolved statement of '{}' is '{}'", exp.asNameExpr().getNameAsString(), statement);
                }

                List<VariableDeclarator> vars = context.getTestMethod().findAll(VariableDeclarator.class, v -> {
                    if(exp.asNameExpr().getNameAsString().equals(v.getNameAsString())) {
                        LOG.debug("found var " + v.getParentNode());

                        return true;
                    }

                    return false;
                });

                if(CollectionUtils.isNotEmpty(vars)) {
                    init.add(vars.get(0).getParentNode().get().toString());
                }
            }

            // FIXME element could also be nested array
        }

        return init;
    }

    private ValueStatement processUnary(JUnitParseContext context, UnaryExpr unaryExpr, Type type) {
        LOG.debug("Found unary expr '{}'", unaryExpr);

        Class<?> typeClazz = null;
        Object value = null;

        String resolvedTypeStr = null;
        try {
            ResolvedType resolvedType = type != null ? type.resolve() : unaryExpr.calculateResolvedType();
            resolvedTypeStr = resolvedType.describe();

            // use Container!!
            typeClazz = context.resolveClass(resolvedTypeStr);
        } catch (Throwable e) {
            LOG.warn("resolving type for unary expr failed '{}'", unaryExpr);
            LOG.warn("Stack trace", e);
        }

        value = context.getEval().expr(unaryExpr.toString());

        return new ValueStatement(typeClazz, value);
    }

    private ValueStatement processBinary(JUnitParseContext context, BinaryExpr binaryExpr, Type type) {
        LOG.debug("Found binary expr '{}'", binaryExpr);

        Class<?> typeClazz = null;
        Object value = null;

        String resolvedTypeStr = null;
        try {
            ResolvedType resolvedType = type != null ? type.resolve() : binaryExpr.calculateResolvedType();
            resolvedTypeStr = resolvedType.describe();

            // use Container!!
            typeClazz = context.resolveClass(resolvedTypeStr);
        } catch (Throwable e) {
            LOG.warn("resolving type for binary expr failed '{}'", binaryExpr);
            LOG.warn("Stack trace", e);
        }

        value = context.getEval().expr(binaryExpr.toString());

        return new ValueStatement(typeClazz, value);
    }

    /**
     * Resolve "pseudo" methods based on {@link InterfaceSpecification}.
     *
     * @param context
     * @param methodCallExpr
     * @return
     */
    private MethodSignature resolveMethod(JUnitParseContext context, MethodCallExpr methodCallExpr) {
        LOG.debug("Attempting to resolve method based on interface specification '{}'", methodCallExpr);

        // check if it is CUT
        Optional<Expression> scopeOptional = methodCallExpr.getScope();
        if (scopeOptional.isPresent()) {
            // instance call: scope here is the instance (method owner), var name
            if (scopeOptional.get().isNameExpr()) {
                // map var name to row id
                String varName = scopeOptional.get().asNameExpr().getNameAsString();

                if (!context.getLocalFields().contains(varName)) {
                    //
                    throw new IllegalArgumentException("unexpected static method");
                } else {
                    int rowNum = context.getLocalFields().lastIndexOf(varName);

                    SpecificationStatement c = context.getSequenceSpecification().getStatement(rowNum);
                    if(c instanceof ConstructorCallStatement) {
                        ConstructorCallStatement ccs = (ConstructorCallStatement) c;
                        if(ccs.isResolved()) {
                            throw new IllegalArgumentException("unexpected resolved constructor for unresolved method " + methodCallExpr);
                        }
                    }
                }
            }
        }


//        PseudoMethodSignature sig = new PseudoMethodSignature();
//        sig.setName(methodCallExpr.getNameAsString());
        String name = methodCallExpr.getNameAsString();

        if(CollectionUtils.isEmpty(context.getSpecification().getMethods())) {
            throw new IllegalArgumentException("No CUT methods defined in interface specification");
        }

        // input parameters
        int params = methodCallExpr.getArguments().size();
        Optional<MethodSignature> methodSignatureOp = context.getSpecification().getMethods().stream().filter(m -> {
            // FIXME more precise: based on type matching as well
            if (StringUtils.equals(m.getName(), name)) {
                if(m.getParameterTypes().length == params) {
                    LOG.debug("Matched interface method '{}'", m.toLQL());

                    return true;
                }
            }

            return false;
        }).findFirst();

        MethodSignature m = methodSignatureOp
                .orElseThrow(() -> new IllegalArgumentException("No CUT method defined like " + methodCallExpr));

//        sig.setName(m.getName());
//        sig.setClassName(m.getClassName());
//        sig.setParameterTypes(Arrays.copyOf(m.getParameterTypes(), m.getParameterTypes().length));
//        sig.setReturnType(m.getReturnType());

        return m;
    }

    /**
     * Resolve "pseudo" constructors based on {@link InterfaceSpecification}.
     *
     * @param context
     * @param objectCreationExpr
     * @return
     */
    private MethodSignature resolveConstructor(JUnitParseContext context, ObjectCreationExpr objectCreationExpr) {
        LOG.debug("Attempting to resolve constructor based on interface specification '{}'", objectCreationExpr);

        //PseudoMethodSignature sig = new PseudoMethodSignature();

        //LOG.debug("CON TYPE '{}'", objectCreationExpr.getType());
        if(!StringUtils.equals(objectCreationExpr.getTypeAsString(), context.getSpecification().getClassName())) {
            LOG.debug("'{}' vs '{}'", objectCreationExpr.getTypeAsString(), context.getSpecification().getClassName());

            throw new IllegalArgumentException("unresolved constructor is not part of the interface specification");
        }

        if(CollectionUtils.isEmpty(context.getSpecification().getMethods())) {
            throw new IllegalArgumentException("No CUT methods defined in interface specification");
        }

        // input parameters
        int params = objectCreationExpr.getArguments().size();
        Optional<MethodSignature> methodSignatureOp = context.getSpecification().getConstructors().stream().filter(m -> {
            // FIXME more precise: based on type matching as well
            //if (StringUtils.equals(m.getName(), sig.getName())) {
                if(m.getParameterTypes().length == params) {
                    LOG.debug("Matched interface constructor '{}'", m.toLQL());

                    return true;
                }
            //}

            return false;
        }).findFirst();

        MethodSignature m = methodSignatureOp
                .orElseThrow(() -> new IllegalArgumentException("No CUT constructor defined like " + objectCreationExpr));

//        sig.setName(m.getName());
//        sig.setClassName(m.getClassName());
//        sig.setParameterTypes(Arrays.copyOf(m.getParameterTypes(), m.getParameterTypes().length));
//        sig.setReturnType(m.getReturnType());

        return m;
    }

    public boolean isResolvePseudoOperations() {
        return resolvePseudoOperations;
    }

    public void setResolvePseudoOperations(boolean resolvePseudoOperations) {
        this.resolvePseudoOperations = resolvePseudoOperations;
    }
}
