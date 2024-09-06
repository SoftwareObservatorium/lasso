package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMember;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.InvocationInterceptor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.InstanceInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.MethodInvocation;

import java.util.List;

/**
 * An executed {@link Invocation} that records the output.
 *
 * @author Marcus Kessel
 */
public class ExecutedInvocation {

    private final Invocation invocation;
    private final ExecutedInvocations executedInvocations;
    private Obj output = null;

    private List<Obj> inputs;

    private InvocationInterceptor interceptor;

    private AdaptedMember adaptedMember;

    private long executionTime;

    public ExecutedInvocation(Invocation invocation, ExecutedInvocations executedInvocations) {
        this.invocation = invocation;
        this.executedInvocations = executedInvocations;
    }

    public Invocation getInvocation() {
        return invocation;
    }

    public Obj getOutput() {
        return output;
    }

    public void setOutput(Obj output) {
        this.output = output;
    }

    /**
     * Return code statement for this executed invocation
     *
     * @return
     */
    public String toCode() {
        // FIXME implement
        return "TODO";
    }

    @Override
    public String toString() {
        return "ExecutedInvocation{" +
                "invocation=" + invocation +
                ", output=" + output +
                ", executionTime=" + executionTime +
                '}';
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public InvocationInterceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(InvocationInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public ExecutedInvocations getExecutedInvocations() {
        return executedInvocations;
    }

    public List<Obj> getInputs() {
        return inputs;
    }

    public void setInputs(List<Obj> inputs) {
        this.inputs = inputs;
    }

    public Obj resolveTargetInstance() {
        // invoke method
        Parameter target = invocation.getTarget();

        ExecutedInvocation ref = executedInvocations.getExecutedInvocation(target.getReference()[0]);
        return ref.getOutput();
    }

    public AdaptedInitializer resolveAdaptedInitializer(AdaptedImplementation adaptedImplementation) {
        InstanceInvocation instanceInvocation = (InstanceInvocation) invocation;
        // FIXME adapt delegate
        MethodSignature constructorSig = executedInvocations.getInvocations().resolve(instanceInvocation.getAsConstructor());
        // FIXME dangerous cast
        PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
        AdaptedInitializer adaptedInitializer = pImpl.resolveAdaptedInitializer(
                executedInvocations.getInvocations().getInterfaceSpecifications().get(instanceInvocation.getAsConstructor().getDeclaringClass().getCanonicalName()),
                constructorSig);

        return adaptedInitializer;
    }

    public AdaptedMethod resolveAdaptedMethod(AdaptedImplementation adaptedImplementation) {
        MethodInvocation methodInvocation = (MethodInvocation) invocation;
        MethodSignature methodSig = executedInvocations.getInvocations().resolve(methodInvocation.getMethod());
        // FIXME dangerous cast
        PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
        AdaptedMethod adaptedMethod = pImpl.resolveAdaptedMethod(
                executedInvocations.getInvocations().getInterfaceSpecifications().get(methodInvocation.getMethod().getDeclaringClass().getCanonicalName()),
                methodSig);

        return adaptedMethod;
    }

    public AdaptedMember getAdaptedMember() {
        return adaptedMember;
    }

    public void setAdaptedMember(AdaptedMember adaptedMember) {
        this.adaptedMember = adaptedMember;
    }

    public boolean isAdapted() {
        return adaptedMember != null;
    }
}
