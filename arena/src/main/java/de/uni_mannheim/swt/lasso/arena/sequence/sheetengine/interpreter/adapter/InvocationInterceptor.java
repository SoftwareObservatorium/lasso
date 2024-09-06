package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Obj;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Parameter;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Invoke;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
        ExecutionResult result = createAdapteeInstance(executedInvocation, inputs);

        // set adapter instance
        adapteeInstance = result.getValue();

        // set proxy instance as output
        executedInvocation.setOutput(Obj.fromValue(proxyInstance, executedInvocation.getInvocation().getIndex()));

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

        AdaptedMethod adaptedMethod = executedInvocation.resolveAdaptedMethod(adaptedImplementation);

        // inputs change proxy to adaptee object
        Object[] cleanInputs;
        if(ArrayUtils.isNotEmpty(inputs)) {
            List<Object> mInputs = new ArrayList<>(inputs.length);
            int p = 0;
            for(Object obj : inputs) {
                LOG.debug("arg {}", obj);
                if(isProxy(obj)) {
                    // modify -- we have to get the correct adaptee instance!!
                    Parameter parameter = executedInvocation.getInvocation().getParameters().get(p);
                    ExecutedInvocation ref = executedInvocations.getExecutedSequence().get(parameter.getReference()[0]);
                    Object resolvedAdapteeInstance = ref.getInterceptor().getAdapteeInstance();

                    LOG.debug("changing input arg from '{}' to '{}'", obj, resolvedAdapteeInstance);

                    mInputs.add(resolvedAdapteeInstance);
                } else {
                    mInputs.add(obj);
                }

                p++;
            }

            cleanInputs = mInputs.toArray();
        } else {
            cleanInputs = inputs;
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
                invoke = () -> adMethod.invoke(null, cleanInputs);
            } else {
                invoke = () -> adMethod.invoke(adapteeInstance, cleanInputs);
            }

            ExecutionResult result = runner.run(invoke);

            // FIXME include or exclude adaptation logic in duration?
            //executedInvocation.setExecutionTime(result.getDurationNanos());

            LOG.debug("cut method call '{}', '{}'", result.getValue(), result.getValue().getClass());

            Object value = result.getValue();

            // FIXME inputs change adaptee instance back to proxy object
            if(value != null && CutUtils.isCut(adaptedMethod.getAdaptee(), value.getClass())) {
                LOG.debug("changing return arg from '{}' to '{}'", value, adapteeInstance);
                // modify
                value = proxyInstance;
            }

            return value;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isProxy(Object obj) {
        return ClassUtils.isCglibProxy(obj);
    }

    public static boolean isProxyClass(Class clazz) {
        return ClassUtils.isCglibProxyClass(clazz);
    }

    public static String getCutClassNameFromProxy(Object obj) {
        return ClassUtils.getUserClass(obj).getCanonicalName();
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

    private ExecutionResult createAdapteeInstance(ExecutedInvocation executedInvocation, Object[] inputs) {
        AdaptedInitializer adaptedInitializer = executedInvocation.resolveAdaptedInitializer(adaptedImplementation);

        try {
            //
            Constructor adaptedConstructor = adaptedInitializer.getAsConstructor();
            if(!adaptedConstructor.isAccessible()) {
                adaptedConstructor.setAccessible(true);
            }

            Runner runner = new Runner();
            ExecutionResult result = runner.run(() -> adaptedConstructor.newInstance(inputs));
            executedInvocation.setOutput(Obj.fromValue(result.getValue(), executedInvocation.getInvocation().getIndex()));
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

    public Object getAdapteeInstance() {
        return adapteeInstance;
    }
}
