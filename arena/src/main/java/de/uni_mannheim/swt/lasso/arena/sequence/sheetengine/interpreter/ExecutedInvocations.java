package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ExecutedInvocations {

    private final Invocations invocations;
    private final List<ExecutedInvocation> executedSequence;

    public ExecutedInvocations(Invocations invocations) {
        this.invocations = invocations;
        this.executedSequence = new ArrayList<>(invocations.getSequence().size());
    }

    public List<Invocation> getSequence() {
        return invocations.getSequence();
    }

    public List<ExecutedInvocation> getExecutedSequence() {
        return executedSequence;
    }

    public ExecutedInvocation create(Invocation invocation) {
        ExecutedInvocation executedInvocation = new ExecutedInvocation(invocation);
        executedSequence.add(executedInvocation);

        return executedInvocation;
    }

    public ExecutedInvocation getExecutedInvocation(int index) {
        return executedSequence.get(index);
    }
}
