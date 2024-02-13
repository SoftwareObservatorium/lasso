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

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.llm.test.Value;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 *
 * @author Marcus Kessel
 */
public class EvalAndSerialize {

    private Gson gson = new Gson();

    public Object deserializePair(String json, Container container) throws ClassNotFoundException {
        return gson.fromJson(json, container.loadClass("org.javatuples.Pair"));
    }

    public Value evalAndSerializeToJson(String expression, ClassLoader classLoader) {
        Object valueObj = expr(expression, classLoader);

        Value value = new Value();
        value.setValue(valueObj);

        if(valueObj != null) {
            value.setType(valueObj.getClass().getName());
        }

        //String json = gson.toJson(valueObj);
        value.setCode(expression);

        return value;
    }

    /**
     * Evaluate Java expression using given {@link Container}.
     *
     * @param expression
     * @return
     */
    public Object expr(String expression, ClassLoader classLoader) {
        Binding sharedData = new Binding();
        // required by MultiPLE
        CompilerConfiguration cc = new CompilerConfiguration();

        if(StringUtils.contains(expression, "Pair")) {
            ImportCustomizer c = new ImportCustomizer();
            c.addStarImports("org.javatuples");
            cc.addCompilationCustomizers(c);
        }

//        // make Groovy string since "" is evaluated differently to Java strings
//        if(StringUtils.startsWith(expression, "\"") && StringUtils.endsWith(expression, "\"")) {
//            StringBuilder sb = new StringBuilder(expression);
//            sb.replace(0, 1, "'''");
//            sb.replace(sb.length() - 1, sb.length(), "'''");
//            return expr(sb.toString());
//        }
//
//        if(!StringUtils.startsWithAny(expression, "\"", "'")) {
//            if(StringUtils.contains(expression, "{")) {
//                // this must be an array
//                if(StringUtils.contains(expression, "new")) {
//                    String arr = StringUtils.substringAfter(expression, "new ");
//
//                    String decl = StringUtils.substringBeforeLast(arr, "]") + "]";
//                    String init = StringUtils.substringAfterLast(arr, "]");
//
//                    // groovy doesn't work well with Java arrays ("{}" is reserved for closures
//                    return expr(String.format("(%s) %s", decl, getAsGroovyArray(init)));
//                } else {
//                    // groovy doesn't work well with Java arrays ("{}" is reserved for closures
//                    throw new UnsupportedOperationException("Short notation not supported");
//                }
//            }
//        }

        // HumanEval 161 - Groovy $s need to be escaped (because of slashy strings)
        if(StringUtils.contains(expression, '$')) {
            expression = StringUtils.replace(expression, "$", "\\$");
        }

        GroovyShell shell = new GroovyShell(classLoader, sharedData, cc);
        Object result = shell.evaluate(expression);

        return result;
    }
}
