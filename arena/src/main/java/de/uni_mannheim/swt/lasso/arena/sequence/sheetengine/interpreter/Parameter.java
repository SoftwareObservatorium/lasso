package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Represents an (input) parameter by its type and value.
 *
 * @author Marcus Kessel
 */
public class Parameter {

    private Class targetClass;
    private String expression;

    private Object value;

    private int[] reference;

    public Parameter(Class targetClass, String expression, Object value) {
        this.targetClass = targetClass;
        this.expression = expression;
        this.value = value;
    }

    public Parameter(int[] reference, Class targetClass, String expression, Object value) {
        this.reference = reference;
        this.targetClass = targetClass;
        this.expression = expression;
        this.value = value;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public String getExpression() {
        return expression;
    }

    public Object getValue() {
        return value;
    }

    public boolean isReference() {
        return ArrayUtils.isNotEmpty(reference);
    }

    public int[] getReference() {
        return reference;
    }

    /**
     * No value defined?
     *
     * @return
     */
    public boolean isUndefined() {
        return StringUtils.isEmpty(expression);
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "targetClass=" + targetClass +
                ", expression='" + expression + '\'' +
                ", value=" + value +
                ", reference=" + Arrays.toString(reference) +
                '}';
    }
}
