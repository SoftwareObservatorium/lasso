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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.gai.eval;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * Evaluate Java expressions using Groovy.
 *
 * @author Marcus Kessel
 */
public class Eval {

    private final ClassLoader classLoader;

    public Eval(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Eval() {
        this(Eval.class.getClassLoader());
    }

    /**
     * Evaluate Java expression using given {@link ClassLoader}.
     *
     * @param expression
     * @return
     */
    public Object expr(String expression) {
        Binding sharedData = new Binding();

        // make Groovy string since "" is evaluated differently to Java strings
        if(StringUtils.startsWith(expression, "\"") && StringUtils.endsWith(expression, "\"")) {
            StringBuilder sb = new StringBuilder(expression);
            sb.replace(0, 1, "'''");
            sb.replace(sb.length() - 1, sb.length(), "'''");
            return expr(sb.toString());
        }

        if(!StringUtils.startsWithAny(expression, "\"", "'")) {
            if(StringUtils.contains(expression, "{")) {
                // this must be an array
                if(StringUtils.contains(expression, "new")) {
                    String arr = StringUtils.substringAfter(expression, "new ");

                    String decl = StringUtils.substringBeforeLast(arr, "]") + "]";
                    String init = StringUtils.substringAfterLast(arr, "]");

                    // groovy doesn't work well with Java arrays ("{}" is reserved for closures
                    return expr(String.format("(%s) %s", decl, getAsGroovyArray(init)));
                } else {
                    // groovy doesn't work well with Java arrays ("{}" is reserved for closures
                    throw new UnsupportedOperationException("Short notation not supported");
                }
            }
        }

        GroovyShell shell = new GroovyShell(classLoader, sharedData);

        Object result = shell.evaluate(expression);

        if(result instanceof BigDecimal) {
            BigDecimal d = (BigDecimal) result;

            // FIXME do something
        }

        return result;
    }

    /**
     * Evaluate raw Java expression using given {@link Container}.
     *
     * @param expression
     * @return
     */
    public Object exprRaw(String expression) {
        Binding sharedData = new Binding();

        GroovyShell shell = new GroovyShell(classLoader, sharedData);

        Object result = shell.evaluate(expression);

        if(result instanceof BigDecimal) {
            BigDecimal d = (BigDecimal) result;

            // FIXME do something
        }

        return result;
    }

    public String getAsGroovyArray(String expression) {
        StringBuilder sb = new StringBuilder(expression);

        boolean strOpen = false;
        for(int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if(c == '=') {
                strOpen = !strOpen;
            }
            if(c == '{') {
                if(!strOpen) {
                    sb.replace(i, i + 1, "[");
                }
            }
            if(c == '}') {
                if(!strOpen) {
                    sb.replace(i, i + 1, "]");
                }
            }
        }

        return sb.toString();
    }
}
