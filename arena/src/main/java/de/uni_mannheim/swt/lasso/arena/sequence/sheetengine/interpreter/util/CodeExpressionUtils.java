package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Marcus Kessel
 */
public class CodeExpressionUtils {

    /**
     * Produce clean Java code from expressions in JSON notation.
     *
     * @param expression
     * @return
     */
    public static String cleanExpression(String expression) {
        if(StringUtils.startsWith(expression, "'") && StringUtils.endsWith(expression, "'")) {
            expression = StringUtils.wrap(StringUtils.substringBetween(expression, "'"), "\"");
        }

        return expression;
    }
}
