package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Marcus Kessel
 */
public class CallableSequence {

    private List<CallableStatement> statements = new ArrayList<>();

    public List<CallableStatement> getStatements() {
        return statements;
    }

    /**
     * Each non-"inline" {@link CallableStatement#isInline()}
     *
     * @param i
     * @return
     */
    public CallableStatement getRowStatement(int i) {
        return getRowStatements().get(i);
    }

    public List<CallableStatement> getRowStatements() {
        return statements.stream().filter(s -> !s.isInline()).collect(Collectors.toList());
    }

    public CallableStatement fromCode(String code) {
        CallableStatement callableStatement = new CallableStatement(statements.size(), code);
        statements.add(callableStatement);

        return callableStatement;
    }

    public CallableStatement fromConstant(String code) {
        CallableStatement callableStatement = new CallableStatement(statements.size(), code);
        callableStatement.setInline(true);

        statements.add(callableStatement);

        return callableStatement;
    }

    public CallableStatement fromCode(String code, List<CallableStatement> inputs) {
        CallableStatement callableStatement = new CallableStatement(statements.size(), code);
        callableStatement.setInputs(inputs);

        statements.add(callableStatement);

        return callableStatement;
    }

    public CallableStatement fromCode(String code, InterfaceSpecification interfaceSpecification, int operation, List<CallableStatement> inputs) {
        CallableStatement callableStatement = new CallableStatement(statements.size(), code);
        callableStatement.setInputs(inputs);

        statements.add(callableStatement);

        callableStatement.setInterfaceSpecification(interfaceSpecification);
        callableStatement.setOperationId(operation);

        return callableStatement;
    }

    public CallableStatement fromCode(String code, InterfaceSpecification interfaceSpecification, int operation) {
        CallableStatement callableStatement = new CallableStatement(statements.size(), code);

        statements.add(callableStatement);

        callableStatement.setInterfaceSpecification(interfaceSpecification);
        callableStatement.setOperationId(operation);

        return callableStatement;
    }
}
