package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Output;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.EvalException;

/**
 * A code expression that is "invoked" (i.e., evaluated).
 *
 * @author Marcus Kessel
 */
public class CodeInvocation extends Invocation {

    private String codeExpression;

    public CodeInvocation(int index) {
        super(index);
    }

    @Override
    public void execute(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation) {
        Object outVal = evalCode(executedInvocation, executedInvocations.getInvocations().getEval());
        executedInvocation.setOutput(Output.fromValue(outVal));
    }

    /**
     * Execute (evaluate) a code expression.
     *
     * @param executedInvocation
     * @param eval
     * @return
     */
    public static Object evalCode(ExecutedInvocation executedInvocation, Eval eval) {
        Invocation invocation = executedInvocation.getInvocation();
        if(!(invocation instanceof CodeInvocation)) {
            throw new IllegalArgumentException("not a code expression invocation");
        }

        String codeExpression = ((CodeInvocation) invocation).getCodeExpression();
        try {
            Object outVal = evalCode(eval, codeExpression);

            //LOG.debug("exp out '{}'", outVal);
            return outVal;
        } catch (EvalException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object evalCode(Eval eval, String codeExpression) throws EvalException {
        Object outVal = eval.eval(codeExpression);

        //LOG.debug("exp out '{}'", outVal);
        return outVal;
    }

    public String getCodeExpression() {
        return codeExpression;
    }

    public void setCodeExpression(String codeExpression) {
        this.codeExpression = codeExpression;
    }
}
