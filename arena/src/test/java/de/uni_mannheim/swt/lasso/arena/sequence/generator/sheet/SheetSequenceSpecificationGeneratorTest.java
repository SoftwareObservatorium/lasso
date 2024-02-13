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

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.*;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.JUnitSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
@Deprecated
public class SheetSequenceSpecificationGeneratorTest {

    String mavenRepoUrl = NexusInstance.LASSOHP12_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    @Test
    public void test_java() throws NoSuchMethodException {
        SequenceSpecification ss = new SequenceSpecification();
        ss.setName("customtest");

        ConstructorCallStatement constructorCallStatement = new ConstructorCallStatement(
                new ReflectionConstructorSignature(String.class.getConstructor(String.class)));
        ss.addStatement(constructorCallStatement, ss.getNextPosition());

        ValueStatement valueStatement = new ValueStatement(String.class, "hi");
        constructorCallStatement.addInput(valueStatement);

        MethodCallStatement methodCallStatement = new MethodCallStatement(
                new ReflectionMethodSignature(String.class.getMethod("length")));
        ss.addStatement(methodCallStatement, ss.getNextPosition());

        // first input always subject
        methodCallStatement.addInput(constructorCallStatement); // subject

        ss.addStatement(new ValueStatement(int.class, 5), ss.getNextPosition()); // omitted by randoop (since not used)
        ss.addStatement(new ValueStatement(int[].class, new int[]{1,2}), ss.getNextPosition());
        ss.addStatement(new ValueStatement(int[].class, new int[5]), ss.getNextPosition());
        ss.addStatement(new ValueStatement(int[][].class, new int[][]{{1}, {2}}), ss.getNextPosition());

        ValueStatement myArr = new ValueStatement(String[].class, new String[]{"hello", "world"});
        ss.addStatement(myArr, ss.getNextPosition());

        ValueStatement arrElement = new ValueStatement(String.class, "marcus");
        ss.addStatement(arrElement, ss.getNextPosition());

        ArraySetStatement arraySetStatement = new ArraySetStatement(1);
        ss.addStatement(arraySetStatement, ss.getNextPosition());
        // add array
        arraySetStatement.addInput(myArr);
        arraySetStatement.addInput(arrElement);

        SheetSequenceSpecificationGenerator generator = new SheetSequenceSpecificationGenerator();

        Sheet sheet = generator.generate(ss);
        printSheet(sheet);
    }

    @Test
    public void testGenerate_ArrayStack() throws IOException {
        String mql = "Stack(" +
                "push(java.lang.Object):java.lang.Object;" +
                "pop():java.lang.Object;" +
                "peek():java.lang.Object;" +
                "size():int;" +
                ")";

        CodeSearch codeSearch = new CodeSearch();

        ClassUnderTest classUnderTest = codeSearch.queryForClass("6ce338e3-3c3c-4f52-b595-9b3ed5bb4025");

        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/ArrayStack_0_Test.java"), StandardCharsets.UTF_8);

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

        InterfaceSpecification specification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());

        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, specification, classUnderTest,classUnderTest.getClassName(), "");

        SheetSequenceSpecificationGenerator generator = new SheetSequenceSpecificationGenerator();

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            SequenceSpecification sequenceSpecification = ssMap.get(name);

            Sheet sheet = generator.generate(sequenceSpecification);
            printSheet(sheet);
        }

        // generate workbook

        XSSFWorkbook workbook = generator.generateWorkbook(new ArrayList<>(ssMap.values()));
        workbook.write(FileUtils.openOutputStream(new File("/tmp/arraystack.xlsx")));
    }

    @Test
    public void test_toSequenceSpecification_Matrix() throws IOException {
        CodeSearch codeSearch = new CodeSearch(SolrInstance.mavenCentral2017());
        ClassUnderTest classUnderTest = codeSearch.queryForClass("379e30b8-cb5f-45c4-93b9-d592450d2743");

        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/MatrixTest_array.java"), StandardCharsets.UTF_8);

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

        InterfaceSpecification specification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());

        Map<String, SequenceSpecification> ssMap = importJUnitClass.toSequenceSpecifications(testClass, specification, classUnderTest,classUnderTest.getClassName(), "");

        SheetSequenceSpecificationGenerator generator = new SheetSequenceSpecificationGenerator();

        for(String name : ssMap.keySet()) {
            System.out.println("SS " + name);
            System.out.println("SS " + ssMap.get(name).toString());

            SequenceSpecification sequenceSpecification = ssMap.get(name);

            Sheet sheet = generator.generate(sequenceSpecification);
            printSheet(sheet);
        }
    }

    public static void printSheet(Sheet sheet){
        Row row;

        for(Iterator<Row> rowIterator = sheet.rowIterator(); rowIterator.hasNext();){
            row = rowIterator.next();

            for(Iterator<Cell> cellIterator = row.cellIterator(); cellIterator.hasNext();){
                Cell cell = cellIterator.next();

                switch (cell.getCellType()) {
                    case STRING:
                        System.out.printf("%-30s", cell.getStringCellValue());
                        break;
                    case NUMERIC:
                        if(DateUtil.isCellDateFormatted(cell))
                            System.out.printf("%-30s", cell.getDateCellValue().toString());
                        System.out.printf("%-30f", cell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                        System.out.printf("%-30b", cell.getBooleanCellValue());
                        break;
                    case FORMULA:
                        System.out.printf("%-30s", cell.getCellFormula());
                        break;
                    case ERROR:
                        System.out.printf("%-30x", cell.getErrorCellValue());
                        break;
                    default:
                        System.err.print("[ERROR]: Unidentified Cell Value: " + cell.getCellType());
                }
            }

            System.out.println();
        }
    }
}
