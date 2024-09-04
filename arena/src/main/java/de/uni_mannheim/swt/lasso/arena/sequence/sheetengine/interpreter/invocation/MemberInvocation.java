package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.EvalException;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CodeExpressionUtils;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public abstract class MemberInvocation extends Invocation {

    private Member member;

    public MemberInvocation(int index) {
        super(index);
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    /**
     * Resolve input values.
     *
     * @param invocations
     * @param executedInvocations
     * @return
     */
    List<Object> resolveInputs(Invocations invocations, ExecutedInvocations executedInvocations) {
        // either value (object) or reference
        List<Object> inputs = new ArrayList<>(getParameters().size());
        for(Parameter parameter : getParameters()) {

            if(parameter.isReference()) {
                // by row
                // get value from ExecutedInvocation
                ExecutedInvocation ref = executedInvocations.getExecutedInvocation(parameter.getReference()[0]);
                inputs.add(ref.getOutput().getValue());
            } else {
                // just interpret expression
                try {
                    Object value = invocations.getEval().eval(CodeExpressionUtils.cleanExpression(parameter.getExpression()));
                    inputs.add(value);
                } catch (EvalException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return inputs;
    }
}
