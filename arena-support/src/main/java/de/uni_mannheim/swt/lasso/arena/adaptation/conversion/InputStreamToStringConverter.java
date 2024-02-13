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

import java.io.InputStream;

/**
 *
 * @author Marcus Kessel
 */
public class InputStreamToStringConverter implements Converter<InputStream, String> {

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
