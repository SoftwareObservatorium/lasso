package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        if(invocations.getInterfaceSpecifications().containsKey(targetClass.getCanonicalName())) {
            cut = true;

            LOG.debug("Found cut '{}'", targetClass);
        }

        // either value (object) or reference
        List<Object> inputs = resolveInputs(invocations, executedInvocations);

        Constructor constructor = getAsConstructor();

        // --- START ADAPTER LOGIC
        // FIXME pre-produce code a) (GoF adapter) or b) adapt dynamically
        // CUT: adapted constructor call
        if(cut) {
            // FIXME adapt delegate
            MethodSignature constructorSig = invocations.resolve(constructor);
            // FIXME dangerous cast
            PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
            AdaptedInitializer adaptedInitializer = pImpl.resolveAdaptedInitializer(
                    invocations.getInterfaceSpecifications().get(targetClass.getCanonicalName()),
                    constructorSig);

            try {
                //
                Constructor adaptedConstructor = adaptedInitializer.getAsConstructor();
                if(!adaptedConstructor.isAccessible()) {
                    adaptedConstructor.setAccessible(true);
                }

                Runner runner = new Runner();
                Object instance = runner.run(() -> adaptedConstructor.newInstance(inputs.toArray()));
                executedInvocation.setOutput(Output.fromValue(instance));
                executedInvocation.setExecutionTime(runner.getStopWatch().getExecutionNanoTime());

                LOG.debug("cut constructor '{}'", executedInvocation.getOutput().getValue());
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            // --- END ADAPTER LOGIC
        } else {
            try {
                if(!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }

                Runner runner = new Runner();
                Object instance = runner.run(() -> constructor.newInstance(inputs.toArray()));
                executedInvocation.setOutput(Output.fromValue(instance));
                executedInvocation.setExecutionTime(runner.getStopWatch().getExecutionNanoTime());

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
