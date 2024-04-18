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
package de.uni_mannheim.swt.lasso.engine.action.utils;

import de.uni_mannheim.swt.lasso.benchmark.Statement;
import de.uni_mannheim.swt.lasso.benchmark.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 *
 * @author Marcus Kessel
 */
public class SequenceUtilsTest {

    @Test
    public void testTranslateInputs() {
//        System.out.println(
//                Arrays.toString(new int[]{1,2,3})
//        );
        System.out.println(
                SequenceUtils.translateInputs(statement(Arrays.asList(value("hello"), value("world"))))
        );
        System.out.println(
                SequenceUtils.translateInputs(statement(Arrays.asList(value(new String[]{"hello", "world"}))))
        );
        System.out.println(
                SequenceUtils.translateInputs(statement(Arrays.asList(value(1.0), value(1f), value(200L))))
        );
        System.out.println(
                SequenceUtils.translateInputs(statement(Arrays.asList(value(true))))
        );
        System.out.println(
                SequenceUtils.translateInputs(statement(Arrays.asList(value(null))))
        );
    }

    private static Value value(Object val) {
        Value value = new Value();
        value.setValue(val);
        if(val != null) {
            value.setType(val.getClass().getCanonicalName());
        }

        return value;
    }

    private static Statement statement(List<Value> valueList) {
        Statement statement = new Statement();
        statement.setInputs(valueList);

        return statement;
    }
}
