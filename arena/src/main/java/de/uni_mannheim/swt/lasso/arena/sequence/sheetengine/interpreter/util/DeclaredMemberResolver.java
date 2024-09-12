package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class DeclaredMemberResolver extends MemberResolver {

    public DeclaredMemberResolver(Class targetClass, String methodName, Class[] argumentClasses) {
        super(targetClass, methodName, argumentClasses);
    }

    public DeclaredMemberResolver(Class targetClass, Class[] argumentClasses) {
        super(targetClass, argumentClasses);
    }

    protected List<Method> getMethods(Class<?> targetClass) {
        return Arrays.asList(targetClass.getDeclaredMethods());
    }

    protected List<Constructor> getConstructors(Class<?> targetClass) {
        return Arrays.asList(targetClass.getDeclaredConstructors());
    }
}
