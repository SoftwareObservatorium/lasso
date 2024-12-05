package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcus Kessel
 */
public class InvocationVisitor {

    private static final Logger LOG = LoggerFactory
            .getLogger(InvocationVisitor.class);

    /**
     * Called BEFORE the execution of a suite/set of sequences.
     *
     * @param adaptedImplementation
     */
    public void visitBeforeExecution(AdaptedImplementation adaptedImplementation) {

    }
    /**
     * Called AFTER the execution of a suite/set of sequences.
     *
     * @param adaptedImplementation
     */
    public void visitAfterExecution(AdaptedImplementation adaptedImplementation) {

    }

    public void visitBeforeStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
        Invocation invocation = executedInvocations.getSequence().get(index);
        ExecutedInvocation executedInvocation = executedInvocations.getExecutedInvocation(index);

        LOG.debug("Statement '{}' -> {}", index, invocation.toCode());
    }

    public void visitAfterStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
        Invocation invocation = executedInvocations.getSequence().get(index);
        ExecutedInvocation executedInvocation = executedInvocations.getExecutedInvocation(index);

        LOG.debug("Statement '{}' -> {}", index, executedInvocation.toCode());
    }

    public void visitBeforeSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
        LOG.debug("Sequence '{}'", executedInvocations.getInvocations());
    }

    public void visitAfterSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
    }
}
