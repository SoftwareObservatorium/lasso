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
package randoop;

import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import randoop.com.github.javaparser.JavaParser;
import randoop.com.github.javaparser.ParseException;
import randoop.com.github.javaparser.ast.*;
import randoop.com.github.javaparser.ast.body.*;
import randoop.com.github.javaparser.ast.expr.*;
import randoop.com.github.javaparser.ast.stmt.*;
import randoop.com.github.javaparser.ast.type.ClassOrInterfaceType;
import randoop.com.github.javaparser.ast.type.ReferenceType;
import randoop.com.github.javaparser.ast.type.VoidType;
import randoop.main.GenTests;
import randoop.output.NameGenerator;
import randoop.sequence.ExecutableSequence;

import java.util.*;

// taken from randoop and modified

/** Creates Java source as {@code String} for a suite of JUnit4 tests. */
@SuppressWarnings("deprecation") // TODO: fix. "new ClassOrInterfaceType()" does not handle generics
public class JUnit4Creator {

  /** An instance of a Java parser. */
  JavaParser javaParser;

  /** The "public" modifier. */
  private final NodeList<Modifier> PUBLIC = new NodeList<>(Modifier.publicModifier());

  /** The "public" and "static" modifiers. */
  private final NodeList<Modifier> PUBLIC_STATIC =
      new NodeList<>(Modifier.publicModifier(), Modifier.staticModifier());

  /** The package name. May be null, but may not be the empty string. */
  private final String packageName;

  /**
   * classMethodCounts maps test class names to the number of methods in each class. This is used to
   * generate lists of method names for a class. Each test method is named TEST_METHOD_NAME_PREFIX+i
   * for some integer i.
   */
  private Map<String, Integer> classMethodCounts;

  /** The Java text for BeforeAll method of generated test class. */
  private BlockStmt beforeAllBody = null;

  /** The Java text for AfterAll method of generated test class. */
  private BlockStmt afterAllBody = null;

  /** The Java text for BeforeEach method of generated test class. */
  private BlockStmt beforeEachBody = null;

  /** The Java text for AfterEach method of generated test class. */
  private BlockStmt afterEachBody = null;

  /** The JUnit annotation for the BeforeAll option. */
  private static final String BEFORE_ALL = "BeforeClass";

  /** The JUnit annotation for the AfterAll option. */
  private static final String AFTER_ALL = "AfterClass";

  /** The JUnit annotation for the BeforeEach option. */
  private static final String BEFORE_EACH = "Before";

  /** The JUnit annotation for the AfterEach option. */
  private static final String AFTER_EACH = "After";

  /** The method name for the BeforeAll option. */
  private static final String BEFORE_ALL_METHOD = "setupAll";

  /** The method name for the AfterAll option. */
  private static final String AFTER_ALL_METHOD = "teardownAll";

  /** The method name for the BeforeEach option. */
  private static final String BEFORE_EACH_METHOD = "setup";

  /** The method name for the AfterEach option. */
  private static final String AFTER_EACH_METHOD = "teardown";

  public static JUnit4Creator getTestCreator(
      String junit_package_name,
      BlockStmt beforeAllBody,
      BlockStmt afterAllBody,
      BlockStmt beforeEachBody,
      BlockStmt afterEachBody) {
    assert !Objects.equals(junit_package_name, "");
    JUnit4Creator junitCreator = new JUnit4Creator(junit_package_name);
    if (beforeAllBody != null) {
      junitCreator.addBeforeAll(beforeAllBody);
    }
    if (afterAllBody != null) {
      junitCreator.addAfterAll(afterAllBody);
    }
    if (beforeEachBody != null) {
      junitCreator.addBeforeEach(beforeEachBody);
    }
    if (afterEachBody != null) {
      junitCreator.addAfterEach(afterEachBody);
    }
    return junitCreator;
  }

  private JUnit4Creator(String packageName) {
    assert !Objects.equals(packageName, "");
    this.packageName = packageName;
    this.classMethodCounts = new LinkedHashMap<>();

    this.javaParser = new JavaParser();
  }

  /**
   * Add text for BeforeClass-annotated method in each generated test class.
   *
   * @param body the (Java) text for method
   */
  private void addBeforeAll(BlockStmt body) {
    this.beforeAllBody = body;
  }

  /**
   * Add text for AfterClass-annotated method in each generated text class.
   *
   * @param body the (Java) text for method
   */
  private void addAfterAll(BlockStmt body) {
    this.afterAllBody = body;
  }

  /**
   * Add text for Before-annotated method in each generated test class.
   *
   * @param body the (Java) text for method
   */
  private void addBeforeEach(BlockStmt body) {
    this.beforeEachBody = body;
  }

  /**
   * Add text for After-annotated method in each generated test class.
   *
   * @param text the (Java) text for method
   */
  private void addAfterEach(BlockStmt text) {
    this.afterEachBody = text;
  }

  /**
   * Create a test class.
   *
   * @param testClassName the class name
   * @param executionResults the contents of the test methods
   * @return the CompilationUnit for a test class
   */
  public CompilationUnit createTestClass(
          String testClassName, List<SequenceExecutionRecord> executionResults) {
    this.classMethodCounts.put(testClassName, executionResults.size());

    CompilationUnit compilationUnit = new CompilationUnit();
    if (packageName != null) {
      compilationUnit.setPackageDeclaration(new PackageDeclaration(new Name(packageName)));
    }

    NodeList<ImportDeclaration> imports = new NodeList<>();
    if (afterEachBody != null) {
      imports.add(new ImportDeclaration(new Name("org.junit.After"), false, false));
    }
    if (afterAllBody != null) {
      imports.add(new ImportDeclaration(new Name("org.junit.AfterClass"), false, false));
    }
    if (beforeEachBody != null) {
      imports.add(new ImportDeclaration(new Name("org.junit.Before"), false, false));
    }
    if (beforeAllBody != null) {
      imports.add(new ImportDeclaration(new Name("org.junit.BeforeClass"), false, false));
    }
    imports.add(new ImportDeclaration(new Name("org.junit.FixMethodOrder"), false, false));
    imports.add(new ImportDeclaration(new Name("org.junit.Test"), false, false));
    imports.add(new ImportDeclaration(new Name("org.junit.runners.MethodSorters"), false, false));
    compilationUnit.setImports(imports);

    // class declaration
    ClassOrInterfaceDeclaration classDeclaration =
        new ClassOrInterfaceDeclaration(PUBLIC, false, testClassName);
    NodeList<AnnotationExpr> annotations =
        new NodeList<>(
            new SingleMemberAnnotationExpr(
                new Name("FixMethodOrder"), new NameExpr("MethodSorters.NAME_ASCENDING")));
    classDeclaration.setAnnotations(annotations);

    NodeList<BodyDeclaration<?>> bodyDeclarations = new NodeList<>();
    // // add debug field
    // VariableDeclarator debugVariable = javaParser.parseVariableDeclarationExpr("boolean
    // debug=false;");
    // debugVariable.setInitializer(new BooleanLiteralExpr(false));
    // FieldDeclaration debugField =
    //     new FieldDeclaration(
    //         PUBLIC_STATIC),
    //         new NodeList<AnnotationExpr>(PrimitiveType.forClass(PrimitiveType.booleanType())),
    //         new NodeList<VariableDeclarator>(debugVariable));
//    BodyDeclaration<?> debugField =
//        javaParser.parseBodyDeclaration("public static boolean debug=false;").getResult().get();
//
//    bodyDeclarations.add(debugField);

    if (beforeAllBody != null) {
      MethodDeclaration fixture =
          createFixture(BEFORE_ALL, PUBLIC_STATIC, BEFORE_ALL_METHOD, beforeAllBody);
      if (fixture != null) {
        bodyDeclarations.add(fixture);
      }
    }
    if (afterAllBody != null) {
      MethodDeclaration fixture =
          createFixture(AFTER_ALL, PUBLIC_STATIC, AFTER_ALL_METHOD, afterAllBody);
      if (fixture != null) {
        bodyDeclarations.add(fixture);
      }
    }
    if (beforeEachBody != null) {
      MethodDeclaration fixture =
          createFixture(BEFORE_EACH, PUBLIC, BEFORE_EACH_METHOD, beforeEachBody);
      if (fixture != null) {
        bodyDeclarations.add(fixture);
      }
    }
    if (afterEachBody != null) {
      MethodDeclaration fixture =
          createFixture(AFTER_EACH, PUBLIC, AFTER_EACH_METHOD, afterEachBody);
      if (fixture != null) {
        bodyDeclarations.add(fixture);
      }
    }

    for (SequenceExecutionRecord record : executionResults) {
      MethodDeclaration testMethod = createTestMethod(testClassName, LassoUtils.compactUUID(record.getSequenceSpecification().getName()), record.getExecutableSequence());
      if (testMethod != null) {
        bodyDeclarations.add(testMethod);
      }
    }
    classDeclaration.setMembers(bodyDeclarations);
    NodeList<TypeDeclaration<?>> types = new NodeList<>(classDeclaration);
    compilationUnit.setTypes(types);

    return compilationUnit;
  }

  /**
   * Creates a test method as a {@code String} for the sequence {@code testSequence}.
   *
   * @param className the name of the test class
   * @param methodName the name of the test method
   * @param testSequence the {@link ExecutableSequence} test sequence
   * @return the {@code String} for the test method
   */
  private MethodDeclaration createTestMethod(
      String className, String methodName, ExecutableSequence testSequence) {
    MethodDeclaration method = new MethodDeclaration(PUBLIC, new VoidType(), methodName);
    NodeList<AnnotationExpr> annotations =
        new NodeList<>(new MarkerAnnotationExpr(new Name("Test")));
    method.setAnnotations(annotations);

    @SuppressWarnings("deprecation") // new ClassOrInterfaceDeclaration dose not handle generics
    NodeList<ReferenceType> throwsList = new NodeList<>(new ClassOrInterfaceType("Throwable"));
    method.setThrownExceptions(throwsList);

    BlockStmt body = new BlockStmt();
    NodeList<Statement> statements = new NodeList<>();
//    FieldAccessExpr field = new FieldAccessExpr(new NameExpr("System"), "out");
//    MethodCallExpr call = new MethodCallExpr(field, "format");
//
//    NodeList<Expression> arguments = new NodeList<>();
//    arguments.add(new StringLiteralExpr("%n%s%n"));
//    arguments.add(new StringLiteralExpr(className + "." + methodName));
//    call.setArguments(arguments);
//    statements.add(new IfStmt(new NameExpr("debug"), new ExpressionStmt(call), null));

    // TODO make sequence generate list of JavaParser statements
    String sequenceBlockString = "{ " + testSequence.toCodeString() + " }";
    // try {
    BlockStmt sequenceBlock = javaParser.parseBlock(sequenceBlockString).getResult().get();
    statements.addAll(sequenceBlock.getStatements());
    // }
    // catch (ParseException e) {
    //   System.out.println(
    //       "Parse error while creating test method " + className + "." + methodName + " for block
    // ");
    //   System.out.println(sequenceBlockString);
    //   throw new RandoopBug("Parse error while creating test method", e);
    // }
    // catch (TokenMgrError e) {
    //   System.out.println(
    //       "Lexical error while creating test method " + className + "." + methodName);
    //   System.out.println("Exception: " + e.getMessage());
    //   System.out.println(sequenceBlockString);
    //   return null;
    // }

    body.setStatements(statements);
    method.setBody(body);
    return method;
  }

  /**
   * Creates the declaration of a single test fixture.
   *
   * @param annotation the fixture annotation
   * @param modifiers the method modifiers for fixture declaration
   * @param methodName the name of the fixture method
   * @param body the {@code BlockStmt} for the fixture
   * @return the fixture method as a {@code String}
   */
  private MethodDeclaration createFixture(
      String annotation, NodeList<Modifier> modifiers, String methodName, BlockStmt body) {
    MethodDeclaration method = new MethodDeclaration(modifiers, new VoidType(), methodName);
    NodeList<AnnotationExpr> annotations =
        new NodeList<>(new MarkerAnnotationExpr(new Name(annotation)));
    method.setAnnotations(annotations);
    method.setBody(body);
    return method;
  }

  /**
   * Creates the JUnit4 suite class for the tests in this object as a {@code String}.
   *
   * @param suiteClassName the name of the suite class created
   * @param testClassNames the names of the test classes in the suite
   * @return the {@code String} with the declaration for the suite class
   */
  public String createTestSuite(String suiteClassName, Iterable<String> testClassNames) {
    CompilationUnit compilationUnit = new CompilationUnit();
    if (packageName != null) {
      compilationUnit.setPackageDeclaration(new PackageDeclaration(new Name(packageName)));
    }
    NodeList<ImportDeclaration> imports = new NodeList<>();
    imports.add(new ImportDeclaration(new Name("org.junit.runner.RunWith"), false, false));
    imports.add(new ImportDeclaration(new Name("org.junit.runners.Suite"), false, false));
    compilationUnit.setImports(imports);

    ClassOrInterfaceDeclaration suiteClass =
        new ClassOrInterfaceDeclaration(PUBLIC, false, suiteClassName);
    NodeList<AnnotationExpr> annotations = new NodeList<>();
    annotations.add(
        new SingleMemberAnnotationExpr(new Name("RunWith"), new NameExpr("Suite.class")));
    StringJoiner classList = new StringJoiner(".class, ", "{ ", ".class }");
    for (String testClassName : testClassNames) {
      classList.add(testClassName);
    }
    annotations.add(
        new SingleMemberAnnotationExpr(
            new Name("Suite.SuiteClasses"), new NameExpr(classList.toString())));
    suiteClass.setAnnotations(annotations);
    NodeList<TypeDeclaration<?>> types = new NodeList<>(suiteClass);
    compilationUnit.setTypes(types);
    return compilationUnit.toString();
  }

  /**
   * Create non-reflective test driver as a main class.
   *
   * @param driverName the name for the driver class
   * @param testClassNames the names of the test classes in the suite
   * @param numMethods the number of methods; used for zero-padding
   * @return the test driver class as a {@code String}
   */
  public String createTestDriver(
      String driverName, Iterable<String> testClassNames, int numMethods) {
    CompilationUnit compilationUnit = new CompilationUnit();
    if (packageName != null) {
      compilationUnit.setPackageDeclaration(new PackageDeclaration(new Name(packageName)));
    }

    MethodDeclaration mainMethod = new MethodDeclaration(PUBLIC_STATIC, new VoidType(), "main");
    NodeList<Parameter> parameters = new NodeList<>();
    @SuppressWarnings("deprecation") // new ClassOrInterfaceType does not handle generics
    Parameter parameter = new Parameter(new ClassOrInterfaceType("String"), "args");
    parameter.setVarArgs(true);
    parameters.add(parameter);
    mainMethod.setParameters(parameters);

    mainMethod.getParameters().get(0).setVarArgs(true);

    NodeList<Statement> bodyStatements = new NodeList<>();

    String failureVariableName = "hadFailure";
    Statement hadFailureDecl =
        javaParser.parseStatement("boolean " + failureVariableName + " = false;").getResult().get();
    bodyStatements.add(hadFailureDecl);

    NameGenerator instanceNameGen = new NameGenerator("t");
    NameGenerator methodNameGen =
        new NameGenerator(GenTests.TEST_METHOD_NAME_PREFIX, 1, numMethods);
    for (String testClass : testClassNames) {
      if (beforeAllBody != null) {
        bodyStatements.add(
            new ExpressionStmt(new MethodCallExpr(new NameExpr(testClass), BEFORE_ALL_METHOD)));
      }

      // ExpressionStmt expressionStmt = new ExpressionStmt();
      // VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
      // VariableDeclarator variableDeclarator = new VariableDeclarator();
      // variableDeclarator.setName("anyVariableName");
      // variableDeclarator.setType(new ClassOrInterfaceType("AnyVariableType"));
      // variableDeclarator.setInitializer("new AnyVariableType()");
      // NodeList<VariableDeclarator> variableDeclarators = new NodeList<>();
      // variableDeclarators.add(variableDeclarator);
      // variableDeclarationExpr.setVariables(variableDeclarators);
      // expressionStmt.setExpression(variableDeclarationExpr);
      // blockStmt.addStatement(expressionStmt);

      String testVariable = instanceNameGen.next();
      ClassOrInterfaceType testClassType = new ClassOrInterfaceType(testClass);
      @SuppressWarnings("deprecation") // "new ClassOrInterfaceType()" does not handle generics
      VariableDeclarator variableDecl =
          new VariableDeclarator(
              testClassType,
              testVariable,
              new ObjectCreationExpr(null, testClassType, new NodeList<Expression>()));
      NodeList<VariableDeclarator> variableList = new NodeList<>(variableDecl);
      @SuppressWarnings("deprecation") // "new ClassOrInterfaceType()" does not handle generics
      VariableDeclarationExpr variableExpr = new VariableDeclarationExpr(variableList);
      bodyStatements.add(new ExpressionStmt(variableExpr));

      int classMethodCount = classMethodCounts.get(testClass);

      for (int i = 0; i < classMethodCount; i++) {
        if (beforeEachBody != null) {
          bodyStatements.add(
              new ExpressionStmt(
                  new MethodCallExpr(new NameExpr(testVariable), BEFORE_EACH_METHOD)));
        }
        String methodName = methodNameGen.next();

        TryStmt tryStmt = new TryStmt();
        NodeList<Statement> tryStatements =
            new NodeList<>(
                new ExpressionStmt(new MethodCallExpr(new NameExpr(testVariable), methodName)));
        BlockStmt tryBlock = new BlockStmt();

        tryBlock.setStatements(tryStatements);
        tryStmt.setTryBlock(tryBlock);
        CatchClause catchClause = new CatchClause();
        catchClause.setParameter(new Parameter(new ClassOrInterfaceType("Throwable"), "e"));
        BlockStmt catchBlock = new BlockStmt();
        NodeList<Statement> catchStatements = new NodeList<>();
        catchStatements.add(
            new ExpressionStmt(
                new AssignExpr(
                    new NameExpr(failureVariableName),
                    new BooleanLiteralExpr(true),
                    AssignExpr.Operator.ASSIGN)));
        catchStatements.add(
            new ExpressionStmt(new MethodCallExpr(new NameExpr("e"), "printStackTrace")));

        catchBlock.setStatements(catchStatements);
        catchClause.setBody(catchBlock);
        NodeList<CatchClause> catches = new NodeList<>(catchClause);
        tryStmt.setCatchClauses(catches);
        bodyStatements.add(tryStmt);

        if (afterEachBody != null) {
          bodyStatements.add(
              new ExpressionStmt(
                  new MethodCallExpr(new NameExpr(testVariable), AFTER_EACH_METHOD)));
        }
      }

      if (afterAllBody != null) {
        bodyStatements.add(
            new ExpressionStmt(new MethodCallExpr(new NameExpr(testClass), AFTER_ALL_METHOD)));
      }
    }

    BlockStmt exitCall = new BlockStmt();
    NodeList<Expression> args = new NodeList<>(new IntegerLiteralExpr("1"));
    NodeList<Statement> exitStatement =
        new NodeList<>(
            new ExpressionStmt(new MethodCallExpr(new NameExpr("System"), "exit", args)));
    exitCall.setStatements(exitStatement);
    bodyStatements.add(new IfStmt(new NameExpr(failureVariableName), exitCall, null));

    BlockStmt body = new BlockStmt();
    body.setStatements(bodyStatements);
    mainMethod.setBody(body);
    NodeList<BodyDeclaration<?>> bodyDeclarations = new NodeList<>(mainMethod);

    ClassOrInterfaceDeclaration driverClass =
        new ClassOrInterfaceDeclaration(PUBLIC, false, driverName);
    driverClass.setMembers(bodyDeclarations);

    NodeList<TypeDeclaration<?>> types = new NodeList<>(driverClass);
    compilationUnit.setTypes(types);
    return compilationUnit.toString();
  }

  public BlockStmt parseFixture(List<String> bodyText) throws ParseException {
    if (bodyText == null) {
      return null;
    }
    StringBuilder blockText = new StringBuilder();
    blockText.append("{").append(Globals.lineSep);
    for (String line : bodyText) {
      blockText.append(line).append(Globals.lineSep);
    }
    blockText.append(Globals.lineSep).append("}");
    return javaParser.parseBlock(blockText.toString()).getResult().get();
  }
}
