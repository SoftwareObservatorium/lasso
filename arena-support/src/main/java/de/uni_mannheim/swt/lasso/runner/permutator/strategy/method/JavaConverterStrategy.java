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
import de.uni_mannheim.swt.lasso.runner.permutator.TypeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Default converters for frequently known java types, e.g. String alike etc.
 *
 * @author Marcus Kessel
 */
public class JavaConverterStrategy implements AdaptationStrategy {

    /**
     * Simply assume system defaults.
     */
    private static String charset = "UTF-8";

    static List<Converter<?,?>> converters = new ArrayList<>();
    static {
        // string conversion

        // byte[]<>String
        converters.add(new ByteArrayToStringConverter());
        converters.add(new StringToByteArrayConverter());
        converters.add(new ByteArrayToStringWrapperConverter());
        converters.add(new StringToByteArrayWrapperConverter());
        // char[]<>String
        converters.add(new StringToCharArrayConverter());
        converters.add(new CharArrayToStringConverter());
        converters.add(new StringToCharArrayWrapperConverter());
        converters.add(new CharArrayToStringWrapperConverter());
        // byte[]<>InputStream
        converters.add(new ByteArrayToInputStreamConverter());
        converters.add(new InputStreamToByteArrayConverter());
        // FIXME add more combinations?
        // byte[]<>char[]
        converters.add(new ByteArrayToCharArrayConverter());
        converters.add(new CharArrayToByteArrayConverter());
        // InputStream<>String
        converters.add(new StringToInputStreamConverter());
        converters.add(new InputStreamToStringConverter());
        // char[]<>InputStream
        converters.add(new CharArrayToInputStreamConverter());
        converters.add(new InputStreamToCharArrayConverter());

        // arrays and collections
        converters.add(new ListToArrayConverter());
        converters.add(new ArrayToListConverter());

        // arrays, primitives wrappers and Object[]
        converters.add(new PrimitiveArrayToWrapperArray());
        converters.add(new WrapperArrayToPrimitiveArray());
        converters.add(new AssignableArray<>());
        converters.add(new PrimitiveArrToObjectArr());

        // TODO assignable lists etc. List<Integer> to List<Double>, but also Integer[] to List assignable

        // FIXME add more collections converter?
    }

    @Override
    public List<Candidate> match(Class<?> cutClass, Class<?> returnType, Class<?>[] paramTypes, Method method, int[] positions) throws Throwable {
        // same amount of params?
        if(paramTypes.length != method.getParameterTypes().length) {
            return null;
        }

        // return type
        boolean returnNeeds = false;
        if(needsConvert(method.getReturnType(), returnType)) {
            returnNeeds = true;
            boolean canConvert = canConvert(method.getReturnType(), returnType);
            if(!canConvert) {
                return null;
            }
        }

        // make input types comparable with method
        Class<?>[] sortedParamTypes = new Class<?>[paramTypes.length];
        int i = 0;
        for (int position : positions) {
            sortedParamTypes[i++] = paramTypes[position];
        }

        // double-check input types
        boolean inputNeeds = false;
        for(int j = 0; j < sortedParamTypes.length; j++) {
            if(needsConvert(sortedParamTypes[j], method.getParameterTypes()[j])) {
                if(!inputNeeds) {
                    inputNeeds = true;
                }
                boolean canConvert = canConvert(sortedParamTypes[j], method.getParameterTypes()[j]);
                if(!canConvert) {

                    return null;
                }
            }
        }

        // no need to convert something
        if(!returnNeeds && !inputNeeds) {
            return null;
        }

        // TODO in the future, we may be interested in computing all kinds of possible combinations of converters
        // or compute an optimized (or most likely) one

        // now construct candidates
        Candidate candidate = new Candidate(method, positions);
        // set required types
        candidate.setParamClasses(paramTypes);
        candidate.setReturnType(returnType);

        candidate.setAdaptationStrategy(this);

        return new ArrayList<>(Arrays.asList(candidate));
    }

    @Override
    public Object[] preProcessInputs(Candidate candidate, Object[] inputs) throws Throwable {
        if(inputs == null) {
            return null;
        }

        if(inputs.length == 0) {
            return inputs;
        }

        // convert
        Method method = (Method) candidate.getMethod();

        // make input types comparable with method
        Class<?>[] sortedParamTypes = new Class<?>[candidate.getParamClasses().length];
        int i = 0;
        for (int position : candidate.getPositions()) {
            sortedParamTypes[i++] = candidate.getParamClasses()[position];
        }

        // double-check input types
        Object[] convertedInputs = new Object[inputs.length];
        for(int j = 0; j < sortedParamTypes.length; j++) {
            if(needsConvert(sortedParamTypes[j], method.getParameterTypes()[j])) {
                Optional<Converter<?,?>> converter = getConverter(sortedParamTypes[j], method.getParameterTypes()[j]);

                convertedInputs[j] = convert(converter.get(), inputs[j], candidate.getReturnType());
            } else {
                convertedInputs[j] = inputs[j];
            }
        }

        return convertedInputs;
    }

    @Override
    public Object postProcessInvocationResult(Candidate candidate, Object[] inputs, Object returned, Throwable throwable) throws Throwable {
        // simply-rethrow
        if(throwable != null) {
            throw throwable;
        }

        Method method = (Method) candidate.getMethod();

        // return type
        if(needsConvert(method.getReturnType(), candidate.getReturnType())) {
            Optional<Converter<?,?>> converter = getConverter(method.getReturnType(), candidate.getReturnType());

            return convert(converter.get(), returned, candidate.getReturnType());
        }

        return returned;
    }

    /**
     * Workaround generics ..
     *
     * @param converter
     * @param object
     * @param type
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object convert(Converter converter, Object object, Class<?> type) throws InvocationTargetException, IllegalAccessException {
        //Optional<Method> method = Arrays.stream(converter.getClass().getMethods()).filter(m -> StringUtils.equals(m.getName(), "convert")).findFirst();

        //return method.get().invoke(converter, object);

        return converter.convert(object, type);
    }

    private boolean needsConvert(Class<?> from, Class<?> to) {
        // equal types are undesired
        if(from == to) {
            return false;
        }

        return true;
    }

    private boolean canConvert(Class<?> from, Class<?> to) {
        // equal types are undesired
        if(!needsConvert(from, to)) {
            return false;
        }

        return converters.stream().filter(c -> c.canConvert(from, to)).findFirst().isPresent();
    }

    private Optional<Converter<?, ?>> getConverter(Class<?> from, Class<?> to) {
        // equal types are undesired
        if(from == to) {
            return Optional.empty();
        }

        return converters.stream().filter(c -> c.canConvert(from, to)).findFirst();
    }

    /**
     * Converter interface
     *
     * @param <S>
     * @param <T>
     */
    interface Converter<S extends Object, T extends Object> {

        T convert(S bytes);

        default T convert(S bytes, Class<T> type) {
            return convert(bytes);
        }

        boolean canConvert(Class<?> from, Class<?> to);
    }

    static class ByteArrayToStringConverter implements Converter<byte[], String> {

        @Override
        public String convert(byte[] bytes) {
            if(bytes == null) {
                return null;
            }

            try {
                return new String(bytes, charset);
            } catch (UnsupportedEncodingException e) {
                return new String(bytes);
            }
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return byte[].class == from && String.class == to;
        }
    }

    static class StringToByteArrayConverter implements Converter<String, byte[]> {

        @Override
        public byte[] convert(String str) {
            if(str == null) {
                return null;
            }

            try {
                return str.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                return str.getBytes();
            }
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return String.class == from && byte[].class == to;
        }
    }

    static class ByteArrayToStringWrapperConverter implements Converter<Byte[], String> {

        @Override
        public String convert(Byte[] bytes) {
            if(bytes == null) {
                return null;
            }

            try {
                return new String(ArrayUtils.toPrimitive(bytes), charset);
            } catch (UnsupportedEncodingException e) {
                return new String(ArrayUtils.toPrimitive(bytes));
            }
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return Byte[].class == from && String.class == to;
        }
    }

    static class StringToByteArrayWrapperConverter implements Converter<String, Byte[]> {

        @Override
        public Byte[] convert(String str) {
            if(str == null) {
                return null;
            }

            try {
                return ArrayUtils.toObject(str.getBytes(charset));
            } catch (UnsupportedEncodingException e) {
                return ArrayUtils.toObject(str.getBytes());
            }
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return String.class == from && Byte[].class == to;
        }
    }

    static class StringToCharArrayConverter implements Converter<String, char[]> {

        @Override
        public char[] convert(String source) {
            if(source == null) {
                return null;
            }

            return source.toCharArray();
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return String.class == from && char[].class == to;
        }
    }

    static class StringToCharArrayWrapperConverter implements Converter<String, Character[]> {

        @Override
        public Character[] convert(String source) {
            if(source == null) {
                return null;
            }

            return ArrayUtils.toObject(source.toCharArray());
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return String.class == from && Character[].class == to;
        }
    }

    static class CharArrayToStringConverter implements Converter<char[], String> {

        @Override
        public String convert(char[] arr) {
            if(arr == null) {
                return null;
            }

            return new String(arr);
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return char[].class == from && String.class == to;
        }
    }

    static class CharArrayToStringWrapperConverter implements Converter<Character[], String> {

        @Override
        public String convert(Character[] arr) {
            if(arr == null) {
                return null;
            }

            return new String(ArrayUtils.toPrimitive(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return Character[].class == from && String.class == to;
        }
    }

    static class ByteArrayToInputStreamConverter implements Converter<byte[], InputStream> {

        @Override
        public InputStream convert(byte[] arr) {
            if(arr == null) {
                return null;
            }

            return new ByteArrayInputStream(arr);
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return byte[].class == from && InputStream.class == to;
        }
    }

    static class InputStreamToByteArrayConverter implements Converter<InputStream, byte[]> {

        @Override
        public byte[] convert(InputStream is) {
            if(is == null) {
                return null;
            }

            // auto-close
            try (BufferedInputStream bis = new BufferedInputStream(is);
                 ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                int result = bis.read();
                while(result != -1) {
                    buf.write((byte) result);
                    result = bis.read();
                }

                return buf.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return InputStream.class == from && byte[].class == to;
        }
    }

    static class ByteArrayToCharArrayConverter implements Converter<byte[], char[]> {

        ByteArrayToStringConverter b2s = new ByteArrayToStringConverter();
        StringToCharArrayConverter s2c = new StringToCharArrayConverter();

        @Override
        public char[] convert(byte[] arr) {
            if(arr == null) {
                return null;
            }

            return s2c.convert(b2s.convert(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return byte[].class == from && char[].class == to;
        }
    }

    static class CharArrayToByteArrayConverter implements Converter<char[],byte[]> {

        CharArrayToStringConverter c2s = new CharArrayToStringConverter();
        StringToByteArrayConverter s2c = new StringToByteArrayConverter();

        @Override
        public byte[] convert(char[] arr) {
            if(arr == null) {
                return null;
            }

            return s2c.convert(c2s.convert(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return char[].class == from && byte[].class == to;
        }
    }

    static class StringToInputStreamConverter implements Converter<String,InputStream> {

        StringToByteArrayConverter s2b = new StringToByteArrayConverter();
        ByteArrayToInputStreamConverter b2i = new ByteArrayToInputStreamConverter();

        @Override
        public InputStream convert(String arr) {
            if(arr == null) {
                return null;
            }

            return b2i.convert(s2b.convert(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return String.class == from && InputStream.class == to;
        }
    }

    static class InputStreamToStringConverter implements Converter<InputStream, String> {

        InputStreamToByteArrayConverter i2b = new InputStreamToByteArrayConverter();
        ByteArrayToStringConverter b2i = new ByteArrayToStringConverter();

        @Override
        public String convert(InputStream arr) {
            if(arr == null) {
                return null;
            }

            return b2i.convert(i2b.convert(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return InputStream.class == from && String.class == to;
        }
    }

    static class CharArrayToInputStreamConverter implements Converter<char[], InputStream> {

        CharArrayToByteArrayConverter c2b = new CharArrayToByteArrayConverter();
        ByteArrayToInputStreamConverter b2i = new ByteArrayToInputStreamConverter();

        @Override
        public InputStream convert(char[] arr) {
            if(arr == null) {
                return null;
            }

            return b2i.convert(c2b.convert(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return char[].class == from && InputStream.class == to;
        }
    }

    static class InputStreamToCharArrayConverter implements Converter<InputStream, char[]> {

        InputStreamToByteArrayConverter i2b = new InputStreamToByteArrayConverter();
        ByteArrayToCharArrayConverter b2s = new ByteArrayToCharArrayConverter();

        @Override
        public char[] convert(InputStream arr) {
            if(arr == null) {
                return null;
            }

            return b2s.convert(i2b.convert(arr));
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return InputStream.class == from && char[].class == to;
        }
    }

    /**
     * Note: conversion does not work for empty lists, so 'null' is returned instead of empty.
     *
     * @param <T>
     */
    static class ListToArrayConverter<T> implements Converter<List<T>, T[]> {

        @Override
        public T[] convert(List<T> list) {
            // conversion does not work for empty lists
            if(list == null || list.size() < 1) {
                return null;
            }

            return toArray(list);
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return TypeUtils.isAssignable(from, List.class, false) && to.isArray();
        }

        // copied from https://stackoverflow.com/a/6522958
        public static <T> T[] toArray(List<T> list) {
            T[] toR = (T[]) java.lang.reflect.Array.newInstance(list.get(0)
                    .getClass(), list.size());
            for (int i = 0; i < list.size(); i++) {
                toR[i] = list.get(i);
            }
            return toR;
        }
    }

    /**
     * Array to list conversion.
     *
     * @param <T>
     *
     * @see Arrays
     */
    static class ArrayToListConverter<T> implements Converter<T[], List<T>> {

        @Override
        public List<T> convert(T[] arr) {
            //
            if(arr == null) {
                return null;
            }

            return Arrays.asList(arr);
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            return from.isArray() && TypeUtils.isAssignable(to, List.class, false);
        }
    }

    /**
     * Primitive Wrapper array to primitive array
     *
     * @param <T>
     */
    static class WrapperArrayToPrimitiveArray<T> implements Converter<T[], Object> {

        @Override
        public Object convert(T[] arr) {
            //
            if(arr == null) {
                return null;
            }

            return ArrayUtils.toPrimitive(arr);
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            // both arrays?
            if(!from.isArray() || !to.isArray()) {
                return false;
            }

            // wrapper + primitive present?
            if(!ClassUtils.isPrimitiveWrapper(from.getComponentType()) || !to.getComponentType().isPrimitive()) {
                return false;
            }

            // compare wrappers
            return from.getComponentType().equals(ClassUtils.primitiveToWrapper(to.getComponentType()));
        }
    }

    /**
     * Primitive array to primitive wrapper array
     *
     * @param <S>
     */
    static class PrimitiveArrayToWrapperArray<S> implements Converter<Object, S[]> {

        /**
         * ({@link Boolean}, {@link Byte}, {@link Character},
         *      * {@link Short}, {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
         *
         * @param arr
         * @return
         */
        @Override
        public S[] convert(Object arr) {
            //
            if(arr == null) {
                return null;
            }

            Object val = arr;
            Class<?> componentType = arr.getClass().getComponentType();
            if(componentType.equals(boolean.class)) {
                return (S[]) ArrayUtils.toObject((boolean[]) val);
            } else if(componentType.equals(byte.class)) {
                return (S[]) ArrayUtils.toObject((byte[]) val);
            } else if(componentType.equals(char.class)) {
                return (S[]) ArrayUtils.toObject((char[]) val);
            } else if(componentType.equals(short.class)) {
                return (S[]) ArrayUtils.toObject((short[]) val);
            } else if(componentType.equals(int.class)) {
                return (S[]) ArrayUtils.toObject((int[]) val);
            } else if(componentType.equals(long.class)) {
                return (S[]) ArrayUtils.toObject((long[]) val);
            } else if(componentType.equals(double.class)) {
                return (S[]) ArrayUtils.toObject((double[]) val);
            } else if(componentType.equals(float.class)) {
                return (S[]) ArrayUtils.toObject((float[]) val);
            }

            throw new IllegalArgumentException("Unsupported Primitive type " + componentType.getName());
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            // both arrays?
            if(!from.isArray() || !to.isArray()) {
                return false;
            }

            // wrapper + primitive present?
            if(!ClassUtils.isPrimitiveWrapper(to.getComponentType()) || !from.getComponentType().isPrimitive()) {
                return false;
            }

            // compare primitives
            return from.getComponentType().equals(ClassUtils.wrapperToPrimitive(to.getComponentType()));
        }
    }

    /**
     * Assignable array
     *
     * @param <T>
     * @param <S>
     */
    static class AssignableArray<T, S> implements Converter<T[], S[]> {

        @Override
        public S[] convert(T[] arr) {
            throw new UnsupportedOperationException();
        }

        @Override
        public S[] convert(T[] arr, Class<S[]> type) {
            //
            if(arr == null) {
                return null;
            }

            Class<?> requiredType = type.getComponentType();
            S[] toR = (S[]) java.lang.reflect.Array.newInstance(requiredType, arr.length);

            boolean number = TypeUtils.isAssignable(requiredType, Number.class, false);
            for (int i = 0; i < arr.length; i++) {
                T val = arr[i];

                if(number) {
                    if(requiredType.equals(Byte.class)) {
                        toR[i] = (S) Byte.valueOf(((Number)val).byteValue());
                    } else if(requiredType.equals(Character.class)) {
                        toR[i] = (S) Character.valueOf((char) ((Number)val).intValue());
                    } else if(requiredType.equals(Short.class)) {
                        toR[i] = (S) Short.valueOf(((Number)val).shortValue());
                    } else if(requiredType.equals(Integer.class)) {
                        toR[i] = (S) Integer.valueOf(((Number)val).intValue());
                    } else if(requiredType.equals(Long.class)) {
                        toR[i] = (S) Long.valueOf(((Number)val).longValue());
                    } else if(requiredType.equals(Double.class)) {
                        toR[i] = (S) Double.valueOf(((Number)val).doubleValue());
                    } else if(requiredType.equals(Float.class)) {
                        toR[i] = (S) Float.valueOf(((Number)val).floatValue());
                    }
                } else {
                    // simple cast
                    toR[i] = (S) arr[i];
                }
            }
            return toR;
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            // both arrays?
            if(!from.isArray() || !to.isArray()) {
                return false;
            }

            // no primitives supported
            if(from.getComponentType().isPrimitive() || to.getComponentType().isPrimitive()) {
                return false;
            }

            // assignable?
            return TypeUtils.isAssignable(from.getComponentType(), to.getComponentType(), false);
        }
    }

    /**
     * Primitive array to object array.
     *
     */
    static class PrimitiveArrToObjectArr implements Converter<Object, Object[]> {

        PrimitiveArrayToWrapperArray p2w = new PrimitiveArrayToWrapperArray();
        AssignableArray assignableArray = new AssignableArray();

        @Override
        public Object[] convert(Object arr) {
            //
            if(arr == null) {
                return null;
            }

            return assignableArray.convert(p2w.convert(arr), Object[].class);
        }

        @Override
        public boolean canConvert(Class<?> from, Class<?> to) {
            // both arrays?
            if(!from.isArray() || !to.isArray()) {
                return false;
            }

            return from.getComponentType().isPrimitive();
        }
    }

    // TODO set to list and list to set
}
