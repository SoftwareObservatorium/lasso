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

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Wrapper around spread sheets.
 *
 * @author Marcus Kessel
 */
public class SpreadSheet {

    private static final Logger LOG = LoggerFactory
            .getLogger(SpreadSheet.class);

    private final List<Sheet> sheets;

    public SpreadSheet(Sheet sheet) throws IOException {
        this.sheets = Arrays.asList(sheet);
    }

    public SpreadSheet(InputStream inp) throws IOException {
        Workbook wb = WorkbookFactory.create(inp);

        this.sheets = IntStream.range(0, wb.getNumberOfSheets()).mapToObj(i -> wb.getSheetAt(i)).collect(Collectors.toList());
    }

    public SpreadSheet(File file) throws IOException {
        try (InputStream inp = new FileInputStream(file)) {
            Workbook wb = WorkbookFactory.create(inp);

            this.sheets = IntStream.range(0, wb.getNumberOfSheets()).mapToObj(i -> wb.getSheetAt(i)).collect(Collectors.toList());
        }
    }

    public List<Sheet> getSheets() {
        return sheets;
    }

    public Sheet getFirstSheet() {
        return sheets.get(0);
    }

    public void close() throws IOException {
        if(getFirstSheet() != null) {
            getFirstSheet().getWorkbook().close();
        }
    }
}
