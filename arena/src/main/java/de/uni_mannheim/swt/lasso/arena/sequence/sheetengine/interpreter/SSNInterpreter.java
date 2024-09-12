package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.BshEval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.Eval;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval.EvalException;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.CodeInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.InstanceInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.MethodInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CodeExpressionUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.FAMarker;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.MemberResolutionUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedCell;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedRow;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SheetResolver;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sequence Sheet Notation backed by {@link Eval} instance.
 *
 * @author Marcus Kessel
 */
public class SSNInterpreter {

    private static final Logger LOG = LoggerFactory
            .getLogger(SSNInterpreter.class);

    /**
     * Alias because of historical reasons
     */
    public static final String CREATE = "create";

    public static final String $_CREATE = "$create";
    public static final String $_EVAL = "$eval";

    /**
     * Interpret sheet specifications.
     *
     * @param parsedSheet
     * @param interfaceSpecificationMap
     * @return
     */
    public Invocations interpret(ParsedSheet parsedSheet, Map<String, InterfaceSpecification> interfaceSpecificationMap) {
        return interpret(parsedSheet, interfaceSpecificationMap, SSNInterpreter.class.getClassLoader());
    }

    /**
     * Interpret sheet specifications.
     *
     * @param parsedSheet
     * @param interfaceSpecificationMap
     * @param classUnderTest
     * @return
     */
    public Invocations interpret(ParsedSheet parsedSheet, Map<String, InterfaceSpecification> interfaceSpecificationMap, ClassUnderTest classUnderTest) {
        return interpret(parsedSheet, interfaceSpecificationMap, classUnderTest.getProject().getContainer());
    }

    /**
     * Interpret
     *
     * @param parsedSheet
     * @param interfaceSpecificationMap
     * @param classLoader
     * @return
     */
    public Invocations interpret(ParsedSheet parsedSheet, Map<String, InterfaceSpecification> interfaceSpecificationMap, ClassLoader classLoader) {
        // our interpreter that holds signatures on the fly for resolution
        Eval eval = new BshEval();
        eval.setClassLoader(classLoader);

        return interpret(parsedSheet, interfaceSpecificationMap, eval);
    }

    /**
     * Interpret sheet specifications.
     *
     * @param parsedSheet
     * @param interfaceSpecificationMap
     * @param eval
     * @return
     */
    public Invocations interpret(ParsedSheet parsedSheet, Map<String, InterfaceSpecification> interfaceSpecificationMap, Eval eval) {
        // all LQL specs to Java (here classes)
        Map<Member, MethodSignature> resolvedMappings = lqlToJava(eval, interfaceSpecificationMap);

        Invocations invocations = new Invocations(interfaceSpecificationMap, parsedSheet, resolvedMappings, eval);

        // now build the call sequence
        for(ParsedRow row : parsedSheet.getRows()) {
            // TODO oracle values
            ParsedCell output = row.getOutput();
            ParsedCell operation = row.getOperation();
            List<ParsedCell> inputs = row.getInputs();

            // operation name
            String operationName = operation.getNodeValue().textValue();
            // either points to class name to instantiate (with "create" operation) or reference to object
            ParsedCell clazzCell = inputs.get(0);
            String clazz = clazzCell.getNodeValue().textValue();
            List<ParsedCell> inputArgs = new LinkedList<>();
            if(inputs.size() > 1) {
                inputArgs = inputs.subList(1, inputs.size()); // remaining input parameters
            }

            Invocation invocation;
            if(StringUtils.equalsAnyIgnoreCase(operationName, CREATE, $_CREATE)) {
                // create invocation
                LOG.debug("create invocation = {}", operationName);

                String className = clazz;
                // create instance
                invocation = instanceInvocation(eval, invocations, className, inputArgs);
            } else if(StringUtils.equalsAnyIgnoreCase(operationName, $_EVAL)) {
                // code to evaluate

                // FIXME code to eval
                // -- use "eval" command? or some simple syntax like in SpEL #{ <expression string> }

                LOG.debug("code invocation = {}", operationName);
                CodeInvocation codeInvocation = codeInvocation(invocations, clazzCell);
                codeInvocation.setCommand(operationName);
                invocation = codeInvocation;
            } else {
                // method invocation
                LOG.debug("method invocation = {}", operationName);
                invocation = methodInvocation(eval, invocations, clazzCell, operationName, inputArgs);
            }

            // resolve expected output (i.e., oracle value)
            try {
                Parameter expectedOutput = resolveParameterType(eval, invocations, output);
                invocation.setExpectedOutput(expectedOutput);
            } catch (EvalException e) {
                throw new RuntimeException(e);
            }
        }

        return invocations;
    }

    /**
     * Execute {@link Invocations}.
     *
     * @param invocations
     * @param adaptedImplementation
     * @param executionListener
     * @return
     */
    public ExecutedInvocations run(Invocations invocations, AdaptedImplementation adaptedImplementation, InvocationVisitor executionListener) {
        //
        ExecutedInvocations executedInvocations = new ExecutedInvocations(invocations);

        try {
            LOG.debug("Execution listener 'visitBeforeSequence'");
            executionListener.visitBeforeSequence(executedInvocations, adaptedImplementation);
        } catch (Throwable e) {
            LOG.warn("Execution listener 'visitBeforeSequence' failed", e);
        }

        for(Invocation invocation : invocations.getSequence()) {
            ExecutedInvocation executedInvocation = executedInvocations.create(invocation);

            // either value (object) or reference
            List<Obj> inputs = resolveInputs(executedInvocations, executedInvocation);
            executedInvocation.setInputs(inputs);

            try {
                LOG.debug("Execution listener 'visitBeforeStatement'");
                executionListener.visitBeforeStatement(executedInvocations, executedInvocation.getInvocation().getIndex(), adaptedImplementation);
            } catch (Throwable e) {
                LOG.warn("Execution listener 'visitBeforeStatement' failed", e);
            }

            // execute invocation
            try {
                LOG.debug("Execution invocation '{}'", invocation.getClass().getCanonicalName());

                invocation.execute(executedInvocations, executedInvocation, adaptedImplementation);
            } catch (Throwable e) {
                LOG.warn("Execution invocation failed", e);
            }

            try {
                LOG.debug("Execution listener 'visitAfterStatement'");
                executionListener.visitAfterStatement(executedInvocations, executedInvocation.getInvocation().getIndex(), adaptedImplementation);
            } catch (Throwable e) {
                LOG.warn("Execution listener 'visitAfterStatement' failed", e);
            }
        }

        try {
            LOG.debug("Execution listener 'visitAfterSequence'");
            executionListener.visitAfterSequence(executedInvocations, adaptedImplementation);
        } catch (Throwable e) {
            LOG.warn("Execution listener 'visitAfterSequence' failed", e);
        }

        return executedInvocations;
    }

    /**
     * Translate LQL into a concrete Java class and return a mapping.
     *
     * @param eval
     * @param interfaceSpecificationMap
     * @return
     */
    Map<Member, MethodSignature> lqlToJava(Eval eval, Map<String, InterfaceSpecification> interfaceSpecificationMap) {
        Map<Member, MethodSignature> mappings = new LinkedHashMap<>();
        for(String clazz : interfaceSpecificationMap.keySet()) {
            mappings.putAll(lqlToJava(eval, interfaceSpecificationMap.get(clazz)));
        }

        return mappings;
    }

    /**
     * Translate LQL into a concrete Java class and return a mapping.
     *
     * @param eval
     * @param interfaceSpecification
     * @return
     */
    Map<Member, MethodSignature> lqlToJava(Eval eval, InterfaceSpecification interfaceSpecification) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ");
        sb.append(interfaceSpecification.getClassName());
        sb.append("{\n");

        if(CollectionUtils.isNotEmpty(interfaceSpecification.getConstructors())) {
            sb.append(interfaceSpecification.getConstructors().stream()
                    .map(this::toJava)
                    .collect(Collectors.joining("\n")));

            sb.append("\n");
        }
        if(CollectionUtils.isNotEmpty(interfaceSpecification.getMethods())) {
            sb.append(interfaceSpecification.getMethods().stream().map(this::toJava).collect(Collectors.joining("\n")));

            sb.append("\n");
        }

        sb.append("}");

        // evaluate
        String inter = "/* LQL */\n" + sb;

        LOG.info("Eval\n{}", inter);

        try {
            eval.eval(inter);
        } catch (EvalException e) {
            throw new RuntimeException(e);
        }

        // now resolve mappings
        try {
            Class clazz = eval.resolveClass(interfaceSpecification.getClassName());

            return Resolver.resolve(clazz, interfaceSpecification);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * LQL to Java class code.
     *
     * @param methodSignature
     * @return
     */
    String toJava(MethodSignature methodSignature) {
        StringBuilder sb = new StringBuilder();

        if(StringUtils.equalsIgnoreCase(methodSignature.getName(), "<init>")) {
            sb.append(methodSignature.getParent().getSimpleClassName());
        } else {
            try {
                sb.append(methodSignature.toReturnString());

                sb.append(" ");
                sb.append(methodSignature.getName());
            } catch (Throwable e) {
                System.err.println("invalid LQL: " + sb.toString());

                e.printStackTrace();

                throw e;
            }
        }

        sb.append("(");
        // FIXME hen/egg problem here .. just use name
        if(!ArrayUtils.isEmpty(methodSignature.getParameterTypes())) {
            String[] inputs = Arrays.stream(methodSignature.getParameterTypes())
                    .map(c -> {
                        if(c.equals(FAMarker.class)) {
                            return methodSignature.getParent().getClassName();
                        } else {
                            return c.getCanonicalName();
                        }
                    })
                    .toArray(String[]::new);
            List<String> in = new LinkedList<>();
            for(int i = 0; i < inputs.length; i++) {
                in.add(inputs[i] + " arg" + i);
            }

            sb.append(String.join(",", in));
        }

        sb.append("){}");

        return sb.toString();
    }

    /**
     * Create an Instance {@link Invocation} (e.g., new constructor etc.)
     *
     * @param eval
     * @param invocations
     * @param className
     * @param inputs
     * @return
     */
    InstanceInvocation instanceInvocation(Eval eval, Invocations invocations, String className, List<ParsedCell> inputs) {
        LOG.debug("classname = {}, inputs = {}", className, inputs);

        try {
            // resolve class
            Class resolvedClass = eval.resolveClass(className);
            LOG.debug("resolved clazz = {}", resolvedClass);

            InstanceInvocation invocation = invocations.createInstanceInvocation();
            invocation.setTargetClass(resolvedClass);

            // resolve parameters
            List<Parameter> parameters = resolveParameterTypes(eval, invocations, inputs);
            invocation.setParameters(parameters);

            Class[] types = resolveTypes(parameters);

            // resolve "constructor" (here just a declared placeholder)
            // FIXME what to do if static methods only and no instance necessary?
//            Constructor constructor = null;
//            try {
//                constructor = resolvedClass.getDeclaredConstructor(types);
//            } catch (NoSuchMethodException e) {
//                throw new RuntimeException(e);
//            } catch (SecurityException e) {
//                throw new RuntimeException(e);
//            }

            Constructor constructor = MemberResolutionUtils.resolveConstructor(resolvedClass, types);

            invocation.setMember(constructor);

            return invocation;

            // FIXME resolve constructor?
        } catch (ClassNotFoundException | EvalException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Class[] resolveTypes(List<Parameter> parameters) {
        // FIXME can happen that we don't know all types (e.g., null)
        Class[] types = parameters.stream().map(Parameter::getTargetClass).collect(Collectors.toList()).toArray(new Class[0]);

        return types;
    }

    /**
     * Create a method invocation
     *
     * @param eval
     * @param invocations
     * @param clazzCell
     * @param methodName
     * @param inputs
     * @return
     */
    MethodInvocation methodInvocation(Eval eval, Invocations invocations, ParsedCell clazzCell, String methodName, List<ParsedCell> inputs) {
        String clazz = clazzCell.getNodeValue().textValue();
        LOG.debug("this {}", clazz);

        // resolve
        Validate.isTrue(clazzCell.isValueReference(), "input cell must be object");
        //ParsedCell resolvedThis = parsedSheet.resolve(clazzCell.getKey());
        // [row,col]
        int[] coordinate = SheetResolver.resolveCellReference(clazz);

        LOG.debug("resolved coordinate {} for {}", Arrays.toString(coordinate), clazz);

        Invocation instanceInvocation = invocations.getInvocation(coordinate[0]);
        Class targetClass = instanceInvocation.getTargetClass();
        String className = targetClass.getCanonicalName();

        try {
            // resolve method
            Class resolvedClass = eval.resolveClass(className);
            LOG.debug("resolved class for method:\n {}", resolvedClass);

            MethodInvocation invocation = invocations.createMethodInvocation();
            invocation.setTargetClass(resolvedClass);

            // set target
            Parameter target = new Parameter(coordinate, targetClass, clazz, null);
            invocation.setTarget(target);

            // resolve parameters
            List<Parameter> parameters = resolveParameterTypes(eval, invocations, inputs);
            invocation.setParameters(parameters);

            // FIXME can happen that we don't know all types (e.g., null)
            Class[] types = parameters.stream().map(Parameter::getTargetClass).collect(Collectors.toList()).toArray(new Class[0]);

            // resolve "constructor" (here just a declared placeholder)
            // FIXME isAssignable (i.e., String->Object) - use more sophisticated matching here.
            Method method = MemberResolutionUtils.resolveMethod(resolvedClass, methodName, types);

            invocation.setMember(method);

            return invocation;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (EvalException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a code expression invocation.
     *
     * @param invocations
     * @param codeCell
     * @return
     */
    CodeInvocation codeInvocation(Invocations invocations, ParsedCell codeCell) {
        String codeExpression = codeCell.getNodeValue().textValue();

        CodeInvocation invocation = invocations.createCodeInvocation();
        invocation.setCodeExpression(codeExpression);

        // FIXME execute code expression to obtain Type .. is this desired to do upfront?
        try {
            Object outVal = CodeInvocation.evalCode(invocations.getEval(), codeExpression);

            Class type = outVal == null ? null : outVal.getClass();
            invocation.setTargetClass(type);
        } catch (EvalException e) {
            throw new RuntimeException(e);
        }

        invocation.setParameters(new LinkedList<>());

        return invocation;
    }

    /**
     * Resolve parameter types
     *
     * @param eval
     * @param invocations
     * @param args
     * @return
     * @throws EvalException
     */
    List<Parameter> resolveParameterTypes(Eval eval, Invocations invocations, List<ParsedCell> args) throws EvalException {
        List<Parameter> parameters = new ArrayList<>(args.size());

        for(ParsedCell arg : args) {
            Parameter parameter = resolveParameterType(eval, invocations, arg);
            parameters.add(parameter);
        }

        return parameters;
    }

    /**
     * Resolve a parameter type.
     *
     * @param eval
     * @param invocations
     * @param arg
     * @return
     * @throws EvalException
     */
    Parameter resolveParameterType(Eval eval, Invocations invocations, ParsedCell arg) throws EvalException {
        // two cases
        // reference to be resolved
        if(arg.isValueReference()) {
            int[] coordinate = SheetResolver.resolveCellReference(arg.getNodeValue().textValue());
            Invocation invocation = invocations.getInvocation(coordinate[0]);

            return new Parameter(coordinate, invocation.getTargetClass(), arg.getNodeValue().textValue(), null);
        }

        // value expression to be evaluated
        String expression = CodeExpressionUtils.cleanExpression(arg.getNodeValue().asText());

        LOG.debug("expression = {}", expression);

        // can be any code expression
        Object output = CodeInvocation.evalCode(eval, expression);
        Class targetClass = output == null ? null : output.getClass();
        if(targetClass != null) {
            // FIXME convert primitive wrappers to primitives - is this reliable?
            if(ClassUtils.isPrimitiveWrapper(targetClass)) {
                targetClass = ClassUtils.wrapperToPrimitive(targetClass);
            }
        }

        return new Parameter(targetClass, expression, output);
    }

    /**
     * Resolve input values.
     *
     * @param executedInvocations
     * @param executedInvocation
     * @return
     */
    List<Obj> resolveInputs(ExecutedInvocations executedInvocations, ExecutedInvocation executedInvocation) {
        Invocation invocation = executedInvocation.getInvocation();
        // either value (object) or reference
        List<Obj> inputs = new ArrayList<>(invocation.getParameters().size());
        for(Parameter parameter : invocation.getParameters()) {

            if(parameter.isReference()) {
                // by row
                // get value from ExecutedInvocation
                ExecutedInvocation ref = executedInvocations.getExecutedInvocation(parameter.getReference()[0]);
                inputs.add(ref.getOutput());
            } else {
                // just interpret expression
                try {
                    Object value = CodeInvocation.evalCode(executedInvocations.getInvocations().getEval(), CodeExpressionUtils.cleanExpression(parameter.getExpression()));
                    inputs.add(Obj.fromValue(value, Obj.PRODUCER_INDEX_NONE));
                } catch (EvalException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return inputs;
    }
}
