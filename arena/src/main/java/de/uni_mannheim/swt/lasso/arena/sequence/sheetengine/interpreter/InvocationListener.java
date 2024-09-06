package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcus Kessel
 */
public class InvocationListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(InvocationListener.class);

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
