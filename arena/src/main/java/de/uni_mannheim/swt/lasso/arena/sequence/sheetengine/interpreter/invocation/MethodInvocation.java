package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

/**
 * A method invocation.
 *
 * @author Marcus Kessel
 */
public class MethodInvocation extends MemberInvocation {

    private static final Logger LOG = LoggerFactory
            .getLogger(MethodInvocation.class);

    public MethodInvocation(int index) {
        super(index);
    }

    public Method getMethod() {
        return (Method) getMember();
    }

    @Override
    public void execute(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation) {
        Invocations invocations = executedInvocations.getInvocations();
        // is CUT?
        Member member = getMember();
        Class targetClass = member.getDeclaringClass();
        boolean cut = false;
        if(invocations.getInterfaceSpecifications().containsKey(targetClass.getCanonicalName())) {
            cut = true;

            LOG.debug("Found cut '{}'", targetClass);
        }

        Method method = getMethod();

        // invoke method
        Parameter target = getTarget();

        // either value (object) or reference
        List<Object> inputs = resolveInputs(invocations, executedInvocations);

        // --- START ADAPTER LOGIC
        // FIXME pre-produce code a) (GoF adapter) or b) adapt dynamically
        // CUT: adapted method invocation
        if(cut) {
            ExecutedInvocation ref = executedInvocations.getExecutedInvocation(target.getReference()[0]);
            Object instance = ref.getOutput().getValue();

            MethodSignature methodSig = invocations.resolve(method);
            // FIXME dangerous cast
            PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
            AdaptedMethod adaptedMethod = pImpl.resolveAdaptedMethod(
                    invocations.getInterfaceSpecifications().get(targetClass.getCanonicalName()),
                    methodSig);

            // FIXME adaptation logic
            try {
                Method adMethod = adaptedMethod.getMethod();

                if(!adMethod.isAccessible()) {
                    adMethod.setAccessible(true);
                }
                Runner runner = new Runner();
                Object out = runner.run(() -> adMethod.invoke(instance, inputs.toArray()));
                executedInvocation.setOutput(Output.fromValue(out));
                executedInvocation.setExecutionTime(runner.getStopWatch().getExecutionNanoTime());

                LOG.debug("cut method call '{}'", executedInvocation.getOutput().getValue());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            // --- END ADAPTER LOGIC
        } else {
            // method invocation
            ExecutedInvocation ref = executedInvocations.getExecutedInvocation(target.getReference()[0]);
            Object instance = ref.getOutput().getValue();
            try {
                if(!method.isAccessible()) {
                    method.setAccessible(true);
                }

                Runner runner = new Runner();
                Object out = runner.run(() -> method.invoke(instance, inputs.toArray()));
                executedInvocation.setOutput(Output.fromValue(out));
                executedInvocation.setExecutionTime(runner.getStopWatch().getExecutionNanoTime());

                LOG.debug("non-cut method call '{}'", executedInvocation.getOutput().getValue());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toCode() {
        // FIXME
        return getMethod().toGenericString();
    }
}
