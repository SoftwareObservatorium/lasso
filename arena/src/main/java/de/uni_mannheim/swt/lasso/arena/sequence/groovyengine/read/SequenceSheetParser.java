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
package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.read;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.check.Oracle;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.*;
import de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.CallableSequence;
import de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.CallableStatement;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SpreadSheet;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Parse (spread)sheets into {@link TestSpec}s.
 *
 * @author Marcus Kessel
 */
public class SequenceSheetParser {

    private static final Logger LOG = LoggerFactory
            .getLogger(SequenceSheetParser.class);

    public Map<String, TestSpec> toTestSpec(SpreadSheet spreadSheet, ClassUnderTest classUnderTest) {
        Map<String, TestSpec> ssMap = new LinkedHashMap<>();

        for(Sheet sheet : spreadSheet.getSheets()) {
            TestSpec ts = toTestSpec(sheet, classUnderTest);
            ssMap.put(ts.getName(), ts);
        }

        return ssMap;
    }

    // pass 1
    public InterfaceSpecification toInterfaceSpecification(Sheet sheet) {
        InterfaceSpecification interfaceSpecification = new InterfaceSpecification();
        interfaceSpecification.setConstructors(new LinkedList<>());
        interfaceSpecification.setMethods(new LinkedList<>());

        Set<String> cSigsSeen = new HashSet<>();
        Set<String> mSigsSeen = new HashSet<>();
        for(Row row : sheet) {
            // operation
            Cell operation = row.getCell(1);
            if (operation == null) {
                //
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Operation does not exist. Skipping row '{}'", row.getRowNum());
                }

                continue;
            }

            String operationName = operation.getStringCellValue();

            // some spreadsheet editors convert a "->" to a "→"
            if(StringUtils.contains(operationName, "→")) {
                operationName = StringUtils.replace(operationName, "→", "->");
            }

            LOG.debug("operation '{}'", operationName);

//            // FIXME alternative: ClassName()
//            if(StringUtils.equalsIgnoreCase(operationName, "CREATE")) {
//                // constructor
//                // get first parameter to check if class under search
//                List<Cell> inputs = new LinkedList<>();
//                for(int i = 2; i < row.getLastCellNum(); i++) {
//                    Cell cell = row.getCell(i);
//                    inputs.add(row.getCell(i));
//                }
//            }

            // alternative notion by starting it with $ to denote a class under search
            if(StringUtils.startsWith(operationName, "$")) {
                if(StringUtils.isEmpty(interfaceSpecification.getClassName())) {
                    String className = StringUtils.substringBetween(operationName, "$", "(");
                    interfaceSpecification.setClassName(className);
                }

                // constructor
                try {
                    List<InterfaceSpecification> specs = CodeSearch.fromLQL("$ {"+ StringUtils.substringAfter(operationName, "$") +"}");
                    LOG.debug(specs.get(0).getConstructors().get(0).toLQL());

                    MethodSignature cSig = specs.get(0).getConstructors().get(0);
                    if(!cSigsSeen.contains(cSig.toLQL())) {
                        interfaceSpecification.getConstructors().add(cSig);
                        cSigsSeen.add(cSig.toLQL());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // method
                try {
                    List<InterfaceSpecification> specs = CodeSearch.fromLQL("$ {"+ operationName +"}");
                    LOG.debug(specs.get(0).getMethods().get(0).toLQL());

                    MethodSignature mSig = specs.get(0).getMethods().get(0);
                    if(!mSigsSeen.contains(mSig.toLQL())) {
                        interfaceSpecification.getMethods().add(mSig);
                        mSigsSeen.add(mSig.toLQL());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return interfaceSpecification;
    }

    public TestSpec toTestSpec(Sheet sheet, ClassUnderTest classUnderTest) {
        SequenceSpecification sequenceSpecification = new SequenceSpecification();
        sequenceSpecification.setName(sheet.getSheetName());

        TestSpec testSpec = new TestSpec();
        testSpec.setName(sheet.getSheetName());

        CallableSequence callableSequence = new CallableSequence();
        testSpec.setCallableSequence(callableSequence);

        Map<String, InterfaceSpecification> interfaces = new LinkedHashMap<>();
        testSpec.setInterfaces(interfaces);

        TestSpecParseContext context = new TestSpecParseContext();

        context.setClassUnderTest(classUnderTest);

        Oracle oracle = null;

        Set<String> cSigsSeen = new HashSet<>();
        Set<String> mSigsSeen = new HashSet<>();
        // set MethodSignature + if CUT
        for(Row row : sheet) {
            // result
            Cell result = row.getCell(0);
            if(result == null) {
                result = row.createCell(0);
            } else {
                if(oracle == null) {
                    oracle = new Oracle();
                    testSpec.setOracle(oracle);
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

            // some spreadsheet editors convert a "->" to a "→"
            if(StringUtils.contains(operationName, "→")) {
                operationName = StringUtils.replace(operationName, "→", "->");
            }

            // alternative notion by starting it with $ to denote a class under search
            if(StringUtils.startsWith(operationName, "$")) {
                String className = StringUtils.substringBetween(operationName, "$", "(");
                if (!interfaces.containsKey(className)) {
                    InterfaceSpecification interfaceSpecification = new InterfaceSpecification();
                    interfaceSpecification.setConstructors(new LinkedList<>());
                    interfaceSpecification.setMethods(new LinkedList<>());
                    interfaces.put(className, interfaceSpecification);

                    interfaceSpecification.setClassName(className);
                }

                InterfaceSpecification interfaceSpecification = interfaces.get(className);

                // identify constructor signatures
                MethodSignature cSigMatched;
                try {
                    List<InterfaceSpecification> specs = CodeSearch.fromLQL("$ {" + StringUtils.substringAfter(operationName, "$") + "}");
                    LOG.debug(specs.get(0).getConstructors().get(0).toLQL());

                    MethodSignature cSig = specs.get(0).getConstructors().get(0);
                    if (!cSigsSeen.contains(cSig.toLQL())) {
                        interfaceSpecification.getConstructors().add(cSig);
                        cSigsSeen.add(cSig.toLQL());

                        cSigMatched = cSig;
                    } else {
                        cSigMatched = interfaceSpecification.getConstructors().stream().filter(s -> StringUtils.equals(s.toLQL(), cSig.toLQL())).findFirst().get();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // callable statement
                // FIXME args
                CallableStatement c = callableSequence.fromCode(interfaceSpecification.getClassName() + "()"/*, Arrays.asList(stmt0)*/);
                c.setInterfaceSpecification(interfaceSpecification);
                c.setOperationId(interfaceSpecification.getConstructors().indexOf(cSigMatched));
            } else {
                // looking for method

                // determine receiver
                Cell receiver = inputs.get(0);
                LOG.debug("receiver is {}", receiver);

                if(!isCellReference(receiver)) {
                    throw new IllegalArgumentException("expected receiver CellId for " + receiver);
                }

                CellAddress resolvedReceiver = new CellAddress(receiver.getStringCellValue());
                LOG.debug("resolved receiver is {}", resolvedReceiver.getRow());

                // self/this instance (object)
                CallableStatement receiverStmt = callableSequence.getStatements().get(resolvedReceiver.getRow());

                InterfaceSpecification interfaceSpecification = receiverStmt.getInterfaceSpecification();

                // identify method signature
                MethodSignature mSigMatched;
                try {
                    List<InterfaceSpecification> specs = CodeSearch.fromLQL("$ {"+ operationName +"}");
                    LOG.debug(specs.get(0).getMethods().get(0).toLQL());

                    MethodSignature mSig = specs.get(0).getMethods().get(0);
                    if (!mSigsSeen.contains(mSig.toLQL())) {
                        interfaceSpecification.getMethods().add(mSig);
                        mSigsSeen.add(mSig.toLQL());

                        mSigMatched = mSig;
                    } else {
                        mSigMatched = interfaceSpecification.getMethods().stream().filter(s -> StringUtils.equals(s.toLQL(), mSig.toLQL())).findFirst().get();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // inputs (in addition to instance)
                List<CallableStatement> inputStmts = new LinkedList<>();
                inputStmts.add(receiverStmt);

                if(inputs.size() > 1) {
                    int p = 0;
                    for (Cell input : inputs.subList(1, inputs.size())) {
                        // reference
                        if (isCellReference(input)) {
                            LOG.debug("Method arg cell ref {}", input);

                            CellAddress inputRef = new CellAddress(input.getStringCellValue());
                            CallableStatement inputStmt = callableSequence.getStatements().get(inputRef.getRow());
                            inputStmts.add(inputStmt);
                        } else {
                            LOG.debug("Value arg cell {}", input);

                            // FIXME santitize value
                            String stringValue = input.getStringCellValue();
                            // Excel encoding “Hello World!”
                            if(StringUtils.startsWith(stringValue, "“") && StringUtils.endsWith(stringValue, "”")) {
                                stringValue = StringUtils.wrap(stringValue.substring(1, stringValue.length() - 1), '"');
                            }

                            CallableStatement inputStmt = callableSequence.fromCode(stringValue);

                            inputStmts.add(inputStmt);
                        }

                        p++;
                    }
                }


                // callable statement
                // FIXME additional inputs args
                CallableStatement c = callableSequence.fromCode(mSigMatched.getName() + "()", inputStmts);
                c.setInterfaceSpecification(interfaceSpecification);
                c.setOperationId(interfaceSpecification.getMethods().indexOf(mSigMatched));
            }

        }

        return testSpec;
    }

    private ValueStatement processValue(TestSpecParseContext context, Class<?> expectedType, Cell cell) {
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
