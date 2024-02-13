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
package de.uni_mannheim.swt.lasso.srm;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import joinery.DataFrame;
import org.junit.jupiter.api.Test;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.Table;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class JDBCTest {

    @Test
    public void test_JDBC() throws IOException {
        JDBC jdbc = new JDBC();

        Table table = jdbc.sqlToTable("SELECT * FROM INFORMATION_SCHEMA.TABLES");
        System.out.println(table.printAll());
    }

    @Test
    public void test_Pivot_Table() throws IOException {
        // db21d270-52fe-4a97-9251-4de76aa15efb

        JDBC jdbc = new JDBC();

        Table table = jdbc.sqlToTable("SELECT SHEETID, X, Y, SYSTEMID, VALUE FROM srm.cellvalue where executionid = '00b368ee-22cb-4618-8678-096ebec2f82f' and type = 'value'");
        System.out.println(table.printAll());
    }
//
//    @Test
//    public void bla() {
//        DataFrame<Object> df = new DataFrame<>("0", "1", "2");
//        df.append("0", Arrays.asList(10, 20, 30));
//        df.append("1", Arrays.asList(40, 50, 60));
//
//        System.out.println("test_db21d270-52fe-4a97-9251-4de76aa15efb".replaceAll("_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", ""));
//    }

//    @Test
//    public void test_DataFrame_statement_system() throws IOException {
//        // db21d270-52fe-4a97-9251-4de76aa15efb
//
//        JDBC jdbc = new JDBC();
//
//        DataFrame df = jdbc.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, SYSTEMID, VALUE FROM srm.cellvalue where executionid = 'db21d270-52fe-4a97-9251-4de76aa15efb' and type = 'value' order by sheetid");
//        System.out.println(df);
//
//        System.out.println("-----------");
//
//        DataFrame wide = df.pivot("STATEMENT", "SYSTEMID", "VALUE").drop(0);
//
//        System.out.println(wide);
//    }

    @Test
    public void lala() {
        System.out.println(new Gson().toJson(null));
    }

    @Test
    public void test_DataFrame_system_statement() throws IOException {
        // db21d270-52fe-4a97-9251-4de76aa15efb

        JDBC jdbc = new JDBC();

        DataFrame df = jdbc.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, SYSTEMID, VALUE FROM srm.cellvalue where executionid = '00b368ee-22cb-4618-8678-096ebec2f82f' and type = 'value' order by sheetid");
        System.out.println(df);

        System.out.println("-----------");

        DataFrame wide_system = df.pivot("SYSTEMID", "STATEMENT", "VALUE");

        System.out.println(wide_system);

        DataFrame wide_statement = df.pivot("STATEMENT", "SYSTEMID", "VALUE");

        System.out.println(wide_statement);

        List oracle = wide_statement.col("oracle");

        for(Object column : wide_statement.drop(0).columns()) {
            List alternative = wide_statement.col(column);

            double sim = similarity(oracle, alternative);

            System.out.println("oracle vs " + column + " => " + sim);
        }

        // alternative
        //System.out.println(wide_statement.collapse());


    }

    @Test
    public void test_SRM_sheet() throws IOException {
        JDBC jdbc = new JDBC();

        DataFrame df = jdbc.sqlToDataFrame("SELECT CONCAT(REGEXP_REPLACE(SHEETID, '_[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}',''),'@',X, ',', Y) as statement, SYSTEMID, VALUE FROM srm.cellvalue where executionid = '00b368ee-22cb-4618-8678-096ebec2f82f' and type = 'value' order by sheetid");
//        System.out.println(df);
//
//        System.out.println("-----------");
//
//        DataFrame wide_system = df.pivot("SYSTEMID", "STATEMENT", "VALUE");
//
//        System.out.println(wide_system);

        DataFrame wide_statement = df.pivot("STATEMENT", "SYSTEMID", "VALUE");

        System.out.println(wide_statement);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        try (JsonGenerator jg = objectMapper.getFactory().createGenerator(
                os, JsonEncoding.UTF8)) {
            writeDataFrameToJson(df, jg);
            jg.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(new String(os.toByteArray()));
    }

    /**
     * Jaccard'ish (Intersection over Union), but for ordered lists
     *
     * @param one
     * @param two
     * @return
     */
    private double similarity(List one, List two) {
        int total = one.size();
        int matches = 0;
        for(int i = 0; i < total; i++) {
            if(Objects.equals(one.get(i), two.get(i))) {
                matches++;
            }
        }

        return (double) matches / total;
    }

    @Test
    public void test_str() throws IOException {
        String str = "abc";
        System.out.println(str.getBytes(StandardCharsets.UTF_8).length);
    }

    private static void writeDataFrameToJson(final DataFrame df,
                                             final JsonGenerator jg)
            throws IOException {
        jg.writeStartArray();
        Iterator<List> it = df.iterator();
        while (it.hasNext()) {
            List row = it.next();
            jg.writeStartObject();
            for (int i = 0; i < row.size(); i++) {
                jg.writeObjectField((String) new ArrayList<>(df.columns()).get(i), row.get(i));
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }
}
