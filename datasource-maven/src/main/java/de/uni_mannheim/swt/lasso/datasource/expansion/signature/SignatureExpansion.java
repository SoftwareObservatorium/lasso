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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple query expansion strategy based on type casting and compatibility.
 *
 * Resembles strategies from Permutator (cf. testrunner).
 *
 * @author Marcus Kessel
 */
public class SignatureExpansion {

    private static List<List<Class>> EQUAL = new LinkedList<>();

    static {
        // String like
        EQUAL.add(Arrays.asList(byte[].class, Byte[].class, String.class, CharSequence.class, char[].class, Character[].class));
        // primitives + wrappers
        EQUAL.add(Arrays.asList(boolean.class, Boolean.class));
        EQUAL.add(Arrays.asList(byte.class, Byte.class));
        EQUAL.add(Arrays.asList(short.class, Short.class));
        EQUAL.add(Arrays.asList(char.class, Character.class));
        EQUAL.add(Arrays.asList(int.class, Integer.class));
        EQUAL.add(Arrays.asList(long.class, Long.class));
        EQUAL.add(Arrays.asList(float.class, Float.class));
        EQUAL.add(Arrays.asList(double.class, Double.class));

        // primitives + wrapper arrays
        EQUAL.add(Arrays.asList(boolean[].class, Boolean[].class));
        //EQUAL.add(Arrays.asList(byte[].class, byte[].class));
        EQUAL.add(Arrays.asList(short[].class, Short[].class));
        //EQUAL.add(Arrays.asList(char[].class, Character[].class));
        EQUAL.add(Arrays.asList(int[].class, Integer[].class));
        EQUAL.add(Arrays.asList(long[].class, Long[].class));
        EQUAL.add(Arrays.asList(float[].class, Float[].class));
        EQUAL.add(Arrays.asList(Double[].class, Double[].class));
    }

    private static List<List<Class>> ONEWAY = new LinkedList<>();

    static {
        ONEWAY.add(Arrays.asList(byte.class, short.class, char.class, int.class, long.class, float.class, double.class));
    }

    private static MultiValuedMap<Class, Class> MAPPINGS = new ArrayListValuedHashMap<>();
    static {
        computeMappings();
    }

    private static void computeMappings() {
        // first pass: one-way
        for(List<Class> list : EQUAL) {
            for(Class type : list) {
                // one way
                for(List<Class> ow : ONEWAY) {
                    if(ow.contains(type)) {
                        int index = ow.indexOf(type) + 1; // skip current type

                        List<Class> subList = ow.subList(index, ow.size());

                        for(Class other : subList) {
                            if(!MAPPINGS.containsMapping(type, other)) {
                                MAPPINGS.put(type, other);

                                // also put equivalents of other
                                MAPPINGS.putAll(type, MAPPINGS.get(other));
                            }
                        }
                    }
                }
            }
        }

        // second pass: equivalent (wrapper types)
        for(List<Class> list : EQUAL) {
            for(Class type : list) {
                // equivalent
                MAPPINGS.putAll(type, list.stream().filter(c -> !c.equals(type)).collect(Collectors.toList()));
            }
        }
    }

    public List<MethodSignature> expand(MethodSignature signature) {
        List<String> orderedParams = new ArrayList<>(signature.getParameterTypes());
        signature.sortParameters(orderedParams);

        List<String> types = new ArrayList<>(orderedParams.size() + 1);
        types.addAll(orderedParams);
        types.add(signature.getReturnValue());

        List<List<Class>> combinations = expand(types);

        return combinations.stream().map(comb -> {
            List<String> params = comb.stream().map(Class::getCanonicalName).collect(Collectors.toList());

           MethodSignature m = new MethodSignature("public",
                   signature.getMethodName(),
                   params.subList(0, params.size() - 1),
                   params.get(params.size() - 1));

           return m;
        }).collect(Collectors.toList());
    }

    public static MethodSignature expandRaw(MethodSignature signature) {
        List<String> orderedParams = new ArrayList<>(signature.getParameterTypes());
        signature.sortParameters(orderedParams);

        List<String> types = new ArrayList<>(orderedParams.size() + 1);
        types.addAll(orderedParams);
        types.add(signature.getReturnValue());

        List<String> params = new ArrayList<>();
        for(String type : types) {
            if(StringUtils.equalsIgnoreCase(type, "void")) {
                params.add(type);
            } else {
                params.add("java.lang.Object");
            }
        }

        MethodSignature m = new MethodSignature("public",
                signature.getMethodName(),
                params.subList(0, params.size() - 1),
                params.get(params.size() - 1));


        return m;
    }

    public List<Class> getMapping(Class type) {
        return new ArrayList<>(MAPPINGS.get(type));
    }

    public List<List<Class>> expand(List<String> params) {
        // last one is return
        List<List<Class>> compatibleTypes = params.stream().map(this::findClass)
                //.peek(c -> System.out.println(c))
                .map(c -> getMapping(c))
                .collect(Collectors.toList());

        List<List<Class>> combinations = combinations(compatibleTypes);

        //combinations.stream().forEach(comb -> System.out.println("Combination: " + comb));

        return combinations;
    }

    private <T> List<List<T>> combinations(List<List<T>> lists) {
        List<List<T>> combinations = new ArrayList<>();
        List<List<T>> newCombinations;

        for (T s: lists.remove(0)) {
            List<T> combination = new ArrayList<>(lists.size());
            combination.add(s);
            combinations.add(combination);
        }

        while (!lists.isEmpty()) {
            List<T> next = lists.remove(0);
            newCombinations =  new ArrayList<>();
            for (List<T> s1: combinations) {
                for (T s2 : next) {
                    List<T> combination = new ArrayList<>(s1);
                    combination.add(s2);

                    newCombinations.add(combination);
                }
            }

            combinations = newCombinations;
        }

        return combinations;
    }

    /**
     * Find Class with given type in classpath
     *
     * @param paramType
     * @return
     */
    private Class<?> findClass(String paramType) {
        // 1. step: try ClassUtils.getClass(className) (including primitives)
        try {
            return ClassUtils.getClass(paramType);
        } catch (Throwable e) {
        }

        // 2. step: guess from default package
        try {
            return ClassUtils.getClass("java.lang." + paramType);
        } catch (Throwable e) {
        }

        // 4. step: void
        if (StringUtils.equals(paramType, "void")) {
            return void.class;
        }

        // FIXME workaround types come in lower case
        if(StringUtils.startsWith(paramType, "java.lang")) {
            String name = StringUtils.substringAfterLast(paramType, ".");

            String corrected = CaseUtils.toCamelCase(name, true);

            try {
                return ClassUtils.getClass("java.lang." + corrected);
            } catch (Throwable e) {
            }
        }

        //throw new IllegalArgumentException(String.format("unknown type '%s'", paramType));

        return Object.class;
    }
}
