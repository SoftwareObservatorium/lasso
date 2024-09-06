package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;

import java.lang.reflect.Member;

/**
 *
 * @author Marcus Kessel
 */
public abstract class MemberInvocation extends Invocation {

    private Member member;

    public MemberInvocation(int index) {
        super(index);
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    protected boolean isCut(Invocations invocations) {
        Member member = getMember();
        Class targetClass = member.getDeclaringClass();

        return CutUtils.isFaCut(invocations, targetClass);
    }
}
