package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.read;

import de.uni_mannheim.swt.lasso.arena.check.Oracle;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.CallableSequence;

import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class TestSpec {

    private String name;

    private CallableSequence callableSequence;
    private Map<String, InterfaceSpecification> interfaces;

    private Oracle oracle;

    public CallableSequence getCallableSequence() {
        return callableSequence;
    }

    public void setCallableSequence(CallableSequence callableSequence) {
        this.callableSequence = callableSequence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Oracle getOracle() {
        return oracle;
    }

    public void setOracle(Oracle oracle) {
        this.oracle = oracle;
    }

    public Map<String, InterfaceSpecification> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Map<String, InterfaceSpecification> interfaces) {
        this.interfaces = interfaces;
    }
}
