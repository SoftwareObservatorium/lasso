package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.InvocationInterceptor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.ExecutionResult;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run.Runner;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.List;

/**
 * An instance invocation (e.g., new etc.)
 *
 * @author Marcus Kessel
 */
public class InstanceInvocation extends MemberInvocation {

    private static final Logger LOG = LoggerFactory
            .getLogger(InstanceInvocation.class);

    public InstanceInvocation(int index) {
        super(index);
    }

    public Constructor getAsConstructor() {
        return (Constructor) getMember();
    }

    public void execute(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation) {
        Invocations invocations = executedInvocations.getInvocations();
        // is CUT?
        Member member = getMember();
        Class targetClass = member.getDeclaringClass();
        boolean cut = false;
        if(CutUtils.isCut(invocations, targetClass)) {
            cut = true;

            LOG.debug("Found cut '{}'", targetClass);
        }

        // either value (object) or reference
        List<Object> inputs = resolveInputs(invocations, executedInvocations);

        Constructor constructor = getAsConstructor();

        // FIXME pre-produce code a) (GoF adapter) or b) adapt dynamically
        // CUT: adapted constructor call
        if(cut) {
            // we need one interceptor for EACH instance
            InvocationInterceptor invocationInterceptor = new InvocationInterceptor(executedInvocations, adaptedImplementation, invocations.getInterfaceSpecifications().get(targetClass.getCanonicalName()));
            Object proxyInstance = invocationInterceptor.create(executedInvocation, constructor, inputs.toArray());
            executedInvocation.setInterceptor(invocationInterceptor);
            LOG.debug("created proxy {}", proxyInstance.getClass().getCanonicalName());
        } else {
            try {
                if(!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }

                Runner runner = new Runner();
                ExecutionResult result = runner.run(() -> constructor.newInstance(inputs.toArray()));
                executedInvocation.setOutput(Output.fromValue(result.getValue()));
                executedInvocation.setExecutionTime(result.getDurationNanos());

                LOG.debug("non-cut constructor '{}'", executedInvocation.getOutput().getValue());
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toCode() {
        // FIXME
        return getMember().toString();
    }
}
