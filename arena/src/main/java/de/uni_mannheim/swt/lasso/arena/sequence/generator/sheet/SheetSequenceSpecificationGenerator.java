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
package de.uni_mannheim.swt.lasso.arena.sequence.generator.sheet;

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An old SSN-based generator to produce spreadsheets (Excel format).
 *
 * @author Marcus Kessel
 */
@Deprecated
public class SheetSequenceSpecificationGenerator {

    private static final Logger LOG = LoggerFactory
            .getLogger(SheetSequenceSpecificationGenerator.class);

    public XSSFWorkbook generateWorkbook(List<SequenceSpecification> sequenceSpecificationList) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        for(SequenceSpecification ss : sequenceSpecificationList) {
            generate(ss, workbook);
        }

        return workbook;
    }

    public XSSFSheet generate(SequenceSpecification sequenceSpecification) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        return generate(sequenceSpecification, workbook);
    }

    public XSSFSheet generate(SequenceSpecification sequenceSpecification, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet(sequenceSpecification.getName());

        Map<SpecificationStatement, Integer> positions = new LinkedHashMap<>();

//        XSSFRow header = sheet.createRow(0);
//        header.createCell(0).setCellValue("OUTPUT");
//        header.createCell(1).setCellValue("OPERATION");

        int rows = 0;
        for(SpecificationStatement statement : sequenceSpecification.getStatements()) {
//            if(statement.getPosition() < 0) {
//                // ignore
//                continue;
//            }

            XSSFRow row = sheet.createRow(rows++);
            Cell output = row.createCell(0);
            output.setCellValue("");
            Cell operation = row.createCell(1);

            int param = 2;
            if (statement.getClass().equals(ConstructorCallStatement.class)) {
                ConstructorCallStatement constructorCallStatement = (ConstructorCallStatement) statement;

                operation.setCellValue("CREATE");

                Cell clazz = row.createCell(param++);
                if (!constructorCallStatement.isClassUnderTest()) { // doesn't require adapting
                    Constructor<?> constructor = constructorCallStatement.getResolvedConstructor();

                    clazz.setCellValue(String.format("^%s", constructor.getName()));
                } else {
                    // adapt
                    MethodSignature methodSignature = constructorCallStatement.getMethodSignature();

                    clazz.setCellValue(String.format("%s", methodSignature.getClassName()));
                }

                // FIXME inputs
                param = createInputs(row, param, statement, positions);

                positions.put(statement, row.getRowNum());
            }

            if (statement.getClass().equals(MethodCallStatement.class)) {
                MethodCallStatement methodCallStatement = (MethodCallStatement) statement;

                if (!methodCallStatement.isClassUnderTest()) {
                    Method method = methodCallStatement.getResolvedMethod();

                    operation.setCellValue(String.format("^%s.%s", method.getDeclaringClass().getName(), method.getName()));

                } else {
                    // adapt
                    MethodSignature methodSignature = methodCallStatement.getMethodSignature();

                    operation.setCellValue(methodSignature.getName());
                }

                // FIXME inputs
                param = createInputs(row, param, statement, positions);

                positions.put(statement, row.getRowNum());
            }

            // field operation
            if (statement.getClass().equals(ValueStatement.class)) {
                ValueStatement value = (ValueStatement) statement;

                operation.setCellValue("VALUE");

                Cell in = row.createCell(param++);

                if(value.isAlias()) {
                    SpecificationStatement var = value.getInputs().get(0);
                    int rowNum = positions.get(var);
                    String ref = row.getSheet().getRow(rowNum).getCell(0).getReference(); // link to output
                    in.setCellValue(ref);
                } else {
                    in.setCellValue(format(value));
                }

                positions.put(statement, row.getRowNum());
            }

            // array element set
            if (statement.getClass().equals(ArraySetStatement.class)) {
                ArraySetStatement arraySetStatement = (ArraySetStatement) statement;

                operation.setCellValue("ARRAYSET");

                // FIXME inputs
                param = createInputs(row, param, statement, positions);

                positions.put(statement, row.getRowNum());
            }
        }

        return sheet;
    }

    private int createInputs(XSSFRow row, int start, SpecificationStatement statement, Map<SpecificationStatement, Integer> positions) {
        if(CollectionUtils.isEmpty(statement.getInputs())) {
            return start;
        }

        for(SpecificationStatement input : statement.getInputs()) {
            Cell in = row.createCell(start++);

            if(positions.containsKey(input)) {
                // resolve reference
                int rowNum = positions.get(input);
                String ref = row.getSheet().getRow(rowNum).getCell(0).getReference(); // link to output
                in.setCellValue(ref);
            } else {
                if(input instanceof ValueStatement) {
                    ValueStatement value = (ValueStatement) input;
                    in.setCellValue(format(value));
                }
            }
        }

        return start;
    }

    private String format(ValueStatement valueStatement) {
        if(valueStatement.isArray()) {
            return new Gson().toJson(valueStatement.getValue());
        }

        //return String.valueOf(valueStatement.getValue());
        return new Gson().toJson(valueStatement.getValue());
    }
}
