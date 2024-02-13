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
package de.uni_mannheim.swt.lasso.arena.sequence;

import java.math.BigDecimal;

/**
 * A value statement.
 *
 * @author Marcus Kessel
 */
public class ValueStatement extends SpecificationStatement {

    private final Class<?> type;
    private final Object value;

    private String code;

    private boolean alias;

    /**
     * FIXME add for late-evaluation using Eval.me
     */
    private String javaExpression;

    public ValueStatement(Class<?> type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getValue() {
        // workaround
        if(type != null && type.equals(double.class)) {
            if(value != null && value.getClass().equals(BigDecimal.class)) {
                return ((BigDecimal) value).doubleValue();
            }
        }

        return value;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean isArray() {
        return type != null && type.isArray();
    }

    public boolean isAlias() {
        return alias;
    }

    @Override
    public boolean isClassUnderTest() {
        return false;
    }

    @Override
    public String toString() {
        return "type=" + type + ", value=" + value;
    }

    public void setAlias(boolean alias) {
        this.alias = alias;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
