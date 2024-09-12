package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.runner.permutator.TypeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author Marcus Kessel
 */
public class MemberResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MemberResolver.class);

    private Class targetClass;
    private String methodName;
    private Class[] argumentClasses;

    private Method method;
    private Constructor constructor;

    public MemberResolver(Class targetClass, String methodName, Class[] argumentClasses) {
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

        Method methods[] = targetClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().equals(methodName)) {
                continue;
            }

            Class ptypes[] = methods[i].getParameterTypes();
            if (ptypes.length != argumentClasses.length) {
                continue;
            }

            if (!TypeUtils.isAssignable(argumentClasses, ptypes)) {
                continue;
            }

            if (method == null) {
                method = methods[i];
                continue;
            }
            Class mptypes[] = method.getParameterTypes();
            if (isMoreSpecific(ptypes, mptypes)) {
                method = methods[i];
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

        Constructor ctors[] = targetClass.getDeclaredConstructors();

        for (int i = 0; i < ctors.length; i++) {
            Class ptypes[] = ctors[i].getParameterTypes();

            if (ptypes.length != argumentClasses.length) {
                continue;
            }

            // Check if constructor matches
            if (!TypeUtils.isAssignable(argumentClasses, ptypes)) {
                continue;
            }

            if (constructor == null) {
                constructor = ctors[i];
                continue;
            }
            Class mptypes[] = constructor.getParameterTypes();
            if (isMoreSpecific(ptypes, mptypes)) {
                constructor = ctors[i];
            }
        }
        if (constructor == null) {
            throw new NoSuchMethodException("No matching constructor for statement " + toString());
        }

        return constructor;
    }

}

