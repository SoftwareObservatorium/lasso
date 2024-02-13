/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import org.apache.commons.collections4.CollectionUtils;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Matches factory methods: Can be type itself: Singleton or any other known type creating an instance of the required type.
 *
 * @author Marcus Kessel
 */
public class FactoryMethodStrategy implements ProducerStrategy {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(FactoryMethodStrategy.class);

    private String[] packageNames;

    public FactoryMethodStrategy(String[] packageNames) {
        this.packageNames = packageNames;
    }

    // TODO support builder pattern

    // FIXME paramTypes and positions
    @Override
    public List<Candidate> match(Class<?> t, Class<?>[] paramTypes) throws Throwable {
        List<Method> matches = scan(t, paramTypes /*FIXME support paramTypes*/);

        if(CollectionUtils.isEmpty(matches)) {
            return null;
        }

        List<Candidate> candidates = new ArrayList<>(matches.size());
        for(Method method : matches) {
            // register as member
            // TODO positions
            Candidate candidate = new Candidate(method, new int[0]);
            candidate.setProducerStrategy(this);

            candidates.add(candidate);
        }

        return candidates;
    }

    @Override
    public Object createInstance(Candidate candidate, Object[] inputs) throws Throwable {
        if(candidate.getMethod() instanceof Method) {
            try {
                Method method = (Method) candidate.getMethod();
                // make accessible
                method.setAccessible(true);

                Object instance;
                if(Modifier.isStatic(method.getModifiers())) {
                    // static field has no instance
                    instance = null;
                } else {
                    // try to create new instance
                    instance = candidate.getMethod().getDeclaringClass().newInstance();
                }

                // static field has no instance
                Object value = method.invoke(instance, inputs);

                return value;
            } catch (Throwable e) {
                // signal error
                return null;
            }
        } else {
            throw new IllegalArgumentException("Candidate member must be Method");
        }
    }

    private List<Method> scan(Class<?> cut, Class<?>[] paramTypes) {
        List<Method> methods = new LinkedList<>();
        for(Method method : cut.getDeclaredMethods()) {
            // require static for now
            // FIXME visibility and check input parameter types
            if(Modifier.isStatic(method.getModifiers()) && method.getReturnType().equals(cut)) {
                // no params
                if(method.getParameterCount() == 0) {
                    methods.add(method);
                }
            }
        }

        return methods;
    }

//    private List<Method> scan(Class<?> t, Class<?>[] paramTypes) {
//        final int paramSize;
//        if(paramTypes != null) {
//            paramSize = paramTypes.length;
//        } else {
//            paramSize = 0;
//        }
//
//        String[] packageNames = ProducerUtils.getPackageNames(this.packageNames, t);
//
//        try (ScanResult scanResult =
//                     new ClassGraph()
//                             .enableMethodInfo()
//                             .whitelistPackages(packageNames)
//                             .scan()) {
//            //
//            //System.out.println(scanResult.toJSON(2));
//
//            List<Method> matches = new LinkedList<>();
//
//            ClassInfoList classes = scanResult.getAllClasses();
//            for(ClassInfo clazz : classes) {
//                // only methods having same param size
//                MethodInfoList methods = clazz.getMethodInfo()
//                        .filter(m -> !Modifier.isAbstract(m.getModifiers()) && !m.isBridge() && !m.isNative() && !m.isConstructor() && (m.getParameterInfo().length == paramSize));
//                for(MethodInfo method : methods) {
//                    //
//                    if(StringUtils.equals(t.getName(), method.getTypeDescriptor().getResultType().toString())) {
//                        try {
//                            Class<?> ownerClass = clazz.loadClass();
//                            Method methodMember = ownerClass.getDeclaredMethod(method.getName() /*FIXME parameter types*/);
//                            matches.add(methodMember);
//                        } catch (Throwable e) {
//                            //
//                        }
//                    }
//                }
//            }
//
//            return matches;
//        } catch(Throwable e) {
//
//        }
//        return null;
//    }
}
