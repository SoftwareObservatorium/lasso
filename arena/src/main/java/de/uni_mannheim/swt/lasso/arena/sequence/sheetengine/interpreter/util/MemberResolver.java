package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.runner.permutator.TypeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Marcus Kessel
 */
public abstract class MemberResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MemberResolver.class);

    private Class targetClass;
    private String methodName;
    private Class[] argumentClasses;

    private Method method;
    private Constructor constructor;

    public MemberResolver(Class targetClass, String methodName, Class[] argumentClasses) {
        LOG.debug("resolve {},{},{}", targetClass, methodName, argumentClasses);

        this.targetClass = targetClass;
        this.methodName = methodName;
        this.argumentClasses = ArrayUtils.isEmpty(argumentClasses) ? new Class[0] : argumentClasses;
    }

    public MemberResolver(Class targetClass, Class[] argumentClasses) {
        this(targetClass, null, argumentClasses);
    }

    private boolean isMoreSpecific(Class[] args1, Class[] args2) {
        for (int j = 0; j < args1.length; j++) {
            if (args2[j].isAssignableFrom(args1[j])) {
                continue;
            }

            return false;
        }
        return true;
    }

    public final Method resolveMethod() throws NoSuchMethodException {
        if (method != null) {
            return method;
        }

        if (targetClass.isArray()) {
            if (methodName.equals("get") && argumentClasses.length == 1) {
                // FIXME array
                method = Array.class.getDeclaredMethod("get", int.class);
                return method;
            }

            if (methodName.equals("set") && argumentClasses.length == 2) {
                // FIXME array
                method = Array.class.getDeclaredMethod("set", int.class, Object.class);
                return method;
            }

            throw new NoSuchMethodException("No matching method for statement");
        }

        List<Method> methods = getMethods(targetClass);
        LOG.debug("methods {}", methods.size());

        for (int i = 0; i < methods.size(); i++) {
            if (!methods.get(i).getName().equals(methodName)) {
                continue;
            }

            Class parameterTypes[] = methods.get(i).getParameterTypes();
            if (parameterTypes.length != argumentClasses.length) {
                continue;
            }

            if (!TypeUtils.isAssignable(argumentClasses, parameterTypes)) {
                continue;
            }

            if (method == null) {
                method = methods.get(i);
                continue;
            }

            if (isMoreSpecific(parameterTypes, method.getParameterTypes())) {
                method = methods.get(i);
            }
        }
        if (method == null) {
            throw new NoSuchMethodException("No matching method");
        }

        return method;
    }

    public final Constructor resolveConstructor() throws NoSuchMethodException {
        if (constructor != null) {
            return constructor;
        }

        List<Constructor> constructors = getConstructors(targetClass);

        LOG.debug("constructors {}", constructors.size());

        for (int i = 0; i < constructors.size(); i++) {
            Class parameterTypes[] = constructors.get(i).getParameterTypes();

            if (parameterTypes.length != argumentClasses.length) {
                continue;
            }

            // Check if constructor matches
            if (!TypeUtils.isAssignable(argumentClasses, parameterTypes)) {
                continue;
            }

            if (constructor == null) {
                constructor = constructors.get(i);
                continue;
            }

            if (isMoreSpecific(parameterTypes, constructor.getParameterTypes())) {
                constructor = constructors.get(i);
            }
        }
        if (constructor == null) {
            throw new NoSuchMethodException("No matching constructor for method");
        }

        return constructor;
    }

    protected abstract List<Method> getMethods(Class<?> targetClass);

    protected abstract List<Constructor> getConstructors(Class<?> targetClass);
}

