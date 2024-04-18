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
package de.uni_mannheim.swt.lasso.testing.generate.typeaware;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Type-aware mutation of seed values.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://dl.acm.org/doi/pdf/10.1145/3428261">Type-Aware Operator Mutations</a>
 */
public class TypeAwareMutator {

    private static final Logger LOG = LoggerFactory
            .getLogger(TypeAwareMutator.class);

    private final MutatorSettings settings;
    private final Random random;

    public TypeAwareMutator() {
        this(new MutatorSettings());
    }

    public TypeAwareMutator(MutatorSettings settings) {
        this.settings = settings;
        this.random = settings.getRandom();
    }

    /**
     * Just negate the value.
     *
     * @param value
     * @return
     */
    public boolean mutate(Boolean value) {
        return !value;
    }

    /**
     * Randomly choose between adding or subtracting a mutation factor.
     *
     * @param value
     * @return
     */
    public byte mutate(Byte value) {
        byte mutationFactor = (byte) (random.nextInt(settings.getByteBound()) - settings.getByteFactorRange());
        return (byte) (value + mutationFactor);
    }

    /**
     * Randomly choose between adding or subtracting a mutation factor.
     *
     * @param value
     * @return
     */
    public short mutate(Short value) {
        short mutationFactor = (short) (random.nextInt(settings.getShortBound()) - settings.getShortFactorRange());
        return (short) (value + mutationFactor);
    }

    /**
     * Randomly choose between adding or subtracting a mutation factor.
     *
     * @param value
     * @return
     */
    public int mutate(Integer value) {
        int mutationFactor = random.nextInt(settings.getIntBound()) - settings.getIntFactorRange();
        return value + mutationFactor;
    }

    /**
     * Randomly choose between adding or subtracting a mutation factor.
     *
     * @param value
     * @return
     */
    public long mutate(Long value) {
        long mutationFactor = random.nextLong() % settings.getLongBound() - settings.getLongFactorRange();
        return value + mutationFactor;
    }

    /**
     * Randomly choose a mutation factor within a small range
     *
     * @param value
     * @return
     */
    public double mutate(Float value) {
        double mutationFactor = (random.nextDouble() * 2 - 1) * settings.getFloatFactor();
        return value + mutationFactor * value;
    }

    /**
     * Randomly choose a mutation factor within a small range
     *
     * @param value
     * @return
     */
    public double mutate(Double value) {
        double mutationFactor = (random.nextDouble() * 2 - 1) * settings.getDoubleFactor();
        return value + mutationFactor * value;
    }

    /**
     * Mutates a string.
     *
     * Operators
     *
     * <ul>
     *     <li>INSERT</li>
     *     <li>REPLACE</li>
     *     <li>DELETE</li>
     * </ul>
     *
     * @param value
     * @return
     */
    public String mutate(String value) {
        // randomly choose an index to mutate
        int index = random.nextInt(value.length());
        // randomly choose a mutation type: insert, delete, or replace
        int mutationType = random.nextInt(3);
        StringBuilder mutatedString = new StringBuilder(value);

        // FIXME wider range of characters?
        //char r = settings.getRandomObjectGenerator().random(char.class);
        switch (mutationType) {
            case 0: // insert a random character at the chosen index
                char insertedChar = (char) (random.nextInt(26) + 'a');
                mutatedString.insert(index, insertedChar);
                break;
            case 1: // delete the character at the chosen index
                mutatedString.deleteCharAt(index);
                break;
            case 2: // replace the character at the chosen index with a random character
                char replacedChar = (char) (random.nextInt(26) + 'a');
                mutatedString.setCharAt(index, replacedChar);
                break;
        }
        return mutatedString.toString();
    }

    /**
     * Mutates a list
     *
     * Operators
     *
     * <ul>
     *     <li>INSERT</li>
     *     <li>REPLACE</li>
     *     <li>DELETE</li>
     * </ul>
     *
     * @param list
     * @return
     * @param <T>
     */
    public <T> List<T> mutate(List<T> list) {
        // randomly choose an index to mutate
        int index = random.nextInt(list.size());

        // randomly choose a mutation type: insert, delete, or replace
        int mutationType = random.nextInt(3);
        List<T> mutatedList = new ArrayList<>(list);
        switch (mutationType) {
            case 0: // insert a random element at the chosen index
                T element = (T) settings.getRandomObjectGenerator().random(list.get(0).getClass());
                mutatedList.add(index, element);
                break;
            case 1: // delete the element at the chosen index
                mutatedList.remove(index);
                break;
            case 2: // replace the element at the chosen index with a random element
                mutatedList.set(index, (T) settings.getRandomObjectGenerator().random(list.get(0).getClass()));
                break;
        }

        return mutatedList;
    }

    /**
     * Mutates a Set
     *
     * Operators
     *
     * <ul>
     *     <li>INSERT</li>
     *     <li>REPLACE</li>
     *     <li>DELETE</li>
     * </ul>
     *
     * @param set
     * @return
     * @param <T>
     */
    public <T> Set<T> mutate(Set<T> set) {
        // randomly choose an index to mutate
        int index = random.nextInt(set.size());

        // randomly choose a mutation type: insert, delete, or replace
        int mutationType = random.nextInt(3);
        Set<T> mutatedSet = null;
        switch (mutationType) {
            case 0: // insert a random element
                T element = (T) settings.getRandomObjectGenerator().random(set.iterator().next().getClass());
                mutatedSet = new HashSet<>(set);
                mutatedSet.add(element);
                break;
            case 1: // delete a random element
                List<T> delList = new ArrayList<>(set);
                // randomly choose an index to mutate
                delList.remove(index);
                mutatedSet = new HashSet<>(delList);
                break;
            case 2: // replace the element with a random element
                List<T> replaceList = new ArrayList<>(set);
                replaceList.set(index, (T) settings.getRandomObjectGenerator().random(replaceList.get(0).getClass()));
                mutatedSet = new HashSet<>(replaceList);
                break;
        }

        return mutatedSet;
    }

    /**
     * Mutates a list
     *
     * Operators
     *
     * <ul>
     *     <li>INSERT</li>
     *     <li>REPLACE</li>
     *     <li>DELETE</li>
     * </ul>
     *
     * @param map
     * @return
     */
    public Map mutate(Map map) {
        // randomly choose a mutation type: insert, delete, or replace
        int mutationType = random.nextInt(3);

        Map mutatedMap = new HashMap(map);
        Object key = mutatedMap.keySet().iterator().next();
        switch (mutationType) {
            case 0: // insert a random value
                Object newKey = settings.getRandomObjectGenerator().random(key.getClass());
                Object newValue = settings.getRandomObjectGenerator().random(mutatedMap.get(key).getClass());
                mutatedMap.put(newKey, newValue);
                break;
            case 1: // delete the element at the chosen index
                mutatedMap.remove(key);
                break;
            case 2: // replace the element at the chosen index with a random element
                mutatedMap.replace(key, settings.getRandomObjectGenerator().random(mutatedMap.get(key).getClass()));
                break;
        }

        return mutatedMap;
    }

    /**
     * Mutates an array.
     *
     * Operators
     *
     * <ul>
     *     <li>INSERT</li>
     *     <li>REPLACE</li>
     *     <li>DELETE</li>
     * </ul>
     *
     * @param array
     * @return
     * @param <T>
     */
    public <T> T[] mutate(T[] array) {
        // randomly choose an index to mutate
        int index = random.nextInt(array.length);

        T[] mutatedArray = array.clone();

        // randomly choose a mutation type: insert, delete, or replace
        int mutationType = random.nextInt(3);

        switch (mutationType) {
            case 0: // insert a random element at the chosen index
                T element = (T) settings.getRandomObjectGenerator().random(array[0].getClass());
                return ArrayUtils.insert(index, mutatedArray, element);
            case 1: // delete the element at the chosen index
                System.arraycopy(mutatedArray, index + 1, mutatedArray, index, mutatedArray.length - 1 - index);
                break;
            case 2: // replace the element at the chosen index with a random element
                mutatedArray[index] = (T) settings.getRandomObjectGenerator().random(array[0].getClass());
                return mutatedArray;
        }

        return array;
    }

    /**
     * FIXME does not return a copy, but rather mutates the object itself
     * FIXME Serialization options? SerializationUtils (commons), JSON?
     *
     * Mutates an object field value at random.
     *
     * @param obj
     * @return
     */
    public Object mutateObjectField(Object obj) {
        if (obj == null) {
            return null;
        }
        Field[] fields = obj.getClass().getDeclaredFields();
        if (fields.length == 0) {
            return obj;
        }
        int index = random.nextInt(fields.length);
        Field field = fields[index];
        field.setAccessible(true);
        //Class<?> type = field.getType();
        try {
            // mutate field value
            Object value = mutateValue(field.get(obj));
            FieldUtils.writeField(field, obj, value, true);
        } catch (IllegalAccessException e) {
            LOG.warn("Failed to write value for random field", e);
        }

        return obj;
    }

    /**
     * Delegates to corresponding type method
     *
     * @param value
     * @return
     * @param <T>
     */
    public <T> T mutateValue(T value) {
        if(List.class.isAssignableFrom(value.getClass())) {
            return (T) mutate((List) value);
        }

        if(Set.class.isAssignableFrom(value.getClass())) {
            return (T) mutate((Set) value);
        }

        if(Map.class.isAssignableFrom(value.getClass())) {
            return (T) mutate((Map) value);
        }

        if(value.getClass().isArray()) {
            Class<?> componentType = value.getClass().getComponentType();
            if(componentType.equals(boolean.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((boolean[]) value)));
            } else if(componentType.equals(byte.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((byte[]) value)));
            } else if(componentType.equals(char.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((char[]) value)));
            } else if(componentType.equals(short.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((short[]) value)));
            } else if(componentType.equals(int.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((int[]) value)));
            } else if(componentType.equals(long.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((long[]) value)));
            } else if(componentType.equals(double.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((double[]) value)));
            } else if(componentType.equals(float.class)) {
                return (T) ArrayUtils.toPrimitive(mutate((T[]) ArrayUtils.toObject((float[]) value)));
            } else {
                // non-primitives (including wrapper types)
                return (T) mutate((T[]) value);
            }
        }

        // try exact types
        Method method = null;
        try {
            method = getClass().getMethod("mutate", value.getClass());
            return (T) method.invoke(this, value);
        } catch (Throwable e) {
            //LOG.debug("Failed", e);
        }

        // try object
        if(method == null) {
            try {
                return (T) mutateObjectField(value);
            } catch (Exception e) {
                //LOG.debug("Failed", e);
            }
        }

        // for other types, just return the value as-is
        return value;
    }
}

