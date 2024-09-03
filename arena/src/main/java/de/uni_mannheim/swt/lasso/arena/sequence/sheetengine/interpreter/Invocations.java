package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import bsh.Interpreter;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a sequence of {@link Invocation}s.
 *
 * @author Marcus Kessel
 */
public class Invocations {

    private final Map<String, InterfaceSpecification> interfaceSpecifications;
    private final ParsedSheet parsedSheet;
    private final Interpreter bsh;
    private final Map<Member, MethodSignature> resolvedMappings;

    private List<Invocation> sequence = new ArrayList<>();

    public Invocations(Map<String, InterfaceSpecification> interfaceSpecifications, ParsedSheet parsedSheet, Map<Member, MethodSignature> resolvedMappings, Interpreter bsh) {
        this.interfaceSpecifications = interfaceSpecifications;
        this.parsedSheet = parsedSheet;
        this.resolvedMappings = resolvedMappings;
        this.bsh = bsh;
    }

    public Invocation create() {
        Invocation invocation = new Invocation(sequence.size());
        sequence.add(invocation);

        return invocation;
    }

    public Invocation getInvocation(int index) {
        return sequence.get(index);
    }

    public List<Invocation> getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Invocation invocation : getSequence()) {
            sb.append(invocation.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public Interpreter getBsh() {
        return bsh;
    }

    public Map<String, InterfaceSpecification> getInterfaceSpecifications() {
        return interfaceSpecifications;
    }

    public Map<Member, MethodSignature> getResolvedMappings() {
        return resolvedMappings;
    }

    public MethodSignature resolve(Member member) {
        return resolvedMappings.get(member);
    }
}
