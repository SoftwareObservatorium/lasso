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
package de.uni_mannheim.swt.lasso.gai.openai.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class ContentParserTest {

    @Test
    public void testChatGPTParse() {
        String content = "Here's a Java method that encodes a string to base64 without padding and returns a byte array:\n" +
                "\n" +
                "```java\n" +
                "import java.util.Base64;\n" +
                "\n" +
                "public class Base64Encoder {\n" +
                "    public static byte[] encodeToBase64WithoutPadding(String input) {\n" +
                "        byte[] encodedBytes = Base64.getEncoder().withoutPadding().encode(input.getBytes());\n" +
                "        return encodedBytes;\n" +
                "    }\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        String input = \"Hello, World!\";\n" +
                "        byte[] encodedBytes = encodeToBase64WithoutPadding(input);\n" +
                "        \n" +
                "        // Print the encoded byte array\n" +
                "        for (byte b : encodedBytes) {\n" +
                "            System.out.print(b + \" \");\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "In this code, we use the `Base64.getEncoder().withoutPadding()` method from the `java.util.Base64` class to create a base64 encoder without padding. We then encode the input string by converting it to bytes using `input.getBytes()`, and finally return the encoded byte array.\n" +
                "\n" +
                "In the `main` method, we provide a sample input string, encode it using the `encodeToBase64WithoutPadding` method, and print the encoded byte array.";

        ContentParser contentParser = new ContentParser();

        List<String> matches = contentParser.extractCode(content);

        assertEquals(1, matches.size());

        assertEquals("import java.util.Base64;\n" +
                "\n" +
                "public class Base64Encoder {\n" +
                "    public static byte[] encodeToBase64WithoutPadding(String input) {\n" +
                "        byte[] encodedBytes = Base64.getEncoder().withoutPadding().encode(input.getBytes());\n" +
                "        return encodedBytes;\n" +
                "    }\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        String input = \"Hello, World!\";\n" +
                "        byte[] encodedBytes = encodeToBase64WithoutPadding(input);\n" +
                "        \n" +
                "        // Print the encoded byte array\n" +
                "        for (byte b : encodedBytes) {\n" +
                "            System.out.print(b + \" \");\n" +
                "        }\n" +
                "    }\n" +
                "}\n", matches.get(0));
    }
}
