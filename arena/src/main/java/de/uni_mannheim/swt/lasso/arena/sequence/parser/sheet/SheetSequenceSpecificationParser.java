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
package de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.check.Oracle;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import de.uni_mannheim.swt.lasso.arena.sequence.*;

import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

/**
 * Parse (spread)sheets into {@link SequenceSpecification}s.
 *
 * @author Marcus Kessel
 */
public class SheetSequenceSpecificationParser {

    private static final Logger LOG = LoggerFactory
            .getLogger(SheetSequenceSpecificationParser.class);

    public Map<String, SequenceSpecification> toSequenceSpecifications(SpreadSheet spreadSheet, InterfaceSpecification specification, ClassUnderTest classUnderTest, String postfix) {
        Map<String, SequenceSpecification> ssMap = new LinkedHashMap<>();

        for(Sheet sheet : spreadSheet.getSheets()) {
            SequenceSpecification ss = toSequenceSpecification(sheet, specification, classUnderTest, postfix);
            ssMap.put(ss.getName(), ss);
        }

        return ssMap;
    }

    /**
     * Translates sheets into {@link SequenceSpecification}s.
     *
     * @param sheet
     * @param specification
     * @param classUnderTest
     * @param postfix
     * @return
     */
    public SequenceSpecification toSequenceSpecification(Sheet sheet, InterfaceSpecification specification, ClassUnderTest classUnderTest, String postfix) {
        SequenceSpecification sequenceSpecification = new SequenceSpecification();
        sequenceSpecification.setName(String.format("%s_%s", sheet.getSheetName(), postfix));

        // set custom interface specification
        sequenceSpecification.setInterfaceSpecification(specification);

        SheetParseContext context = new SheetParseContext();
        context.setSpecification(specification);
        context.setClassUnderTest(classUnderTest);
        context.setSequenceSpecification(sequenceSpecification);

        Oracle oracle = null;

        // set MethodSignature + if CUT
        for(Row row : sheet) {
            // result
            Cell result = row.getCell(0);
            if(result == null) {
                result = row.createCell(0);
            } else {
                if(oracle == null) {
                    oracle = new Oracle();
                    sequenceSpecification.setOracle(oracle);
                }

                if(result.getCellType() != CellType.BLANK) {
                    // set value
                    int position = row.getRowNum();

                    // cell is available
                    if(isCellReference(result)) {
                        // FIXME lookup .. also need to check expected return type in specification
                    } else {
                        Class<?> expectedType = Object.class; // FIXME
                        ValueStatement output = processValue(context, expectedType, result);
                        oracle.addExpectedValueForStatement(position, output);

                        LOG.debug("Added oracle '{}' for cell '{}'", position, result.getCellType());
                    }
                }
            }

            // operation
            Cell operation = row.getCell(1);
            if (operation == null) {
                //
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Operation does not exist. Skipping row '{}'", row.getRowNum());
                }

                continue;
            }

            List<Cell> inputs = new LinkedList<>();
            for(int i = 2; i < row.getLastCellNum(); i++) {
                // FIXME check if empty
                Cell cell = row.getCell(i);

                inputs.add(row.getCell(i));
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Row '{}' > Operation = '{}'", row, operation.getStringCellValue());
            }

            // FIXME switch getCellType
            String operationName = operation.getStringCellValue();

            // constructor
            if(StringUtils.equalsIgnoreCase(operationName, "CREATE")) {
                // FIXME check if CUT (based on Specification)
                //ConstructorCallStatement callStatement = new ConstructorCallStatement();

                Cell className = inputs.get(0);
                String name = className.getStringCellValue();

                LOG.debug("Specification class name '{}'", specification.getClassName());

                // FIXME endsWithIgnoreCase (sometimes specification has fully qualified name)
                if(StringUtils.equalsIgnoreCase(specification.getClassName(), name)) {
                    LOG.debug("Found CUT '{}'", name);

                    int numConstructors = context.getSpecification().getConstructors().size();

                    Optional<MethodSignature> methodSignatureOp;
                    if(numConstructors == 1) { // only one constructor
                        methodSignatureOp = Optional.ofNullable(context.getSpecification().getConstructors().get(0));
                    } else { // multiple constructors
                        // input parameters
                        int params = inputs.size() - 1;
                        methodSignatureOp = context.getSpecification().getConstructors().stream().filter(m -> {
                            // FIXME more precise: based on type matching as well
                            LOG.debug("Constructor params {} vs {}", m.getParameterTypes(classUnderTest.loadClassUnsafe()).length, inputs.size() - 1);
                            //if (StringUtils.equals(m.getName(), sig.getName())) {
                            if(m.getParameterTypes(classUnderTest.loadClassUnsafe()).length == params) {
                                LOG.debug("Matched interface constructor '{}'", m.toLQL());

                                return true;
                            }
                            //}

                            return false;
                        }).findFirst();
                    }

                    if(methodSignatureOp.isPresent()) {
                        MethodSignature methodSignature = methodSignatureOp.get();

                        ConstructorCallStatement callStatement = new ConstructorCallStatement(methodSignature);
                        callStatement.setClassUnderTest(true);

                        context.getLocalFields().add(result.getAddress());
                        int position = context.getLocalFields().lastIndexOf(result.getAddress());

                        context.getSequenceSpecification().addStatement(callStatement, position);

                        // now determine inputs
                        if(methodSignature.getParameterTypes(classUnderTest.loadClassUnsafe()).length > 0) {
                            int p = 0;
                            for(Cell input : inputs.subList(1, inputs.size())) {
                                // reference
                                if(isCellReference(input)) {
                                    //CellAddress inputRef = input.getAddress();
                                    CellAddress inputRef = new CellAddress(input.getStringCellValue());
                                    int inputPos = context.getLocalFields().lastIndexOf(inputRef);
                                    SpecificationStatement inputStmt = context.getSequenceSpecification().getStatement(inputPos);
                                    callStatement.addInput(inputStmt);
                                } else {
                                    // direct value passing
                                    Class<?> expectedType = methodSignature.getParameterTypes(classUnderTest.loadClassUnsafe())[p];

                                    ValueStatement valueStatement = processValue(context, expectedType, input);
                                    //context.getSequenceSpecification().addStatement(callStatement, context.getSequenceSpecification().getNextPosition());
                                    callStatement.addInput(valueStatement);
                                }

                                p++;
                            }
                        }
                    }
                } else {
                    // add NoOp if no constructor available
                    LOG.warn("Did not find constructor for '{}'", name);

                    boolean success = false;
                    // 1st: lookup class
                    if(StringUtils.contains(name, ".")) {
                        // fully-qualified reference, so try to read class
                        try {
                            Class clazz = classUnderTest.getProject().getContainer().loadClass(name);

                            LOG.debug("Resolved constructor class '{}'", clazz);

                            LOG.debug("Constructor inputs '{}'", inputs.size() - 1);

                            // determine best Constructor candidate
                            ConstructorCallStatement constructorCallStatement;
                            if(inputs.size() - 1 > 0) {
                                Class[] parameterTypes = new Class[inputs.size() - 1];
                                int p = 0;
                                for(Cell input : inputs.subList(1, inputs.size())) {
                                    // reference
                                    if(isCellReference(input)) {
                                        CellAddress inputRef = new CellAddress(input.getStringCellValue());
                                        Cell resolvedCell = sheet.getRow(inputRef.getRow()).getCell(inputRef.getColumn());
                                        ValueStatement valueStatement = processValue(context, Object.class, resolvedCell);
                                        // determine type
                                        LOG.debug("input type resolved to {}", valueStatement.getType());

                                        parameterTypes[p] = valueStatement.getType();
                                    } else {
                                        // direct value passing
                                        ValueStatement valueStatement = processValue(context, Object.class, input);

                                        LOG.debug("input type resolved to {}", valueStatement.getType());

                                        parameterTypes[p] = valueStatement.getType();
                                    }

                                    p++;
                                }

                                constructorCallStatement = new ConstructorCallStatement(
                                        new ReflectionConstructorSignature(clazz.getConstructor(parameterTypes)));
                            } else {
                                constructorCallStatement = new ConstructorCallStatement(
                                        new ReflectionConstructorSignature(clazz.getConstructor()));
                            }

                            context.getLocalFields().add(result.getAddress());
                            int position = context.getLocalFields().lastIndexOf(result.getAddress());

                            context.getSequenceSpecification().addStatement(constructorCallStatement, position);

                            success = true;

                            // now determine inputs
                            if(constructorCallStatement.getResolvedConstructor().getParameterTypes().length > 0) {
                                int p = 0;
                                for(Cell input : inputs.subList(1, inputs.size())) {
                                    // reference
                                    if(isCellReference(input)) {
                                        //CellAddress inputRef = input.getAddress();
                                        CellAddress inputRef = new CellAddress(input.getStringCellValue());
                                        int inputPos = context.getLocalFields().lastIndexOf(inputRef);
                                        SpecificationStatement inputStmt = context.getSequenceSpecification().getStatement(inputPos);
                                        constructorCallStatement.addInput(inputStmt);
                                    } else {
                                        // direct value passing
                                        Class<?> expectedType = constructorCallStatement.getResolvedConstructor().getParameterTypes()[p];

                                        ValueStatement valueStatement = processValue(context, expectedType, input);
                                        //context.getSequenceSpecification().addStatement(callStatement, context.getSequenceSpecification().getNextPosition());
                                        constructorCallStatement.addInput(valueStatement);
                                    }

                                    p++;
                                }
                            }
                        } catch (ClassNotFoundException | NoSuchMethodException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else if(StringUtils.equalsIgnoreCase(operationName, "VALUE")) {
                // value statements (i.e local fields)
                Cell input = inputs.get(0);

                // reference
                if(isCellReference(input)) {
                    CellAddress inputRef = input.getAddress();
                    int inputPos = context.getLocalFields().lastIndexOf(inputRef);
                    SpecificationStatement inputStmt = context.getSequenceSpecification().getStatement(inputPos);

                    ValueStatement alias = new ValueStatement(null, null);
                    alias.setAlias(true);
                    alias.addInput(inputStmt);

                    context.getLocalFields().add(result.getAddress());
                    int position = context.getLocalFields().lastIndexOf(result.getAddress());

                    context.getSequenceSpecification().addStatement(alias, position);
                } else {
                    // direct value passing
                    ValueStatement valueStatement = processValue(context, Object.class, input);
                    context.getSequenceSpecification().addStatement(valueStatement, context.getSequenceSpecification().getNextPosition());
                }
            } else {
                LOG.debug("Looking for operation '{}'", operationName);

                // FIXME we need to check receiver of operation
                // ---------

                // determine receiver
                Cell receiver = inputs.get(0);

                LOG.debug("receiver is {}", receiver);

                if(!isCellReference(receiver)) {
                    throw new IllegalArgumentException("expected receiver CellId for " + receiver);
                }

                // add inputs
                SpecificationStatement receiverStmt = context.getSequenceSpecification().getStatement(context.getLocalFields().lastIndexOf(new CellAddress(receiver.getStringCellValue())));

                Optional<MethodSignature> methodSignatureOp = Optional.empty();
                if(receiverStmt.isClassUnderTest()) {
                    methodSignatureOp = context.getSpecification().getMethods().stream().filter(m -> {
                        // FIXME more precise: based on type matching as well
                        if (StringUtils.equalsIgnoreCase(m.getName(), operationName)) {
                            LOG.debug("Method params {} vs {}", m.getParameterTypes(classUnderTest.loadClassUnsafe()).length, inputs.size() - 1);
                            if(m.getParameterTypes(classUnderTest.loadClassUnsafe()).length == inputs.size() - 1) { // minus receiver instance
                                LOG.debug("Matched interface method '{}'", m.toLQL());

                                return true;
                            }
                        }

                        return false;
                    }).findFirst();
                }

                if(methodSignatureOp.isPresent()) {
                    // CUT method
                    MethodSignature methodSignature = methodSignatureOp.get();
                    MethodCallStatement callStatement = new MethodCallStatement(methodSignature);
                    callStatement.setClassUnderTest(true);

                    context.getLocalFields().add(result.getAddress());
                    int position = context.getLocalFields().lastIndexOf(result.getAddress());

//                    // determine receiver
//                    Cell receiver = inputs.get(0);
//                    String cellId = receiver.getStringCellValue();
//
//                    LOG.debug("receiver is {}", receiver);
//
//                    if(!isCellReference(receiver)) {
//                        throw new IllegalArgumentException("expected receiver CellId for " + receiver);
//                    }
//
//                    CellAddress ref = new CellAddress(cellId);

                    context.getSequenceSpecification().addStatement(callStatement, position);

                    // add inputs
                    //SpecificationStatement receiverStmt = context.getSequenceSpecification().getStatement(context.getLocalFields().lastIndexOf(ref));
                    callStatement.addInput(receiverStmt);

                    if(inputs.size() > 1) {
                        int p = 0;
                        for(Cell input : inputs.subList(1, inputs.size())) {
                            // reference
                            if(isCellReference(input)) {
                                LOG.debug("Method arg cell ref {}", input);

                                //CellAddress inputRef = input.getAddress();
                                CellAddress inputRef = new CellAddress(input.getStringCellValue());
                                int inputPos = context.getLocalFields().lastIndexOf(inputRef);

                                LOG.debug("input ref and position {} {}", inputRef, inputPos);

                                SpecificationStatement inputStmt = context.getSequenceSpecification().getStatement(inputPos);
                                callStatement.addInput(inputStmt);

                                LOG.debug("added stmt {}", inputStmt);

                                context.getSequenceSpecification().getStatements().stream().forEach(System.out::println);
                            } else {
                                // direct value passing
                                Class<?> expectedType = methodSignature.getParameterTypes(classUnderTest.loadClassUnsafe())[p];

                                ValueStatement valueStatement = processValue(context, expectedType, input);
                                callStatement.addInput(valueStatement);

                                if(LOG.isDebugEnabled()) {
                                    LOG.debug("Value statement '{}'", valueStatement);
                                }
                            }

                            p++;
                        }
                    }
                } else {
                    // other (e.g., we call a Map)
                    LOG.debug("Found unmapped operation '{}'", operationName);

                    LOG.debug("Potential callee of unmapped operation '{}'", inputs.get(0));

                    //SpecificationStatement resolved = context.getSequenceSpecification().getStatements().get(context.getSequenceSpecification().getStatements().size() -1 );

                    SpecificationStatement resolved = receiverStmt;

                    LOG.debug("Resolved '{}'", resolved);

                    ReflectionMethodSignature sig = null;
                    if(resolved instanceof MethodCallStatement) {
                        MethodCallStatement mcs = (MethodCallStatement) resolved;
                        Class returnClazz = mcs.getMethodSignature().getReturnType();

                        LOG.debug("Resolved return clazz '{}'", returnClazz);

                        // FIXME to error-prone
                        Method method = Arrays.stream(returnClazz.getMethods()).filter(
                                m -> StringUtils.equalsIgnoreCase(m.getName(), operationName) && m.getParameterTypes().length == (inputs.size() - 1)).findFirst().get();

                        sig = new ReflectionMethodSignature(method);

                        LOG.debug("Resolved '{}' to '{}'", operationName, method);
                    } else if(resolved instanceof ConstructorCallStatement) {
                        ConstructorCallStatement mcs = (ConstructorCallStatement) resolved;
                        Class returnClazz = mcs.getMethodSignature().getReturnType();

                        LOG.debug("Return type is '{}'", returnClazz);

                        // FIXME to error-prone
                        Method method = Arrays.stream(returnClazz.getMethods()).filter(
                                m -> StringUtils.equalsIgnoreCase(m.getName(), operationName) && m.getParameterTypes().length == (inputs.size() - 1)).findFirst().get();

                        sig = new ReflectionMethodSignature(method);

                        LOG.debug("Resolved '{}' to '{}'", operationName, method);
                    } else {
                        LOG.warn("is {}", resolved.getClass());

                        // FIXME
                        throw new UnsupportedOperationException("");
                    }

                    // CUT method
                    MethodCallStatement callStatement = new MethodCallStatement(sig);
                    callStatement.setClassUnderTest(false);

                    context.getLocalFields().add(result.getAddress());
                    int position = context.getLocalFields().lastIndexOf(result.getAddress());

//                    // determine receiver
//                    Cell receiver = inputs.get(0);
//                    String cellId = receiver.getStringCellValue();
//
//                    if(!isCellReference(receiver)) {
//                        throw new IllegalArgumentException("expected receiver CellId for " + receiver);
//                    }
//
//                    CellAddress ref = new CellAddress(cellId);

                    context.getSequenceSpecification().addStatement(callStatement, position);

                    // add inputs
                    //LOG.debug("Local fields is set to '{}'", context.getLocalFields().lastIndexOf(ref));
                    //SpecificationStatement receiverStmt = context.getSequenceSpecification().getStatement(context.getLocalFields().lastIndexOf(ref));

                    LOG.debug("Receiver is set to '{}'", receiverStmt);

                    callStatement.addInput(receiverStmt);

                    if(inputs.size() > 1) {
                        int p = 0;
                        for(Cell input : inputs.subList(1, inputs.size())) {
                            // reference
                            if(isCellReference(input)) {
                                //CellAddress inputRef = input.getAddress();
                                CellAddress inputRef = new CellAddress(input.getStringCellValue());
                                int inputPos = context.getLocalFields().lastIndexOf(inputRef);
                                SpecificationStatement inputStmt = context.getSequenceSpecification().getStatement(inputPos);
                                callStatement.addInput(inputStmt);
                            } else {
                                // direct value passing
                                Class<?> expectedType = callStatement.getResolvedMethod().getParameterTypes()[p];

                                ValueStatement valueStatement = processValue(context, expectedType, input);
                                callStatement.addInput(valueStatement);

                                if(LOG.isDebugEnabled()) {
                                    LOG.debug("Value statement '{}'", valueStatement);
                                }
                            }

                            p++;
                        }
                    }
                }
            }
        }

        return sequenceSpecification;
    }

    private ValueStatement processValue(SheetParseContext context, Class<?> expectedType, Cell cell) {
        Object value = null;
        String code = null;
        switch (cell.getCellType()) {
            case _NONE:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), "none");
                break;
            case NUMERIC:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getNumericCellValue());

                // always returns a double
                // FIXME empty numerical cells
                double num = cell.getNumericCellValue();

                if(expectedType.equals(float.class)) {
                    value = Double.valueOf(num).floatValue();
                } else if(expectedType.equals(long.class)) {
                    value = Double.valueOf(num).longValue();
                } else if(expectedType.equals(int.class)) {
                    value = Double.valueOf(num).intValue();
                } else if(expectedType.equals(short.class)) {
                    value = Double.valueOf(num).shortValue();
                } else if(expectedType.equals(byte.class)) {
                    value = Double.valueOf(num).byteValue();
                } else {
                    // double
                    value = num;
                }

                break;
            case STRING:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getStringCellValue());

                String stringValue = cell.getStringCellValue();
                // Excel encoding “Hello World!”
                if(StringUtils.startsWith(stringValue, "“") && StringUtils.endsWith(stringValue, "”")) {
                    stringValue = StringUtils.wrap(stringValue.substring(1, stringValue.length() - 1), '"');
                }

                // FIXME workaround to make primitive arrays work
                if(expectedType.isArray()) {
                    stringValue = "new " + expectedType.getCanonicalName() + stringValue.replace("[", "{").replace("]", "}");

                    LOG.debug("Array appended '{}'", stringValue);
                }

                try {
                    value = context.getEval().expr(stringValue);
                } catch (groovy.lang.MissingPropertyException e) {
                    LOG.warn("Eval failed. Falling to back to string value", e);

                    // FIXME fall back to string?
                    //value = stringValue;

                    throw e;
                }

                code = stringValue;

                break;
            case FORMULA:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getCellFormula());
                break;
            case BLANK:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), "blank");
                break;
            case BOOLEAN:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getBooleanCellValue());

                value = cell.getBooleanCellValue();

                break;
            case ERROR:
                LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getErrorCellValue());
                break;
            default:

        }

        // TODO why do we override?
        if(value != null) {
            // origin is Eval
            if(value instanceof BigDecimal && expectedType.equals(double.class)) {
                value = ((BigDecimal) value).doubleValue();
            } else if(value instanceof BigDecimal && expectedType.equals(float.class)) {
                value = ((BigDecimal) value).floatValue();
            } else if(value instanceof BigDecimal && expectedType.equals(long.class)) {
                value = ((BigDecimal) value).longValue();
            } else if(value instanceof BigDecimal && expectedType.equals(int.class)) {
                value = ((BigDecimal) value).intValue();
            } else if(value instanceof BigDecimal && expectedType.equals(short.class)) {
                value = ((BigDecimal) value).shortValue();
            } else if(value instanceof BigDecimal && expectedType.equals(byte.class)) {
                value = ((BigDecimal) value).byteValue();
            } else {
                expectedType = value.getClass();
            }
        }

        ValueStatement valueStatement = new ValueStatement(expectedType, value);
        valueStatement.setCode(code);

        return valueStatement;
    }

    public boolean isCellReference(Cell cell) {
        if(cell.getCellType() != CellType.STRING) {
            return false;
        }

        String cellId = cell.getStringCellValue();
        try {
            CellReference ref = new CellReference(cellId);
            return ref.getCol() >= 0 && ref.getRow() >= 0;
        } catch (Throwable e) {

            return false;
        }
    }
}
