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

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;

/**
 *
 *
 * @author Marcus Kessel
 */
@Deprecated
public class CSV2XSLXTest {

    @Test
    public void test_string() throws Exception {
        String contents =
                "|CREATE|Base64|\n" +
                "|VALUE|\"Hello World!\"\n" +
                "|encode|A1|A2";

        System.out.println(contents);

        Sheet sheet = CSV2XSLX.parseSheet("mysheet", contents);

        sheet.getWorkbook().write(new FileOutputStream("/tmp/sheet_" + System.currentTimeMillis() + ".xlsx"));
    }

    @Test
    public void test_array() throws Exception {
        String contents =
                        "|CREATE|Sort|\n" +
                        "|VALUE|[10,1,5,7,4]\n" +
                        "|sort|A1|A2";

        System.out.println(contents);

        Sheet sheet = CSV2XSLX.parseSheet("mysheet", contents);

        sheet.getWorkbook().write(new FileOutputStream("/tmp/sheet_" + System.currentTimeMillis() + ".xlsx"));
    }

    @Test
    public void test_number() throws Exception {
        String contents =
                        "|CREATE|Search||\n" +
                        "|VALUE|[1,4,5,7,10]|\n" +
                        "|VALUE|5|\n" +
                        "|search|A1|A2|A3";

        System.out.println(contents);

        Sheet sheet = CSV2XSLX.parseSheet("mysheet", contents);

        sheet.getWorkbook().write(new FileOutputStream("/tmp/sheet_" + System.currentTimeMillis() + ".xlsx"));
    }

    @Test
    public void test_Stack() throws Exception {
        String contents =
                "|CREATE|Stack||\n" +
                        "|push|A1|\"hi!\"|\n" +
                        "|size|A1||\n" +
                        "|peek|A1||\n" +
                        "|pop|A1||\n" +
                        "|size|A1||";

        System.out.println(contents);

        Sheet sheet = CSV2XSLX.parseSheet("mysheet", contents);

        sheet.getWorkbook().write(new FileOutputStream("/tmp/sheet_" + System.currentTimeMillis() + ".xlsx"));
    }
}
