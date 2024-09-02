package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.op.InvocableOperation;

import java.util.LinkedList;
import java.util.List;

/**
 * The resolved {@link StimulusSheet} for an
 * {@link de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation} that can be executed.
 *
 * @author Marcus Kessel
 */
public class AdapterSheet {

    private final StimulusSheet stimulusSheet;
    private final AdaptedImplementation implementation;

    private List<InvocableOperation> invocableOperations = new LinkedList<>();

    public AdapterSheet(StimulusSheet stimulusSheet, AdaptedImplementation implementation) {
        this.stimulusSheet = stimulusSheet;
        this.implementation = implementation;
    }

    public StimulusSheet getStimulusSheet() {
        return stimulusSheet;
    }

    public AdaptedImplementation getImplementation() {
        return implementation;
    }

    public List<InvocableOperation> getInvocableOperations() {
        return invocableOperations;
    }

    public void setInvocableOperations(List<InvocableOperation> invocableOperations) {
        this.invocableOperations = invocableOperations;
    }
}
