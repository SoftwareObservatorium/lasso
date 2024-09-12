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
     * @param hierarchy lookup class hierarchy for members
     * @return
     * @throws NoSuchMethodException
     */
    public static Method resolveDeclaredMethod(Class<?> clazz, String methodName, Class<?>[] args, boolean hierarchy) throws NoSuchMethodException {
        MemberResolver resolver;
        if(hierarchy) {
            resolver = new HierarchyMemberResolver(clazz, methodName, args);
        } else {
            resolver = new DeclaredMemberResolver(clazz, methodName, args);
        }

        return resolver.resolveMethod();
    }

    /**
     * Constructor resolution based on type relaxation (e.g., String->Object) .. is assignable etc.
     *
     * @param clazz
     * @param args
     * @param hierarchy lookup class hierarchy for members
     * @return
     * @throws NoSuchMethodException
     */
    public static Constructor resolveDeclaredConstructor(Class<?> clazz, Class<?>[] args, boolean hierarchy) throws NoSuchMethodException {
        MemberResolver resolver;
        if(hierarchy) {
            resolver = new HierarchyMemberResolver(clazz, args);
        } else {
            resolver = new DeclaredMemberResolver(clazz, args);
        }

        return resolver.resolveConstructor();
    }


}
