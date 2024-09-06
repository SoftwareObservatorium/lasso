package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.CodeInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.InstanceInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.MethodInvocation;

import java.util.List;

/**
 * Represents an invocation of an operation (e.g., method etc.).
 *
 * @author Marcus Kessel
 */
public abstract class Invocation {

    private final int index;

    private Class targetClass;
    private Parameter target;

    private List<Parameter> parameters;

    private Parameter expectedOutput;

    public Invocation(int index) {
        this.index = index;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "Invocation{" +
                "index=" + index +
                ", targetClass=" + targetClass +
                ", target=" + target +
                ", parameters=" + parameters +
                ", expectedOutput=" + expectedOutput +
                '}';
    }

    public Parameter getTarget() {
        return target;
    }

    public void setTarget(Parameter target) {
        this.target = target;
    }

    public int getIndex() {
        return index;
    }

    public abstract void execute(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation);

    public abstract String toCode();

    public Parameter getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(Parameter expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public boolean isCodeInvocation() {
        return getClass() == CodeInvocation.class;
    }

    public boolean isInstanceInvocation() {
        return getClass() == InstanceInvocation.class;
    }

    public boolean isMethodInvocation() {
        return getClass() == MethodInvocation.class;
    }
}
