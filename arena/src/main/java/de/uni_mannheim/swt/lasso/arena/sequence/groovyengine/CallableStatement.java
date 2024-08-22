package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class CallableStatement {

    private int index;
    private String code;

    private List<CallableStatement> inputs = Collections.emptyList();

    private InterfaceSpecification interfaceSpecification;
    private int operationId;

    public CallableStatement(int index, String code) {
        this.index = index;
        this.code = code;
    }

    public MethodSignature getMethod() {
        return interfaceSpecification.getMethods().get(operationId);
    }

    public MethodSignature getConstructor() {
        return interfaceSpecification.getConstructors().get(operationId);
    }

    public String getCode() {
        return code;
    }

    public int getIndex() {
        return index;
    }

    public List<CallableStatement> getInputs() {
        return inputs;
    }

    public void setInputs(List<CallableStatement> inputs) {
        this.inputs = inputs;
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(InterfaceSpecification interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public int getOperationId() {
        return operationId;
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
    }
}
