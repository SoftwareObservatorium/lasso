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

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.arena.ArenaUtils;
import de.uni_mannheim.swt.lasso.arena.Observation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.srm.CellId;

import de.uni_mannheim.swt.lasso.srm.CellValue;
import org.apache.commons.collections4.MapUtils;
import randoop.ExceptionalExecution;
import randoop.ExecutionOutcome;
import randoop.NormalExecution;
import randoop.NotExecuted;
import randoop.sequence.ExecutableSequence;
import randoop.sequence.Sequence;
import randoop.sequence.Statement;
import randoop.sequence.Variable;

import java.util.*;

/**
 * Record of a sequence.
 *
 * @author Marcus Kessel
 */
public class SequenceExecutionRecord {

    public static final String INSTANCE = "_INSTANCE_";
    private final SequenceSpecification sequenceSpecification;
    private final InterfaceSpecification specification;
    private final AdaptedImplementation adaptedImplementation;

    private Map<Integer, CallRecord> records = new LinkedHashMap<>();

    private Sequence sequence;
    private ExecutableSequence executableSequence;

    private Map<String, Observation> observations = new LinkedHashMap<>();

    /**
     * Serialize inputs as well
     */
    private boolean serializeInputs = true;

    /**
     * Serialize operations
     */
    private boolean serializeOperations = true;

    public SequenceExecutionRecord(SequenceSpecification sequenceSpecification, InterfaceSpecification specification, AdaptedImplementation adaptedImplementation) {
        this.sequenceSpecification = sequenceSpecification;
        this.specification = specification;
        this.adaptedImplementation = adaptedImplementation;
    }

    public void add(CallRecord record) {
        records.put(record.getPosition(), record);
    }

    public CallRecord getRecord(SpecificationStatement statement) {
        return records.values().stream().filter(s -> statement.equals(s.getStatement())).findFirst().orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    public Optional<CallRecord> getFirstCutInstanceRecord() {
        return records.values().stream().filter(s -> {
                    if((s.getStatement() instanceof ConstructorCallStatement)) {
                        ConstructorCallStatement c = (ConstructorCallStatement) s.getStatement();
                        return c.isClassUnderTest();
                    }

                    return false;
                }).findFirst();
    }

    public CallRecord getRecord(int position) {
        return records.get(position);
    }

    public Map<CellId, de.uni_mannheim.swt.lasso.srm.CellValue> toSheetCells() {
        Map<CellId, de.uni_mannheim.swt.lasso.srm.CellValue> cells = new LinkedHashMap<>();

        if (MapUtils.isNotEmpty(records)) {
            for (int stmt : records.keySet()) {
                int colId = 0;

                CallRecord record = records.get(stmt);

                // original position
                int position = record.getStatement().getPosition();

                if(position < 0) {
                    System.out.println("WARN: Position is less than 0 " + position + " - " + record.getStatement().getClass());
                }
//                else {
//                    System.out.println("Position is positive " + position);
//                }

                SpecificationStatement specStmt = record.getStatement();

                CellId cellId = ArenaUtils.cellIdOf(String.valueOf(sequenceSpecification.getName()),
                        colId,
                        position,
                        "value",
                        adaptedImplementation);
                de.uni_mannheim.swt.lasso.srm.CellValue cellValue = null;

                ExecutionOutcome outcome = executableSequence.getResult(stmt);
                Statement statement = executableSequence.sequence.statements.get(stmt);

                // operation
                if(isSerializeOperations()) {
                        try {
                            CellId opCellId = ArenaUtils.cellIdOf(String.valueOf(sequenceSpecification.getName()),
                                    1, // second column
                                    position,
                                    "op",
                                    adaptedImplementation);
                            de.uni_mannheim.swt.lasso.srm.CellValue opCellValue = new CellValue();

                            opCellValue.setValueType(statement.getDeclaringClass().getCanonicalName());

                            String value = new Gson().toJson(statement.getOperation().getSignatureString());
                            opCellValue.setRawValue(value);
                            opCellValue.setValue(value);

                            if (opCellValue != null) {
                                cells.put(opCellId, opCellValue);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                }

                // inputs for each statement
                if(isSerializeInputs()) {
                    //Object[] inputs = executableSequence.getRuntimeInputs(executableSequence.sequence.getInputs(stmt));

                    for(Variable variable : executableSequence.sequence.getInputs(stmt)) {
                        int i = variable.getDeclIndex();

                        executableSequence.getResult(i);
                    }

                    // input variables
                    int i = 2;
                    for(Variable variable : executableSequence.sequence.getInputs(stmt)) {
                        try {
                            // get outcome of statement
                            ExecutionOutcome inOutcome = executableSequence.getResult(variable.getDeclIndex());
                            Statement inStatement = variable.getDeclaringStatement();

                            CellId inputCellId = ArenaUtils.cellIdOf(String.valueOf(sequenceSpecification.getName()),
                                    i++, // starts after output column and operation column
                                    position,
                                    "input_value",
                                    adaptedImplementation);
                            de.uni_mannheim.swt.lasso.srm.CellValue inputCellValue = null;

                            if (inOutcome instanceof NormalExecution) {
                                Object val = ((NormalExecution) inOutcome).getRuntimeValue();

                                inputCellValue = new CellValue();

                                inputCellValue.setValueType(inStatement.getOutputType().getCanonicalName());

                                String value = new Gson().toJson(val);
                                inputCellValue.setRawValue(value);
                                inputCellValue.setValue(value);
                            }

                            //
                            if (inOutcome instanceof ExceptionalExecution) {
                                Throwable val = ((ExceptionalExecution) inOutcome).getException();

                                inputCellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of(val == null ? "null" : val.toString());

                                inputCellValue.setValueType(inStatement.getOutputType().getCanonicalName());
                                inputCellValue.setRawValue(val == null ? "null" : val.getClass().getCanonicalName());
                                inputCellValue.setValue("_EXCEPTION_");
                            }

                            //
                            if (inOutcome instanceof NotExecuted) {
                                inputCellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of("_NOT_EXECUTED_");
                            }

                            if (inputCellValue != null) {
                                // set execution time
                                try {
                                    inputCellValue.setExecutionTime(inOutcome.getExecutionTime());
                                } catch (Throwable e) {
                                    inputCellValue.setExecutionTime(-1);
                                }

                                cells.put(inputCellId, inputCellValue);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }

                // output
                if (outcome instanceof NormalExecution) {
                    Object val = ((NormalExecution) outcome).getRuntimeValue();

                    //if (statement.isConstructorCall()) {
                    if (specStmt instanceof ConstructorCallStatement) {
                        //cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of(val.getClass().getCanonicalName());
                        cellValue = new CellValue();
                        cellValue.setValueType(statement.getOutputType().getCanonicalName());
                        cellValue.setRawValue(val.getClass().getCanonicalName());
                        cellValue.setValue(INSTANCE);
                    } else {
                        cellValue = new CellValue();
                        cellValue.setValueType(statement.getOutputType().getCanonicalName());

//                        boolean isArray = val != null && val.getClass().isArray();
//                        if(!isArray) {
//                            String value = val == null ? "null" : val.toString();
//                            cellValue.setRawValue(value);
//                            cellValue.setValue(value);
//                        } else {
//                            //cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of(new Gson().toJson(val));
//                            String value = new Gson().toJson(val);
//                            cellValue.setRawValue(value);
//                            cellValue.setValue(value);
//                        }

                        String value = new Gson().toJson(val);
                        cellValue.setRawValue(value);
                        cellValue.setValue(value);
                    }
                }

                //
                if (outcome instanceof ExceptionalExecution) {
                    Throwable val = ((ExceptionalExecution) outcome).getException();

                    cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of(val == null ? "null" : val.toString());

                    cellValue.setValueType(statement.getOutputType().getCanonicalName());
                    cellValue.setRawValue(val == null ? "null" : val.getClass().getCanonicalName());
                    cellValue.setValue("_EXCEPTION_");
                }

                //
                if (outcome instanceof NotExecuted) {
                    cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of("_NOT_EXECUTED_");
                }

                if (cellValue != null) {
                    // set execution time
                    try {
                        cellValue.setExecutionTime(outcome.getExecutionTime());
                    } catch (Throwable e) {
                        cellValue.setExecutionTime(-1);
                    }

                    cells.put(cellId, cellValue);
                }
            }
        }

        // FIXME check if we are mutant
        //!"original".equals(adaptedImplementation.getAdaptee().getVariantId());

        if(sequenceSpecification.hasOracle()) {
            try {
                Map<CellId, CellValue> values = writeOracle(); // FIXME this should be an "adapted" oracle particular to the adapted implementation
                cells.putAll(values);
            } catch (Throwable e) {

            }
        }

        return cells;
    }

    public Map<CellId, CellValue> writeSequenceRecord() {
        Map<CellId, de.uni_mannheim.swt.lasso.srm.CellValue> cells = new LinkedHashMap<>();
        CellId cellIdSeq = ArenaUtils.cellIdOf(String.valueOf(sequenceSpecification.getName()),
                -1,
                -1,
                "seq",
                adaptedImplementation);

        de.uni_mannheim.swt.lasso.srm.CellValue cellValueSeq = new CellValue();
        cellValueSeq.setValue(this.executableSequence.sequence.toCodeString());
        cells.put(cellIdSeq, cellValueSeq);

        CellId cellIdExSeq = ArenaUtils.cellIdOf(String.valueOf(sequenceSpecification.getName()),
                -1,
                -1,
                "exseq",
                adaptedImplementation);

        de.uni_mannheim.swt.lasso.srm.CellValue cellValueExSeq = new CellValue();
        cellValueExSeq.setValue(this.executableSequence.toCodeString());
        cells.put(cellIdExSeq, cellValueExSeq);

        return cells;
    }

    public Map<CellId, CellValue> writeOracle() {
        if(!sequenceSpecification.hasOracle()) {
            return Collections.emptyMap();
        }

        Map<CellId, de.uni_mannheim.swt.lasso.srm.CellValue> cells = new LinkedHashMap<>();

        if (MapUtils.isNotEmpty(records)) {
            for (int stmt : records.keySet()) {
                int colId = 0;

                CallRecord record = records.get(stmt);

                // original position
                int position = record.getStatement().getPosition();

                SpecificationStatement specStmt = record.getStatement();

                CellId cellId = ArenaUtils.cellIdOfOracle(String.valueOf(sequenceSpecification.getName()),
                        colId,
                        position,
                        "value"); //,                        adaptedImplementation);

                //Statement statement = executableSequence.sequence.statements.get(stmt);

                de.uni_mannheim.swt.lasso.srm.CellValue cellValue = new CellValue();

                if(sequenceSpecification.getOracle().hasOracle(position)) {
                    ValueStatement valueStatement = sequenceSpecification.getOracle().getExpectedValueForStatement(position);

                    Object val = valueStatement.getValue();

                    //if (statement.isConstructorCall()) {
                    if (specStmt instanceof ConstructorCallStatement) {
                        cellValue.setValueType(null);
                        cellValue.setRawValue(null);
                        cellValue.setValue(INSTANCE);
                    } else {
                        if(val != null) {
                            cellValue.setValueType(val.getClass().getCanonicalName());
                        }

                        String value = new Gson().toJson(val);
                        cellValue.setRawValue(value);
                        cellValue.setValue(value);
                    }

//                    boolean isArray = val != null && val.getClass().isArray();
//                    if(!isArray) {
//                        cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of(val == null ? "null" : val.toString());
//                    } else {
//                        cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of(new Gson().toJson(val));
//                    }
//
//                    if(val != null) {
//                        cellValue.setRawValue(val == null ? "null" : val.toString());
//                        //cellValue.setValueType(statement.getOutputType().getCanonicalName());
//                        cellValue.setValueType(val.getClass().getCanonicalName());
//                    }
//                    //cellValue.setValue("_INSTANCE_");


                } else {
                    cellValue = de.uni_mannheim.swt.lasso.srm.CellValue.of("_NA_");
                }

                if (cellValue != null) {
                    cells.put(cellId, cellValue);
                }
            }
        }

        return cells;
    }

    public void addObservation(String name, Observation observation) {
        observations.put(name, observation);
    }

    public Map<String, Observation> getObservations() {
        return observations;
    }

    public boolean isExecuted() {
        return executableSequence != null;
    }

    public Sequence getSequence() {
        return sequence;
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public SequenceSpecification getSequenceSpecification() {
        return sequenceSpecification;
    }

    public ExecutableSequence getExecutableSequence() {
        return executableSequence;
    }

    public void setExecutableSequence(ExecutableSequence executableSequence) {
        this.executableSequence = executableSequence;
    }

    public void setSerializeInputs(boolean serializeInputs) {
        this.serializeInputs = serializeInputs;
    }

    public boolean isSerializeInputs() {
        return serializeInputs;
    }

    public boolean isSerializeOperations() {
        return serializeOperations;
    }

    public void setSerializeOperations(boolean serializeOperations) {
        this.serializeOperations = serializeOperations;
    }
}
