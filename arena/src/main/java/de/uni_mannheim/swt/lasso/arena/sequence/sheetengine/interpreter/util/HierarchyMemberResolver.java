package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class HierarchyMemberResolver extends DeclaredMemberResolver {

    public HierarchyMemberResolver(Class targetClass, String methodName, Class[] argumentClasses) {
        super(targetClass, methodName, argumentClasses);
    }

    public HierarchyMemberResolver(Class targetClass, Class[] argumentClasses) {
        super(targetClass, argumentClasses);
    }

    @Override
    protected List<Method> getMethods(Class<?> targetClass) {
        return getAllDeclaredMethods(targetClass);
    }

    @Override
    protected List<Constructor> getConstructors(Class<?> targetClass) {
        return Arrays.asList(targetClass.getDeclaredConstructors());
    }

    public static List<Method> getAllDeclaredMethods(Class<?> cutClass) {
        List<Method> methods = new ArrayList<>(
                Arrays.asList(cutClass.getDeclaredMethods()));
        // find all protected/public methods from super hierarchy NOT
        // overridden
        List<Method> superMethods = new LinkedList<>();

        // is cutClass included?
        Iterator<Class<?>> mIt = ClassUtils.hierarchy(cutClass).iterator();
        while (mIt.hasNext()) {
            Class<?> superClass = mIt.next();

//            if(superClass.equals(Object.class)) {
//                // skip this one
//                continue;
//            }

            // we are only interested in protected, public members
            for (Method method : superClass.getDeclaredMethods()) {
                // only add if NOT private, so we can actually access it in
                // subclass
                if (!Modifier.isPrivate(method.getModifiers())) {
                    // if(methods.contains(method))
                    superMethods.add(method);
                }
            }
        }

        if (!superMethods.isEmpty()) {
            // remove all methods overridden in cut class
            for (Method superMethod : superMethods) {
                List<Class<?>> superMethodParams = Arrays
                        .asList(superMethod.getParameterTypes());

                boolean overridden = false;
                // check if super methods are overridden in cut class
                for (Method method : methods) {
                    List<Class<?>> methodParams = Arrays
                            .asList(method.getParameterTypes());

                    // same signature?
                    if (method.getName().equals(superMethod.getName())
                            && CollectionUtils.isEqualCollection(
                            methodParams, superMethodParams)) {
                        //
                        overridden = true;

                        break;
                    }
                }

                if (!overridden) {
                    methods.add(superMethod);
                }
            }
        }

        return methods;
    }
}
