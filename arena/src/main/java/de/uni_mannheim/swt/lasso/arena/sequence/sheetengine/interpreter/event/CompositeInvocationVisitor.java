package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.InvocationVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A composite listener
 *
 * @author Marcus Kessel
 */
public class CompositeInvocationVisitor extends InvocationVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeInvocationVisitor.class);

    private List<InvocationVisitor> listenerList;

    public CompositeInvocationVisitor(List<InvocationVisitor> listenerList) {
        this.listenerList = listenerList;
    }


    @Override
    public void visitBeforeStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
        listenerList.forEach(listener -> {
            try {
                listener.visitBeforeStatement(executedInvocations, index, adaptedImplementation);
            } catch (Throwable e){
                LOG.warn("listener failed '{}'", listener.getClass());
                LOG.warn("listener failed", e);
            }
        });
    }

    @Override
    public void visitAfterStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
        listenerList.forEach(listener -> {
            try {
                listener.visitAfterStatement(executedInvocations, index, adaptedImplementation);
            } catch (Throwable e){
                LOG.warn("listener failed '{}'", listener.getClass());
                LOG.warn("listener failed", e);
            }
        });
    }

    @Override
    public void visitBeforeSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
        listenerList.forEach(listener -> {
            try {
                listener.visitBeforeSequence(executedInvocations, adaptedImplementation);
            } catch (Throwable e){
                LOG.warn("listener failed '{}'", listener.getClass());
                LOG.warn("listener failed", e);
            }
        });
    }

    @Override
    public void visitAfterSequence(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation) {
        listenerList.forEach(listener -> {
            try {
                listener.visitAfterSequence(executedInvocations, adaptedImplementation);
            } catch (Throwable e){
                LOG.warn("listener failed '{}'", listener.getClass());
                LOG.warn("listener failed", e);
            }
        });
    }
}
