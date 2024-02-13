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
package de.uni_mannheim.swt.lasso.benchmark;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.OptionalInt;

import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.core.model.System;

/**
 * Plain simple parser for textual representations of sheets.
 *
 * @author Marcus Kessel
 */
public class Test2XSLX {

    public static final String INSTANCE = "_INSTANCE_";

    private static final Logger LOG = LoggerFactory
            .getLogger(Test2XSLX.class);

    private static OptionalInt getRowLength(List<Statement> statements) {
        return statements.stream().mapToInt(s -> s.getExpectedOutputs().size() + 1 + s.getInputs().size()).max();
    }

    /**
     * Read Sequence to workbook with single sheet
     *
     * @param sequence
     * @param name
     * @throws Exception
     * @return
     */
    public XSSFSheet createSheet(Sequence sequence, String name) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFSheet sheet = workbook.createSheet(name);

        List<Statement> statements = sequence.getStatements();

//        // FIXME add blank cells
//        OptionalInt rowLength = getRowLength(statements);

        // first do create statement
        XSSFRow first = sheet.createRow(0);
        first.createCell(1).setCellValue("CREATE");
        first.createCell(2).setCellValue("Problem"); // FIXME

        for(int r = 0; r < statements.size(); r++) {
            XSSFRow myRow = sheet.createRow(r + 1);

            Statement row = statements.get(r);

            // operation
            String operation = row.getOperation();
            XSSFCell operationCell = myRow.createCell(1);
            operationCell.setCellValue(operation);

            //boolean isCreate = StringUtils.equalsIgnoreCase(operation, "CREATE");

            // result
            Value oracle = row.getExpectedOutputs().get(0); // FIXME single output

            XSSFCell resultCell = myRow.createCell(0);
            resultCell.setCellValue(oracle.getCode());

            if(row.getInputs().size() > 0) {
                // inputs

                // receiver instance
                myRow.createCell(2).setCellValue("A1");

                for (int col = 3; col < row.getInputs().size() + 3; col++) {
                    Value value = row.getInputs().get(col - 3);

                    XSSFCell inputCell = myRow.createCell(col);
                    // sets pre-existing JSON
                    inputCell.setCellValue(value.getCode());
                }
            }
        }

        return sheet;
    }

    public void write(System executable, XSSFSheet sheet, String name) throws IOException {
        MavenProject project = executable.getProject();
        File srcTest = project.getSrcTest();
        String pkg = StringUtils.replace(executable.getCode().getPackagename(), ".", "/");
        File target = new File(srcTest, pkg);
        target.mkdirs();

        try(OutputStream out = FileUtils.openOutputStream(new File(target, String.format("%s.xlsx", name)))) {
            sheet.getWorkbook().write(out);
        }
    }

//    void setCell(Object val, XSSFCell cell) {
//        if(val == null) {
//            cell.setCellValue("null");
//        } else if(val instanceof String) {
//            String strValue = (String) val;
//            strValue = isCellId(strValue) ? strValue : StringUtils.wrap(strValue, '"');
//            cell.setCellValue(strValue);
//        } else if(val instanceof Number || val instanceof Boolean) {
//            cell.setCellValue(String.valueOf(val));
//        } else if(val.getClass().isArray()) {
//            // array handling
//            cell.setCellValue(new Gson().toJson(val));
//        } else {
//            cell.setCellValue(val.toString());
//        }
//    }
}
