package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Obj;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.EvalException;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;

/**
 * A code expression that is "invoked" (i.e., evaluated).
 *
 * @author Marcus Kessel
 */
public class CodeInvocation extends Invocation {

    private String codeExpression;
    private String command;

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
            ExecutionResult result = runner.run(() -> evalCode(executedInvocations.getInvocations().getEval(), codeExpression));
            executedInvocation.setOutput(Obj.fromValue(result.getValue(), Obj.PRODUCER_INDEX_NONE));
            executedInvocation.setExecutionTime(result.getDurationNanos());
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
        Runner runner = new Runner();
        ExecutionResult result = null;
        try {
            result = runner.run(() -> eval.eval(codeExpression));
        } catch (Throwable e) {
            throw new EvalException("Evaluation run failed", e);
        }

        //LOG.debug("exp out '{}'", outVal);
        return result.getValue();
    }

    public String getCodeExpression() {
        return codeExpression;
    }

    public void setCodeExpression(String codeExpression) {
        this.codeExpression = codeExpression;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
