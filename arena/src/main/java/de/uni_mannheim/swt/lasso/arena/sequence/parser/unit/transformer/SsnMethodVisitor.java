package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.transformer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Visit methods of CU's to collect invocations for stimulus sheets.
 *
 * @author Marcus Kessel
 */
public class SsnMethodVisitor extends VoidVisitorAdapter<SsnState> {

    /**
     * get FQN if possible, otherwise fallback to string representation.
     *
     * @param expr
     * @return
     */
    private String getFqn(Expression expr) {
        try {
            return expr.calculateResolvedType().describe();
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
            // Fallback for literals or unresolved types
            return expr.toString();
        }
    }

    @Override
    public void visit(ExpressionStmt n, SsnState state) {
        // We let the more specific visitors (like for AssignmentExpr) handle this.
        // We still need to call super.visit to ensure children are visited.
        super.visit(n, state);

        // This now only handles method calls that are not part of an assignment,
        // e.g. `list.add("item");`
        if (n.getExpression() instanceof MethodCallExpr) {
            handleMethodCall((MethodCallExpr) n.getExpression(), state, Optional.empty());
        }
    }

    @Override
    public void visit(AssignExpr n, SsnState state) {
        // It's important to visit children first, though for this logic it's not strictly necessary.
        super.visit(n, state);

        String varName = n.getTarget().toString();
        Expression valueExpr = n.getValue();
        String newCell = null;

        // Process the RHS of the assignment to generate a row and get the output cell
        if (valueExpr instanceof MethodCallExpr) {
            newCell = handleMethodCall((MethodCallExpr) valueExpr, state, Optional.empty());
        } else if (valueExpr instanceof ObjectCreationExpr) {
            newCell = handleObjectCreation((ObjectCreationExpr) valueExpr, state, Optional.empty());
        } else {
            // Reassignment to a literal, e.g., x = 10;
            state.mapVariableToValue(varName, valueExpr.toString());
            // No new cell is created, so we return.
            return;
        }

        // CRUCIAL: Update the map to point the variable to the new cell.
        if (newCell != null) {
            state.mapVariableToCell(varName, newCell);
        }
    }

    @Override
    public void visit(VariableDeclarator n, SsnState state) {
        super.visit(n, state);
        Optional<Expression> initializer = n.getInitializer();
        if (initializer.isPresent()) {
            String varName = n.getNameAsString();
            Expression initExpr = initializer.get();
            // Pass the variable name so the initial mapping can be created.
            if (initExpr instanceof ObjectCreationExpr) {
                handleObjectCreation((ObjectCreationExpr) initExpr, state, Optional.of(varName));
            } else if (initExpr instanceof MethodCallExpr) {
                handleMethodCall((MethodCallExpr) initExpr, state, Optional.of(varName));
            } else {
                state.mapVariableToValue(varName, initExpr.toString());
            }
        }
    }

    // Unchanged from before
    private String handleMethodCall(MethodCallExpr mce, SsnState state, Optional<String> varToAssign) {
        if (isAssertionMethod(mce.getNameAsString())) {
            handleAssertion(mce, state);
            return null;
        }

        final String resolvedTarget = mce.getScope()
                .map(scope -> resolveTarget(scope, state))
                .orElse(null);

        final List<String> resolvedArgs = mce.getArguments().stream()
                .map(arg -> resolveArgument(arg, state))
                .collect(Collectors.toList());

        int rowNum = state.newRow();
        String outputCell = "A" + rowNum;

        state.addCell(rowNum, "B", mce.getNameAsString());

        if (resolvedTarget != null) {
            state.addCell(rowNum, "C", resolvedTarget);
        }

        char col = 'D';
        for (String argValue : resolvedArgs) {
            state.addCell(rowNum, String.valueOf(col++), argValue);
        }

        // This handles the initial assignment for VariableDeclarators
        varToAssign.ifPresent(v -> state.mapVariableToCell(v, outputCell));
        return outputCell;
    }

    private String resolveTarget(Expression scope, SsnState state) {
        // Priority 1: Handle chained calls recursively.
        if (scope instanceof MethodCallExpr) {
            return handleMethodCall((MethodCallExpr) scope, state, Optional.empty());
        }

        String scopeStr = scope.toString();

        // Priority 2: Check if the scope is ALREADY a variable pointing to a cell.
        String cell = state.getCellForVariable(scopeStr);
        if (cell != null) {
            return cell;
        }

        // Priority 3: Check if it's a variable pointing to a literal.
        String value = state.getValueForVariable(scopeStr);
        if (value != null) {
            // PROMOTION: This variable holds a literal. We must create a row for it.
            //System.out.println("Promoting variable '" + scopeStr + "' with value " + value + " to an object.");
            String typeFqn;
            try {
                typeFqn = scope.calculateResolvedType().describe();
            } catch (Exception e) {
                // Fallback if type resolution fails for some reason
                typeFqn = "java.lang.Object";
            }

            int rowNum = state.newRow();
            String outputCell = "A" + rowNum;
            state.addCell(rowNum, "B", "create");
            state.addCell(rowNum, "C", typeFqn);
            state.addCell(rowNum, "D", value);

            // CRUCIAL: Update the mapping to point to the new cell for future use.
            state.mapVariableToCell(scopeStr, outputCell);
            return outputCell;
        }

        // Priority 4: It's not a known variable, so assume it's a class name for a static call.
        try {
            return scope.calculateResolvedType().describe();
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
            return scopeStr; // Final fallback
        }
    }

    private String resolveArgument(Expression arg, SsnState state) {
        if (arg instanceof ObjectCreationExpr) {
            return handleObjectCreation((ObjectCreationExpr) arg, state, Optional.empty());
        }
        if (arg instanceof MethodCallExpr) {
            return handleMethodCall((MethodCallExpr) arg, state, Optional.empty());
        }
        return state.resolveExpression(arg);
    }

    /**
     * solver for Object Creation
     *
     * @param oce
     * @param state
     * @param varToAssign
     * @return
     */
    private String handleObjectCreation(ObjectCreationExpr oce, SsnState state, Optional<String> varToAssign) {
        final List<String> resolvedArgs = oce.getArguments().stream()
                .map(arg -> resolveArgument(arg, state))
                .collect(Collectors.toList());

        int rowNum = state.newRow();
        String outputCell = "A" + rowNum;

        state.addCell(rowNum, "B", "create");
        // USE SOLVER: Get the fully qualified name of the type.
        try {
            state.addCell(rowNum, "C", oce.calculateResolvedType().describe());
        } catch(UnsolvedSymbolException e) {
            state.addCell(rowNum, "C", oce.getTypeAsString()); // Fallback
        }

        char col = 'D';
        for (String argValue : resolvedArgs) {
            state.addCell(rowNum, String.valueOf(col++), argValue);
        }

        varToAssign.ifPresent(v -> state.mapVariableToCell(v, outputCell));
        return outputCell;
    }

    private boolean isAssertionMethod(String name) {
        return name.startsWith("assert") || name.equals("fail");
    }

    /**
     * find the single, meaningful method call inside a lambda.
     *
     * @param lambda
     * @return
     */
    private MethodCallExpr findInnermostMethodCall(LambdaExpr lambda) {
        Node body = lambda.getBody();
        if (body instanceof BlockStmt) {
            BlockStmt block = (BlockStmt) body;
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                if (stmt instanceof ExpressionStmt && ((ExpressionStmt) stmt).getExpression() instanceof MethodCallExpr) {
                    return (MethodCallExpr) ((ExpressionStmt) stmt).getExpression();
                }
            }
        } else if (body instanceof MethodCallExpr) {
            return (MethodCallExpr) body;
        }
        return null; // Return null if the lambda is too complex for this simple extraction.
    }

    private void handleAssertion(MethodCallExpr mce, SsnState state) {
        String methodName = mce.getNameAsString();
        List<Expression> args = mce.getArguments();
        if (args.isEmpty() /*&& !methodName.equals("fail")*/) return;

        if (methodName.equals("assertEquals") && args.size() == 2) {
            String expected = state.resolveExpression(args.get(0));
            Expression actualExpr = args.get(1);
            String targetCell = null;

            // Check if the 'actual' parameter is a method call itself.
            if (actualExpr instanceof MethodCallExpr) {
                // If so, process it to generate a row and get its output cell.
                targetCell = handleMethodCall((MethodCallExpr) actualExpr, state, Optional.empty());
            } else {
                // Otherwise, use the old logic for simple variables.
                targetCell = state.getCellForVariable(actualExpr.toString());
            }

            // Back-patch the cell with the expected value.
            if (targetCell != null) {
                state.updateCell(targetCell, expected);
            }
        } else if ((methodName.equals("assertTrue") || methodName.equals("assertFalse")) && args.size() == 1) {
            if (args.get(0) instanceof MethodCallExpr) {
                String cell = handleMethodCall((MethodCallExpr) args.get(0), state, Optional.empty());
                if (cell != null) {
                    // Determine expected value based on which assert was used.
                    Object expectedValue = methodName.equals("assertTrue") ? Boolean.TRUE : Boolean.FALSE;
                    state.updateCell(cell, expectedValue);
                }
            }
        } else if ((methodName.equals("assertNull") || methodName.equals("assertNotNull")) && args.size() == 1) {
            Expression arg = args.get(0);
            String targetCell = null;
            if (arg instanceof MethodCallExpr) {
                targetCell = handleMethodCall((MethodCallExpr) arg, state, Optional.empty());
            } else {
                targetCell = state.getCellForVariable(arg.toString());
            }

            // For assertNull, we back-patch. For assertNotNull, we just ensure the row exists.
            if (methodName.equals("assertNull") && targetCell != null) {
                state.updateCell(targetCell, null);
            }
        }
    }
}