package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Utilities to resolve compatible {@link java.lang.reflect.Member}s including {@link Method}s and {@link Constructor}s.
 *
 * @author Marcus Kessel
 */
public class MemberResolutionUtils {

    /**
     * Method resolution based on type relaxation (e.g., String->Object) .. is assignable etc.
     *
     * @param clazz
     * @param methodName
     * @param args
     * @return
     * @throws NoSuchMethodException
     */
    public static Method resolveMethod(Class<?> clazz, String methodName, Class<?>[] args) throws NoSuchMethodException {
        MemberResolver st = new MemberResolver(clazz, methodName, args);

        return st.resolveMethod();
    }

    /**
     * Constructor resolution based on type relaxation (e.g., String->Object) .. is assignable etc.
     *
     * @param clazz
     * @param args
     * @return
     * @throws NoSuchMethodException
     */
    public static Constructor resolveConstructor(Class<?> clazz, Class<?>[] args) throws NoSuchMethodException {
        // "new" seems like an ugly hack
        MemberResolver st = new MemberResolver(clazz, args);

        return st.resolveConstructor();
    }
}
