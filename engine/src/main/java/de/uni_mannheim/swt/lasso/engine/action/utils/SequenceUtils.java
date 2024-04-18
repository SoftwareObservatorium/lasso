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
package de.uni_mannheim.swt.lasso.engine.action.utils;

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.benchmark.*;
import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.arena.Sheet2XSLX;
import de.uni_mannheim.swt.lasso.lql.LQLLexer;
import de.uni_mannheim.swt.lasso.lql.LQLParser;
import de.uni_mannheim.swt.lasso.lql.listener.InterfaceListener;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import de.uni_mannheim.swt.lasso.lsl.spec.SheetSpec;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marcus Kessel
 */
public class SequenceUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(SequenceUtils.class);

    private static Gson GSON = new Gson();

    /**
     * Write sequences as XLSX (sequence sheets).
     *
     * @param systems
     * @param sequences
     * @param specification
     */
    public static void writeSequences2Xlsx(Action action, Systems systems, List<de.uni_mannheim.swt.lasso.benchmark.Sequence> sequences, Specification specification) {
        Test2XSLX test2XSLX = new Test2XSLX();

        int s = 0;
        for (de.uni_mannheim.swt.lasso.benchmark.Sequence sequence : sequences) {
            String name = sequence.getId() + "_" + s;
            LOG.info("Writing sheet '{}'", name);

            de.uni_mannheim.swt.lasso.core.model.Sequence seq = new de.uni_mannheim.swt.lasso.core.model.Sequence();
            seq.setName(name);
            seq.setId(action.getName() + "_" + name);
            seq.setActionId(action.getName());
            systems.addSequence(seq);

            try {
                XSSFSheet xssfSheet = test2XSLX.createSheet(sequence, specification, name);

                for (System executable : systems.getExecutables()) {
                    try {
                        LOG.info("Writing sheet '{}' for '{}'", name, executable.getId());

                        test2XSLX.write(executable, xssfSheet, name);
                    } catch (Throwable e) {
                        LOG.warn("Failed to write sheet for '{}'", executable.getId());
                        LOG.warn("stack trace", e);
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Failed to write sheet '{}'", name);
                LOG.warn("stack trace", e);
            }

            s++;
        }
    }

    /**
     * Generate sequences based on list of lists. Each nested list contains alternative statements.
     *
     * @param action
     * @param statementList
     * @return
     */
    public static List<Sequence> generateLinearSequences(Action action, int noOfTests, List<List<Statement>> statementList) {
        List<Sequence> randomList = new ArrayList<>();

        for (int i = 0; i < noOfTests; i++) {
            Sequence sequence = new Sequence();
            sequence.setId(LassoUtils.compactUUID(UUID.randomUUID().toString()) + "_" + action.getName());

            for (List<Statement> statements : statementList) {
                if (statements.isEmpty()) {
                    throw new IllegalArgumentException("Nested list cannot be empty");
                }

                // we ran out of statements
                if (i == statements.size()) {
                    return randomList;
                }

                sequence.addStatement(statements.get(i));
            }

            randomList.add(sequence);
        }

        return randomList;
    }

    /**
     * Write {@link Sequence} to JSON
     *
     * @param sequence
     * @return
     */
    public static String toJson(Sequence sequence) {
        String json = GSON.toJson(sequence);

        return json;
    }

    /**
     * Write {@link Sequence} to JSON file
     *
     * @param sequence
     * @param toFile
     * @throws IOException
     */
    public static void toJson(Sequence sequence, File toFile) throws IOException {
        GSON.toJson(sequence, new FileWriter(toFile));
    }

    /**
     * Read from JSON
     *
     * @param json
     * @return
     */
    public static Sequence fromJson(String json) {
        return GSON.fromJson(json, Sequence.class);
    }

    /**
     * Collect sequences
     *
     * @param context
     * @param systems
     * @param benchmark
     * @param sequenceActions
     * @return
     */
    public static List<Sequence> collectSequences(LSLExecutionContext context, Systems systems, String benchmark, List<String> sequenceActions) {
        List<Sequence> sequenceList = new ArrayList<>();
        if (StringUtils.isNotBlank(benchmark)) {
            sequenceList.addAll(loadSequencesFromBenchmark(context, benchmark, systems.getAbstractionName()));
        }

        // obtain existing tests from store / database
        // check previous actions
        sequenceList.addAll(collectSequences(context, systems, sequenceActions));

        return sequenceList;
    }

    /**
     * Collect {@link Sequence} from DFS.
     *
     * @param context
     * @param systems
     * @param sequenceActions
     * @return
     */
    public static List<Sequence> collectSequences(LSLExecutionContext context, Systems systems, List<String> sequenceActions) {
        List<Sequence> sequenceList = new ArrayList<>();

        // obtain existing tests from store / database
        // check previous actions
        if (CollectionUtils.isNotEmpty(sequenceActions)) {
            String executionId = context.getExecutionId();
            String abstractionId = systems.getAbstractionName();
            for (String actionId : sequenceActions) {
                // may be an external study
                ActionRef actionRef = ActionRef.from(actionId);
                if (actionRef.hasExecutionId()) {
                    executionId = actionRef.getExecutionId();
                }

                try {
                    // read
                    List<Sequence> actionSequences = DistributedFileSystemUtils.readSequences(context,
                            executionId, actionRef.getActionId(), abstractionId);

                    LOG.info("Adding {} from action {} for abstraction {}", actionSequences.size(), actionId, abstractionId);

                    sequenceList.addAll(actionSequences);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return sequenceList;
    }

    /**
     * Load {@link Sequence}s from benchmark.
     *
     * @param context
     * @param benchmark
     * @param problemId
     * @return
     */
    public static List<Sequence> loadSequencesFromBenchmark(LSLExecutionContext context, String benchmark, String problemId) {
        LOG.info("Trying to load sequences from benchmark '{}' using abstraction '{}'", benchmark, problemId);
        Benchmark b = context.getBenchmarkManager().load(benchmark);

        FunctionalAbstraction ab = b.getAbstractions().get(problemId);
        return ab.getSequences();
    }

    /**
     * Load {@link Specification} from benchmark.
     *
     * @param context
     * @param benchmark
     * @param problemId
     * @return
     */
    public static Specification loadSpecificationFromBenchmark(LSLExecutionContext context, String benchmark, String problemId) {
        Benchmark b = context.getBenchmarkManager().load(benchmark);
        FunctionalAbstraction ab = b.getAbstractions().get(problemId);

        Specification specification = new Specification();
        Interface iFace = parseLQL(ab.getLql()).getInterfaceSpecification();
        specification.setInterfaceSpecification(iFace);

        return specification;
    }

    /**
     * Parse {@link Specification} from LQL.
     *
     * @param lql
     * @return
     */
    public static Specification parseSpecificationFromLQL(String lql) {
        Specification specification = new Specification();
        Interface iFace = parseLQL(lql).getInterfaceSpecification();
        specification.setInterfaceSpecification(iFace);

        return specification;
    }

    /**
     * Parse LQL
     *
     * @param lql
     * @return
     */
    public static LQLParseResult parseLQL(String lql) {
        LQLLexer lexer = new LQLLexer(CharStreams.fromString(lql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LQLParser parser = new LQLParser(tokens);
        LQLParser.ParseContext tree = parser.parse();

        ParseTreeWalker walker = new ParseTreeWalker();
        InterfaceListener listener = new InterfaceListener();

        walker.walk(listener, tree);

        listener.getParseResult().getInterfaceSpecification().setLqlQuery(lql);

        return listener.getParseResult();
    }

    /**
     * Sequence sheets to {@link Sequence} model.
     *
     * @param sheets
     * @param iFace
     * @return
     */
    public static List<Sequence> toSequences(Map<String, Object> sheets, Interface iFace) {
        List<Sequence> sequences = new ArrayList<>();
        if (MapUtils.isNotEmpty(sheets)) {
            for (String name : sheets.keySet()) {
                Object sheet = sheets.get(name);

                LOG.debug("sheet '{}' is '{}'", name, sheet);

                if (sheet instanceof String) {
                    //
                    throw new UnsupportedOperationException("currently not implemented");
                }

                if (sheet instanceof SheetSpec) {
                    SheetSpec spec = (SheetSpec) sheet;

                    LOG.info("Translating sheet '{}' with input parameters '{}'", name, spec.getInputParameters());

                    Sequence sequence = toSequence(spec, name, iFace);
                    sequences.add(sequence);
                }
            }
        }

        return sequences;
    }

    public static void toXlsx(Map<String, Object> sheets, Action action, Systems systems) {
        Sheet2XSLX sheet2XSLX = new Sheet2XSLX();

        for (String name : sheets.keySet()) {
            Object sheet = sheets.get(name);

            LOG.debug("sheet '{}' is '{}'", name, sheet);

            if (sheet instanceof String) {
                //
                throw new UnsupportedOperationException("currently not implemented");
            }

            if (sheet instanceof SheetSpec) {
                SheetSpec spec = (SheetSpec) sheet;

                LOG.info("Writing sheet '{}' with input parameters '{}'", name, spec.getInputParameters());

                de.uni_mannheim.swt.lasso.core.model.Sequence sequence = new de.uni_mannheim.swt.lasso.core.model.Sequence();
                sequence.setName(name);
                sequence.setId(action.getName() + "_" + name);
                sequence.setActionId(action.getName());
                systems.addSequence(sequence);

                try {
                    XSSFSheet xssfSheet = sheet2XSLX.createSheet(spec, name);

                    for (System executable : systems.getExecutables()) {
                        try {
                            LOG.info("Writing sheet '{}' for '{}'", name, executable.getId());

                            sheet2XSLX.write(executable, xssfSheet, name);
                        } catch (Throwable e) {
                            LOG.warn("Failed to write sheet for '{}'", executable.getId());
                            LOG.warn("stack trace", e);
                        }
                    }
                } catch (Throwable e) {
                    LOG.warn("Failed to write sheet '{}'", name);
                    LOG.warn("stack trace", e);
                }
            }
        }
    }

    /**
     * Translates {@link SheetSpec} to {@link Sequence}.
     *
     * @param spec
     * @param name
     * @param iFace
     * @return
     */
    public static Sequence toSequence(SheetSpec spec, String name, Interface iFace) {
        Sequence sequence = new Sequence();
        sequence.setId(name);

        Map<String, ?> inputParameters = spec.getInputParameters();
        List<Object[]> rows = spec.getRows();

        for(int r = 0; r < rows.size(); r++) {
            Object[] row = rows.get(r);

            // operation
            String operation = (String) row[1];

            boolean isCreate = StringUtils.equalsIgnoreCase(operation, "CREATE");
            if(isCreate) {
                continue;
            }

            Statement statement = new Statement();
            sequence.addStatement(statement);
            statement.setOperation(operation);

            // result
            Object result = row[0];
//            if(isCreate) {
//                result = INSTANCE; // sync oracle value for constructor calls
//            }

            // is parameter placeholder? e.g., "?param"
            if(result instanceof String && StringUtils.startsWith((String) result, "?")) {
                String paramName = StringUtils.substringAfter((String) result, "?");
                if(inputParameters.containsKey(paramName)) {
                    if(LOG.isInfoEnabled()) {
                        Object paramValue = inputParameters.get(paramName);
                        LOG.info("Resolved parameter in first column from '{}' to '{}'", paramName, paramValue);
                        result = paramValue;
                    }
                }
            }

            // FIXME weak .. could be more .. also check types
            Optional<MethodSignature> methodSignatureOp = SequenceUtils.findFirstMethodSignature(
                    iFace, operation);

            MethodSignature methodSignature = methodSignatureOp.get();
            Value output = new Value();
            output.setValue(result);
            output.setType(methodSignature.getOutputs().get(0));
            statement.setExpectedOutputs(Arrays.asList(output));

            if(row.length > 2) {
                // inputs

                List<Value> inputs = new LinkedList<>();
                statement.setInputs(inputs);
                for (int col = 3; col < row.length; col++) {
                    Object val = row[col];

                    // is parameter placeholder? e.g., "?param"
                    if(val instanceof String && StringUtils.startsWith((String) val, "?")) {
                        String paramName = StringUtils.substringAfter((String) val, "?");
                        if(inputParameters.containsKey(paramName)) {
                            if(LOG.isInfoEnabled()) {
                                Object paramValue = inputParameters.get(paramName);
                                LOG.info("Resolved parameter '{}' to '{}'", paramName, paramValue);
                                val = paramValue;
                            }
                        }
                    }

//                    if(isCreate) {
//                        // TODO do we need that?
//                        // special case: no "wrap"
//                        if(col == 2) {
//                            inputCell.setCellValue((String) val);
//                        } else {
//                            setCell(val, inputCell);
//                        }
//                        //
//                    } else {
//                        setCell(val, inputCell);
//                    }

                    Value input = new Value();
                    input.setValue(val);
                    input.setType(methodSignature.getInputs().get(col - 3));

                    inputs.add(input);
                }
            }
        }

        return sequence;
    }

    /**
     * Returns the first {@link MethodSignature} match based on given operation name (FIXME inexact!)
     *
     * @param iFace
     * @param operation
     * @return
     */
    public static Optional<MethodSignature> findFirstMethodSignature(Interface iFace, String operation) {
        Optional<MethodSignature> methodSignatureOp = iFace.getMethods().stream()
                .filter(m -> StringUtils.equals(m.getName(), operation))
                .findFirst();

        return methodSignatureOp;
    }

    /**
     * Resolve {@link Interface} specification.
     *
     * @param context
     * @param systems
     * @param benchmark
     * @return
     */
    public static Interface resolveInterface(LSLExecutionContext context, Systems systems, String benchmark) {
        Interface iFace = systems.getSpecification().getInterfaceSpecification();
        if(StringUtils.isNotBlank(benchmark)) {
            LOG.info("Loading specification from benchmark {} for abstraction {}", benchmark, systems.getAbstractionName());

            Specification bSpec = SequenceUtils.loadSpecificationFromBenchmark(context, benchmark, systems.getAbstractionName());
            iFace = bSpec.getInterfaceSpecification();
        }

        return iFace;
    }

    public static String translateInputs(Statement statement) {
        String testSamples = "(" + statement.getInputs().stream()
                .map(v -> {
//                    Object val = v.getValue();
//                    if (val == null) {
//                        return "null";
//                    }
//
//                    if(val.getClass().isArray()) {
//                        return GSON.toJson(val);
//                    }
//
//                    if (val instanceof String) {
//                        return "\"" + val + "\"";
//                    }
//
//                    if(val instanceof Number) {
//
//                    }
//
//                    return Objects.toString(v.getValue());

                    return GSON.toJson(v.getValue());
                })
                .collect(Collectors.joining(","))  + ")";

        return testSamples;
    }
}
