package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.ssn.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.CodeInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.InstanceInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.MethodInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.SheetSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;

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

    private final Test test;
    private final Eval eval;
    private final Map<Member, MethodSignature> resolvedMappings;
    private final TestInvocation testInvocation;

    private List<Invocation> sequence = new ArrayList<>();

    public Invocations(Test test, TestInvocation testInvocation, Map<Member, MethodSignature> resolvedMappings, Eval eval) {
        this.test = test;
        this.testInvocation = testInvocation;
        this.resolvedMappings = resolvedMappings;
        this.eval = eval;
    }

    public MethodInvocation createMethodInvocation() {
        MethodInvocation invocation = new MethodInvocation(sequence.size());
        sequence.add(invocation);

        return invocation;
    }

    public InstanceInvocation createInstanceInvocation() {
        InstanceInvocation invocation = new InstanceInvocation(sequence.size());
        sequence.add(invocation);

        return invocation;
    }

    public CodeInvocation createCodeInvocation() {
        CodeInvocation invocation = new CodeInvocation(sequence.size());
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

    public Eval getEval() {
        return eval;
    }

    public Map<Member, MethodSignature> getResolvedMappings() {
        return resolvedMappings;
    }

    public MethodSignature resolve(Member member) {
        return resolvedMappings.get(member);
    }

    public ParsedSheet getParsedSheet() {
        return test.getParsedSheet();
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return test.getInterfaceSpecification();
    }

    public SheetSignature getSheetSignature() {
        return test.getSignature();
    }

    public TestInvocation getTestInvocation() {
        return testInvocation;
    }

    public Test getTest() {
        return test;
    }
}
