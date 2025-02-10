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
package de.uni_mannheim.swt.lasso.llm.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.YamlPrinter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.core.model.Specification;
import de.uni_mannheim.swt.lasso.llm.problem.Problem;
import de.uni_mannheim.swt.lasso.llm.util.EvalAndSerialize;
import de.uni_mannheim.swt.lasso.lql.parser.LQL;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import de.uni_mannheim.swt.lasso.ssn.SheetResolver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class ProblemToSheetParser {

    private static final Logger LOG = LoggerFactory
            .getLogger(ProblemToSheetParser.class);

    private final EvalAndSerialize evalAndSerialize;
    private final Container container;

    public ProblemToSheetParser(Container container) {
        this.evalAndSerialize = new EvalAndSerialize();

        this.container = container;
    }

    public LQLParseResult parseLQL(Problem problem) throws IOException {
        //
        String lql = parseMethodSignature(problem);

        LQLParseResult parseResult = LQL.parse(lql);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Parse result\n'{}'", parseResult);
        }

        if (parseResult != null) {
            return parseResult;
        }

        throw new IOException("unsupported LQL query " + lql);
    }

    public String parseMethodSignature(Problem problem) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        combinedTypeSolver.add(new ClassLoaderTypeSolver(container));
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        String content = problem.getPrompt() + "\n}\n}";

        if(LOG.isDebugEnabled()) {
            LOG.debug("Parsing\n{}", content);
        }

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(content).getResult().get();

        // inject
        try {
            solver.inject(cu);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(new YamlPrinter(true).output(cu));
        }


        List<MethodDeclaration> testMethods = cu.getType(0).getMethods().stream().filter(m -> !m.getName().toString().equals("main")).collect(Collectors.toList());

        LOG.debug("{}", testMethods.get(0).getNameAsString());
        LOG.debug("{}", testMethods.get(0));

        MethodDeclaration md = testMethods.get(0);

//        System.out.println(md.getParameters().stream().map(p -> p.getType().resolve()).collect(Collectors.toList()));
//        System.out.println(md.getType().resolve());

        StringBuilder sb = new StringBuilder();
        sb.append("Problem"); // predefined class name of all
        sb.append(" {\n  ");
        sb.append(md.getNameAsString());
        sb.append("(");
        sb.append(md.getParameters().stream().map(p -> p.getType().resolve().describe()).collect(Collectors.joining(",")));
        sb.append(")->");
        sb.append(md.getType().resolve().describe());
        sb.append("\n}");

        return sb.toString();
    }

    public List<String> parseImports(Problem problem) {
        String content = problem.getPrompt() + "\n}\n}";

        if(LOG.isDebugEnabled()) {
            LOG.debug("Parsing\n{}", content);
        }

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(content).getResult().get();

        // get imports
        Set<String> imports = new LinkedHashSet<>();
        cu.getImports().forEach(implDecl -> {
            String name = implDecl.getNameAsString();
            if (implDecl.isStatic()) {
                // pkg.class.method OR pkg.class.*
                name = StringUtils.substringBeforeLast(name, ".");
            }

            if (!implDecl.isStatic() && implDecl.isAsterisk()) {
                // FIXME here we simply add pkg.* to the list for now
            }

            imports.add(name);
        });

        LOG.debug("Found imports '{}'", imports);

        return new ArrayList<>(imports);
    }

    public String parseDescription(Problem problem) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        combinedTypeSolver.add(new ClassLoaderTypeSolver(container));
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        String content = problem.getPrompt() + "\n}\n}";

        if(LOG.isDebugEnabled()) {
            LOG.debug("Parsing\n{}", content);
        }

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(content).getResult().get();

        // inject
        try {
            solver.inject(cu);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

        return cu.getType(0).getOrphanComments().stream()
                .map(c -> StringUtils.replace(c.getContent(), "//", ""))
                .collect(Collectors.joining("\n"));
    }

    public List<Sequence> parse(Problem problem) throws IOException {
        LQLParseResult lqlParseResult = parseLQL(problem);

        //String content = "public class Problem {\n" + StringUtils.substringBetween(problem.getTests(),"}", "}") + "\n}\n}";
        String content = "public class Problem {\n" + StringUtils.substringAfter(problem.getTests(),"}");

        if(LOG.isDebugEnabled()) {
            LOG.debug("Parsing\n{}", content);
        }

        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(content).getResult().get();

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ClassLoaderTypeSolver(container));
        combinedTypeSolver.add(new ImportSolver(container, parseImports(problem)));
        JavaSymbolSolver solver = new JavaSymbolSolver(combinedTypeSolver);

        // inject
        try {
            solver.inject(cu);
        } catch (Throwable e) {
            LOG.warn("Injecting CU failed", e);
        }

//        if (LOG.isDebugEnabled()) {
//            LOG.debug(new YamlPrinter(true).output(cu));
//        }

        List<MethodDeclaration> testMethods = cu.getType(0).getMethods();

        // we only need the inputs and outputs
        testMethods.forEach(testMethod -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug(new YamlPrinter(true).output(testMethod));
            }
        });

        // only main should be there
        List<Sequence> sequences = parseIO(testMethods.get(0), lqlParseResult.getInterfaceSpecification());

        return sequences;
    }

    /**
     * Returns multiple sequences (each assert statement translates into sequence)
     *
     * @param md
     * @param interfaceSpecification
     * @return
     */
    private List<Sequence> parseIO(MethodDeclaration md, Interface interfaceSpecification) {
        List<Sequence> sequences = new ArrayList<>();
        String mut = interfaceSpecification.getMethods().get(0).getName();

        List<AssertStmt> assertStmts = md.findAll(AssertStmt.class);

        int t = 0;
        for(AssertStmt assertStmt : assertStmts) {
            Sequence sequence = new Sequence();
            sequence.setId("test" + t++);
            sequences.add(sequence);

            Expression check = assertStmt.getCheck();
            if(check.isEnclosedExpr()) {
                check = check.asEnclosedExpr().getInner();
            }

            LOG.debug("Check is '{}'", check.getClass());

            // BinaryExpr ==
            if(check.getClass().equals(BinaryExpr.class)) {
                LOG.debug("BinaryExpr (==)");

                // left is method call
                MethodCallExpr mce = ((BinaryExpr) check).getLeft().asMethodCallExpr();
                if(StringUtils.equalsIgnoreCase(mut, mce.getNameAsString())) {
                    // mut
                    LOG.debug("Found MUT '{}'", mce.getNameAsString());
                    Statement statement = mutToStatement(mce, interfaceSpecification);
                    sequence.addStatement(statement);

                    // right is oracle (expected output)
                    Expression right = ((BinaryExpr) check).getRight();
                    if(right.isEnclosedExpr()) {
                        right = right.asEnclosedExpr().getInner();
                    }

                    LOG.debug("Found right expression '{}'", right.getClass());

                    //
                    List<Value> expectedOutputs = parseParameters(Arrays.asList(right));
                    statement.setExpectedOutputs(expectedOutputs);
                }
            } else if(check.getClass().equals(MethodCallExpr.class)) {
                MethodCallExpr mce = check.asMethodCallExpr();
                LOG.debug("Checker method '{}'", mce.getNameAsString());

                if(StringUtils.equalsIgnoreCase("equals", mce.getNameAsString())) {
                    // expected output (equals method for instance)
                    LOG.debug("Found ORACLE '{}'", mce.getNameAsString());
                    // scope contains mut
                    MethodCallExpr mutMce = mce.getScope().get().asMethodCallExpr();
                    if(StringUtils.equalsIgnoreCase(mut, mutMce.getNameAsString())) {
                        // mut
                        LOG.debug("Found scoped MUT '{}'", mutMce.getNameAsString());
                        Statement statement = mutToStatement(mutMce, interfaceSpecification);
                        sequence.addStatement(statement);

                        // expected values from checker method
                        List<Value> expectedOutputs = parseParameters(mce.getArguments());
                        statement.setExpectedOutputs(expectedOutputs);
                    }
                } else {
                    throw new RuntimeException("uncovered checker method " + mce.getNameAsString());
                }
            } else {
                throw new RuntimeException("uncovered check " + check.getClass().getName());
            }
        }

        return sequences;
    }

    private Statement mutToStatement(MethodCallExpr mce, Interface interfaceSpecification) {
        List<Value> inputs = parseParameters(mce.getArguments());

        Statement statement = new Statement();
        String mut = interfaceSpecification.getMethods().get(0).getName();
        statement.setOperation(mut);

        statement.setInputs(inputs);

        return statement;
    }

    private List<Value> parseParameters(List<Expression> args) {
        List<Value> valueList = new ArrayList<>(args.size());
        for (Expression arg : args) {
            // for whatever reason, inputs are wrapper inside '(input)'
            if (arg.isEnclosedExpr()) {
                arg = arg.asEnclosedExpr().getInner();
            }

            makeFullyQualified(arg);

            LOG.debug("Parsed input '{}'", arg.toString());

            // make sure to fully qualify casts
            List<CastExpr> casts = arg.findAll(CastExpr.class);
            for(CastExpr castExpr : casts) {
                if(castExpr.getType().isClassOrInterfaceType()) {
                    ClassOrInterfaceType type = castExpr.getType().asClassOrInterfaceType();

                    ResolvedReferenceType resolved = type.resolve();

                    type.getName().setIdentifier(resolved.getQualifiedName());
                }
            }


//            if(StringUtils.containsIgnoreCase(arg.toString(), "Pair")) {
//                throw new RuntimeException("PAIR");
//            }

//            Value value = evalAndSerialize.evalAndSerializeToJson(
//                    arg.toString(),
//                    container);

            Value value = new Value();
            value.setValue(arg.toString());
            value.setCode(arg.toString());

            valueList.add(value);
        }

        return valueList;
    }

    private void makeFullyQualified(Expression arg) {
        LOG.debug("type '{}'", arg.getClass());

        arg.walk(ObjectCreationExpr.class, n -> {
            LOG.debug(n.toString());
            ObjectCreationExpr o = n.asObjectCreationExpr();
            ResolvedConstructorDeclaration rc = o.resolve();

            LOG.debug("Found ObjectCreationExpr '{}'", rc.getPackageName() + "." + rc.getName());

            o.getType().getName().setIdentifier(rc.getPackageName() + "." + rc.getName());

            JavaParser javaParser = new JavaParser();
            if(o.getType().getTypeArguments().isPresent()) {
                NodeList<Type> types = new NodeList<>();
                for(Type type : o.getType().getTypeArguments().get()) {
                    //type.
                    ResolvedType rp = type.resolve();
                    LOG.debug(rp.toString());

                    Type fqp = javaParser.parseClassOrInterfaceType(rp.describe()).getResult().get();

                    LOG.debug("resolved " + fqp);

                    types.add(fqp);
                }

                o.getType().setTypeArguments(types);
            }
        });

        arg.walk(MethodCallExpr.class, n -> {
            LOG.debug(n.toString());
            MethodCallExpr o = n.asMethodCallExpr();

            if(o.getScope().isPresent() && o.getScope().get().isNameExpr()) {
                NameExpr scope = o.getScope().get().asNameExpr();
                LOG.debug("found mn " + scope);

                ResolvedMethodDeclaration m = o.resolve();

                if(scope.getName().getIdentifier().equals(m.getClassName())) {
                    LOG.debug("Found MethodCallExpr '{}'", m.getPackageName() + "." + m.getClassName() + "#" + m.getName());
                    scope.getName().setIdentifier(m.getPackageName() + "." + m.getClassName());
                }
            }
        });
    }

    public List<Sheet> sequences2SheetsJSONL(List<Sequence> sequences, Specification specification, boolean initialize) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        List<Sheet> stimulusSheets = new LinkedList<>();

        for (Sequence sequence : sequences) {
            String signature = sequence.getId() + "()";

            List<Map<String, Object>> jsonRows = new ArrayList<>();

            if(initialize) {
                Map<String, Object> rowData = new LinkedHashMap<>();

                // operation
                rowData.put(SheetResolver.toColumnLabel(1) + SheetResolver.toRowLabel(0), "create");
                // service
                rowData.put(SheetResolver.toColumnLabel(2) + SheetResolver.toRowLabel(0), specification.getInterfaceSpecification().getName());
//                for(int p = 0; p < row.getInputs().size(); p++) {
//                    Value pValue = row.getInputs().get(p);
//                    // FIXME as code
//                    String value = objectMapper.writeValueAsString(pValue.getValue());
//                    rowData.put(toColumnLabel(p + 2) + toRowLabel(r), value);
//                }

                jsonRows.add(rowData);
            }

            List<Statement> rows = sequence.getStatements();

            for(int r = 0; r < rows.size(); r++) {
                int currentRow = initialize ? r + 1 : r;

                Map<String, Object> rowData = new LinkedHashMap<>();

                Statement row = rows.get(r);

                // output
                rowData.put(SheetResolver.toColumnLabel(0) + SheetResolver.toRowLabel(r), row.getExpectedOutputs().get(0).getValue());
                // operation
                rowData.put(SheetResolver.toColumnLabel(1) + SheetResolver.toRowLabel(currentRow), row.getOperation());
                // service
                rowData.put(SheetResolver.toColumnLabel(2) + SheetResolver.toRowLabel(currentRow), "A1");
                for(int p = 0; p < row.getInputs().size(); p++) {
                    Value pValue = row.getInputs().get(p);
                    rowData.put(SheetResolver.toColumnLabel(p + 3) + SheetResolver.toRowLabel(currentRow), pValue.getValue());
                }

                jsonRows.add(rowData);
            }

            StringBuilder jsonl = new StringBuilder();
            for (Map<String, Object> row : jsonRows) {
                Map<String, Map<String, Object>> m = new LinkedHashMap<>();
                m.put("cells", row);
                String json = objectMapper.writeValueAsString(m);

                jsonl.append(json);
                jsonl.append("\n");
            }

            String body = jsonl.toString();

            Sheet stimulusSheet = new Sheet(signature, body, specification.getInterfaceSpecification().getLqlQuery());
            stimulusSheets.add(stimulusSheet);
        }

        return stimulusSheets;
    }
}
