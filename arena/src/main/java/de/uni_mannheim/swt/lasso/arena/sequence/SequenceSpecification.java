/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.arena.sequence;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.*;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.Converter;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.ClassPermutation;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.check.Oracle;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.FactoryMethodStrategy;
import de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.StaticFieldInstance;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.field.AccessibleField;
import randoop.operation.*;
import randoop.org.plumelib.util.CollectionsPlume;
import randoop.sequence.Sequence;
import randoop.sequence.Variable;
import randoop.types.*;
import randoop.types.Type;

import java.lang.reflect.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A sequence specification.
 *
 * @author Marcus Kessel
 */
public class SequenceSpecification {

    private static final Logger LOG = LoggerFactory
            .getLogger(SequenceSpecification.class);

    /**
     * Name of sequence
     */
    private String name;

    /**
     * Statements in sequence
     */
    private List<SpecificationStatement> statements = new LinkedList<>();

    /**
     * An oracle to check the statements of the sequence (optional)
     */
    private Oracle oracle;

    /**
     * Custom {@link InterfaceSpecification}.
     */
    private InterfaceSpecification interfaceSpecification;

    /**
     * Ignore invisible members
     */
    private boolean ignoreVisibility = false;

    /**
     * Extract interface signatures from this Sequence. If {@link SequenceSpecification#interfaceSpecification} is set, it is used instead.
     *
     * @return
     */
    public InterfaceSpecification toInterfaceSpecification() {
        if(getInterfaceSpecification() != null) {
            return getInterfaceSpecification();
        }

        InterfaceSpecification specification = new InterfaceSpecification();
        List<MethodSignature> cSpecs = new LinkedList<>();
        specification.setConstructors(cSpecs);
        List<MethodSignature> mSpecs = new LinkedList<>();
        specification.setMethods(mSpecs);

        Class<?> cutClazz = null;
        for(SpecificationStatement statement : statements) {
            if(statement instanceof CallStatement) {
                if(statement.isClassUnderTest()) {
                    if(statement instanceof MethodCallStatement) {
                        MethodCallStatement m = (MethodCallStatement) statement;

                        if(cutClazz == null) {
                            cutClazz = m.getResolvedMethod().getDeclaringClass();
                        }

                        ReflectionMethodSignature sig = new ReflectionMethodSignature(m.getResolvedMethod());
                        if(!mSpecs.contains(sig)) {
                            mSpecs.add(sig);
                        }
                    }

                    if(statement instanceof ConstructorCallStatement) {
                        ConstructorCallStatement c = (ConstructorCallStatement) statement;

                        if(cutClazz == null) {
                            cutClazz = c.getResolvedConstructor().getDeclaringClass();
                        }

                        ReflectionConstructorSignature sig = new ReflectionConstructorSignature(c.getResolvedConstructor());
                        if(!cSpecs.contains(sig)) {
                            cSpecs.add(sig);
                        }
                    }
                }
            }
        }

        if(cutClazz == null) {
            //throw new IllegalArgumentException("Could not find CUT class");

            // sequence specification does not contain CUT
            return specification;
        }

        specification.setClassName(cutClazz.getSimpleName());

        // add default constructor if none is used (required for adaptation)
        if(CollectionUtils.isEmpty(cSpecs)) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Adding default constructor");
            }

            MethodSignature defaultConstructor = new MethodSignature(specification) {
                @Override
                public String toLQL() {
                    StringBuilder sb = new StringBuilder();
//        sb.append(className);
//        sb.append("(");

                    sb.append(getName());
                    sb.append("(");
                    sb.append(String.join(",", toParameterString()));
                    sb.append(")");
                    //sb.append(toReturnString());

                    return sb.toString();
                }
            };
            defaultConstructor.setName(cutClazz.getSimpleName());
            defaultConstructor.setReturnType(cutClazz);
            defaultConstructor.setParameterTypes(new Class[0]);
            cSpecs.add(defaultConstructor);
        }

        return specification;
    }

    public void addStatement(SpecificationStatement statement, int position) {
        //validate(position);

//        if(position < 0) {
//            throw new AssertionError("position was negative for " + statement.getClass());
//        }

        statement.setPosition(position);

        statements.add(statement);
    }

    private void validate(int position) {
        Validate.isTrue(statements.stream()
                .mapToInt(SpecificationStatement::getPosition)
                .noneMatch(i -> position == i), "position is taken = " + position);
    }

    public SpecificationStatement getStatement(int position) {
        return statements.stream().filter(s -> s.getPosition() == position).findFirst().orElse(null);

        //return statements.get(position);
    }

    public int getLength() {
        return this.statements.size();
    }

    public int getNextPosition() {
        return getLength();
    }

    public SpecificationStatement getLastStatement() {
        if (getLength() < 1) {
            return null;
        }

        return statements.get(statements.size() - 1);
    }

    public SequenceExecutionRecord instantiate(InterfaceSpecification specification, AdaptedImplementation adaptedImplementation) {
        Sequence sequence = new Sequence();

        SequenceExecutionRecord sequenceExecutionRecord = new SequenceExecutionRecord(this, specification, adaptedImplementation);

        Map<SpecificationStatement, Integer> rows = new LinkedHashMap<>();
        for (SpecificationStatement statement : statements) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Current statement = '{}', type = '{}'", statement, statement.getClass().getName());
            }

            // constructor call
            if (statement.getClass().equals(ConstructorCallStatement.class)) {
                ConstructorCallStatement constructorCallStatement = (ConstructorCallStatement) statement;

                if (!constructorCallStatement.isClassUnderTest()) { // doesn't require adapting
                    Constructor<?> constructor = constructorCallStatement.getResolvedConstructor();

                    sequence = addOperation(null, null, sequence, toRawOperation(constructor), constructorCallStatement, rows, sequenceExecutionRecord);
                } else {
                    // adapt
                    MethodSignature methodSignature = constructorCallStatement.getMethodSignature();
                    int c = specification.getConstructors().indexOf(methodSignature);
                    AdaptedInitializer adaptedInitializer = adaptedImplementation.getInitializer(specification, c);

                    if(adaptedInitializer.isConstructor() || !adaptedInitializer.hasMember()) {
                        if (!adaptedInitializer.hasMember()) {
                            // replace by NoOp constructor (e.g. in case of single static methods etc.)
                            adaptedInitializer.setAlternativeConstructor(noOp());
                        }

                        // is constructor
                        Constructor<?> constructor = adaptedInitializer.getAsConstructor();
                        if(!isIgnoreVisibility() && Modifier.isPrivate(constructor.getModifiers())) {
                            LOG.warn("Cannot access " + constructor);

                            // try producer strategy instead (if further candidates are available)
                            Sequence tryProdSeq = null;
                            if(adaptedImplementation instanceof PermutatorAdaptedImplementation) {
                                PermutatorAdaptedImplementation pImpl = (PermutatorAdaptedImplementation) adaptedImplementation;
                                ClassPermutation classPermutation = pImpl.getAdapter();
                                if(classPermutation.getConstructors().get(c).size() > 1) {
                                    LOG.debug("Identified alternative candidates for constructor '{}'", c);

                                    // try second
                                    Candidate candidate = classPermutation.getConstructors().get(c).get(1);

                                    tryProdSeq = attemptProducerStrategy(
                                            candidate, sequence, adaptedImplementation, adaptedInitializer, constructorCallStatement, rows, sequenceExecutionRecord);

                                    if(tryProdSeq != null) {
                                        sequence = tryProdSeq;
                                    }
                                }
                            }

                            if(tryProdSeq == null) {
                                adaptedInitializer.setAlternativeConstructor(noOp());
                                sequence = addOperation(adaptedImplementation, adaptedInitializer, sequence, toRawOperation(constructor), constructorCallStatement, rows, sequenceExecutionRecord);
                            }
                        } else {
                            sequence = addOperation(adaptedImplementation, adaptedInitializer, sequence, toRawOperation(constructor), constructorCallStatement, rows, sequenceExecutionRecord);
                        }
                    } else if(adaptedInitializer instanceof PermutatorAdaptedInitializer) {
                        // producer strategy
                        PermutatorAdaptedInitializer pInit = (PermutatorAdaptedInitializer) adaptedInitializer;
                        Candidate candidate = pInit.getCandidate();

                        Sequence prodSeq = attemptProducerStrategy(
                                candidate, sequence, adaptedImplementation, adaptedInitializer, constructorCallStatement, rows, sequenceExecutionRecord);

                        if(prodSeq != null) {
                            sequence = prodSeq;
                        } else {
                            throw new IllegalArgumentException("couldn't find initializer");
                        }
                    }
                }

                rows.put(statement, sequence.getLastVariable().getDeclIndex());
                sequenceExecutionRecord.add(new CallRecord(sequence.getLastVariable().getDeclIndex(), statement));
            }

            // method call
            if (statement.getClass().equals(MethodCallStatement.class)) {
                MethodCallStatement methodCallStatement = (MethodCallStatement) statement;

                if (!methodCallStatement.isClassUnderTest()) {
                    Method method = methodCallStatement.getResolvedMethod();

                    sequence = addOperation(null, null, sequence, toRawOperation(method), methodCallStatement, rows, sequenceExecutionRecord);
                } else {
                    // adapt
                    MethodSignature methodSignature = methodCallStatement.getMethodSignature();

                    // FIXME BUG LQLMethodSignature vs ReflectionMethodSignature
                    int m = specification.getMethods().indexOf(methodSignature);

                    if(m < 0) {
                        for(int c = 0; c < specification.getMethods().size(); c++) {
                            MethodSignature other = specification.getMethods().get(c);
                            if(Objects.equals(methodSignature.getName(), other.getName())) {
                                m = c;
                                break;
                            }
                        }
                    }

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Requested method signature '{}' for m '{}'", ToStringBuilder.reflectionToString(methodSignature), m);
                        LOG.debug(ToStringBuilder.reflectionToString(specification));
                    }

                    AdaptedMethod adaptedMethod = adaptedImplementation.getMethod(specification, m);

                    Method method = adaptedMethod.getMethod();

                    if(!isIgnoreVisibility() && Modifier.isPrivate(method.getModifiers())) {
                        LOG.warn("Cannot access " + method);

                        throw new IllegalArgumentException("Cannot access " + method);
                    }

                    sequence = addOperation(adaptedImplementation, adaptedMethod, sequence, toRawOperation(method), methodCallStatement, rows, sequenceExecutionRecord);
                }

                rows.put(statement, sequence.getLastVariable().getDeclIndex());
                sequenceExecutionRecord.add(new CallRecord(sequence.getLastVariable().getDeclIndex(), statement));
            }

            // field operation
            if (statement.getClass().equals(ValueStatement.class)) {
                ValueStatement value = (ValueStatement) statement;

                sequence = handleValue(value, sequenceExecutionRecord, sequence);
                rows.put(statement, sequence.getLastVariable().getDeclIndex());
            }

            // array element set
            if (statement.getClass().equals(ArraySetStatement.class)) {
                ArraySetStatement arraySetStatement = (ArraySetStatement) statement;
                SpecificationStatement input = arraySetStatement.getInputs().get(0);
                if (!(input instanceof ValueStatement) || !((ValueStatement) input).isArray()) {
                    //throw new IllegalArgumentException("expected array value statement but got " + input);
                }

                ValueStatement arrStmt = (ValueStatement) input;
                LOG.debug(arrStmt.toString());

                if (arrStmt.isAlias()) {
                    //arrStmt
                }

                TypedOperation op = TypedOperation.createArrayElementAssignment((ArrayType) asType(arrStmt.getType()));

                // array, index, value

                // array
                CallRecord callRecord = sequenceExecutionRecord.getRecord(arrStmt);
                int arrayVar = callRecord.getPosition();

                // index
                TypedOperation index = TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(asType(int.class), arraySetStatement.getIndex()));
                sequence = sequence.extend(index);
                int indexVar = sequence.getLastVariable().getDeclIndex();
                // value
                if (arraySetStatement.getInputs().size() == 2) { // handle reference
                    SpecificationStatement ref = arraySetStatement.getInputs().get(1);
                    CallRecord record = sequenceExecutionRecord.getRecord(ref);
                    Variable variable = sequence.getVariable(record.getPosition());

                    sequence = sequence.extend(op, Arrays.asList(sequence.getVariable(arrayVar), sequence.getVariable(indexVar), sequence.getVariable(variable.getDeclIndex())));
                }
            }
        }

        // DO NOT INLINE SEQUENCE (Randoop is buggy here)
        sequence.doNotInlineLiterals();

        sequenceExecutionRecord.setSequence(sequence);

        return sequenceExecutionRecord;
    }

    Sequence attemptProducerStrategy(Candidate candidate, Sequence sequence, AdaptedImplementation adaptedImplementation, AdaptedInitializer adaptedInitializer, ConstructorCallStatement constructorCallStatement, Map<SpecificationStatement, Integer> rows, SequenceExecutionRecord sequenceExecutionRecord) {
        //candidate.getProducerStrategy
        LOG.debug("Producer strategy '{}'", candidate.getProducerStrategy());

        if(candidate.getProducerStrategy() instanceof FactoryMethodStrategy) {
            TypedOperation op = toRawOperation((Method) candidate.getMethod());
            LOG.debug("op = {}", op);

            return addOperation(adaptedImplementation, adaptedInitializer, sequence, op, constructorCallStatement, rows, sequenceExecutionRecord);
        } else if(candidate.getProducerStrategy() instanceof StaticFieldInstance) {
            Field field = (Field) candidate.getMethod();
            TypedOperation op = toFieldGetOperation(field);

            LOG.debug("op = {}", op);

            return addOperation(adaptedImplementation, adaptedInitializer, sequence, op, constructorCallStatement, rows, sequenceExecutionRecord);
        }

        return null;
    }

    private Sequence handleValue(ValueStatement value, SequenceExecutionRecord sequenceExecutionRecord, Sequence sequence) {
        if (value.isAlias()) {
            SpecificationStatement ref = value.getInputs().get(0);
            CallRecord record = sequenceExecutionRecord.getRecord(ref);
            Variable variable = sequence.getVariable(record.getPosition());

            sequenceExecutionRecord.add(new CallRecord(variable.getDeclIndex(), value));
        } else if (value.isNull()) {
            sequence = sequence.extend(TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(asType(value.getType()), null)));
            sequenceExecutionRecord.add(new CallRecord(sequence.getLastVariable().getDeclIndex(), value));
        } else if (value.isArray()) {
            // get dimensions
            ArrayType arrayType = (ArrayType) asType(value.getType());

            Sequence arrSeq = toArray(arrayType, value.getValue());

            // join with current sequence
            sequence = Sequence.concatenate(Arrays.asList(sequence, arrSeq));
            // FIXME is this correct?
            sequenceExecutionRecord.add(new CallRecord(sequence.getLastVariable().getDeclIndex(), value));
        } else {
            // FIXME what about boxed primitive
            if (asType(value.getType()).isPrimitive()) {
                sequence = sequence.extend(TypedOperation.createPrimitiveInitialization(asType(value.getType()), value.getValue()));
                sequenceExecutionRecord.add(new CallRecord(sequence.getLastVariable().getDeclIndex(), value));
            } else {
                //
                sequence = sequence.extend(TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(asType(value.getType()), value.getValue())));
                sequenceExecutionRecord.add(new CallRecord(sequence.getLastVariable().getDeclIndex(), value));
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Sequence after adding ValueStatement\n'{}'", sequence.toCodeString());
        }

        return sequence;
    }

    private Sequence toArray(ArrayType arrayType, Object arr) {
        int arrLength = Array.getLength(arr);
        TypedOperation op = TypedOperation.createInitializedArrayCreation(arrayType, arrLength);

        Sequence array = new Sequence();
        List<Variable> vars = new LinkedList<>();
        // create all elements
        // FIXME only works for up to two dimensions
        for (int i = 0; i < arrLength; i++) {
            Object val = Array.get(arr, i);

            if (val != null && val.getClass().isArray()) {
                Sequence nested = toArray((ArrayType) asType(val.getClass()), val);
                LOG.debug("nested " + nested.toString());

                array = Sequence.concatenate(Arrays.asList(array, nested));
                vars.add(array.getLastVariable());
            } else {
                TypedOperation typedOperation = TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(arrayType.getComponentType(), val));

                array = array.extend(typedOperation);
                vars.add(array.getLastVariable());
            }
        }

        LOG.debug("array " + array.toString());

        // update vars
        List<Variable> arrVars = new LinkedList<>();
        for (int i = 0; i < vars.size(); i++) {
            arrVars.add(array.getVariable(vars.get(i).getDeclIndex()));
        }

        return array.extend(op, arrVars);
    }

    private Sequence addOperation(AdaptedImplementation adaptedImplementation, AdaptedMember adaptedMember, Sequence sequence, TypedOperation operation, CallStatement statement, Map<SpecificationStatement, Integer> rows, SequenceExecutionRecord sequenceExecutionRecord) {
        List<Integer> indices = new LinkedList<>();

        List<SpecificationStatement> inputs = statement.getInputs();

        int param = 0;
        for (SpecificationStatement input : inputs) {
            if (rows.containsKey(input)) {
                indices.add(rows.get(input));
            } else {
                // FIXME check if actual value etc.

                if (input instanceof ValueStatement) {
                    ValueStatement valueStatement = (ValueStatement) input;

                    if(valueStatement.isArray()) {
                        Sequence arrSeq = toArray((ArrayType) asType(valueStatement.getType()), valueStatement.getValue());

                        // join with current sequence
                        sequence = Sequence.concatenate(Arrays.asList(sequence, arrSeq));

                        indices.add(sequence.getLastVariable().getDeclIndex());
                    } else {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("specified value type = '{}', value = '{}', value type = '{}'", valueStatement.getType(), valueStatement.getValue(), valueStatement.getValue() != null ? valueStatement.getValue().getClass() : "unknown");
                        }

                        TypedOperation valueOperation = null;

                        // is code term evaluation?
                        if(valueStatement.getCode() != null) {
                            try {
                                valueOperation = RandoopHelper.createCodeTerm(new CodeTerm(asType(valueStatement.getType()), valueStatement.getValue(), valueStatement.getCode()));
                            } catch (IllegalArgumentException e) {
                                LOG.warn("ValueStatement error", e);

                                throw e;
                            }
                        } else {
                            try {
                                valueOperation = TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(asType(valueStatement.getType()), valueStatement.getValue()));
                            } catch (IllegalArgumentException e) {
                                LOG.warn("ValueStatement error", e);

                                throw e;
                            }
                        }

                        sequence = sequence.extend(valueOperation);
                        indices.add(sequence.getLastVariable().getDeclIndex());
                    }

//                    TypedOperation valueOperation = TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(asType(valueStatement.getType()), valueStatement.getValue()));
//                    sequence = sequence.extend(valueOperation);
//                    indices.add(sequence.getLastVariable().getDeclIndex());
                }
            }

            param++;
        }



        if(adaptedMember instanceof AdaptedMethod) {
            MethodSignature methodSignature = statement.getMethodSignature();

            AdaptedMethod adaptedMethod = (AdaptedMethod) adaptedMember;
            // spec says this is a static method ..
            // if TRUE, FIXME check if we find a constructor or if we need to create a new one
            if(methodSignature.isStatic() && !Modifier.isStatic(adaptedMethod.getMethod().getModifiers())) {
                // findOrCreate constructor
                // we also need to modify inputs to consider "this" (i.e receiver)

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Need static method, but found instance method. Using default constructor for '{}'", adaptedMethod.getMethod());
                }

                // reuse constructor
                Optional<CallRecord> callRecordOp = sequenceExecutionRecord.getFirstCutInstanceRecord();
                if(callRecordOp.isPresent()) {
                    int position = callRecordOp.get().getPosition();
                    Variable c = sequence.getVariable(position);

                    // add receiver to indices
                    indices.add(0, c.getDeclIndex());
                } else {
                    // strategy: create a new constructor each time (simply use default constructor)
                    AdaptedInitializer defaultInitializer = adaptedImplementation.getDefaultInitializer();
                    if(!defaultInitializer.hasMember()) { // cannot really happen in this case
                        defaultInitializer.setAlternativeConstructor(noOp());
                    }

                    TypedOperation defaultInitializerOp = toRawOperation(defaultInitializer.getAsConstructor());
                    sequence = sequence.extend(defaultInitializerOp);

                    // add receiver to indices
                    indices.add(0, sequence.getLastVariable().getDeclIndex());
                }
            }
        }

        //
        List<Variable> variables = toVariables(sequence, indices);
        LOG.debug("Indices size {}, variables size {}", indices.size(), variables.size());
        if (adaptedMember != null) {
            // switch params
            switchVariables(adaptedMember, variables);

            // convert types
            List<Variable> updatedVariables = new LinkedList<>();
            // look if we need to adapt
            for (int var = 0; var < variables.size(); var++) {
                Variable variable = variables.get(var);

                // check if expected types match
                int start = variables.size() - operation.getInputTypes().size();
                // ignore receiver instance if given operation is static
                if(start > 0 && var == 0) {
                    continue;
                }

                Type expectedType = operation.getInputTypes().get(var - start);
                Type varType = variable.getType();

                // lookup adapted method if we can convert
                Class<?> expectedClazz = expectedType.getRuntimeClass();
                Class<?> actualClazz = varType.getRuntimeClass();

                // ignore placeholder init call
                boolean isNoOp = expectedType.getRuntimeClass().equals(NoOp.class);

                // isAssignable
                boolean assignable = DefaultAdaptationStrategy.isAssignable(actualClazz, expectedClazz, false);

                // ADAPT convert input value if possible
                if (!isNoOp && !assignable && adaptedMember.canConvert(actualClazz, expectedClazz)) {
                    LOG.info("input var '{}' does not match => '{}' vs '{}'", variable.getName(), expectedType, varType);

                    // add converter statements
                    sequence = addConverter(sequence, variable, adaptedMember, actualClazz, expectedClazz);
                    Variable adaptedInput = sequence.getLastVariable();
                    updatedVariables.add(adaptedInput);
                } else { // may also fail if no converter present
                    LOG.info("Direct input var type match for '{}' => '{}' (actual '{}')", variable.getName(), expectedType, actualClazz);

                    updatedVariables.add(search(variable, sequence));
                }
            }

            // update again
            List<Variable> nvars = new ArrayList<>(updatedVariables.size());
            for (Variable uvar : updatedVariables) {
                nvars.add(search(uvar, sequence));
            }

            LOG.debug("nvars {}", nvars.size());
            LOG.debug("Current sequence {}", sequence);

            // update
            sequence = sequence.extend(operation, nvars);

            // check return types
            sequence = afterOperation(adaptedImplementation, adaptedMember, operation, sequence);
        } else {
            sequence = sequence.extend(operation, variables); // simply add

            LOG.debug("{}", operation.isNonreceivingValue());
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Sequence after adding Operation\n'{}'", sequence.toCodeString());
        }

        return sequence;
    }

    private Sequence afterOperation(AdaptedImplementation adaptedImplementation, AdaptedMember adaptedMember, TypedOperation operation, Sequence sequence) {
        // convert output type if possible
        if (!(adaptedMember instanceof AdaptedInitializer && ((AdaptedInitializer) adaptedMember).isConstructor())) {
            Type actualReturnType = operation.getOutputType();
            Class<?> expectedReturnType;
            try {
                expectedReturnType = adaptedMember.getSpecification().getReturnType(adaptedImplementation.getAdapteeClass());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            // ignore placeholder init call
            boolean isNoOp = actualReturnType.getRuntimeClass().equals(NoOp.class);

            // FIXME allowed alternatives to specification: for Stack: Object or void in push
            boolean isVoid = actualReturnType.isVoid() || expectedReturnType.equals(void.class) || expectedReturnType.equals(Void.class);

            boolean assignable = DefaultAdaptationStrategy.isAssignable(actualReturnType.getRuntimeClass(), expectedReturnType, false);

            if (!isVoid && !isNoOp && !assignable) {
                LOG.info("return type does not match => '{}' vs '{}'", expectedReturnType, actualReturnType.getRuntimeClass());

                Variable variable = sequence.getLastVariable();

                // add converter statements
                sequence = addConverter(sequence, variable, adaptedMember, actualReturnType.getRuntimeClass(), expectedReturnType);
                //Variable adaptedInput = sequence.getLastVariable();
            } else {
                LOG.info("return type match => '{}'", expectedReturnType);
            }
        }

        return sequence;
    }

    /**
     * Switch variables (i.e based on given permutation).
     *
     * @param variables
     * @return
     */
    protected List<Variable> switchVariables(AdaptedMember adaptedMember, List<Variable> variables) {
        if (adaptedMember == null) {
            return variables;
        }

        if (CollectionUtils.isEmpty(variables)) {
            return variables;
        }

        // include parameter switching
        List<Variable> switchedParams = new LinkedList<>();
        //boolean isStatic = adaptedMember instanceof AdaptedMethod && ((AdaptedMethod) adaptedMember).isStatic();
        //List<Variable> subVars = isStatic ? variables : variables.subList(1, variables.size()); // skip first

        // handling of receiver (instance vs static method)
        int start = variables.size() - adaptedMember.getPositions().length;
        for (int i = start; i < variables.size(); i++) {
            int position = adaptedMember.getPositions()[i - start];
            switchedParams.add(variables.get(position + start));
        }

        if (start > 0) {
            switchedParams.add(0, variables.get(0));
        }

        LOG.debug("switch args '{}'", switchedParams.stream().map(Variable::toString).collect(Collectors.joining(",")));

        return switchedParams;
    }

    private List<Variable> toVariables(final Sequence sequence, List<Integer> indices) {
        return indices.stream().map(sequence::getVariable).collect(Collectors.toList());
    }

    public static Type asType(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        if (clazz.isArray()) {
            return ArrayType.forClass(clazz);
        } else if (clazz.isPrimitive()) {
            //
            Optional<PrimitiveType> primitiveTypeOptional = JavaTypes.getPrimitiveTypes().stream().filter(pt -> pt.getRuntimeClass().equals(clazz)).findFirst();
            return primitiveTypeOptional.orElseThrow(() -> new IllegalArgumentException("cannot find PrimitiveType for " + clazz));
        } else {
            return NonParameterizedType.forClass(clazz);
        }
    }

    TypedOperation toFieldGetOperation(
            Field field) {
        NonParameterizedType declaringType = NonParameterizedType.forClass(field.getDeclaringClass());
        AccessibleField f = new AccessibleField(field, declaringType);
        List<Type> getInputTypesList = new ArrayList<>();
        if (!Modifier.isStatic(field.getModifiers() & Modifier.fieldModifiers())) {
            getInputTypesList.add(declaringType);
        }
        FieldGet getOp = new FieldGet(f);
        return new TypedClassOperation(
                getOp, declaringType, new TypeTuple(getInputTypesList), asType(field.getType()));
    }

    TypedOperation toRawOperation(Method method) {
        List<Type> methodParamTypes =
                CollectionsPlume.mapList(Type::forType, method.getParameterTypes());

        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnonymousClass()
                && declaringClass.getEnclosingClass() != null
                && declaringClass.getEnclosingClass().isEnum()) {
            // is a method in anonymous class for enum constant
            //return getAnonEnumOperation(method, methodParamTypes, declaringClass.getEnclosingClass());
            throw new UnsupportedOperationException("is a method in anonymous class for enum constant");
        }

        List<Type> paramTypes = new ArrayList<>();
        MethodCall op = new MethodCall(method);
        NonParameterizedType declaringType = NonParameterizedType.forClass(method.getDeclaringClass());
        if (!op.isStatic()) {
            paramTypes.add(declaringType);
        }
        paramTypes.addAll(methodParamTypes);
        TypeTuple inputTypes = new TypeTuple(paramTypes);
        Type outputType = Type.forType(method.getReturnType());
        if (outputType.isVariable()) {
            return RandoopHelper.createTypedClassOperationWithCast(op, declaringType, inputTypes, outputType);
        }

        return new TypedClassOperation(op, declaringType, inputTypes, outputType);
    }

    @Deprecated
    TypedOperation toOperation(Method method) {
        TypedOperation typedOperation = TypedOperation.forMethod(method);

        boolean generic = isGeneric(method.getDeclaringClass());
        if (generic) {
            // FIXME determine generic type
            ClassOrInterfaceType genericType = ClassOrInterfaceType.forClass(Object.class);

            typedOperation = typedOperation.substitute(GenericClassType.forClass(method.getDeclaringClass()).instantiate(genericType).getTypeSubstitution());
        }

        return typedOperation;
    }

    @Deprecated
    TypedOperation toOperation(Constructor<?> constructor) {
        TypedOperation typedOperation = TypedOperation.forConstructor(constructor);

        boolean generic = isGeneric(constructor.getDeclaringClass());
        if (generic) {
            // FIXME determine generic type
            ClassOrInterfaceType genericType = ClassOrInterfaceType.forClass(Object.class);

            typedOperation = typedOperation.substitute(GenericClassType.forClass(constructor.getDeclaringClass()).instantiate(genericType).getTypeSubstitution());
        }

        return typedOperation;
    }

    TypedOperation toRawOperation(Constructor<?> constructor) {
        ConstructorCall call = new ConstructorCall(constructor);

        NonParameterizedType declaringType = NonParameterizedType.forClass(constructor.getDeclaringClass());
        List<Type> paramTypes = CollectionsPlume.mapList(Type::forType, constructor.getGenericParameterTypes());
        TypeTuple inputTypes = new TypeTuple(paramTypes);
        TypedOperation typedOperation = new TypedClassOperation(call, declaringType, inputTypes, declaringType);

        //TypedOperation typedOperation = TypedOperation.forConstructor(constructor);

        return typedOperation;
    }

    /**
     * If given class has generic signature.
     *
     * @param clazz
     * @return
     */
    public static boolean isGeneric(Class<?> clazz) {
        boolean generic = false;
        try {
            generic = GenericClassType.forClass(clazz).isGeneric(false);
        } catch (Throwable e) {
            //e.printStackTrace();
        }

        return generic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SpecificationStatement> getStatements() {
        return statements;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (SpecificationStatement statement : statements) {
            sb.append(statement + " => " + "(" + statement.getInputs().stream().map(Objects::toString).collect(Collectors.joining(",")) + ") " + statement.getPosition());
            sb.append("\n");
        }

        return sb.toString();
    }

    private Constructor<?> noOp() {
        return ConstructorUtils.getAccessibleConstructor(NoOp.class);
    }

    /**
     * Since {@link Sequence} is immutable we need to refresh old variables based on their name.
     *
     * @param variable
     * @param sequence
     * @return
     */
    private Variable search(Variable variable, Sequence sequence) {
        for (int stmt = 0; stmt < sequence.size(); stmt++) {
            Variable var = sequence.getVariable(stmt);

            if (StringUtils.equals(variable.getName(), var.getName())) {
                return var;
            }
        }

        return null;
    }

    private Sequence addConverter(Sequence s, Variable variable, AdaptedMember adaptedMember, Class<?> actualClazz, Class<?> expectedClazz) {
        LOG.debug("Conversion '{}' to '{}'", actualClazz, expectedClazz);

        // introduce field operation for conversion. Use JavaConversionStrategy.
        Class<? extends Converter> converterClazz = adaptedMember.getConverterClass(actualClazz, expectedClazz);

        LOG.debug("Found converter class '{}'", converterClazz);

        TypedOperation helperOperationCon = toRawOperation(ConstructorUtils.getMatchingAccessibleConstructor(converterClazz));//TypedOperation.forConstructor(ConstructorUtils.getMatchingAccessibleConstructor(converterClazz));

        s = s.extend(helperOperationCon);

        Variable converterInstance = s.getLastVariable();

        // FIXME randoop has a bug .. class.getName instead of class.getCanonicalName() (e.g. [B instead of byte[])
        // add Class type
//        TypedOperation secondParam = TypedOperation.createNonreceiverInitialization(new NonreceiverTerm(JavaTypes.CLASS_TYPE, expectedClazz));
//        s = s.extend(secondParam);

        TypedOperation helperOperation = toRawOperation(getConverterMethod(converterClazz));//TypedOperation.forMethod(getConverterMethod(converterClazz));

        //        // add cast?
//        TypedOperation cast = TypedOperation.createCast(s.getLastVariable().getType(), Type.forClass(expectedClazz));
//        s = s.extend(cast, s.getLastVariable());

        // given inputs: instance
        s = s.extend(helperOperation, Arrays.asList(search(converterInstance, s), search(variable, s)));

        // add cast?
        TypedOperation cast = TypedOperation.createCast(s.getLastVariable().getType(), Type.forClass(expectedClazz));
        s = s.extend(cast, s.getLastVariable());

        return s;
    }

    /**
     * Gets {@link Converter#convert(Object, Class)} for given converter class.
     *
     * @param converterClazz
     * @return
     */
    private static Method getConverterMethod(Class<?> converterClazz) {
        Optional<Method> methodOptional = Arrays.stream(converterClazz.getMethods())
                .filter(m -> m.getName().equals("convertRaw") && m.getParameterCount() == 1)
                .findFirst();

        return methodOptional.orElseThrow(() -> new IllegalStateException("converter method not found"));
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(InterfaceSpecification interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public Oracle getOracle() {
        return oracle;
    }

    public void setOracle(Oracle oracle) {
        this.oracle = oracle;
    }

    public boolean hasOracle() {
        return getOracle() != null;
    }

    public boolean isIgnoreVisibility() {
        return ignoreVisibility;
    }

    public void setIgnoreVisibility(boolean ignoreVisibility) {
        this.ignoreVisibility = ignoreVisibility;
    }
}
