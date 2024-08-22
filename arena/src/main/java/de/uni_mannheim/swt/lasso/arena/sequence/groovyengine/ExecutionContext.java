package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.read.TestSpec;

/**
 *
 * @author Marcus Kessel
 */
public class ExecutionContext {

    private TestSpec testSpec;
    private AdaptedImplementation implementation;
    private ExecutedSequence executedSequence;

    public ExecutedSequence getExecutedSequence() {
        return executedSequence;
    }

    public void setExecutedSequence(ExecutedSequence executedSequence) {
        this.executedSequence = executedSequence;
    }

    public CallableSequence getCallableSequence() {
        return testSpec.getCallableSequence();
    }

    public InterfaceSpecification getInterfaceSpecification(String name) {
        return getTestSpec().getInterfaces().get(name);
    }

    public AdaptedImplementation getImplementation() {
        return implementation;
    }

    public void setImplementation(AdaptedImplementation implementation) {
        this.implementation = implementation;
    }

    public TestSpec getTestSpec() {
        return testSpec;
    }

    public void setTestSpec(TestSpec testSpec) {
        this.testSpec = testSpec;
    }
}
