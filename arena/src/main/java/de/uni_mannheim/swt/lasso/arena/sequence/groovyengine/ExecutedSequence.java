package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Marcus Kessel
 */
public class ExecutedSequence {

    private List<ExecutedStatement> statements;

    public void addStatement(ExecutedStatement executedStatement) {
        statements.add(executedStatement);
    }

    public int getNoOfExecutedStatements() {
        return statements.size();
    }

    public ExecutedStatement getCurrentExecutedStatement() {
        return statements.get(statements.size() - 1);
    }

    public ExecutedStatement getStatement(int index) {
        return statements.get(index);
    }

    public List<ExecutedStatement> getRowStatements() {
        return statements.stream().filter(s -> !s.getCallableStatement().isInline()).collect(Collectors.toList());
    }

    public List<ExecutedStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<ExecutedStatement> statements) {
        this.statements = statements;
    }
}
