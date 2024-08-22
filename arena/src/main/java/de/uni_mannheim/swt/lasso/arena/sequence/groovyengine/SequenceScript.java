package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import groovy.lang.GroovyInterceptable;
import groovy.lang.Script;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 */
public abstract class SequenceScript extends Script implements GroovyInterceptable {

    private static final Logger LOG = LoggerFactory.getLogger(SequenceScript.class);

    public static final String NEW_INSTANCE_OPERATION = "create";

    @Override
    public Object invokeMethod(String name, Object args) {
        LOG.debug("Resolving method => '{}' with args '{}'", name, Arrays.toString((Object[]) args));

        try {
            return super.invokeMethod(name, args);
        } catch (groovy.lang.MissingMethodException e) {
            LOG.warn("Method not found", e);
        }

        // EITHER: "create" or "ClassName()" allowed
        ExecutionContext context = getExecutionContext();

        boolean isCreate = StringUtils.equalsIgnoreCase(NEW_INSTANCE_OPERATION, name) ||
                context.getTestSpec().getInterfaces().containsKey(name);
        if(isCreate) {
            return onCreate((Object[]) args);
        }

        // normal method
        return onMethod(name, (Object[]) args);
    }

    /**
     * On create new instance
     *
     * @param args
     * @return
     */
    public Object onCreate(Object[] args) {
        ExecutionContext context = getExecutionContext();

        AdaptedImplementation implementation = context.getImplementation();
        AdaptedInitializer initializer = implementation.getDefaultInitializer();

        LOG.debug("{}", initializer);

        ExecutedStatement statementUnderExecution = context.getExecutedSequence().getCurrentExecutedStatement();

        CallableStatement callableStatement = statementUnderExecution.getCallableStatement();

        // get additional inputs from sequence statement
        int start = 0;
        Object[] inputs = handleInputs(callableStatement, statementUnderExecution, args, start);

        Object result = null;
        try {
            result = InvokerHelper.invokeConstructorOf(initializer.getAdaptee().loadClassUnsafe(), inputs);
        } catch (groovy.lang.MissingMethodException e) {
            println("Wasn't able to invoke method");
            println(e);
        }

        return result;
    }

    public Object onMethod(String name, Object[] args) {
        ExecutionContext context = getExecutionContext();

        ExecutedStatement statementUnderExecution = context.getExecutedSequence().getCurrentExecutedStatement();

        CallableStatement callableStatement = statementUnderExecution.getCallableStatement();

        // self/this (always first parameter)
        CallableStatement self = callableStatement.getInputs().get(0);
        println(self.getIndex());
        ExecutedStatement selfInstance = context.getExecutedSequence().getStatements().get(self.getIndex());

        // get additional inputs from sequence statement
        int start = 1; // 0 is self/this
        Object[] inputs = handleInputs(callableStatement, statementUnderExecution, args, start);

        // identify which method is called
        InterfaceSpecification interfaceSpecification = callableStatement.getInterfaceSpecification();
        int methodSignatureMatch = callableStatement.getOperationId();
                //int methodSignatureMatch = -1;
//        for(int m = 0; m < interfaceSpecification.getMethods().size(); m++) {
//            MethodSignature methodSignature = interfaceSpecification.getMethods().get(m);
//            if(StringUtils.equalsIgnoreCase(name, methodSignature.getName())) {
//                methodSignatureMatch = m;
//
//                LOG.debug("Matched {}", methodSignature.toLQL());
//
//                break;
//            }
//        }

        //
        AdaptedImplementation implementation = context.getImplementation();
        AdaptedMethod method = implementation.getMethod(interfaceSpecification, methodSignatureMatch);

        LOG.debug("{}", method);

        Object result = null;
        try {
            // does not work
            //result = this.getMetaClass().invokeMethod(selfInstance.getOutput(), name, args);
            // here's the code from InvokerHelper
            /*
               static Object invokePojoMethod(Object object, String methodName, Object arguments) {
                    MetaClass metaClass = InvokerHelper.getMetaClass(object);
                    return metaClass.invokeMethod(object, methodName, asArray(arguments));
                }
             */

            // use our own inputs (from sequence statement specification)
            result = InvokerHelper.invokeMethod(selfInstance.getOutput(), name, inputs);
        } catch (groovy.lang.MissingMethodException e) {
            println("Wasn't able to invoke method");
            println(e);
        }

        return result;
    }

    Object[] handleInputs(CallableStatement callableStatement, ExecutedStatement statementUnderExecution, Object[] args, int start) {
        ExecutionContext context = getExecutionContext();

        // get additional inputs from sequence statement
        Object[] inputs = null;
        if(callableStatement.getInputs().size() > start) {
            inputs = callableStatement.getInputs().subList(start, callableStatement.getInputs().size()).stream()
                    .map(i -> context.getExecutedSequence().getStatements().get(i.getIndex()).getOutput())
                    .toArray();
        }

        // serialize inputs
        if(ArrayUtils.isEmpty(inputs) && ArrayUtils.isNotEmpty(args)) {
            inputs = args;
        }

        if(Objects.nonNull(inputs)) {
            Arrays.stream(inputs).forEach(statementUnderExecution::addInput);
        }

        return inputs;
    }

    ExecutionContext getExecutionContext() {
        return (ExecutionContext) getProperty("_sequenceCtx");
    }
}
