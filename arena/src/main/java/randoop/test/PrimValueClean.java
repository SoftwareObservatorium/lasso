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
package randoop.test;

import java.util.Arrays;
import java.util.Objects;
import randoop.contract.ObjectContract;
import randoop.org.plumelib.util.StringsPlume;
import randoop.sequence.StringTooLongException;
import randoop.sequence.Value;
import randoop.types.JavaTypes;
import randoop.types.Type;
import randoop.types.TypeTuple;

// taken from randoop and modified

/**
 * A check recording the value of a primitive value (or String) obtained during execution (e.g.,
 * {@code var3 == 1} where {@code var3} is an integer-valued variable in a Randoop test).
 */
// BUG FIXed for primitive wrappers and issues with Assert.equals ambigious calls
public final class PrimValueClean extends ObjectContract {

    /**
     * The expected run-time value. It is a boxed primitive or String (checked during construction).
     */
    public final Object value;

    /** Whether to use {@code ==} or {@code .equals()} to test for equality. */
    private final randoop.contract.PrimValue.EqualityMode equalityMode;

    private boolean boxedPrimitive;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PrimValueClean)) {
            return false;
        }
        PrimValueClean other = (PrimValueClean) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * @param value the value for the expression: a primitive value or string
     * @param equalityMode what equality test the assertion uses
     */
    public PrimValueClean(Object value, randoop.contract.PrimValue.EqualityMode equalityMode) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        Type type = Type.forClass(value.getClass());
        if (!type.isBoxedPrimitive() && !type.isString()) {
            throw new IllegalArgumentException(
                    "value is not a primitive or string : " + value.getClass());
        }
        if (value instanceof String && !Value.escapedStringLengthOk((String) value)) {
            throw new StringTooLongException((String) value);
        }
        this.value = value;
        this.equalityMode = equalityMode;
    }

    @Override
    public boolean evaluate(Object... objects) throws Throwable {
        assert objects.length == 1;
        return value.equals(objects[0]);
    }

    @Override
    public int getArity() {
        return 1;
    }

    /** The arguments to which this contract can be applied. */
    static TypeTuple inputTypes = new TypeTuple(Arrays.asList(JavaTypes.OBJECT_TYPE));

    @Override
    public TypeTuple getInputTypes() {
        return inputTypes;
    }

    @Override
    public String toString() {
        return "randoop.PrimValue, value=" + StringsPlume.escapeJava(value.toString());
    }

    @Override
    public String get_observer_str() {
        return "PrimValue";
    }

    @Override
    public String toCodeString() {
        // ValueExpression represents the value of a variable.
        // We special-case printing for this type of expression,
        // to improve readability.
        if (value.equals(Double.NaN)) {
            return "org.junit.Assert.assertTrue(Double.isNaN(x0));";
        } else if (value.equals(Float.NaN)) {
            return "org.junit.Assert.assertTrue(Float.isNaN(x0));";
        }

        if (equalityMode.equals(randoop.contract.PrimValue.EqualityMode.EQUALSMETHOD)) {
            StringBuilder b = new StringBuilder();
            b.append("org.junit.Assert.assertEquals(");
            // First add a message
            b.append("\"'\" + " + "x0" + " + \"' != '\" + ")
                    .append(Value.toCodeString(value))
                    .append("+ \"'\", ");
            if(boxedPrimitive) {
                b.append("(java.lang.Object) x0"); // mkessel changed to make it non-ambigious
            } else {
                b.append("x0");
            }
            b.append(", ");
            b.append(Value.toCodeString(value));
            // Close assert.
            b.append(");");
            return b.toString();
        } else if (equalityMode.equals(randoop.contract.PrimValue.EqualityMode.EQUALSEQUALS)) {
            StringBuilder b = new StringBuilder();
            b.append("org.junit.Assert.assertTrue(");
            b.append("\"'\" + " + "x0" + " + \"' != '\" + ")
                    .append(Value.toCodeString(value))
                    .append("+ \"'\", ");
            b.append("x0 == ").append(Value.toCodeString(value));
            b.append(");");
            return b.toString();
        } else {
            throw new Error("unexpected equalityMode " + equalityMode);
        }
    }

    @Override
    public String toCommentString() {
        return null;
    }

    public boolean isBoxedPrimitive() {
        return boxedPrimitive;
    }

    public void setBoxedPrimitive(boolean boxedPrimitive) {
        this.boxedPrimitive = boxedPrimitive;
    }
}
