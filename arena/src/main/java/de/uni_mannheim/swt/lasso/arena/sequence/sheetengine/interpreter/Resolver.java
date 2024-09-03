package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolver for {@link Member}s and {@link MethodSignature}
 *
 * @author Marcus Kessel
 */
public class Resolver {

    private static final Logger LOG = LoggerFactory.getLogger(Resolver.class);

    /**
     * Resolve all bindings between an {@link InterfaceSpecification} and {@link Class} {@link Member}s.
     *
     * @param clazz
     * @param interfaceSpecification
     * @return
     * @throws NoSuchMethodException
     */
    public static Map<Member, MethodSignature> resolve(Class clazz, InterfaceSpecification interfaceSpecification) throws NoSuchMethodException {
        Map<Member, MethodSignature> resolved = new LinkedHashMap<>();
        for(MethodSignature c : interfaceSpecification.getConstructors()) {
            Constructor constructor = resolveConstructor(clazz, c);
            resolved.put(constructor, c);
        }

        for(MethodSignature m : interfaceSpecification.getMethods()) {
            Member method = resolveMethod(clazz, m);
            resolved.put(method, m);
        }

        return resolved;
    }

    /**
     * Resolve {@link Method} for {@link MethodSignature}.
     *
     * @param clazz
     * @param methodSignature
     * @return
     * @throws NoSuchMethodException
     */
    public static Method resolveMethod(Class clazz, MethodSignature methodSignature) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodSignature.getName(), methodSignature.getParameterTypes(clazz));
    }

    /**
     * Resolve {@link Constructor} for {@link MethodSignature}.
     *
     * @param clazz
     * @param methodSignature
     * @return
     * @throws NoSuchMethodException
     */
    public static Constructor resolveConstructor(Class clazz, MethodSignature methodSignature) throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(methodSignature.getParameterTypes(clazz));
    }
}
