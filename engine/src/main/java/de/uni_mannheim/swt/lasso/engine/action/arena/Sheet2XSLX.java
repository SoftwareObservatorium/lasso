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
package de.uni_mannheim.swt.lasso.engine.action.arena;

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.lsl.spec.SheetSpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.util.CellAddress;
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
import java.util.Map;
import java.util.OptionalInt;

/**
 * Plain simple parser for textual representations of sheets.
 *
 * @author Marcus Kessel
 */
public class Sheet2XSLX {

    public static final String INSTANCE = "_INSTANCE_";

    private static final Logger LOG = LoggerFactory
            .getLogger(Sheet2XSLX.class);

    private static OptionalInt getRowLength(List<Object[]> rows) {
        return rows.stream().mapToInt(a -> a.length).max();
    }

    /**
     * Read SheetSpec to workbook with single sheet
     *
     * @param spec
     * @param name
     * @throws Exception
     * @return
     */
    public XSSFSheet createSheet(SheetSpec spec, String name) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFSheet sheet = workbook.createSheet(name);

        Map<String, ?> inputParameters = spec.getInputParameters();
        List<Object[]> rows = spec.getRows();

        // FIXME add blank cells
        OptionalInt rowLength = getRowLength(rows);

        for(int r = 0; r < rows.size(); r++) {
            XSSFRow myRow = sheet.createRow(r);

            Object[] row = rows.get(r);

            // operation
            String operation = (String) row[1];
            XSSFCell operationCell = myRow.createCell(1);
            operationCell.setCellValue(operation);

            boolean isCreate = StringUtils.equalsIgnoreCase(operation, "CREATE");

            // result
            Object result = row[0];
//            if(isCreate) {
//                result = INSTANCE; // sync oracle value for constructor calls
//            }

            XSSFCell resultCell = myRow.createCell(0);

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

            setCell(result, resultCell);

            if(row.length > 2) {
                // inputs

                for (int col = 2; col < row.length; col++) {
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

                    XSSFCell inputCell = myRow.createCell(col);

                    if(isCreate) {
                        // TODO do we need that?
                        // special case: no "wrap"
                        if(col == 2) {
                            inputCell.setCellValue((String) val);
                        } else {
                            setCell(val, inputCell);
                        }
                        //
                    } else {
                        setCell(val, inputCell);
                    }
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

    void setCell(Object val, XSSFCell cell) {
        if(val == null) {
            cell.setCellValue("null");
        } else if(val instanceof String) {
            String strValue = (String) val;

            if(!INSTANCE.equals(strValue)) {
                strValue = isCellId(strValue) ? strValue : StringUtils.wrap(strValue, '"');
            }

            cell.setCellValue(strValue);
        } else if(val instanceof Number || val instanceof Boolean) {
            cell.setCellValue(String.valueOf(val));
        } else {
            // array/map handling
            cell.setCellValue(new Gson().toJson(val));
        }
    }

    boolean isCellId(String val) {
        try {
            CellAddress ref = new CellAddress(val);
            return ref.getColumn() >= 0 && ref.getRow() >= 0;
        } catch (Throwable e) {
            return false;
        }
    }
}
