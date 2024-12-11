package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.LQLMethodSignature;
import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The signature of a sheet
 *
 * @author Marcus Kessel
 */
public class SheetSignature {

    private final MethodSignature method;

    public SheetSignature(MethodSignature method) {
        this.method = method;
    }

    public MethodSignature getMethod() {
        return method;
    }

    public String getName() {
        return method.getName();
    }

    public int getParameterSize() {
        return method.getInputs().size();
    }

    public LQLMethodSignature resolveMethodSignature() {
        return new LQLMethodSignature(new InterfaceSpecification(), new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", method.getName(),
                method.getInputs(), method.getOutputs().get(0)));
    }

    public String toLQL() {
        StringBuilder sb = new StringBuilder();

        sb.append(getName());
        sb.append("(");
        if(CollectionUtils.isNotEmpty(method.getInputNames())) {
            List<String> p = new ArrayList<>(method.getInputNames().size());
            for(int i = 0; i < getParameterSize(); i++) {
                p.add(method.getInputNames().get(i) + "=" + method.getInputs().get(i));
            }

            sb.append(String.join(",", p));
        } else {
            sb.append(String.join(",", method.toParameterString()));
        }

        sb.append(")");

        return sb.toString();
    }
}
