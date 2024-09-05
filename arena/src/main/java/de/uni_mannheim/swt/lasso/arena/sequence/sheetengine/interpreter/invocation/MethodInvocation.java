package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Invoke;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * A method invocation.
 *
 * @author Marcus Kessel
 */
public class MethodInvocation extends MemberInvocation {

    private static final Logger LOG = LoggerFactory
            .getLogger(MethodInvocation.class);

    public MethodInvocation(int index) {
        super(index);
    }

    public Method getMethod() {
        return (Method) getMember();
    }

    public boolean isStatic() {
        return Modifier.isStatic(getMethod().getModifiers());
    }

    @Override
    public void execute(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation) {
        Invocations invocations = executedInvocations.getInvocations();
        // is CUT?
        Member member = getMember();
        Class targetClass = member.getDeclaringClass();

        LOG.debug("Found class {}", targetClass.getCanonicalName());

        Method method = getMethod();

        // invoke method
        Parameter target = getTarget();

        LOG.debug("Found target {}", target);

        // either value (object) or reference
        List<Object> inputs = resolveInputs(invocations, executedInvocations);

        ExecutedInvocation ref = executedInvocations.getExecutedInvocation(target.getReference()[0]);
        Object instance = ref.getOutput().getValue();

        try {
            if(!method.isAccessible()) {
                method.setAccessible(true);
            }

            Runner runner = new Runner();
            Invoke invoke;
            if(isStatic()) {
                invoke = () -> method.invoke(null, inputs.toArray());
            } else {
                invoke = () -> method.invoke(instance, inputs.toArray());
            }

            ExecutionResult result = runner.run(invoke);
            executedInvocation.setOutput(Output.fromValue(result.getValue()));
            executedInvocation.setExecutionTime(result.getDurationNanos());

            LOG.debug("method call '{}'", executedInvocation.getOutput().getValue());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toCode() {
        // FIXME
        return getMethod().toGenericString();
    }
}
