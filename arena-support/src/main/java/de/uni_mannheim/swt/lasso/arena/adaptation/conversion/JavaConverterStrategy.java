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
package de.uni_mannheim.swt.lasso.arena.adaptation.conversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default converters for frequently known java types, e.g. String alike etc.
 *
 * @author Marcus Kessel
 */
public class JavaConverterStrategy {

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

        // arrays
        converters.add(new PrimitiveAssignableArray());

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

    public boolean needsConvert(Class<?> from, Class<?> to) {
        // equal types are undesired
        if(from == to) {
            return false;
        }

        return true;
    }

    public boolean canConvert(Class<?> from, Class<?> to) {
        // equal types are undesired
        if(!needsConvert(from, to)) {
            return false;
        }

        return converters.stream().filter(c -> c.canConvert(from, to)).findFirst().isPresent();
    }

    public Optional<Converter<?, ?>> getConverter(Class<?> from, Class<?> to) {
        // equal types are undesired
        if(from == to) {
            return Optional.empty();
        }

        return converters.stream().filter(c -> c.canConvert(from, to)).findFirst();
    }
}
