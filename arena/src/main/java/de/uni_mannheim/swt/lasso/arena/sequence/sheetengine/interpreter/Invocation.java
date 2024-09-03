package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Member;
import java.util.List;

/**
 * Represents an invocation of an operation (e.g., method etc.).
 *
 * @author Marcus Kessel
 */
public class Invocation {

    private final int index;

    private Member member;

    private Class targetClass;
    private Parameter target;

    private List<Parameter> parameters;

    private String codeExpression;

    public Invocation(int index) {
        this.index = index;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "Invocation{" +
                "index=" + index +
                ", member=" + member +
                ", targetClass=" + targetClass +
                ", parameters=" + parameters +
                ", codeExpression=" + codeExpression +
                '}';
    }

    public Parameter getTarget() {
        return target;
    }

    public void setTarget(Parameter target) {
        this.target = target;
    }

    public String getCodeExpression() {
        return codeExpression;
    }

    public void setCodeExpression(String codeExpression) {
        this.codeExpression = codeExpression;
    }

    public boolean isCodeExpression() {
        return StringUtils.isNotBlank(codeExpression);
    }
}
