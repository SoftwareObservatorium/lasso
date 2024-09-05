package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Output;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Invoke;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  Create proxies based on CGLIB (supports proxying classes!).
 *
 * @author Marcus Kessel
 */
public class InvocationInterceptor implements MethodInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(InvocationInterceptor.class);

    private final ExecutedInvocations executedInvocations;
    private final AdaptedImplementation adaptedImplementation;
    private final InterfaceSpecification interfaceSpecification;

    private Object adapteeInstance;

    public InvocationInterceptor(ExecutedInvocations executedInvocations, AdaptedImplementation adaptedImplementation, InterfaceSpecification interfaceSpecification) {
        this.executedInvocations = executedInvocations;
        this.adaptedImplementation = adaptedImplementation;
        this.interfaceSpecification = interfaceSpecification;
    }

    /**
     * Returns proxy instance
     *
     * @param executedInvocation
     * @param constructor
     * @param inputs
     * @return
     */
    public Object create(ExecutedInvocation executedInvocation, Constructor constructor, Object[] inputs) {
        LOG.debug("Creating proxy for constructor '{}'", constructor);

        // create proxy instance
        Object proxyInstance = createProxy(constructor.getDeclaringClass(), constructor.getParameterTypes(), inputs);

        // create adaptee instance
        ExecutionResult result = createAdapteeInstance(executedInvocations, executedInvocation, constructor, inputs);

        // set adapter instance
        adapteeInstance = result.getValue();

        // set proxy instance as output
        executedInvocation.setOutput(Output.fromValue(proxyInstance));

        return proxyInstance;
    }

    @Override
    public Object intercept(Object proxyInstance, Method method, Object[] inputs, MethodProxy methodProxy) throws Throwable {
        LOG.debug("Intercepting method '{}'", method);

        MethodSignature methodSig = executedInvocations.getInvocations().resolve(method);

        if(methodSig == null) {
            // redirect calls such as .hashCode etc.
            return methodProxy.invokeSuper(proxyInstance, inputs);
        }

        ExecutedInvocation executedInvocation = executedInvocations.getLastExecutedInvocation();

        // FIXME dangerous cast
        PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
        AdaptedMethod adaptedMethod = pImpl.resolveAdaptedMethod(
                executedInvocations.getInvocations().getInterfaceSpecifications().get(method.getDeclaringClass().getCanonicalName()),
                methodSig);

        // FIXME inputs change proxy to real object
        if(ArrayUtils.isNotEmpty(inputs)) {
            for(Object obj : inputs) {
                if(obj == proxyInstance) {
                    // modify
                }
            }
        }

        // FIXME adaptation logic
        try {
            Method adMethod = adaptedMethod.getMethod();

            LOG.debug("Calling '{}'", adMethod);

            if(!adMethod.isAccessible()) {
                adMethod.setAccessible(true);
            }
            Runner runner = new Runner();
            Invoke invoke;
            if(adaptedMethod.isStatic()) {
                invoke = () -> adMethod.invoke(null, inputs);
            } else {
                invoke = () -> adMethod.invoke(adapteeInstance, inputs);
            }

            ExecutionResult result = runner.run(invoke);
            executedInvocation.setOutput(Output.fromValue(result.getValue()));
            executedInvocation.setExecutionTime(result.getDurationNanos());

            LOG.debug("cut method call '{}'", executedInvocation.getOutput().getValue());

            return executedInvocation.getOutput().getValue();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private Object createProxy(Class clazz, Class[] argumentTypes, Object[] arguments) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(this);

        if(ArrayUtils.isNotEmpty(argumentTypes)) {
            return enhancer.create(argumentTypes, arguments);
        } else {
            return enhancer.create();
        }
    }

    private ExecutionResult createAdapteeInstance(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation, Constructor constructor, Object[] inputs) {
        // FIXME adapt delegate
        MethodSignature constructorSig = executedInvocations.getInvocations().resolve(constructor);
        // FIXME dangerous cast
        PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
        AdaptedInitializer adaptedInitializer = pImpl.resolveAdaptedInitializer(
                executedInvocations.getInvocations().getInterfaceSpecifications().get(constructor.getDeclaringClass().getCanonicalName()),
                constructorSig);

        try {
            //
            Constructor adaptedConstructor = adaptedInitializer.getAsConstructor();
            if(!adaptedConstructor.isAccessible()) {
                adaptedConstructor.setAccessible(true);
            }

            Runner runner = new Runner();
            ExecutionResult result = runner.run(() -> adaptedConstructor.newInstance(inputs));
            executedInvocation.setOutput(Output.fromValue(result.getValue()));
            executedInvocation.setExecutionTime(result.getDurationNanos());

            LOG.debug("cut constructor '{}'", executedInvocation.getOutput().getValue());

            return result;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
