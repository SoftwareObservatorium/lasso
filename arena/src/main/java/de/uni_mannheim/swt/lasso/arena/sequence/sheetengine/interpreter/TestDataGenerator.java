package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.ssn.ParsedCell;
import de.uni_mannheim.swt.lasso.ssn.ParsedRow;
import de.uni_mannheim.swt.lasso.ssn.ParsedSheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;

import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class TestDataGenerator {

    SSNInterpreter interpreter = new SSNInterpreter();
    ObjectMapper objectMapper = new ObjectMapper();

    public interface Generator {
        Object generateData(ParsedCell parsedCell, Parameter parameter);
    }

    public List<de.uni_mannheim.swt.lasso.core.dto.srm.Sheet> generateData(StimulusResponseMatrix<Test, ClassUnderTest, TestInvocation> stimulusMatrix, Generator testDataGenerator) throws JsonProcessingException {
        List<de.uni_mannheim.swt.lasso.core.dto.srm.Sheet> generatedSheets = new LinkedList<>();

        // now test gen
        for (de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test test : stimulusMatrix.getRows()) {
            // prepare executable sheet
            ParsedSheet parsedSheet = test.getParsedSheet();

            // pick random one
            ClassUnderTest classUnderTest = stimulusMatrix.getColumns().iterator().next();

            // test invocation
            TestInvocation testInvocation = stimulusMatrix.get(test, classUnderTest);

            // prepare invocations

            Invocations invocations = interpreter.interpret(test, classUnderTest, testInvocation);

            InterfaceSpecification interfaceSpecification = test.getInterfaceSpecification();

            List<Map<String, Object>> rows = new ArrayList<>();

            for (Invocation invocation : invocations.getSequence()) {
                Map<String, Object> rowData = new LinkedHashMap<>();

                ParsedRow parsedRow = invocations.getParsedSheet().getRows().get(invocation.getIndex());

                rowData.put(parsedRow.getOutput().getKey(), parsedRow.getOutput().getNodeValue());
                rowData.put(parsedRow.getOperation().getKey(), parsedRow.getOperation().getNodeValue());
                rowData.put(parsedRow.getCells().get(2).getKey(), parsedRow.getCells().get(2).getNodeValue());

                List<Parameter> parameterList = invocation.getParameters();

                int c = 3;
                for (Parameter parameter : parameterList) {
                    ParsedCell parsedCell = parsedRow.getCells().get(c);

                    if (parsedCell.isTestParameter() || parsedCell.isValueReference()) {
                        rowData.put(parsedCell.getKey(), parsedCell.getNodeValue());
                        continue;
                    }

                    Object object = parameter.getValue();
                    if (object == null) {
                        rowData.put(parsedCell.getKey(), parsedCell.getNodeValue());
                    } else {
                        Object obj = testDataGenerator.generateData(parsedCell, parameter);

                        System.out.println(obj);

                        // FIXME to correct java expression (e.g., "Cmg5".getBytes()) -- use parser?
                        rowData.put(parsedCell.getKey(), objectMapper.writeValueAsString(obj));
                    }

                    c++;
                }

                rows.add(rowData);
            }

            StringBuilder jsonl = new StringBuilder();
            for (Map<String, Object> row : rows) {
                Map<String, Map<String, Object>> m = new LinkedHashMap<>();
                m.put("cells", row);
                String json = objectMapper.writeValueAsString(m);

                jsonl.append(json);
                jsonl.append("\n");
            }

            de.uni_mannheim.swt.lasso.core.dto.srm.Sheet generatedSheet = new Sheet("test" + System.currentTimeMillis() + "()", jsonl.toString(), interfaceSpecification.toLQL());
            generatedSheet.setInvocations(Arrays.asList(testInvocation.getInvocationExpression()));
            generatedSheets.add(generatedSheet);
        }

        return generatedSheets;
    }
}
