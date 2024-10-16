package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of {@link ExecutedInvocation}s.
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
        ExecutedInvocation executedInvocation = new ExecutedInvocation(invocation, this);
        executedSequence.add(executedInvocation);

        return executedInvocation;
    }

    public ExecutedInvocation getExecutedInvocation(int index) {
        return executedSequence.get(index);
    }

    public ExecutedInvocation getLastExecutedInvocation() {
        return executedSequence.get(executedSequence.size() - 1);
    }

    /**
     * Total execution time in nanos.
     *
     * @return
     */
    public long getExecutionTime() {
        return executedSequence.stream().mapToLong(ExecutedInvocation::getExecutionTime).sum();
    }

    /**
     * Return code statement for this executed sequence of invocations
     *
     * @return
     */
    public String toCode() {
        // FIXME implement
        return "TODO";
    }

    public Invocations getInvocations() {
        return invocations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(ExecutedInvocation executedInvocation : getExecutedSequence()) {
            sb.append(executedInvocation.toString());
            sb.append("\n");
        }
        sb.append("Total execution time (nanos): ");
        sb.append(getExecutionTime());
        sb.append("\n");

        return sb.toString();
    }
}
