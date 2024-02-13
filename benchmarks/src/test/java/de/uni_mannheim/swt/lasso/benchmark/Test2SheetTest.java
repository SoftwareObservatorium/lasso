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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.OutputStream;
import java.util.Iterator;

/**
 *
 * @author Marcus Kessel
 */
public class Test2SheetTest {

    @Test
    public void test_HumanEval_23_strlen() throws Exception {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_23_strlen");

        File dir = new File("/tmp/sheets_" + System.currentTimeMillis());
        File sheetOut = new File(dir, ab.getId());
        sheetOut.mkdirs();

        int s = 0;
        for(Sequence sequence : ab.getSequences()) {
//            for(Statement stmt : sequence.getStatements()) {
//                System.out.println(stmt.getInputs().get(0).getValue());
//            }

            Test2XSLX test2XSLX = new Test2XSLX();
            XSSFSheet sheet = test2XSLX.createSheet(sequence, sequence.getId() + "_" + s);

            printSheetAsString(sheet);

            System.out.println("------------");

            try(OutputStream out = FileUtils.openOutputStream(new File(sheetOut, "306f1800-5e2f-4286-9273-d68a0d53ee3d/src/test/java/" + String.format("%s.xlsx", s)))) {
                sheet.getWorkbook().write(out);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            s++;
        }

        //Sequence sequence = ab.getSequences().get(0);
    }

    // copied from here: https://stackoverflow.com/a/61462780
    public void printSheetAsString(XSSFSheet sheet){
        //a row iterator which iterates through rows in a specified sheet
        for(Iterator<Row> rowIterator = sheet.rowIterator(); rowIterator.hasNext();){
            Row row = rowIterator.next(); //store the next element into the variable "row"

            //a cell iterator which iterates through cells in the sheet's rows
            for(Iterator<Cell> cellIterator = row.cellIterator(); cellIterator.hasNext();){
                Cell               cell = cellIterator.next(); //store the next element into the variable "cell"
                //cell.setCellType(CellType.STRING); //set all the cells to type string so that we can print them out easily

                //print "%" for place holder/variable, "-" for left justified table, "30" for spaces between each column, "s" for string
                //this will print the cell then move to the next cell, print that cell, so on, till we reach the next row, which repeats the process, them move to next row, and so on till rowIterator.hasNext() returns false
                System.out.printf("%-30s", cell);
            }

            System.out.println();
        }
    }

    @Test
    public void test_HumanEval_1_separate_paren_groups() throws Exception {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_1_separate_paren_groups");

        File dir = new File("/tmp/sheets_" + System.currentTimeMillis());
        File sheetOut = new File(dir, ab.getId());
        sheetOut.mkdirs();

        int s = 0;
        for(Sequence sequence : ab.getSequences()) {
//            for(Statement stmt : sequence.getStatements()) {
//                System.out.println(stmt.getInputs().get(0).getValue());
//            }

            Test2XSLX test2XSLX = new Test2XSLX();
            XSSFSheet sheet = test2XSLX.createSheet(sequence, sequence.getId() + "_" + s);

            printSheetAsString(sheet);

            System.out.println("------------");

            try(OutputStream out = FileUtils.openOutputStream(new File(sheetOut, "5704e5b9-5209-4bc5-8bbe-f2bc35461545/src/test/java/" + String.format("%s.xlsx", s)))) {
                sheet.getWorkbook().write(out);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            s++;
        }

        //Sequence sequence = ab.getSequences().get(0);
    }

    @Test
    public void test_HumanEval_3_below_zero() throws Exception {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_3_below_zero");

        File dir = new File("/tmp/sheets_" + System.currentTimeMillis());
        File sheetOut = new File(dir, ab.getId());
        sheetOut.mkdirs();

        int s = 0;
        for(Sequence sequence : ab.getSequences()) {
//            for(Statement stmt : sequence.getStatements()) {
//                System.out.println(stmt.getInputs().get(0).getValue());
//            }

            Test2XSLX test2XSLX = new Test2XSLX();
            XSSFSheet sheet = test2XSLX.createSheet(sequence, sequence.getId() + "_" + s);

            printSheetAsString(sheet);

            System.out.println("------------");

            try(OutputStream out = FileUtils.openOutputStream(new File(sheetOut, "908a2ac9-e4bc-4279-aa38-63498168cbb7/src/test/java/" + String.format("%s.xlsx", s)))) {
                sheet.getWorkbook().write(out);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            s++;
        }

        //Sequence sequence = ab.getSequences().get(0);
    }

    @Test
    public void test_HumanEval_95_check_dict_case() throws Exception {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_95_check_dict_case");

        File dir = new File("/tmp/sheets_" + System.currentTimeMillis());
        File sheetOut = new File(dir, ab.getId());
        sheetOut.mkdirs();

        int s = 0;
        for(Sequence sequence : ab.getSequences()) {
//            for(Statement stmt : sequence.getStatements()) {
//                System.out.println(stmt.getInputs().get(0).getValue());
//            }

            Test2XSLX test2XSLX = new Test2XSLX();
            XSSFSheet sheet = test2XSLX.createSheet(sequence, sequence.getId() + "_" + s);

            printSheetAsString(sheet);

            System.out.println("------------");

            try(OutputStream out = FileUtils.openOutputStream(new File(sheetOut, "e4489b04-a124-449c-85f3-e68aa7571f23/src/test/java/" + String.format("%s.xlsx", s)))) {
                sheet.getWorkbook().write(out);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            s++;
        }

        //Sequence sequence = ab.getSequences().get(0);
    }

    //
    @Test
    public void test_HumanEval_107_even_odd_palindrome() throws Exception {
        ClasspathBenchmarkLoader loader = new ClasspathBenchmarkLoader();
        Benchmark benchmark = loader.load("humaneval-java-reworded");

        FunctionalAbstraction ab = benchmark.getAbstractions().get("HumanEval_107_even_odd_palindrome");

        File dir = new File("/tmp/sheets_" + System.currentTimeMillis());
        File sheetOut = new File(dir, ab.getId());
        sheetOut.mkdirs();

        int s = 0;
        for(Sequence sequence : ab.getSequences()) {
//            for(Statement stmt : sequence.getStatements()) {
//                System.out.println(stmt.getInputs().get(0).getValue());
//            }

            Test2XSLX test2XSLX = new Test2XSLX();
            XSSFSheet sheet = test2XSLX.createSheet(sequence, sequence.getId() + "_" + s);

            printSheetAsString(sheet);

            System.out.println("------------");

            try(OutputStream out = FileUtils.openOutputStream(new File(sheetOut, "0257984d-0b0b-47b7-8393-4a2ac998ba41/src/test/java/" + String.format("%s.xlsx", s)))) {
                sheet.getWorkbook().write(out);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            s++;
        }

        //Sequence sequence = ab.getSequences().get(0);
    }
}
