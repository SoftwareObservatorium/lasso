package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.EvalException;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.CodeInvocation;

/**
 * A sheet invocation
 *
 * @author Marcus Kessel
 */
public class TestInvocation {

    // name of the sheet
    private String name;
    // code expression
    private final String invocationExpression;

    public TestInvocation(String name, String invocationExpression) {
        this.name = name;
        this.invocationExpression = invocationExpression;
    }

    /**
     * Assumes a list of comma, separated objects and returns an Object array.
     *
     * @param eval
     * @return
     * @throws EvalException
     */
    public Object[] resolveInputParameters(Eval eval) throws EvalException {
        // dirty workaround to create arrays in an ad hoc manner with Bsh
        Object[] obj = (Object[]) CodeInvocation.evalCode(eval, "new Object[]{"+invocationExpression+"}");

        return obj;
    }

    public String getInvocationExpression() {
        return invocationExpression;
    }

    public String getName() {
        return name;
    }
}
