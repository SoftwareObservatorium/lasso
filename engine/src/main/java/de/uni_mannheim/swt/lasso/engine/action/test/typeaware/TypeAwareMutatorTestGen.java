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
package de.uni_mannheim.swt.lasso.engine.action.test.typeaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.classloader.Container;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.utils.ClazzContainerUtils;
import de.uni_mannheim.swt.lasso.ssn.ParsedCell;
import de.uni_mannheim.swt.lasso.ssn.ParsedRow;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;
import de.uni_mannheim.swt.lasso.ssn.SSNParser;
import de.uni_mannheim.swt.lasso.ssn.eval.BshEval;
import de.uni_mannheim.swt.lasso.ssn.eval.Eval;
import de.uni_mannheim.swt.lasso.testing.generate.typeaware.TypeAwareMutator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Test generation based on a type-aware mutator for test inputs. See {@link TypeAwareMutator}.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Type-aware mutator for test generation")
@Stable
@Local
public class TypeAwareMutatorTestGen extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(TypeAwareMutatorTestGen.class);

    @LassoInput(desc = "how many tests to generate", optional = true)
    public int noOfTests = 1;

    @LassoInput(desc = "dependencies to resolve for types", optional = true)
    public List<String> dependencies = Arrays.asList("org.javatuples:javatuples:1.2");

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        // custom class loader for special types
        Container container = ClazzContainerUtils.createClazzContainer(context, dependencies);

        // get original sequences
        List<Sheet> stimulusSheets = abstraction.getSpecification().getTests();

        // only for SSN sheets right now
        List<Sheet> ssnSheets = stimulusSheets.stream().filter(Sheet::isSSN).toList();

        // mutate sequences
        List<Sheet> mutatedSheets = generate(context, abstraction.getSpecification(), ssnSheets, container);
        // set to FA
        abstraction.getSpecification().getTests().addAll(mutatedSheets);

        // set executables
        setExecutables(Systems.fromAbstraction(abstraction, getName()));

        // cleanup
        ClazzContainerUtils.dispose(container);
    }

    /**
     * Generate test sequences based on {@link Sequence}s.
     *
     * @param context
     * @param specification
     * @param originalSheets
     * @param container
     * @return
     */
    protected List<Sheet> generate(LSLExecutionContext context, Specification specification, List<Sheet> originalSheets, Container container) {
        LOG.debug("Found specification in abstraction '{}'", specification.getInterfaceSpecification());

        TypeAwareMutator mutator = new TypeAwareMutator();

        ObjectMapper objectMapper = new ObjectMapper();
        Eval eval = new BshEval();
        eval.setClassLoader(container);
        SSNParser ssnParser = new SSNParser();

        List<Map<String, Object>> jsonRows = new ArrayList<>();

        List<Sheet> mutatedSheets = new ArrayList<>();
        for (int i = 0; i < noOfTests; i++) {
            for(Sheet originalSheet : originalSheets) {
                LOG.debug("Original sheet: {}", originalSheet.getSignature());
                LOG.debug("{}", originalSheet.getBody());

                try {
                    ParsedSheet parsedSheet = ssnParser.parseJsonl(originalSheet);

                    for(int r = 0; r < parsedSheet.getRows().size(); r++) {
                        Map<String, Object> rowData = new LinkedHashMap<>();

                        ParsedRow parsedRow = parsedSheet.getRows().get(r);

                        // do not take oracle value!
                        //rowData.put(parsedRow.getOutput().getKey(), parsedRow.getOutput().getNodeValue());

                        rowData.put(parsedRow.getOperation().getKey(), parsedRow.getOperation().getNodeValue());
                        rowData.put(parsedRow.getCells().get(2).getKey(), parsedRow.getCells().get(2).getNodeValue());

                        if(!StringUtils.equalsAnyIgnoreCase(parsedRow.getOperation().getNodeValue().asText(), "$create", "create")) {
                            for (ParsedCell parameter : parsedRow.getInputs()) {

                                if (parameter.isTestParameter() || parameter.isValueReference()) {
                                    rowData.put(parameter.getKey(), parameter.getNodeValue());
                                    continue;
                                }

                                LOG.debug("json {}", parameter.getNodeValue());

                                // mutate evaluated code expression
                                String codeExpression = parameter.getNodeValue().asText();
                                Object object = eval.eval(codeExpression);

                                Object mutatedValue = mutator.mutateValue(object);

                                String jsonValue = objectMapper.writeValueAsString(mutatedValue);
                                rowData.put(parameter.getKey(), jsonValue);
                            }
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

                    String signature = "mutated_" + originalSheet.getSignature();

                    Sheet stimulusSheet = new Sheet(signature, body, specification.getInterfaceSpecification().getLqlQuery());
                    mutatedSheets.add(stimulusSheet);
                } catch (Throwable e) {
                    LOG.warn("SSN parse failed", e);

                }
            }
        }

        return mutatedSheets;
    }
}
