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
package randoop.operation;

import randoop.ExecutionOutcome;
import randoop.NormalExecution;
import randoop.org.plumelib.util.StringsPlume;

import randoop.sequence.Variable;
import randoop.types.*;

import java.util.List;
import java.util.Objects;

// taken from randoop and modified

/**
 * Based on NonreceiverTerm, but allows for any code expression that evaluates to a value.
 *
 *
 * @see de.uni_mannheim.swt.lasso.arena.sequence.eval.Eval
 *
 */
public final class CodeTerm extends CallableOperation {

  /** The {@link Type} of this non-receiver term. */
  private final Type type;

  /** The value of this non-receiver term. Must be null, a String, a boxed primitive, or a Class. */
  private final Object value;
  private final String code;

  /**
   * Constructs a NonreceiverTerm with type t and value o.
   *
   * @param type the type of the term
   * @param value the value of the term
   */
  public CodeTerm(Type type, Object value, String code) {

    this.type = type;
    this.value = value;

    this.code = code;
  }

  /** Indicates whether this object is equal to o. */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CodeTerm)) {
      return false;
    }
    CodeTerm other = (CodeTerm) o;

    return this.type.equals(other.type) && Objects.equals(this.value, other.value)  && Objects.equals(this.code, other.code);
  }

  /** Returns a hash code value for this NonreceiverTerm. */
  @Override
  public int hashCode() {
    return this.type.hashCode() + (this.value == null ? 0 : this.value.hashCode()) + this.code.hashCode();
  }

  /** Returns string representation of this NonreceiverTerm. */
  @Override
  public String toString() {
    return Objects.toString(value);
  }

  @Override
  public String getName() {
    return this.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @return {@link NormalExecution} object enclosing value of this non-receiver term
   */
  @Override
  public ExecutionOutcome execute(Object[] statementInput) {
    assert statementInput.length == 0;
    return new NormalExecution(this.value, 0);
  }

  /**
   * {@inheritDoc}
   *
   * <p>For NonreceiverTerm, simply adds a code representation of the value to the string builder.
   * Note: this does not explicitly box primitive values.
   *
   * @param inputVars ignored
   * @param b {@link StringBuilder} to which string representation is appended
   */
  @Override
  public void appendCode(
      Type declaringType,
      TypeTuple inputTypes,
      Type outputType,
      List<Variable> inputVars,
      StringBuilder b) {
    //b.append(Value.toCodeString(getValue()));
    b.append(code);
  }

  /**
   * {@inheritDoc}
   *
   * @return value of this {@link NonreceiverTerm}
   */
  @Override
  public Object getValue() {
    return value;
  }

  /**
   * Return the type.
   *
   * @return the type
   */
  public Type getType() {
    return this.type;
  }

  public String getCode() {
    return code;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns a string representing this primitive declaration. The string is of the form:<br>
   * {@code TYPE:VALUE}<br>
   * Where TYPE is the type of the primitive declaration, and VALUE is its value. If VALUE is "null"
   * then the value is null (not the String "null"). If TYPE is "char" then
   * (char)Integer.parseInt(VALUE, 16) yields the character value.
   *
   * <p>Examples:
   *
   * <pre>
   * String:null                  represents: String x = null
   * java.lang.String:""          represents: String x = "";
   * String:""                    represents: String x = "";
   * String:" "                   represents: String x = " ";
   * String:"\""                  represents: String x = "\"";
   * String:"\n"                  represents: String x = "\n";
   * String:"\u263A"              represents: String x = "\u263A";
   * java.lang.Object:null        represents: Object x = null;
   * [[Ljava.lang.Object;:null    represents: Object[][] = null;
   * int:0                        represents: int x = 0;
   * boolean:false                represents: boolean x = false;
   * char:20                      represents: char x = ' ';
   * </pre>
   *
   * Note that a string type can be given as both "String" or "java.lang.String".
   *
   * @return string representation of primitive, String or null value
   */
  @Override
  public String toParsableString(Type declaringType, TypeTuple inputTypes, Type outputType) {

    String valStr;
    if (value == null) {
      valStr = "null";
    } else if (type.equals(JavaTypes.CHAR_TYPE)) {
      valStr = Integer.toHexString((Character) value);
    } else if (type.equals(JavaTypes.CLASS_TYPE)) {
      valStr = ((Class<?>) value).getName() + ".class";
    } else {
      valStr = value.toString();
      if (type.isString()) {
        valStr = "\"" + StringsPlume.escapeJava(valStr) + "\"";
      }
    }

    return type.getBinaryName() + ":" + valStr;
  }

  /**
   * {@inheritDoc}
   *
   * @return true, since all of objects are non-receivers
   */
  @Override
  public boolean isNonreceivingValue() {
    return false;
  }
}
