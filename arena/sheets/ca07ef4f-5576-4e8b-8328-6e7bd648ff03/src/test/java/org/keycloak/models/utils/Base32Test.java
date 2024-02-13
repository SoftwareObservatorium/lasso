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
import org.junit.Test;
import java.lang.Exception;
import org.keycloak.models.utils.Base32;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.Base32'
 * Method Signature = 'encode(byte[]):java.lang.String'
 *
 * START Functionality Description
 *
 *       Encodes byte array to Base32 String.
 *
 *       @param bytes Bytes to encode.
 *       @return Encoded byte array <code>bytes</code> as a String.
 *
 * END Functionality Description
 *
 * Doc ID: ca07ef4f-5576-4e8b-8328-6e7bd648ff03
 */
public class Base32Test {

    /**
     * JUnit Test Case Example
     *
     * Class Under Test = 'com.XXX.adapter.Base32'
     * Method Signature = 'encode(byte[]):java.lang.String'
     */
    // Length = 32
    // Type = String
    // Input paramters
    @Test
    public void testStringLength() throws Exception {
        byte[] base32Lookup = new byte[] { 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 };
        Base32 _cut = new Base32();
        String value = new String(base32Lookup);
        String actual = _cut.encode(base32Lookup);
        assertTrue(value.length() < actual.length());
    }

    @Test
    public void testIsString() throws Exception {
        byte[] base32Lookup = new byte[] { 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 };
        Base32 _cut = new Base32();
        String actual = _cut.encode(base32Lookup);
        assertTrue(actual instanceof String);
    }

    @Test
    public void testNull() throws Exception {
        Base32 _cut = new Base32();
        try {
            String actual = _cut.encode(null);
            fail();
        } catch (Throwable e) {
        }
    }

    @Test
    public void testIsEmpty() throws Exception {
        Base32 _cut = new Base32();
        byte[] testByteArray = new byte[] {};
        String actual = _cut.encode(testByteArray);
        assertEquals("", actual);
    }
}
