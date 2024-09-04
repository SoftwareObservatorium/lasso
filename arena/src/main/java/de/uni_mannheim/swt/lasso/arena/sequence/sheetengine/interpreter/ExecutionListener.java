package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcus Kessel
 */
public class ExecutionListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(ExecutionListener.class);

    void visitBeforeStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
        Invocation invocation = executedInvocations.getSequence().get(index);
        ExecutedInvocation executedInvocation = executedInvocations.getExecutedInvocation(index);

        LOG.debug("Statement '{}' -> {}", index, invocation.toCode());
    }

    void visitAfterStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
        Invocation invocation = executedInvocations.getSequence().get(index);
        ExecutedInvocation executedInvocation = executedInvocations.getExecutedInvocation(index);

        LOG.debug("Statement '{}' -> {}", index, executedInvocation.toCode());
    }

    void visitBeforeSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
    }

    void visitAfterSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
    }
}
