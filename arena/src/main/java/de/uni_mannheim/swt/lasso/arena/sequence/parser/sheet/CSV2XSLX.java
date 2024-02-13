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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain simple parser for textual representations of sheets.
 *
 * @author Marcus Kessel
 */
@Deprecated
public class CSV2XSLX {

    private static final Logger LOG = LoggerFactory
            .getLogger(CSV2XSLX.class);

    /**
     * Read CSV to workbook with single sheet
     *
     * @param name
     * @param csvContents
     * @throws Exception
     * @return
     */
    public static Sheet parseSheet(String name, String csvContents) throws Exception {
        CSVParser csvParser = CSVParser.parse(csvContents, CSVFormat.DEFAULT.withIgnoreEmptyLines().withQuote(null).withDelimiter('|').withTrim());
        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFSheet sheet = workbook.createSheet(name);

        for(CSVRecord csvRecord : csvParser) {
            XSSFRow myRow = sheet.createRow((int) csvRecord.getRecordNumber() - 1);

            for (int col = 0; col < csvRecord.size(); col++) {
                String val = csvRecord.get(col);

                if(StringUtils.isBlank(val)) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("blank " + csvRecord.getRecordNumber() + " / " + col);
                    }

                    continue;
                }

                XSSFCell cell = myRow.createCell(col);
                cell.setCellValue(val);

//                if(StringUtils.startsWith(val, "'") && StringUtils.endsWith(val, "'")) {
//                    cell.setCellValue(val.substring(1, val.length() - 1));
//                } else {
//                    cell.setCellValue(new BigInteger(csvRecord.get(col)).doubleValue());
//                    //cell.setCellValue(val);
//                }

                if(LOG.isDebugEnabled()) {
                    //LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.ge);

                    switch (cell.getCellType()) {
                        case _NONE:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), "none");
                            break;
                        case NUMERIC:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getNumericCellValue());
                            break;
                        case STRING:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getStringCellValue());
                            break;
                        case FORMULA:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getCellFormula());
                            break;
                        case BLANK:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), "blank");
                            break;
                        case BOOLEAN:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getBooleanCellValue());
                            break;
                        case ERROR:
                            LOG.debug("cell type / value: '{}' / '{}' ", cell.getCellType(), cell.getErrorCellString());
                            break;
                        default:

                    }
                }
            }
        }

        return sheet;
    }
}
