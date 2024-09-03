package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.UtilEvalError;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedCell;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedRow;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SheetResolver;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sequence Sheet Notation backed by BSH (see {@link Interpreter}).
 *
 * @author Marcus Kessel
 */
public class SSNInterpreter {

    private static final Logger LOG = LoggerFactory
            .getLogger(SSNInterpreter.class);
    public static final String CODE_PREFIX = "#{";
    public static final String CODE_POSTFIX = "}";

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
     * Interpret sheet specifications.
     *
     * @param parsedSheet
     * @param interfaceSpecificationMap
     * @param classLoader
     * @return
     */
    public Invocations interpret(ParsedSheet parsedSheet, Map<String, InterfaceSpecification> interfaceSpecificationMap, ClassLoader classLoader) {
        // our interpreter that holds signatures on the fly for resolution
        Interpreter bsh = new Interpreter();
        bsh.setClassLoader(classLoader);
        // all LQL specs to Java (here classes)
        Map<Member, MethodSignature> resolvedMappings = lqlToJava(bsh, interfaceSpecificationMap);

        Invocations invocations = new Invocations(interfaceSpecificationMap, parsedSheet, resolvedMappings, bsh);

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

            if(StringUtils.equalsIgnoreCase(operationName, "create")) {
                // create invocation
                LOG.debug("create invocation = {}", operationName);

                String className = clazz;
                // create instance
                Invocation createInstance = instanceInvocation(bsh, invocations, className, inputArgs);

            } else if(StringUtils.startsWith(operationName, CODE_PREFIX) && StringUtils.startsWith(operationName, CODE_POSTFIX)) {
                // code to evaluate

                // FIXME code to eval
                // -- use "eval" command? or some simple syntax like in SpEL #{ <expression string> }

                LOG.debug("code invocation = {}", operationName);

                String codeToEvaluate = StringUtils.substringBetween(operationName, CODE_PREFIX, CODE_POSTFIX);

            } else {
                // method invocation
                LOG.debug("method invocation = {}", operationName);
                Invocation methodInvocation = methodInvocation(bsh, invocations, clazzCell, operationName, inputArgs);
            }
        }

        return invocations;
    }

    /**
     * Execute {@link Invocations}.
     *
     * @param invocations
     * @param adaptedImplementation
     * @return
     */
    public ExecutedInvocations run(Invocations invocations, AdaptedImplementation adaptedImplementation) {
        //Interpreter bsh = invocations.getBsh();

        //
        ExecutedInvocations executedInvocations = new ExecutedInvocations(invocations);

        for(Invocation invocation : invocations.getSequence()) {
            ExecutedInvocation executedInvocation = executedInvocations.create(invocation);

            // is CUT?
            Member member = invocation.getMember();
            Class targetClass = member.getDeclaringClass();
            boolean cut = false;
            if(invocations.getInterfaceSpecifications().containsKey(targetClass.getCanonicalName())) {
                cut = true;

                LOG.debug("Found cut '{}'", targetClass);
            }

            // either value (object) or reference
            List<Object> inputs = resolveInputs(invocations, invocation, executedInvocations);

            if(invocation.getMember() instanceof Constructor) {
                Constructor constructor = (Constructor) invocation.getMember();

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

                        Object instance = adaptedConstructor.newInstance(inputs.toArray());
                        executedInvocation.setOutput(Output.fromValue(instance));

                        LOG.debug("cut constructor '{}'", executedInvocation.getOutput().getValue());
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        if(!constructor.isAccessible()) {
                            constructor.setAccessible(true);
                        }

                        Object instance = constructor.newInstance(inputs.toArray());
                        executedInvocation.setOutput(Output.fromValue(instance));

                        LOG.debug("non-cut constructor '{}'", executedInvocation.getOutput().getValue());
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                Method method = (Method) invocation.getMember();

                // invoke method
                Parameter target = invocation.getTarget();

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
                        Object out = adMethod.invoke(instance, inputs.toArray());
                        executedInvocation.setOutput(Output.fromValue(out));

                        LOG.debug("cut method call '{}'", executedInvocation.getOutput().getValue());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // method invocation
                    ExecutedInvocation ref = executedInvocations.getExecutedInvocation(target.getReference()[0]);
                    Object instance = ref.getOutput().getValue();
                    try {
                        if(!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        Object out = method.invoke(instance, inputs.toArray());
                        executedInvocation.setOutput(Output.fromValue(out));

                        LOG.debug("non-cut method call '{}'", executedInvocation.getOutput().getValue());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return executedInvocations;
    }

    /**
     * Resolve input values.
     *
     * @param invocations
     * @param invocation
     * @param executedInvocations
     * @return
     */
    List<Object> resolveInputs(Invocations invocations, Invocation invocation, ExecutedInvocations executedInvocations) {
        // either value (object) or reference
        List<Object> inputs = new ArrayList<>(invocation.getParameters().size());
        for(Parameter parameter : invocation.getParameters()) {

            if(parameter.isReference()) {
                // by row
                // get value from ExecutedInvocation
                ExecutedInvocation ref = executedInvocations.getExecutedInvocation(parameter.getReference()[0]);
                inputs.add(ref.getOutput().getValue());
            } else {
                // just interpret expression
                try {
                    Object value = invocations.getBsh().eval(cleanExpression(parameter.getExpression()));
                    inputs.add(value);
                } catch (EvalError e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return inputs;
    }

    /**
     * Translate LQL into a concrete Java class and return a mapping.
     *
     * @param bsh
     * @param interfaceSpecificationMap
     * @return
     */
    Map<Member, MethodSignature> lqlToJava(Interpreter bsh, Map<String, InterfaceSpecification> interfaceSpecificationMap) {
        Map<Member, MethodSignature> mappings = new LinkedHashMap<>();
        for(String clazz : interfaceSpecificationMap.keySet()) {
            mappings.putAll(lqlToJava(bsh, interfaceSpecificationMap.get(clazz)));
        }

        return mappings;
    }

    /**
     * Translate LQL into a concrete Java class and return a mapping.
     *
     * @param bsh
     * @param interfaceSpecification
     * @return
     */
    Map<Member, MethodSignature> lqlToJava(Interpreter bsh, InterfaceSpecification interfaceSpecification) {
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
        try {
            eval(bsh, inter);
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }

        // now resolve mappings
        try {
            Class clazz = resolveClass(bsh, interfaceSpecification.getClassName());

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
            sb.append(methodSignature.getParent().getClassName());
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
        if(!ArrayUtils.isEmpty(methodSignature.getParameterTypes())) {
            String[] inputs = Arrays.stream(methodSignature.getParameterTypes())
                    .map(Class::getCanonicalName)
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
     * @param bsh
     * @param invocations
     * @param className
     * @param inputs
     * @return
     */
    Invocation instanceInvocation(Interpreter bsh, Invocations invocations, String className, List<ParsedCell> inputs) {
        LOG.debug("classname = {}, inputs = {}", className, inputs);

        try {
            // resolve class
            Class resolvedClass = resolveClass(bsh, className);
            LOG.debug("resolved clazz = {}", resolvedClass);

            Invocation invocation = invocations.create();
            invocation.setTargetClass(resolvedClass);

            // resolve parameters
            List<Parameter> parameters = resolveParameterTypes(bsh, invocations, inputs);
            invocation.setParameters(parameters);

            // FIXME can happen that we don't know all types (e.g., null)
            Class[] types = parameters.stream().map(Parameter::getTargetClass).collect(Collectors.toList()).toArray(new Class[0]);

            // resolve "constructor" (here just a declared placeholder)
            Constructor constructor = resolvedClass.getDeclaredConstructor(types);

            invocation.setMember(constructor);

            return invocation;

            // FIXME resolve constructor?
        } catch (UtilEvalError | EvalError e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a method invocation
     *
     * @param bsh
     * @param invocations
     * @param clazzCell
     * @param methodName
     * @param inputs
     * @return
     */
    Invocation methodInvocation(Interpreter bsh, Invocations invocations, ParsedCell clazzCell, String methodName, List<ParsedCell> inputs) {
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
            Class resolvedClass = bsh.getNameSpace().getClass(className);
            LOG.debug("resolved class for method:\n {}", resolvedClass);

            Invocation invocation = invocations.create();
            invocation.setTargetClass(resolvedClass);

            // set target
            Parameter target = new Parameter(coordinate, targetClass, clazz, null);
            invocation.setTarget(target);

            // resolve parameters
            List<Parameter> parameters = resolveParameterTypes(bsh, invocations, inputs);
            invocation.setParameters(parameters);

            // FIXME can happen that we don't know all types (e.g., null)
            Class[] types = parameters.stream().map(Parameter::getTargetClass).collect(Collectors.toList()).toArray(new Class[0]);

            // resolve "constructor" (here just a declared placeholder)
            Method method = resolvedClass.getDeclaredMethod(methodName, types);

            invocation.setMember(method);

            return invocation;
        } catch (UtilEvalError e) {
            throw new RuntimeException(e);
        } catch (EvalError e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Evaluate a code expression in BSH
     *
     * @param bsh
     * @param expr
     * @throws EvalError
     */
    void eval(Interpreter bsh, String expr) throws EvalError {
        LOG.debug("EVAL:\n {}", expr);
        bsh.eval(expr);
    }

    /**
     * Resolve a {@link Class} from BSH
     *
     * @param bsh
     * @param className
     * @return
     * @throws UtilEvalError
     */
    Class resolveClass(Interpreter bsh, String className) throws UtilEvalError {
        // resolve class
        Class resolvedClass = bsh.getNameSpace().getClass(className);

        return resolvedClass;
    }

    /**
     * Resolve parameter types
     *
     * @param bsh
     * @param invocations
     * @param args
     * @return
     * @throws EvalError
     */
    List<Parameter> resolveParameterTypes(Interpreter bsh, Invocations invocations, List<ParsedCell> args) throws EvalError {
        List<Parameter> parameters = new ArrayList<>(args.size());

        for(ParsedCell arg : args) {
            Parameter parameter = resolveParameterType(bsh, invocations, arg);
            parameters.add(parameter);
        }

        return parameters;
    }

    /**
     * Resolve a parameter type.
     *
     * @param bsh
     * @param invocations
     * @param arg
     * @return
     * @throws EvalError
     */
    Parameter resolveParameterType(Interpreter bsh, Invocations invocations, ParsedCell arg) throws EvalError {
        // two cases
        // reference to be resolved
        if(arg.isValueReference()) {
            int[] coordinate = SheetResolver.resolveCellReference(arg.getNodeValue().textValue());
            Invocation invocation = invocations.getInvocation(coordinate[0]);

            return new Parameter(coordinate, invocation.getTargetClass(), arg.getNodeValue().textValue(), null);
        }

        // value expression to be evaluated
        String expression = cleanExpression(arg.getNodeValue().asText());

        LOG.debug("expression = {}", expression);

        Object output = bsh.eval(expression);
        Class targetClass = output == null ? null : output.getClass();

        return new Parameter(targetClass, arg.getNodeValue().textValue(), output);
    }

    /**
     * Produce clean Java code from expressions in JSON notation.
     *
     * @param expression
     * @return
     */
    String cleanExpression(String expression) {
        if(StringUtils.startsWith(expression, "'") && StringUtils.endsWith(expression, "'")) {
            expression = StringUtils.wrap(StringUtils.substringBetween(expression, "'"), "\"");
        }

        return expression;
    }
}
