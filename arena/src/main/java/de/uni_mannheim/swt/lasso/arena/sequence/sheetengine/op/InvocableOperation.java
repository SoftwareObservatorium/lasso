package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.op;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.StimulusSheet;

/**
 * An invocable operation as part of a {@link StimulusSheet}.
 *
 * @author Marcus Kessel
 */
public class InvocableOperation {

    public ExecutedOperation execute() {
        ExecutedOperation executedOperation = new ExecutedOperation();

        return executedOperation;
    }

    public String toCode() {
        return "FIXME";
    }
}
