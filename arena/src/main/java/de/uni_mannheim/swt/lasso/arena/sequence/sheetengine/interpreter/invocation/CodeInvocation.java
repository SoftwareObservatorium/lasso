package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Output;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.EvalException;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf.Runner;

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
        Invocation invocation = executedInvocation.getInvocation();
        if(!(invocation instanceof CodeInvocation)) {
            throw new IllegalArgumentException("not a code expression invocation");
        }

        String codeExpression = ((CodeInvocation) invocation).getCodeExpression();
        try {
            Runner runner = new Runner();
            Object outVal = runner.run(() -> evalCode(executedInvocations.getInvocations().getEval(), codeExpression));
            executedInvocation.setOutput(Output.fromValue(outVal));
            executedInvocation.setExecutionTime(runner.getStopWatch().getExecutionNanoTime());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toCode() {
        return codeExpression;
    }

    /**
     * Evaluate code expression.
     *
     * @param eval
     * @param codeExpression
     * @return
     * @throws EvalException
     */
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
