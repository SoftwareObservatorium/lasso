package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.op.ExecutedOperation;

import java.util.LinkedList;
import java.util.List;

/**
 * actuation sequence sheets
 * provide full information about the stimuli and responses. Actuation sheets therefore augment the invocation details in
 * a stimulus sheets with response records
 *
 * @author Marcus Kessel
 */
public class ActuationSheet {

    private final StimulusSheet stimulusSheet;
    private final AdaptedImplementation implementation;

    private List<ExecutedOperation> executedOperations = new LinkedList<>();

    public ActuationSheet(StimulusSheet stimulusSheet, AdaptedImplementation implementation) {
        this.stimulusSheet = stimulusSheet;
        this.implementation = implementation;
    }

    public StimulusSheet getStimulusSheet() {
        return stimulusSheet;
    }

    public AdaptedImplementation getImplementation() {
        return implementation;
    }

    public List<ExecutedOperation> getExecutedOperations() {
        return executedOperations;
    }

    public void setExecutedOperations(List<ExecutedOperation> executedOperations) {
        this.executedOperations = executedOperations;
    }
}
