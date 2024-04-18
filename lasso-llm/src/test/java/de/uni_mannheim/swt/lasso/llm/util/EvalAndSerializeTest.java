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
package de.uni_mannheim.swt.lasso.llm.util;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.llm.problem.MultiPLE;
import de.uni_mannheim.swt.lasso.llm.test.Value;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class EvalAndSerializeTest {


    @Test
    public void test_string() throws ClassNotFoundException {
        EvalAndSerialize evalAndSerialize = new EvalAndSerialize();

        Value value = evalAndSerialize.evalAndSerializeToJson("\"hello world\"", EvalAndSerializeTest.class.getClassLoader());

        assertEquals("hello world", value.getValue());
        assertEquals("\"hello world\"", value.getCode());
        assertEquals(String.class, Class.forName(value.getType()));
    }

    @Test
    public void test_hashmap() throws ClassNotFoundException {
        EvalAndSerialize evalAndSerialize = new EvalAndSerialize();

        Value value = evalAndSerialize.evalAndSerializeToJson("new HashMap<String, String>(Map.of(\"p\", \"pineapple\", \"b\", \"banana\"))", EvalAndSerializeTest.class.getClassLoader());

        assertEquals(new HashMap<String, String>(Map.of("p", "pineapple", "b", "banana")), value.getValue());
        assertEquals("{\"p\":\"pineapple\",\"b\":\"banana\"}", value.getCode());
        assertEquals(HashMap.class, Class.forName(value.getType()));
    }

    @Test
    public void test_javatuples_Pair() throws IOException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();

        EvalAndSerialize evalAndSerialize = new EvalAndSerialize();

        Value value = evalAndSerialize.evalAndSerializeToJson("Pair.with(8l, 13l)", container);

        // FIXME precision and types
        assertEquals("[8, 13]", value.getValue().toString());
        assertEquals("{\"val0\":8,\"val1\":13,\"valueArray\":[8,13],\"valueList\":[8,13]}", value.getCode());
        assertEquals("org.javatuples.Pair", value.getType());
    }

    @Test
    public void test_javatuples_Pair_deserialize() throws IOException, ClassNotFoundException {
        MultiPLE multiple = new MultiPLE();
        Container container = multiple.createContainer();

        EvalAndSerialize evalAndSerialize = new EvalAndSerialize();

        Object obj = evalAndSerialize.deserializePair("{\"val0\":8,\"val1\":13,\"valueArray\":[8,13],\"valueList\":[8,13]}", container);
        assertEquals("[8.0, 13.0]", obj.toString());
        assertEquals("org.javatuples.Pair", obj.getClass().getName());
    }
}
