package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.InvocationInterceptor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Invoke;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

        List<Object> inputValues;
        List<Object> rawInputValues = executedInvocation.getInputs().stream().map(i -> i.getValue()).toList();
        // varargs parameter: single Object[].class
        if(method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object[].class // varargs criteria
                && !rawInputValues.isEmpty() && rawInputValues.get(0) != null && !rawInputValues.get(0).getClass().isArray()) { // non-empty, non-null, non-array
            LOG.debug("Potential VARARGS for running '{}' vs number of inputs '{}' ", method, rawInputValues.size());

            // convert to Object
            inputValues = new ArrayList<>(1);
            inputValues.add(rawInputValues.toArray());
        } else {
            inputValues = rawInputValues;
        }

        try {
            if(!method.isAccessible()) {
                method.setAccessible(true);
            }

            Runner runner = new Runner();
            Invoke invoke;
            if(isStatic()) {
                invoke = () -> method.invoke(null, inputValues.toArray());
            } else {
                // if CUT contains only static methods, no instance is available, so we need to create a proxy for each CUT call on a static method
                Obj target = null;
                try {
                    target = executedInvocation.resolveTargetInstance();
                } catch (Throwable e) {
                }

                Object instance;
                // now detect if static call
                if(target == null) {
                    LOG.debug("STATIC CUT CALL");

                    // CHECK FI CUT
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(method.getDeclaringClass());
                    enhancer.setCallback(new InvocationInterceptor(executedInvocations, adaptedImplementation, executedInvocations.getInvocations().getInterfaceSpecification()));
                    enhancer.setClassLoader(method.getDeclaringClass().getClassLoader());

                    instance = enhancer.create();
                } else {
                    instance = target.getValue();
                }

                // invoke
                invoke = () -> method.invoke(instance, inputValues.toArray());
            }

            ExecutionResult result = runner.run(invoke);

            if(result.getExceptionThrown() != null) {
                executedInvocation.setOutput(Obj.fromException(ExceptionUtils.getRootCause(result.getExceptionThrown()), executedInvocation.getInvocation().getIndex()));
            } else {
                executedInvocation.setOutput(Obj.fromValue(result.getValue(), executedInvocation.getInvocation().getIndex()));
            }

            executedInvocation.setExecutionTime(result.getDurationNanos());

            LOG.debug("method call '{}'", executedInvocation.getOutput().getValue());
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
            // FIXME accessbility issues
            Throwable throwable = e.getCause();
            executedInvocation.setOutput(Obj.fromException(throwable, executedInvocation.getInvocation().getIndex()));
        } catch (InvocationTargetException e) {
            //e.printStackTrace();
            // FIXME this one is of interest (underlying exception from adaptee)
            Throwable throwable = e.getCause();
            executedInvocation.setOutput(Obj.fromException(throwable, executedInvocation.getInvocation().getIndex()));
        } catch (Throwable e) {
            //e.printStackTrace();
            // FIXME any other ..
            Throwable throwable = e.getCause();
            executedInvocation.setOutput(Obj.fromException(throwable, executedInvocation.getInvocation().getIndex()));
        }
    }

    @Override
    public String toCode() {
        // FIXME
        return getMethod().toGenericString();
    }
}
