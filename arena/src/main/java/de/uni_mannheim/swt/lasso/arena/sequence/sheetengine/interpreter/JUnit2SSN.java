package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.*;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.JUnitSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.transformer.JUnitToSsnTransformer;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.ssn.SheetResolver;

import java.io.IOException;
import java.util.*;

/**
 * Utilities for transforming JUnit test classes/methods to stimulus sheets
 *
 * @author Marcus Kessel
 */
public class JUnit2SSN {

    /**
     *
     * @deprecated use #newJunit2Sheets
     * @param testClass
     * @param classUnderTest
     * @param interfaceSpecification
     * @param adaptedImplementation
     * @param testPrefix
     * @return
     * @throws IOException
     */
    @Deprecated
    public static List<Sheet> junit2Sheets(String testClass, ClassUnderTest classUnderTest, InterfaceSpecification interfaceSpecification, AdaptedImplementation adaptedImplementation, String testPrefix) throws IOException {
        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();
        //importJUnitClass.setResolvePseudoOperations(true);

        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, interfaceSpecification, classUnderTest, classUnderTest.getClassName(), "");

        List<Sheet> stimulusSheets = new LinkedList<>();
        for(String name : ssMap.keySet()) {
            try {
                SequenceSpecification sequenceSpecification = ssMap.get(name);

                Sheet stimulusSheet = sequences2SheetsJSONL(sequenceSpecification, interfaceSpecification);
                // MUST BE UNIQUE
                String implId = LassoUtils.compactUUID(classUnderTest.getId());
                stimulusSheet.setSignature(testPrefix + "_" + implId + "_" + stimulusSheet.getSignature());

                stimulusSheets.add(stimulusSheet);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return stimulusSheets;
    }

    public static List<Sheet> newJunit2Sheets(String testClass, ClassUnderTest classUnderTest, InterfaceSpecification interfaceSpecification, AdaptedImplementation adaptedImplementation, String testPrefix) throws IOException {
        JUnitToSsnTransformer transformer = new JUnitToSsnTransformer();

        try {
            return transformer.transform(transformer.create(testClass, classUnderTest), interfaceSpecification.toLQL(), testPrefix);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return new LinkedList<>();
    }

    public static Sheet sequences2SheetsJSONL(SequenceSpecification sequenceSpecification, InterfaceSpecification interfaceSpecification) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String signature = sequenceSpecification.getName() + "()";

        List<Map<String, Object>> jsonRows = new ArrayList<>();

        List<SpecificationStatement> rows = sequenceSpecification.getStatements();

        // FIXME use interface signature instead of CUT signatures (we need to resolve it using adaptation strategy ...)

        // we need to preprocess to add statements for create if static methods used etc.
        List<SpecificationStatement> preprocessedRows = new LinkedList<>();
        Map<String, ConstructorCallStatement> constructorCallStatementMap = new LinkedHashMap<>();
        int skippedProcessedRows = 0;
        for(int r = 0; r < rows.size(); r++) {
            SpecificationStatement statement = rows.get(r);

            if(statement instanceof ConstructorCallStatement) {
                CallStatement m = (CallStatement) statement;

                // set service for create statement
                if(m.isClassUnderTest()) {
                    constructorCallStatementMap.put(interfaceSpecification.getClassName(), (ConstructorCallStatement) m);
                } else {
                    constructorCallStatementMap.put(m.getMethodSignature().getClassName(), (ConstructorCallStatement) m);
                }

                preprocessedRows.add(m);
            } else if(statement instanceof MethodCallStatement) {
                CallStatement m = (CallStatement) statement;

                preprocessedRows.add(m);

                boolean isStatic = m.getMethodSignature().isStatic();

                if(isStatic) {
                    // create new one if unavailable
                    String className = m.isClassUnderTest() ? interfaceSpecification.getClassName() : m.getMethodSignature().getClassName();
                    ConstructorCallStatement constructorCallStatement = constructorCallStatementMap.get(className);
                    if(constructorCallStatement == null) {
                        // FIXME DOES NOT WORK FOR STATIC NON-CUT METHODS
                        MethodSignature c = new MethodSignature(interfaceSpecification);
                        c.setClassName(className);
                        constructorCallStatement = new ConstructorCallStatement(c);
                        constructorCallStatement.setClassUnderTest(m.isClassUnderTest());

                        constructorCallStatementMap.put(className, constructorCallStatement);

                        int index = r == 0 ? 0 : Math.max(0, r -1 - skippedProcessedRows) ;
                        preprocessedRows.add(index, constructorCallStatement);
                    }

                    statement.getInputs().add(0, constructorCallStatement);
                }

            }  else if(statement instanceof ValueStatement) {
                // we can skip here
                skippedProcessedRows++;
                continue;
            } else if(statement instanceof ArraySetStatement) {
                System.out.println("ArraySetStatement " + statement.toString());

                preprocessedRows.add(statement);
            }
        }

        // reset positions
        for(int r = 0; r < preprocessedRows.size(); r++) {
            preprocessedRows.get(r).setPosition(r);
        }

        int skipRows = 0;
        for(int r = 0; r < preprocessedRows.size(); r++) {
            Map<String, Object> rowData = new LinkedHashMap<>();

            SpecificationStatement statement = preprocessedRows.get(r);

            int currentRow = r - skipRows;

            // fill first column with empty
            //System.out.println("first column missing " + SheetResolver.toColumnLabel(0) + SheetResolver.toRowLabel(currentRow));
            // FIXME set oracle values from spec
            rowData.put(SheetResolver.toColumnLabel(0) + SheetResolver.toRowLabel(currentRow), Collections.emptyMap());

            if(statement instanceof ConstructorCallStatement) {
                CallStatement m = (CallStatement) statement;
                MethodSignature methodSignature = m.getMethodSignature();

                rowData.put(SheetResolver.toColumnLabel(1) + SheetResolver.toRowLabel(currentRow), "create");

                // set service for create statement
                if(m.isClassUnderTest()) {
                    rowData.put(SheetResolver.toColumnLabel(2) + SheetResolver.toRowLabel(currentRow), interfaceSpecification.getClassName());

                    constructorCallStatementMap.put(interfaceSpecification.getClassName(), (ConstructorCallStatement) m);
                } else {
                    rowData.put(SheetResolver.toColumnLabel(2) + SheetResolver.toRowLabel(currentRow), m.getMethodSignature().getClassName());

                    constructorCallStatementMap.put(m.getMethodSignature().getClassName(), (ConstructorCallStatement) m);
                }

                // inputs
                for(int p = 0; p < statement.getInputs().size(); p++) {
                    int currentCol = 3 + p;

                    SpecificationStatement input = statement.getInputs().get(p);

                    if(input instanceof ValueStatement) {
                        ValueStatement valueStatement = (ValueStatement) input;
                        // ignore position
                        String value = valueStatement.getCode();

                        if(value == null) {
                            value = "null";
                        }

                        rowData.put(SheetResolver.toColumnLabel(currentCol) + SheetResolver.toRowLabel(currentRow), value);
                    } else if(input instanceof CallStatement) {
                        // CANNOT HAPPEN
                        //rowData.put(SheetResolver.toColumnLabel(currentCol) + SheetResolver.toRowLabel(r), SheetResolver.toColumnLabel(0) + SheetResolver.toRowLabel(input.getPosition()));
                    } else if(input instanceof ArraySetStatement) {
                        rowData.put(SheetResolver.toColumnLabel(currentCol) + SheetResolver.toRowLabel(currentRow), "ARRAY");
                    } else {
                        // TODO
                    }
                }


            } else if(statement instanceof MethodCallStatement) {
                CallStatement m = (CallStatement) statement;
                MethodSignature methodSignature = m.getMethodSignature();

                // operation
                if(m.isClassUnderTest()) {
                    rowData.put(SheetResolver.toColumnLabel(1) + SheetResolver.toRowLabel(currentRow), methodSignature.getName());
                } else {
                    rowData.put(SheetResolver.toColumnLabel(1) + SheetResolver.toRowLabel(currentRow), methodSignature.getName());
                }

                // inputs
                for(int p = 0; p < statement.getInputs().size(); p++) {
                    int currentCol = 2 + p;

                    SpecificationStatement input = statement.getInputs().get(p);

                    if(input instanceof ValueStatement) {
                        ValueStatement valueStatement = (ValueStatement) input;

                        String value = valueStatement.getCode();

                        if(value == null) {
                            value = "null";
                        }

                        rowData.put(SheetResolver.toColumnLabel(currentCol) + SheetResolver.toRowLabel(currentRow), value);

                    } else if(input instanceof CallStatement) {
                        rowData.put(SheetResolver.toColumnLabel(currentCol) + SheetResolver.toRowLabel(currentRow), SheetResolver.toColumnLabel(0) + SheetResolver.toRowLabel(input.getPosition() - skipRows));
                    } else if(input instanceof ArraySetStatement) {
                        rowData.put(SheetResolver.toColumnLabel(currentCol) + SheetResolver.toRowLabel(currentRow), "ARRAY");
                    } else {
                        // TODO
                    }
                }
            }  else if(statement instanceof ValueStatement) {
                // we can skip here
                skipRows++;

                continue;
            } else if(statement instanceof ArraySetStatement) {
                System.out.println("ArraySetStatement " + statement.toString());

                ArraySetStatement arraySetStatement = (ArraySetStatement) statement;

                rowData.put(SheetResolver.toColumnLabel(1) + SheetResolver.toRowLabel(currentRow), arraySetStatement.toString());
            }

            jsonRows.add(rowData);
        }

        StringBuilder jsonl = new StringBuilder();
        for (Map<String, Object> row : jsonRows) {
            Map<String, Map<String, Object>> m = new LinkedHashMap<>();
            m.put("cells", row);
            String json = objectMapper.writeValueAsString(m);

            jsonl.append(json);
            jsonl.append("\n");
        }

        String body = jsonl.toString();

        de.uni_mannheim.swt.lasso.core.dto.srm.Sheet stimulusSheet = new Sheet(signature, body, interfaceSpecification.toLQL());

        return stimulusSheet;
    }
}
