package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
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
        LOG.info("visitBeforeExecution {}", adaptedImplementation.getAdapterId());
    }
    /**
     * Called AFTER the execution of a suite/set of sequences.
     *
     * @param adaptedImplementation
     */
    public void visitAfterExecution(AdaptedImplementation adaptedImplementation) {
    }

    public void visitBeforeStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
    }

    public void visitAfterStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
    }

    public void visitBeforeSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
    }

    public void visitAfterSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
    }
}
