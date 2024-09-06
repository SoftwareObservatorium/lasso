package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Invoke;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
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
        Method method = getMethod();

        // either value (object) or reference
        List<Object> inputValues = executedInvocation.getInputs().stream().map(i -> i.getValue()).toList();

        Obj targetInstance = executedInvocation.resolveTargetInstance();

        try {
            if(!method.isAccessible()) {
                method.setAccessible(true);
            }

            Runner runner = new Runner();
            Invoke invoke;
            if(isStatic()) {
                invoke = () -> method.invoke(null, inputValues.toArray());
            } else {
                invoke = () -> method.invoke(targetInstance.getValue(), inputValues.toArray());
            }

            ExecutionResult result = runner.run(invoke);
            executedInvocation.setOutput(Obj.fromValue(result.getValue(), executedInvocation.getInvocation().getIndex()));
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
