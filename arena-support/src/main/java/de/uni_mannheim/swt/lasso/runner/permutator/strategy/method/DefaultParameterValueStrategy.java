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
package de.uni_mannheim.swt.lasso.runner.permutator.strategy.method;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.MethodSignature;
import de.uni_mannheim.swt.lasso.runner.permutator.PermutatorException;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Find and set default parameters (if available).
 *
 * @author Marcus Kessel
 */
@Deprecated
public class DefaultParameterValueStrategy implements AdaptationStrategy {

    public static final String OVERFIT_POS = "overfit_pos";
    public static final String OVERFIT_FIELD = "overfit_field_";

    private String[] packageNames;

    /**
     * Allow a maximum of two overfitting parameters
     */
    private int maxParam = 2;

    public DefaultParameterValueStrategy(String[] packageNames) {
        this.packageNames = packageNames;
    }

    @Override
    public List<Candidate> matchMethod(Class<?> cutClass, Class<?> returnType, Class<?>[] paramTypes, Method method) throws Throwable {
//        // TODO what about no param at all?
//
//        Class<?>[] methodParameters = method.getParameterTypes();
//        // 1. check if subset of given paramTypes is assignable
//        int overfitSize = methodParameters.length - paramTypes.length;
//        if(method.getParameterCount() < 1 || overfitSize < 1 || overfitSize > maxParam) {
//            return null;
//        }
//
//        // 2. step: create permutations from method's params and check
//        List<Integer> paramPositions = IntStream.range(0, methodParameters.length)
//                .mapToObj(index -> new Integer(index))
//                .collect(Collectors.toList());
//
//        Iterator<List<Integer>> paramPermutations = Permutator.createPermutations(
//                paramPositions);
//
//        List<Candidate> candidates = new LinkedList<>();
//
//        ScanResult scanResult = null;
//        try {
//            Set<List<Integer>> known = new HashSet<>();
//            while (paramPermutations.hasNext()) {
//                List<Integer> permutatedParamPositions = paramPermutations.next();
//
//                // limit to amount of desired parameter
//                List<Integer> desiredParamPositions = permutatedParamPositions
//                        .stream()
//                        // filter to limit supported indices
//                        .filter(i -> i < paramTypes.length)
//                        .collect(Collectors.toList());
//
//                // avoid duplicates
//                if(known.contains(desiredParamPositions)) {
//                    continue;
//                }
//
//                known.add(desiredParamPositions);
//
//                Class<?>[] methodParamClasses = desiredParamPositions
//                        .stream()
//                        .map(index -> (Class<?>) methodParameters[index])
//                        .collect(Collectors.toList()).toArray(new Class<?>[0]);
//
//                // check if we have compatible parameter types (including
//                // supertypes)
//                Class<?>[] permutatedParamClasses = desiredParamPositions
//                        .stream()
//                        .map(index -> (Class<?>) paramTypes[index])
//                        .collect(Collectors.toList()).toArray(new Class<?>[0]);
//
//                //System.out.println("Comparing perms/method => " + Arrays.toString(permutatedParamClasses) + " vs " + Arrays.toString(methodParamClasses));
//
//                boolean isAssignable = Permutator.isAssignable(permutatedParamClasses,
//                        methodParamClasses);
//                if(isAssignable) {
//                    Candidate candidate = new Candidate(method, desiredParamPositions.stream().mapToInt(i -> i).toArray());
//                    // add overfit to data map
//                    int[] overfit = permutatedParamPositions.subList(desiredParamPositions.size(), permutatedParamPositions.size()).stream().mapToInt(i -> i).toArray();
//                    candidate.addValue("overfit_pos", overfit);
//
//                    //System.out.println("Comparing pos => " + Arrays.toString(permutatedParamPositions.toArray()) + " vs " + Arrays.toString(desiredParamPositions.toArray())
//                    //+ " overfit " + Arrays.toString(overfit));
//
//                    // 2. scan for default values (public static final fields)
//                    List<Field>[] allFields = new List[overfit.length];
//
//                    boolean failed = false;
//                    for(int i = 0; i < overfit.length; i++) {
//                        // init scanner
//                        if(scanResult == null) {
//                            scanResult = doScan(cutClass);
//                        }
//
//                        // ATTENTION: we need to use the overfitting parameters AS IS (no position swaps allowed)
//                        List<Field> fields = scan(scanResult, cutClass, methodParameters[overfit[i]]);
//
//                        //System.out.println("Fields returned " + fields);
//
//                        if(CollectionUtils.isNotEmpty(fields)) {
//                            allFields[i] = fields;
//                        } else {
//                            // TODO provide default values, see DefaultValues
//
//                            failed = true;
//                            break;
//                        }
//                    }
//
//                    // only add if all default values found
//                    if(!failed && allFields.length == overfit.length) {
//                        // add all combinations of default values
//                        if(overfit.length < 2) {
//                            // add directly
//                            int index = 0;
//                            for(int i = 0; i < allFields[index].size(); i++) {
//                                Candidate fCandidate = new Candidate(method, candidate.getPositions());
//                                fCandidate.addValue(OVERFIT_POS, overfit);
//                                Field field = allFields[index].get(i);
//
//                                fCandidate.addValue(OVERFIT_FIELD + index, field);
//
//                                // add strategy
//                                fCandidate.setAdaptationStrategy(this);
//                                // add candidate
//                                candidates.add(fCandidate);
//                            }
//                        } else {
//                            // cartesian product of field combinations
//                            List<List<Field>> product = CombinationUtils.product(allFields);
//                            // combination of fields
//                            for(List<Field> fieldCombination : product) {
//                                Candidate fCandidate = new Candidate(method, candidate.getPositions());
//                                fCandidate.addValue(OVERFIT_POS, overfit);
//
//                                for(int i = 0; i < fieldCombination.size(); i++) {
//                                    Field field = fieldCombination.get(i);
//                                    fCandidate.addValue(OVERFIT_FIELD + i, field);
//                                }
//
//                                // add strategy
//                                fCandidate.setAdaptationStrategy(this);
//                                // add candidate
//                                candidates.add(fCandidate);
//                            }
//                        }
//                    }
//                }
//            }
//        } finally {
//            if(scanResult != null) {
//                try {
//                    scanResult.close();
//                } catch (Throwable e) {
//                    //
//                }
//            }
//        }

        //return candidates;

        return null;
    }

    @Override
    public Object[] preProcessInputs(Candidate candidate, Object[] inputs) throws Throwable {
        // add more inputs having the default values
        int[] overfit = (int[]) candidate.getValue(OVERFIT_POS);

        int paramLength = candidate.getPositions().length + overfit.length;

        Object[] newInputs = new Object[paramLength];
        // note: inputs are already switched by Permutator
        for(int i = 0; i < inputs.length; i++) {
            newInputs[i] = inputs[i];
        }

        // add field values
        for(int i = 0; i < overfit.length; i++) {
            int index = candidate.getPositions().length + i;

            try {
                Field field = (Field) candidate.getValue(OVERFIT_FIELD + i);
                // make accessible
                field.setAccessible(true);

                // static field has no instance
                Object value = field.get(null);

                //System.out.println("Field val => " + value);

                newInputs[index] = value;
            } catch (Throwable e) {
                // signal error
                throw new IllegalArgumentException(getClass().getName() + " for " + i, e);
            }
        }

        //System.out.println("Overfitted inputs => " + Arrays.toString(newInputs));

        return newInputs;
    }

    @Override
    public void serialize(Candidate candidate, MethodSignature methodSignature) {
        int[] overfit = (int[]) candidate.getValue(OVERFIT_POS);

        Map<String, String> data = new HashMap<>();

        data.put(OVERFIT_POS, Arrays.stream(overfit).mapToObj(i -> Integer.toString(i)).collect(Collectors.joining(",")));

        for(int i = 0; i < overfit.length; i++) {
            Field field = (Field) candidate.getValue(OVERFIT_FIELD + i);
            data.put(OVERFIT_FIELD + i, field.getDeclaringClass().getName() + "." + field.getName());
        }

        methodSignature.setData(data);
    }

    @Override
    public void deserialize(Candidate candidate, MethodSignature methodSignature) {
        String overfitPos = methodSignature.getData().get(OVERFIT_POS);
        String[] rawArr = StringUtils.split(overfitPos, ',');
        int[] overfit = Arrays.stream(rawArr).mapToInt(o -> Integer.parseInt(o)).toArray();

        candidate.addValue(OVERFIT_POS, overfit);

        for(int i = 0; i < overfit.length; i++) {
            String rawField = methodSignature.getData().get(OVERFIT_FIELD + i);

            String className = StringUtils.substringBeforeLast(rawField, ".");
            String fieldName = StringUtils.substringAfterLast(rawField, ".");

            try {
                Class clazz = Class.forName(className);

                Field field = clazz.getDeclaredField(fieldName);

                candidate.addValue(OVERFIT_FIELD + i, field);
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                throw new PermutatorException("Could not load field", e);
            }
        }
    }

//    private ScanResult doScan(Class<?> ownerClass) {
//        String[] packageNames = ProducerUtils.getPackageNames(this.packageNames, ownerClass);
//
//        ScanResult scanResult =
//                new ClassGraph()
//                        .enableStaticFinalFieldConstantInitializerValues()
//                        .whitelistPackages(packageNames)
//                        .scan();
//
//        return scanResult;
//    }

//    private List<Field> scan(ScanResult scanResult, Class<?> ownerClass, Class<?> searchClass) {
//        try {
//            List<Field> matches = new LinkedList<>();
//
//            // only owner class
//            ClassInfo clazz = scanResult.getClassInfo(ownerClass.getName());
//
//            // (public) static final
//            FieldInfoList fields = clazz.getFieldInfo()
//                    .filter(f -> f.isStatic() && f.isFinal());
//            for(FieldInfo field : fields) {
//                TypeSignature fieldDesc = field.getTypeDescriptor();
//
//                String classStr = getClassStr(fieldDesc);
//
//                if(StringUtils.equals(searchClass.getName(), classStr)) {
//                    //System.out.println("Found field in --> " + clazz.getName());
//
//                    try {
//                        Field fieldMember = ownerClass.getDeclaredField(field.getName());
//                        if(fieldMember == null) {
//                            throw new IllegalArgumentException("Null field " + field.getName());
//                        }
//                        matches.add(fieldMember);
//                    } catch (Throwable e) {
//                        //
//                    }
//
//                }
//            }
//
//            return matches;
//        } catch(Throwable e) {
//            //
//        }
//
//        return null;
//    }

//    private String getClassStr(TypeSignature typeSignature) {
//        String classStr;
//        if(typeSignature instanceof ClassRefTypeSignature) {
//            ClassRefTypeSignature type  = (ClassRefTypeSignature) typeSignature;
//
//            classStr = type.getFullyQualifiedClassName();
//        } else if(typeSignature instanceof BaseTypeSignature) {
//            BaseTypeSignature type  = (BaseTypeSignature) typeSignature;
//
//            classStr = type.getType().getName();
//        }  else if(typeSignature instanceof ArrayTypeSignature) {
//            ArrayTypeSignature type  = (ArrayTypeSignature) typeSignature;
//
//            classStr = getClassStr(type.getElementTypeSignature()) + StringUtils.repeat("[]", type.getNumDimensions());
//        } else {
//            throw new IllegalArgumentException("Unknown TypeSignature class => " + typeSignature.getClass());
//        }
//
//        return classStr;
//    }
}
